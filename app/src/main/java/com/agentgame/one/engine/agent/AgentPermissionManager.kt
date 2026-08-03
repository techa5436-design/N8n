package com.agentgame.one.engine.agent

import java.io.File

/**
 * Controls what the AI agent is allowed to do and how much the user must approve — the Cline
 * permission model (auto-approve read, confirm edits, confirm commands; optional YOLO modes).
 */
class AgentPermissionManager(
    val workspaceRoot: File,
    val blockedDirectories: List<String> = listOf(
        "node_modules", ".git", ".gradle", "build", "out", "dist", "target",
        "android", "secrets", ".ssh", "credentials",
    ),
) {

    enum class Permission { READ, EDIT, EXEC }

    enum class ApprovalMode {
        /** Auto-approve reads; ask for every edit and command. */
        DEFAULT,
        /** Auto-approve reads and edits; ask only for commands. */
        AUTO_EDITS,
        /** Auto-approve everything (Cline "YOLO" mode). */
        AUTO_ALL,
    }

    var approvalMode: ApprovalMode = ApprovalMode.DEFAULT

    /** A regex allowlist of command strings that may run without approval (safe commands). */
    var safeCommandPatterns: List<Regex> = listOf(
        Regex("^ls(\\s.*)?$"),
        Regex("^pwd$"),
        Regex("^cat(\\s.+)?$"),
        Regex("^echo(\\s.+)?$"),
    )

    /** True if a tool with the given permission must pause for user approval. */
    fun requiresApproval(permission: Permission): Boolean {
        return when (approvalMode) {
            ApprovalMode.AUTO_ALL -> false
            ApprovalMode.AUTO_EDITS -> permission == Permission.EXEC
            ApprovalMode.DEFAULT -> permission == Permission.EDIT || permission == Permission.EXEC
        }
    }

    fun isSafeCommand(command: String): Boolean =
        safeCommandPatterns.any { it.matches(command.trim()) }

    /** Whether [file] is inside the workspace and not in a blocked directory. */
    fun isAllowedPath(file: File): Boolean {
        val root = workspaceRoot.absoluteFile
        val f = file.absoluteFile
        if (!f.path.startsWith(root.path)) return false
        for (blocked in blockedDirectories) {
            val seg = File.separator + blocked + File.separator
            if (f.path.contains(seg)) return false
        }
        return true
    }

    /** Resolves a relative path against the workspace (guards against escaping). */
    fun resolve(relative: String): File? {
        val target = File(workspaceRoot, relative).absoluteFile.normalize()
        if (!isAllowedPath(target)) return null
        return target
    }
}
