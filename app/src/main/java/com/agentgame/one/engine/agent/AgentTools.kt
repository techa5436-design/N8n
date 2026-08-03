package com.agentgame.one.engine.agent

import com.agentgame.one.engine.agent.AgentPermissionManager.Permission
import com.agentgame.one.engine.scripting.attachScript
import java.io.File

/** Registry of tools the agent can use, keyed by name. */
object AgentTools {
    private val registry = mutableMapOf<String, AgentTool>()

    fun register(tool: AgentTool) { registry[tool.name] = tool }
    fun get(name: String): AgentTool? = registry[name]
    fun all(): List<AgentTool> = registry.values.toList()
    fun names(): List<String> = registry.keys.toList()

    init {
        register(ListFilesTool)
        register(ReadFileTool)
        register(WriteFileTool)
        register(EditFileTool)
        register(SearchFilesTool)
        register(AddToContextTool)
        register(RunCommandTool)
    }
}

/** `list_files` — lists the workspace tree (READ). */
object ListFilesTool : AgentTool {
    override val name = "list_files"
    override val description = "List all files in the workspace."
    override val permission = Permission.READ
    override fun execute(ctx: AgentContext, args: Map<String, Any?>): ToolResult {
        val files = ctx.workspace.listFiles()
        val sb = StringBuilder("Workspace (${files.size} files):\n")
        for (f in files) sb.append("  - $f\n")
        return ToolResult(true, sb.toString())
    }
}

/** `read_file` — reads a file into context (READ). */
object ReadFileTool : AgentTool {
    override val name = "read_file"
    override val description = "Read the contents of a file (relative path)."
    override val permission = Permission.READ
    override fun execute(ctx: AgentContext, args: Map<String, Any?>): ToolResult {
        val path = args["path"] as? String ?: return ToolResult(false, "Missing 'path'")
        val resolved = ctx.permissions.resolve(path)
            ?: return ToolResult(false, "Path '$path' is outside the workspace or blocked")
        if (!resolved.exists()) return ToolResult(false, "File not found: $path")
        val content = resolved.readText()
        ctx.addFileToContext(path, content)
        return ToolResult(true, content, changedFiles = listOf(path))
    }
}

/** `write_file` — creates/overwrites a file (EDIT). */
object WriteFileTool : AgentTool {
    override val name = "write_file"
    override val description = "Create or overwrite a file with the given content."
    override val permission = Permission.EDIT
    override fun execute(ctx: AgentContext, args: Map<String, Any?>): ToolResult {
        val path = args["path"] as? String ?: return ToolResult(false, "Missing 'path'")
        val content = args["content"] as? String ?: ""
        val resolved = ctx.permissions.resolve(path)
            ?: return ToolResult(false, "Path '$path' is outside the workspace or blocked")
        val old = if (resolved.exists()) resolved.readText() else null
        resolved.parentFile?.mkdirs()
        resolved.writeText(content)
        val diff = diff(path, old, content)
        return ToolResult(true, "Wrote $path (${content.length} bytes)", diff = diff, changedFiles = listOf(path))
    }
}

/** `edit_file` — applies a find/replace edit to a file (EDIT). */
object EditFileTool : AgentTool {
    override val name = "edit_file"
    override val description = "Replace occurrences of `search` with `replace` in a file."
    override val permission = Permission.EDIT
    override fun execute(ctx: AgentContext, args: Map<String, Any?>): ToolResult {
        val path = args["path"] as? String ?: return ToolResult(false, "Missing 'path'")
        val search = args["search"] as? String ?: return ToolResult(false, "Missing 'search'")
        val replace = args["replace"] as? String ?: ""
        val resolved = ctx.permissions.resolve(path)
            ?: return ToolResult(false, "Path '$path' is outside the workspace or blocked")
        if (!resolved.exists()) return ToolResult(false, "File not found: $path")
        val old = resolved.readText()
        if (!old.contains(search)) return ToolResult(false, "Search text not found in $path")
        val updated = old.replace(search, replace)
        resolved.writeText(updated)
        val diff = diff(path, old, updated)
        return ToolResult(true, "Edited $path (${diff.count { it == '+' } - 1} additions)", diff = diff, changedFiles = listOf(path))
    }
}

/** `search_files` — regex search across workspace files (READ). */
object SearchFilesTool : AgentTool {
    override val name = "search_files"
    override val description = "Regex search for text across all workspace files."
    override val permission = Permission.READ
    override fun execute(ctx: AgentContext, args: Map<String, Any?>): ToolResult {
        val pattern = args["query"] as? String ?: return ToolResult(false, "Missing 'query'")
        val regex = try { Regex(pattern) } catch (e: Exception) { return ToolResult(false, "Bad regex: ${e.message}") }
        val sb = StringBuilder()
        var count = 0
        for (path in ctx.workspace.listFiles()) {
            val content = ctx.workspace.readFile(path) ?: continue
            content.lineSequence().forEachIndexed { idx, line ->
                if (regex.containsMatchIn(line)) {
                    sb.append("$path:${idx + 1}: $line\n")
                    count++
                }
            }
        }
        return ToolResult(true, if (count == 0) "No matches for '$pattern'" else sb.toString())
    }
}

/** `add_to_context` — pulls a file into the model's context (READ). */
object AddToContextTool : AgentTool {
    override val name = "add_to_context"
    override val description = "Add a file's contents to the model context."
    override val permission = Permission.READ
    override fun execute(ctx: AgentContext, args: Map<String, Any?>): ToolResult {
        val path = args["path"] as? String ?: return ToolResult(false, "Missing 'path'")
        val content = ctx.workspace.readFile(path) ?: return ToolResult(false, "File not found: $path")
        ctx.addFileToContext(path, content)
        return ToolResult(true, "Added $path to context (${content.length} bytes)")
    }
}

/** `run_command` — runs a command in the workspace (EXEC). */
object RunCommandTool : AgentTool {
    override val name = "run_command"
    override val description = "Run a command (ls, cat, echo, pwd, run <script>)."
    override val permission = Permission.EXEC
    override fun execute(ctx: AgentContext, args: Map<String, Any?>): ToolResult {
        val cmd = args["command"] as? String ?: return ToolResult(false, "Missing 'command'")
        return CommandExecutor.execute(ctx, cmd)
    }
}

/**
 * Executes commands in the workspace. Supports built-ins (ls/cat/echo/pwd/run/tree) directly,
 * and falls back to the real shell for anything else, capturing stdout/stderr.
 */
object CommandExecutor {

    fun execute(ctx: AgentContext, command: String): ToolResult {
        val parts = command.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return ToolResult(false, "Empty command")
        val root = ctx.workspace.root
        return when (parts[0]) {
            "ls" -> {
                val files = ctx.workspace.listFiles()
                ToolResult(true, files.joinToString("\n") { "- $it" })
            }
            "pwd" -> ToolResult(true, root.absolutePath)
            "echo" -> ToolResult(true, parts.drop(1).joinToString(" "))
            "cat" -> {
                val f = ctx.permissions.resolve(parts.getOrNull(1) ?: "")
                    ?: return ToolResult(false, "Invalid path")
                if (!f.exists()) ToolResult(false, "File not found") else ToolResult(true, f.readText())
            }
            "tree" -> {
                val sb = StringBuilder()
                fun walk(file: File, indent: String) {
                    sb.append(indent).append(file.name).append(if (file.isDirectory) "/" else "").append("\n")
                    file.listFiles()?.sortedBy { it.name }?.forEach { walk(it, "$indent  ") }
                }
                walk(root, "")
                ToolResult(true, sb.toString())
            }
            "run" -> {
                val script = parts.getOrNull(1) ?: return ToolResult(false, "Usage: run <file>")
                runMDScript(ctx, script)
            }
            else -> shell(command, root)
        }
    }

    private fun runMDScript(ctx: AgentContext, scriptPath: String): ToolResult {
        val content = ctx.workspace.readFile(scriptPath)
            ?: return ToolResult(false, "Script not found: $scriptPath")
        // Compile & run against a throwaway node using the engine interpreter.
        return try {
            val node = com.agentgame.one.engine.core.Node("__cmd__")
            val result = StringBuilder()
            val originalLog = com.agentgame.one.engine.scripting.MDScriptLog.hook
            com.agentgame.one.engine.scripting.MDScriptLog.hook = { s -> result.appendLine(s) }
            try {
                node.attachScript(content)
                // scripts relying on callbacks won't run here; execute print() calls via main
                com.agentgame.one.engine.scripting.MDScriptDebug.runMain(node, content)
            } finally {
                com.agentgame.one.engine.scripting.MDScriptLog.hook = originalLog
            }
            val output = result.toString().ifEmpty { "Script executed successfully." }
            ToolResult(true, output)
        } catch (t: Throwable) {
            ToolResult(false, "Script error: $t")
        }
    }

    private fun shell(command: String, cwd: File): ToolResult {
        return try {
            val proc = ProcessBuilder("/system/bin/sh", "-c", command)
                .directory(cwd)
                .redirectErrorStream(true)
                .start()
            val output = proc.inputStream.bufferedReader().readText()
            val code = proc.waitFor()
            ToolResult(code == 0, output.ifEmpty { "Command exited with code $code" })
        } catch (t: Throwable) {
            ToolResult(false, "Shell unavailable: $t")
        }
    }
}

/** Simple unified-style diff. */
fun diff(path: String, before: String?, after: String): String {
    val sb = StringBuilder("--- a/$path\n+++ b/$path\n")
    val b = before?.split("\n") ?: emptyList()
    val a = after.split("\n")
    var i = 0
    while (i < b.size || i < a.size) {
        val oldLine = b.getOrNull(i)
        val newLine = a.getOrNull(i)
        when {
            oldLine == newLine -> { /* unchanged */ }
            oldLine != null && newLine != null ->
                sb.append("- $oldLine\n+ $newLine\n")
            oldLine != null -> sb.append("- $oldLine\n")
            newLine != null -> sb.append("+ $newLine\n")
        }
        i++
    }
    return sb.toString().trimEnd('\n')
}
