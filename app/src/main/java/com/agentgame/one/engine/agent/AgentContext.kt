package com.agentgame.one.engine.agent

import java.io.File

/**
 * Shared state passed to every tool and the planner: the workspace, permissions, and the growing
 * conversation (goal, plan, tool inputs/outputs) the planner reasons over.
 */
class AgentContext(
    val workspace: AgentWorkspace,
    val permissions: AgentPermissionManager,
) {

    data class Message(val role: String, val content: String)

    val conversation = mutableListOf<Message>()
    val contextFiles = LinkedHashMap<String, String>()

    /** Files currently loaded into context for the model. */
    val filesInContext: Set<String> get() = contextFiles.keys

    fun addMessage(role: String, content: String) {
        conversation.add(Message(role, content))
    }

    fun addFileToContext(relative: String, content: String) {
        contextFiles[relative] = content
    }

    /** Human-readable summary of the project: file list + contents of context files. */
    fun projectSummary(includeContents: Boolean = true): String {
        val sb = StringBuilder()
        sb.append("Workspace files:\n")
        for (f in workspace.listFiles()) sb.append("  - $f\n")
        if (includeContents && contextFiles.isNotEmpty()) {
            sb.append("\nFile contents in context:\n")
            for ((path, content) in contextFiles) {
                sb.append("\n===== $path =====\n").append(content.take(3000)).append("\n")
            }
        }
        return sb.toString()
    }
}
