package com.agentgame.one.engine.core

/**
 * Base class for shareable data assets (Godot's `Resource` analogue). Resources are data that can
 * be loaded, saved and shared between nodes (textures, scripts, scenes, config). The engine's
 * resource cache lets the same asset be referenced many places without duplication.
 */
open class Resource {
    var resourceName: String = ""
    var resourcePath: String = ""

    val resourceLocalToScene: Boolean get() = resourcePath.startsWith("res://")

    companion object {
        private val cache = HashMap<String, Resource>()

        fun <T : Resource> load(path: String, factory: () -> T): T {
            val key = path.removePrefix("res://")
            @Suppress("UNCHECKED_CAST")
            return cache.getOrPut(key) {
                factory().apply { resourcePath = path; resourceName = key.substringAfterLast('/') }
            } as T
        }

        fun cached(path: String): Resource? = cache[path.removePrefix("res://")]
        fun clearCache() = cache.clear()
    }
}
