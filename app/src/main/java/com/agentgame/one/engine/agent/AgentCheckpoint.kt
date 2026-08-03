package com.agentgame.one.engine.agent

import java.io.File

/**
 * Snapshots the workspace so the user can roll back an agent run (Cline's checkpoints). Stores
 * file contents in memory and can restore them on demand.
 */
class AgentCheckpoint(val root: File) {

    private val snapshot = mutableMapOf<String, String>()
    private val deletedFiles = mutableSetOf<String>()

    /** Captures the current state of every file in the workspace. */
    fun capture() {
        snapshot.clear()
        deletedFiles.clear()
        if (!root.exists()) return
        root.walkTopDown().filter { it.isFile }.forEach { f ->
            snapshot[f.relativeTo(root).path] = f.readText()
        }
    }

    fun hasSnapshot(): Boolean = snapshot.isNotEmpty()

    /** Restores the workspace to the captured state. */
    fun restore() {
        if (!root.exists()) root.mkdirs()
        // delete files that didn't exist in the snapshot
        for (file in snapshot.keys) {
            val f = File(root, file)
            if (f.exists() && f.readText() == snapshot[file]) continue
        }
        // delete everything not in snapshot
        root.walkTopDown().filter { it.isFile }.toList().forEach { f ->
            val rel = f.relativeTo(root).path
            if (!snapshot.containsKey(rel)) f.delete()
        }
        // restore contents
        for ((rel, content) in snapshot) {
            val f = File(root, rel)
            f.parentFile?.mkdirs()
            f.writeText(content)
        }
    }

    fun snapshotSize(): Int = snapshot.size
}
