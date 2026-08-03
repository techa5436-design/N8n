package com.agentgame.one.engine.demo

import com.agentgame.one.engine.Engine
import com.agentgame.one.engine.core.Camera2D
import com.agentgame.one.engine.core.Node
import com.agentgame.one.engine.core.Node2D
import com.agentgame.one.engine.core.Polygon2D
import com.agentgame.one.engine.core.Rect2
import com.agentgame.one.engine.core.Timer
import com.agentgame.one.engine.core.Vector2
import com.agentgame.one.engine.input.Input
import com.agentgame.one.engine.physics.Area2D
import com.agentgame.one.engine.physics.CollisionShape2D
import com.agentgame.one.engine.physics.RigidBody2D
import com.agentgame.one.engine.physics.StaticBody2D
import com.agentgame.one.engine.render.Color4
import com.agentgame.one.engine.scripting.attachScript
import com.agentgame.one.engine.ui.Button
import com.agentgame.one.engine.ui.Label
import com.agentgame.one.engine.ui.Panel
import kotlin.math.abs

/**
 * A small playable platformer built entirely with the engine, demonstrating nodes, the scene
 * tree, physics, camera, input, UI controls and the MDScript interpreter all working together.
 */
object PlatformerDemo {

    private const val MOVE_SPEED = 260f
    private const val JUMP_SPEED = 620f

    class Player : RigidBody2D("Player") {
        var score = 0
        override fun onPhysicsProcess(delta: Float) {
            val stick = Input.joystickVector()
            velocity = velocity.withX(stick.x * MOVE_SPEED)
            if (Input.isActionJustPressed("jump") && isOnFloor) {
                velocity = velocity.withY(-JUMP_SPEED)
            }
        }
    }

    fun buildScene(engine: Engine): Node {
        val root = Node2D("Main")

        // ---- world: ground + platforms (static bodies) ----
        ground(root, -400f, 340f, 2000f)
        ground(root, 200f, 220f, 320f)
        ground(root, 700f, 120f, 320f)
        ground(root, 1200f, 40f, 260f)

        // ---- walls to keep the player in bounds ----
        ground(root, -420f, -200f, 60f, tall = true)
        ground(root, 1520f, -200f, 60f, tall = true)

        // ---- coins ----
        spawnCoin(root, 220f, 160f)
        spawnCoin(root, 720f, 60f)
        spawnCoin(root, 1220f, -20f)
        spawnCoin(root, 120f, 280f)

        // ---- player ----
        val player = Player()
        player.pos(40f, 250f)
        val pc = CollisionShape2D("PlayerShape").rect(34f, 44f).apply { position = Vector2(0f, 0f) }
        player.addChild(pc)
        val body = Polygon2D("PlayerBody").centeredRect(34f, 44f).color(Color4(0.15f, 0.6f, 0.9f, 1f))
        body.position = Vector2(0f, 0f)
        player.addChild(body)
        root.addChild(player)

        // ---- enemy: a patrol platform that damages the player ----
        val enemy = spawnEnemy(root, 600f, 180f)
        enemy.setOnBodyEntered {
            if (it === player) player.score -= 1
        }

        // ---- camera follows the player ----
        val cam = Camera2D("Camera")
        cam.target = player
        cam.smoothing = 4f
        root.addChild(cam)

        // ---- scripted spinning coin (proves the MDScript interpreter works) ----
        val scripted = Node2D("ScriptedSpinner").pos(1000f, -100f)
        val spinnerBody = Polygon2D("SpinnerVisual").centeredRect(30f, 30f).color(Color4(1f, 0.8f, 0f, 1f))
        scripted.addChild(spinnerBody)
        scripted.attachScript(
            """
            var spinSpeed = 2.5
            var up = true
            var baseY = -100.0
            var off = 0.0
            func _process(delta):
                self.rotation += spinSpeed * delta
                if up:
                    off -= 40 * delta
                else:
                    off += 40 * delta
                if off < -40:
                    up = false
                if off > 40:
                    up = true
                self.position = Vector2(self.position.x, baseY + off)
            """.trimIndent()
        )
        root.addChild(scripted)

        // ---- HUD (screen space controls) ----
        buildHud(root, player, engine)

        return root
    }

    private fun ground(parent: Node, x: Float, y: Float, w: Float, tall: Boolean = false) {
        val h = if (tall) 800f else 60f
        val body = StaticBody2D("Ground")
        body.pos(x, y)
        val cs = CollisionShape2D("Shape").rect(w, h)
        body.addChild(cs)
        val visual = Polygon2D("Visual").centeredRect(w, h).color(if (tall) Color4(0.4f, 0.42f, 0.5f, 1f) else Color4(0.25f, 0.5f, 0.3f, 1f))
        body.addChild(visual)
        parent.addChild(body)
    }

    private fun spawnCoin(parent: Node, x: Float, y: Float) {
        val coin = Area2D("Coin").pos(x, y)
        coin.collisionLayer = 2
        val cs = CollisionShape2D("Shape").circle(16f)
        coin.addChild(cs)
        val visual = Polygon2D("Visual").circle(14f).color(Color4(1f, 0.84f, 0f, 1f)).border(Color4(0.7f, 0.5f, 0f, 1f), 3f)
        coin.addChild(visual)
        coin.bodyEntered.connect {
            coin.visible = false
            coin.queueFree()
        }
        parent.addChild(coin)
    }

    private fun spawnEnemy(parent: Node, x: Float, y: Float): Enemy {
        val e = Enemy().pos(x, y)
        val cs = CollisionShape2D("Shape").rect(40f, 30f)
        e.addChild(cs)
        val visual = Polygon2D("Visual").centeredRect(40f, 30f).color(Color4(0.8f, 0.2f, 0.2f, 1f))
        e.addChild(visual)
        parent.addChild(e)
        return e
    }

    /** A moving "hazard" body that patrols a platform. */
    class Enemy : RigidBody2D("Enemy") {
        private val startX = 0f
        private var dir = 1f
        override fun onReady() { velocity = Vector2(dir * 120f, 0f) }
        override fun onPhysicsProcess(delta: Float) {
            if (abs(position.x - startX) > 160f) {
                dir *= -1f
                velocity = velocity.withX(dir * 120f)
            }
        }
        fun setOnBodyEntered(f: (com.agentgame.one.engine.physics.PhysicsBody2D) -> Unit) {
            bodyEntered.connect { f(it[0] as com.agentgame.one.engine.physics.PhysicsBody2D) }
        }
    }

    private fun buildHud(root: Node, player: Player, engine: Engine) {
        val scoreLabel = Label("Score").apply {
            text = "SCORE: 0"
            fontSize = 30
            fontColor = Color4.WHITE
            position = Vector2(20f, 24f)
            size = Vector2(260f, 40f)
            zIndex = 100
        }
        root.addChild(scoreLabel)

        val hint = Label("Hint").apply {
            text = "LEFT TOUCH = MOVE   •   RIGHT BUTTON = JUMP"
            fontSize = 16
            fontColor = Color4(0.85f, 0.85f, 0.9f, 0.9f)
            position = Vector2(20f, 620f)
            size = Vector2(700f, 30f)
            zIndex = 100
        }
        root.addChild(hint)

        val jumpBtn = Button("JumpBtn").apply {
            text = "JUMP"
            fontSize = 22
            position = Vector2(760f, 540f)
            size = Vector2(220f, 90f)
            normalColor = Color4(0.2f, 0.45f, 0.9f, 1f)
            backgroundColor = normalColor
            roundedCorners = 12f
            zIndex = 100
            pressedSignal.connect {
                Input.actionPress("jump")
                Input.actionRelease("jump")
            }
        }
        root.addChild(jumpBtn)

        // update score each frame via a lightweight node
        val hudTimer = Node("ScoreUpdater")
        hudTimer.processLambda = {
            scoreLabel.text = "SCORE: ${player.score}"
        }
        root.addChild(hudTimer)
    }
}
