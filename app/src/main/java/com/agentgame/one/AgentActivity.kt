package com.agentgame.one

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import com.agentgame.one.engine.agent.AgentContext
import com.agentgame.one.engine.agent.AgentLoop
import com.agentgame.one.engine.agent.AgentPermissionManager
import com.agentgame.one.engine.agent.AgentSession
import com.agentgame.one.engine.agent.AgentStatus
import com.agentgame.one.engine.agent.AgentWorkspace
import com.agentgame.one.engine.agent.ApprovalGate
import com.agentgame.one.engine.agent.HeuristicPlanner
import com.agentgame.one.engine.agent.LlmClient
import com.agentgame.one.engine.agent.LlmPlanner
import com.agentgame.one.engine.agent.AgentPermissionManager.Permission
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Cline-style AI agent screen: type a goal, the agent plans, asks for approval before each edit
 * or command, applies changes to the on-device workspace with diff previews, and can roll back
 * to a checkpoint.
 */
class AgentActivity : Activity(), ApprovalGate {

    private lateinit var workspace: AgentWorkspace
    private lateinit var permissions: AgentPermissionManager
    private lateinit var session: AgentSession

    private lateinit var logView: TextView
    private lateinit var goalInput: EditText
    private lateinit var statusView: TextView
    private lateinit var rollbackBtn: Button
    private lateinit var autoEditToggle: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        buildUi()

        val root = File(getExternalFilesDir(null) ?: filesDir, "agent-workspace")
        workspace = AgentWorkspace(root)
        permissions = AgentPermissionManager(workspace.root)
        val ctx = AgentContext(workspace, permissions)
        val client = LlmClient()
        val planner = LlmPlanner(client, HeuristicPlanner())
        session = AgentSession(ctx, planner, ::onStatus, this)
        appendLog("AI AGENT READY — workspace: ${root.path}")
        appendLog("Try: \"add a coin pickup to the scene\"  or  \"make the player jump higher\"")
    }

    private fun onStatus(status: AgentStatus) {
        runOnUiThread {
            when (status) {
                is AgentStatus.Working -> statusView.text = status.message
                is AgentStatus.PlanReady -> {
                    statusView.text = "PLAN READY (${status.plan.steps.size} steps)"
                    appendLog("\n[PLAN] ${status.plan.goal}")
                    status.plan.steps.forEachIndexed { i, s -> appendLog("  ${i + 1}. ${s.title}") }
                }
                is AgentStatus.StepStart -> {
                    statusView.text = "▶ ${status.step.title}"
                    appendLog("\n▸ ${status.step.title}")
                }
                is AgentStatus.ToolExecuted -> {
                    appendLog("  · ${status.call.tool} → ${status.result.formatted.take(600)}")
                    status.result.diff?.let { d -> appendLog("      diff: ${d.take(500)}") }
                }
                is AgentStatus.ToolRejected -> appendLog("  ✕ ${status.call.tool} (rejected by user)")
                is AgentStatus.Finished -> {
                    statusView.text = "DONE"
                    appendLog("\n[FINISHED] ${status.messages.size} conversation entries")
                }
            }
        }
    }

    override fun request(permission: Permission, description: String, preview: String?): Boolean {
        if (permissions.isSafeCommand(description)) return true
        val latch = CountDownLatch(1)
        val approved = AtomicBoolean(false)
        val label = when (permission) {
            Permission.READ -> "READ"
            Permission.EDIT -> "EDIT"
            Permission.EXEC -> "RUN COMMAND"
        }
        runOnUiThread {
            val msg = buildString {
                append("[$label] $description")
                preview?.let { append("\n\n$it") }
            }
            AlertDialog.Builder(this)
                .setTitle("Approve ${label.toLowerCase()}")
                .setMessage(msg)
                .setPositiveButton("Approve") { _, _ -> approved.set(true); latch.countDown() }
                .setNegativeButton("Reject") { _, _ -> approved.set(false); latch.countDown() }
                .setOnCancelListener { approved.set(false); latch.countDown() }
                .show()
        }
        latch.await()
        return approved.get()
    }

    private fun buildUi() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        val bg = Color.rgb(16, 18, 26)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val pad = dp(18)

        val title = TextView(this).apply {
            text = "🤖 AI AGENT  (Cline-style)"
            setTextColor(Color.WHITE)
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
        }
        root.addView(title, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setPadding(pad, dp(14), pad, dp(6))
        })

        goalInput = EditText(this).apply {
            hint = "Describe what you want the agent to build or change…"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(120, 125, 140))
            setSingleLine(true)
            setBackgroundColor(Color.rgb(28, 32, 44))
        }
        root.addView(goalInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)).apply {
            setPadding(pad, 0, pad, 0)
            setMargins(dp(18), 0, dp(18), 0)
        })

        // buttons row
        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val runBtn = makeButton("▶ RUN")
        runBtn.setOnClickListener { startRun() }
        rollbackBtn = makeButton("↩ ROLLBACK")
        rollbackBtn.isEnabled = false
        rollbackBtn.setOnClickListener {
            session.rollback()
            appendLog("[ROLLBACK] restored workspace to checkpoint")
            rollbackBtn.isEnabled = false
        }
        autoEditToggle = makeButton("MODE: ask edits")
        autoEditToggle.setOnClickListener {
            permissions.approvalMode = when (permissions.approvalMode) {
                AgentPermissionManager.ApprovalMode.DEFAULT -> {
                    autoEditToggle.text = "MODE: auto edits"
                    AgentPermissionManager.ApprovalMode.AUTO_EDITS
                }
                AgentPermissionManager.ApprovalMode.AUTO_EDITS -> {
                    autoEditToggle.text = "MODE: ask edits"
                    AgentPermissionManager.ApprovalMode.DEFAULT
                }
                else -> AgentPermissionManager.ApprovalMode.DEFAULT
            }
            appendLog("[MODE] ${permissions.approvalMode}")
        }
        btnRow.addView(runBtn, LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(dp(6), dp(10), dp(6), 0) })
        btnRow.addView(rollbackBtn, LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(dp(6), dp(10), dp(6), 0) })
        btnRow.addView(autoEditToggle, LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(dp(6), dp(10), dp(6), 0) })
        root.addView(btnRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setPadding(pad, 0, pad, 0)
        })

        statusView = TextView(this).apply {
            text = "idle"
            setTextColor(Color.rgb(255, 210, 120))
            textSize = 14f
        }
        root.addView(statusView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setPadding(pad, dp(8), pad, 0)
        })

        logView = TextView(this).apply {
            setTextColor(Color.rgb(210, 214, 224))
            textSize = 14f
            setTypeface(Typeface.MONOSPACE)
        }
        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = true; addView(logView) }
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            setMargins(dp(10), dp(8), dp(10), dp(8))
        })

        setContentView(root)
    }

    private fun makeButton(label: String): Button = Button(this).apply {
        text = label
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(38, 48, 70))
        setTypeface(null, Typeface.BOLD)
    }

    private fun appendLog(line: String) {
        logView.append(if (logView.text.isEmpty()) line else "\n$line")
        // auto scroll to bottom
        logView.post {
            val parent = logView.parent as? ScrollView
            parent?.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun startRun() {
        val goal = goalInput.text.toString().trim()
        if (goal.isEmpty()) {
            appendLog("[ERROR] type a goal first")
            return
        }
        goalInput.text.clear()
        rollbackBtn.isEnabled = session.hasCheckpoint()
        appendLog("\n===== NEW RUN: $goal =====")
        thread(name = "agent-run", isDaemon = true) {
            session.run(goal)
            runOnUiThread { rollbackBtn.isEnabled = session.hasCheckpoint() }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
