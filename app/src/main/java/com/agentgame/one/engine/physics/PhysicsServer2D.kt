package com.agentgame.one.engine.physics

import com.agentgame.one.engine.core.Node
import com.agentgame.one.engine.core.SceneTree
import com.agentgame.one.engine.core.Vector2

/**
 * The 2D physics simulation (Godot's `PhysicsServer2D` analogue). Each fixed step it:
 *  - applies gravity & integrates [RigidBody2D] bodies,
 *  - resolves collisions between solid bodies (respecting layer/mask),
 *  - updates [Area2D] overlap enter/exit notifications.
 */
class PhysicsServer2D(private val tree: SceneTree) {

    var gravity: Vector2 = Vector2(0f, 980f)
    var gravityScaleGlobal: Float = 1f
    var timeScale: Float = 1f

    private val bodies = mutableListOf<PhysicsBody2D>()
    private val rigids = mutableListOf<RigidBody2D>()
    private val statics = mutableListOf<StaticBody2D>()
    private val areas = mutableListOf<Area2D>()

    /** Gathers all physics bodies from the scene tree. */
    fun collect() {
        bodies.clear(); rigids.clear(); statics.clear(); areas.clear()
        collectFrom(tree.root)
    }

    private fun collectFrom(node: Node) {
        if (node is RigidBody2D) {
            rigids.add(node); bodies.add(node)
        } else if (node is StaticBody2D) {
            statics.add(node); bodies.add(node)
        } else if (node is Area2D) {
            areas.add(node); bodies.add(node)
        }
        for (c in node._children) collectFrom(c)
    }

    /** Runs one physics step of duration [delta]. */
    fun step(delta: Float) {
        val dt = delta * timeScale
        collect()

        // 1) integrate rigid bodies
        for (body in rigids) {
            if (body.freeze) continue
            val g = gravity * body.gravityScale
            body.velocity += g * dt
            if (body.linearDamp > 0f) {
                body.velocity *= (1f - body.linearDamp * dt).coerceAtLeast(0f)
            }
            body.position = body.position + body.velocity * dt
        }

        // 2) resolve rigid-vs-solid collisions
        for (body in rigids) {
            if (body.freeze || body.sensor) continue
            if (body.shape == null) continue
            for (other in bodies) {
                if (other === body) continue
                if (!other.isSolid()) continue
                if (!body.maskMatches(other.collisionLayer)) continue
                val otherShape = other.shape ?: continue
                val push = Collisions.separation(body.globalPosition, body.shape!!, other.globalPosition, otherShape)
                if (push != null) {
                    body.position += push
                    val normal = push.normalized()
                    // reflect the velocity component along the normal
                    val vDotN = body.velocity.dot(normal)
                    if (vDotN < 0f) {
                        body.velocity -= normal * vDotN * (1f + restitution)
                        body.onContact(other, normal)
                    }
                }
            }
        }

        // 3) rigid-vs-rigid (soft separation)
        for (i in rigids.indices) {
            for (j in i + 1 until rigids.size) {
                val a = rigids[i]; val b = rigids[j]
                if (a.freeze || b.freeze || a.sensor || b.sensor) continue
                val sa = a.shape ?: continue
                val sb = b.shape ?: continue
                if (!a.maskMatches(b.collisionLayer)) continue
                val push = Collisions.separation(a.globalPosition, sa, b.globalPosition, sb)
                if (push != null) {
                    a.position += push / 2f
                    b.position -= push / 2f
                }
            }
        }

        // 4) area overlap notifications
        val allBodies = bodies.toList()
        for (area in areas) {
            if (!area.monitoring) continue
            val areaShape = area.shape ?: continue
            val overlappingNow = allBodies.filter { it !== area && it.shape != null && area.maskMatches(it.collisionLayer) &&
                Collisions.overlap(area.globalPosition, areaShape, it.globalPosition, it.shape!!) }
            // begin / end
            for (b in allBodies) {
                if (b === area) continue
                val overlap = b in overlappingNow
                if (overlap) {
                    if (b is Area2D) area.beginAreaOverlap(b)
                    else area.beginBodyOverlap(b)
                } else {
                    if (b is Area2D) area.endAreaOverlap(b)
                    else area.endBodyOverlap(b)
                }
            }
        }
    }

    /** Bounciness factor applied on contact (0..1). */
    var restitution: Float = 0f
}
