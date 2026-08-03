package com.agentgame.one.engine.agent

import com.agentgame.one.engine.agent.AgentPermissionManager.Permission

/** Result of executing an agent tool. */
class ToolResult(
    val success: Boolean,
    val output: String,
    val permission: Permission = Permission.READ,
    val diff: String? = null,
    val changedFiles: List<String> = emptyList(),
) {
    val formatted: String
        get() = if (success) output else "ERROR: $output"
}

/**
 * A tool the AI agent can invoke (Cline's tool list). Each tool declares its permission level so
 * the permission manager can decide whether the user must approve before it runs.
 */
interface AgentTool {
    val name: String
    val description: String
    val permission: Permission

    fun execute(ctx: AgentContext, args: Map<String, Any?>): ToolResult
}
