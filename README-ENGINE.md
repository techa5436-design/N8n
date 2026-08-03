# GEngine — a Godot-style 2D Game Engine for Android (+ Cline-style AI Agent)

This repository now contains **two** self-contained additions built on top of the existing
jMonkeyEngine battle-royale game:

1. **`com.agentgame.one.engine`** — a fully working, Godot-inspired **2D game engine** written in
   Kotlin. It is a real engine (nodes, scene tree, physics, rendering, input, UI, scripting),
   not a wrapper.
2. **`com.agentgame.one.AgentActivity`** — an **AI coding agent** that works like the VS Code
   **Cline** extension: it plans a task, asks for your approval before every edit/command, shows
   diffs, runs commands, and can roll back to a checkpoint.

Both are reachable from the lobby via the **"⚙ GAME ENGINE"** and **"🤖 AI AGENT"** buttons, or
launched directly (`EngineActivity`, `AgentActivity`).

---

## 1. The Game Engine (Godot-style)

The engine mirrors Godot's core architecture so that the concepts transfer 1:1:

| Godot concept           | This engine                                                        | Where |
|-------------------------|--------------------------------------------------------------------|-------|
| Node / Scene tree       | `Node` arranged in a `SceneTree`; depth-first `_process`/`_physics_process` | `engine/core/Node.kt`, `SceneTree.kt` |
| `_ready` / `_enter_tree` / `_exit_tree` | `onReady` / `onEnterTree` / `onExitTree` hooks + script callbacks | `Node.kt` |
| Node2D (transform)      | `Node2D` with position / rotation / scale + world transforms       | `engine/core/Node2D.kt`, `CanvasItem.kt` |
| Sprite2D                | `Sprite2D` (bitmap, flip, region, offset)                          | `engine/core/Sprite2D.kt` |
| Polygon2D / drawables   | `Polygon2D` (rect / circle / triangle, fill + border)              | `engine/core/Polygon2D.kt` |
| Camera2D                | `Camera2D` (follow target, smoothing, zoom, limits)                | `engine/core/Camera2D.kt` |
| Signals                 | named `Signal` with `connect` / `emit`                             | `engine/core/Signal.kt` |
| Groups                  | `addToGroup` / `getNodesInGroup`                                   | `Node.kt`, `SceneTree.kt` |
| Scene / PackedScene     | `PackedScene` + `.tscn`-style text save/load                        | `engine/core/Scene.kt` |
| Timer                   | `Timer` with `timeout` signal, one-shot/repeat                     | `engine/core/Timer.kt` |
| Tween                   | `Tween` with easing curves on any property                         | `engine/core/Tween.kt` |
| RigidBody2D / StaticBody2D / Area2D | `RigidBody2D`, `StaticBody2D`, `Area2D` + `CollisionShape2D` | `engine/physics/*` |
| PhysicsServer2D         | gravity, integration, collision resolution, area triggers          | `engine/physics/PhysicsServer2D.kt` |
| Input singleton / actions | `Input` with virtual joystick, action buttons, `isActionPressed` | `engine/input/Input.kt` |
| Control / Label / Button / Panel | `Control`, `Label`, `Button`, `Panel` (screen-space UI)     | `engine/ui/*` |
| GDScript                | **MDScript** — a GDScript-like interpreter attached to nodes       | `engine/scripting/MDScript.kt` |
| RenderingServer         | `RenderServer` draws the tree to an Android `Canvas`               | `engine/render/RenderServer.kt` |
| Engine singleton        | `Engine` (owns tree, renderer, input, fixed-timestep loop)         | `engine/Engine.kt` |

### Try it
`EngineActivity` loads **`PlatformerDemo`** — a playable platformer with a player (RigidBody2D),
gravity, platforms, collectible coins (Area2D), a patrolling enemy, a camera that follows, an
MDScript-driven spinning coin, and a screen-space HUD (score + jump button). Controls: drag on the
**left half** of the screen to move, tap **JUMP**.

### Scripting (MDScript)
Attach a GDScript-like script to any node:

```kotlin
val coin = Node2D("Coin")
coin.attachScript("""
    var spinSpeed = 2.5
    func _process(delta):
        self.rotation += spinSpeed * delta
        if self.visible:
            print("spinning")
""")
```

The interpreter supports variables, `if/else`, `while`, `for ... in`, functions, `return`,
arrays, arithmetic/comparison/logic, `self`, property access (`self.position.x`), method calls on
nodes (`self.queueFree()`, `self.getNode("...")`), and the `Input` singleton
(`Input.isActionPressed("ui_right")`).

### Scenes as files
`SceneIO` serializes a node tree to a compact `.tscn`-style text format and `PackedScene` loads it
back. This is exactly what the AI agent edits.

---

## 2. The AI Agent (Cline-style)

`AgentActivity` implements the Cline agentic loop **on-device**:

1. **Goal** — you type a task ("add a coin pickup to the scene").
2. **Plan** — the agent builds a step-by-step plan. If you configure an LLM API key it uses the
   model to plan & refine; otherwise it uses a built-in offline heuristic that understands the
   sample project.
3. **Permission model** — reads are auto-approved; **edits and commands ask for approval** first
   (Approve / Reject). A mode toggle auto-approves edits (Cline's "auto-approve"), and the whole
   thing is sandboxed to the workspace (blocked dirs like `.git`, `node_modules` are off-limits).
4. **Tools** — `list_files`, `read_file`, `write_file`, `edit_file` (with diff preview),
   `search_files`, `add_to_context`, `run_command` (built-in `ls/cat/echo/pwd/tree/run <script>`
   plus a real shell fallback that runs MDScript).
5. **Agentic loop** — tool outputs are fed back to the planner, which decides what to do next and
   iterates until the task is done.
6. **Checkpoints** — the workspace is snapshotted before a run; **ROLLBACK** restores it if the
   agent goes off track (Cline checkpoints).

### How to point the agent at a real LLM
The `LlmClient` is OpenAI-compatible. It reads its settings at construction in
`AgentActivity.onCreate`:

```kotlin
val client = LlmClient(
    baseUrl = "https://api.openai.com/v1/chat/completions",
    apiKey = "<your key>",
    model  = "gpt-4o-mini",
)
```

It works with any OpenAI-compatible endpoint (OpenAI, Ollama at `http://localhost:11434/v1`,
OpenRouter, etc.). Without a key the agent still works fully offline via `HeuristicPlanner`.

### Workspace
The agent operates on a sandboxed workspace created in the app's files dir and seeded with a tiny
project (`project.gts`, `main.tscn`, `player.gs`, `README.md`). Every edit the agent makes is a
real text change you can read in the log, and **ROLLBACK** restores the original files.

---

## Building
Same as the rest of the project (see the main `README.md`): Android Studio with API 34, or the
GitHub Actions workflow. The engine and agent are pure Kotlin + `android.graphics` / `org.json` —
no extra Gradle dependencies were added.
