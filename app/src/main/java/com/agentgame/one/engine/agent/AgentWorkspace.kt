package com.agentgame.one.engine.agent

import java.io.File

/**
 * Manages the on-device workspace the agent reads and edits. On first run it seeds the workspace
 * with sample project files (scene + script, in the engine's formats) so the agent has something
 * to work on out of the box — just like Cline opening a repo.
 */
class AgentWorkspace(val root: File) {

    init {
        root.mkdirs()
        if (root.listFiles()?.isEmpty() != false) {
            seedSampleProject()
        }
    }

    fun resolve(relative: String): File = File(root, relative).normalize()

    private fun seedSampleProject() {
        writeFile("project.gts", "[project]\nname = \"My Game\"\nmain_scene = \"main.tscn\"\n")
        writeFile(
            "main.tscn",
            """
            [scene]
            [node type="Node2D" name="Main"]
            [node type="Polygon2D" name="Platform" parent="Main"]
            position = Vector2(0, 300)
            size = Vector2(600, 60)
            color = Color4(0.2, 0.5, 0.3, 1)
            [node type="RigidBody2D" name="Player" parent="Main"]
            position = Vector2(0, 100)
            [node type="CollisionShape2D" name="Shape" parent="Main/Player"]
            size = Vector2(40, 60)
            [node type="Sprite2D" name="PlayerArt" parent="Main/Player"]
            [node type="Camera2D" name="Camera" parent="Main"]
            """.trimIndent()
        )
        writeFile(
            "player.gs",
            """
            # MDScript (GDScript-style) attached to the Player node
            var speed = 220
            var jump = 560
            func _physics_process(delta):
                var stick = Input.joystickVector()
                self.velocity = Vector2(stick.x * speed, self.velocity.y)
                if Input.isActionJustPressed("jump") and self.isOnFloor:
                    self.velocity = Vector2(self.velocity.x, -jump)
            """.trimIndent()
        )
        writeFile(
            "README.md",
            """
            # My Game

            A tiny platformer project edited by the AI agent.

            ## Files
            - `main.tscn`  - the scene tree (nodes & properties)
            - `player.gs`  - the player controller script (MDScript)
            - `project.gts` - project settings

            Try asking the agent to "add a coin pick-up to the scene".
            """.trimIndent()
        )
    }

    fun writeFile(relative: String, content: String) {
        val f = resolve(relative)
        f.parentFile?.mkdirs()
        f.writeText(content)
    }

    fun readFile(relative: String): String? {
        val f = resolve(relative)
        return if (f.exists()) f.readText() else null
    }

    fun listFiles(): List<String> {
        return root.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(root).path }
            .sorted()
            .toList()
    }
}
