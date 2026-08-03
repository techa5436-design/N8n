package com.agentgame.one.engine.agent

import com.agentgame.one.engine.agent.AgentPermissionManager.Permission

/** UI-facing status events from an agent run. */
sealed class AgentStatus {
    data class Working(val message: String) : AgentStatus()
    data class PlanReady(val plan: AgentPlan) : AgentStatus()
    data class StepStart(val step: AgentStep) : AgentStatus()
    data class ToolExecuted(val call: ToolCall, val result: ToolResult) : AgentStatus()
    data class ToolRejected(val call: ToolCall) : AgentStatus()
    data class Finished(val messages: List<AgentContext.Message>) : AgentStatus()
}

/** A permission gate the UI implements. [request] blocks until the user approves/rejects. */
interface ApprovalGate {
    fun request(permission: Permission, description: String, preview: String?): Boolean
}

/**
 * The Cline-style agentic loop: plan → for each tool call, ask for approval if needed → run the
 * tool → feed results back to the planner → refine until done. Checkpoints allow rollback.
 */
class AgentSession(
    val context: AgentContext,
    private val planner: AgentPlanner,
    private val listener: (AgentStatus) -> Unit,
    var approvalGate: ApprovalGate,
) {

    val checkpoint = AgentCheckpoint(context.workspace.root)
    @Volatile var running = false

    private val guardMax = 12

    fun run(goal: String) {
        if (running) return
        running = true
        try {
            checkpoint.capture()
            listener(AgentStatus.Working("Planning…"))
            val plan = planner.plan(context, goal)
            context.addMessage("user", goal)
            context.addMessage("assistant", "Plan: ${plan.summary}\nSteps: ${plan.steps.size}")
            listener(AgentStatus.PlanReady(plan))

            for (step in plan.steps) {
                if (!running) break
                listener(AgentStatus.StepStart(step))
                for (call in step.toolCalls) {
                    if (!running) break
                    executeToolCall(call)
                }
            }

            var guard = 0
            while (running && guard < guardMax) {
                val extra = planner.nextActions(context, goal)
                if (extra.isEmpty()) break
                val step = AgentStep("Agent refinement", extra)
                listener(AgentStatus.StepStart(step))
                for (call in extra) {
                    if (!running) break
                    executeToolCall(call)
                }
                guard++
            }

            listener(AgentStatus.Finished(context.conversation.toList()))
        } finally {
            running = false
        }
    }

    fun stop() { running = false }

    fun rollback() {
        checkpoint.restore()
        context.conversation.add(AgentContext.Message("system", "Workspace rolled back to checkpoint."))
    }

    fun hasCheckpoint(): Boolean = checkpoint.hasSnapshot()

    private fun executeToolCall(call: ToolCall) {
        val tool = AgentTools.get(call.tool)
        if (tool == null) {
            val res = ToolResult(false, "Unknown tool '${call.tool}'. Available: ${AgentTools.names()}")
            context.addMessage("assistant", res.formatted)
            listener(AgentStatus.ToolExecuted(call, res))
            return
        }
        val description = describe(call)
        val needsApproval = context.permissions.requiresApproval(tool.permission)
        val approved = if (needsApproval) {
            approvalGate.request(tool.permission, description, previewFor(call))
        } else true

        if (!approved) {
            context.addMessage("assistant", "REJECTED: $description")
            listener(AgentStatus.ToolRejected(call))
            return
        }

        val result = tool.execute(context, call.args)
        context.addMessage("assistant", "tool=${call.tool} → ${result.formatted}")
        listener(AgentStatus.ToolExecuted(call, result))
    }

    private fun describe(call: ToolCall): String {
        val sb = StringBuilder()
        for ((k, v) in call.args) {
            val s = v?.toString()?.take(80) ?: "null"
            sb.append("$k=$s; ")
        }
        return "${call.tool}(${sb.toString().trimEnd(';', ' ')})"
    }

    private fun previewFor(call: ToolCall): String? {
        return call.args["content"] as? String ?: call.args["replace"] as? String
    }
}
