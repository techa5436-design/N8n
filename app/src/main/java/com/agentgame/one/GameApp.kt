package com.agentgame.one

import com.jme3.app.SimpleApplication
import com.jme3.bounding.BoundingSphere
import com.jme3.light.AmbientLight
import com.jme3.light.DirectionalLight
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera
import com.jme3.scene.Geometry
import com.jme3.scene.shape.Line
import com.jme3.scene.shape.Sphere

/**
 * The Infinite Zombie game mode. A never-ending zombie horde attacks the player on the 1km map.
 * The player has unlimited ammo. Kills are counted at the top of the screen. The run only ends if
 * the zombies kill the player.
 */
class GameApp : SimpleApplication() {

    lateinit var world: WorldBuilder
    lateinit var player: Player
    lateinit var zombieManager: ZombieManager
    lateinit var hud: Hud
    lateinit var touch: TouchControl

    var elapsed = 0f

    private var fireTimer = 0f
    private val tracers = ArrayList<Tracer>()

    private data class Tracer(val geo: Geometry, var life: Float)

    override fun simpleInitApp() {
        setDisplayFps(false)
        setDisplayStatView(false)
        setPauseOnLostFocus(false)
        flyCam.isEnabled = false
        cam.setFrustumFar(1600f)

        addLights()
        addSky()

        world = WorldBuilder(assetManager)
        rootNode.attachChild(world.build())

        player = Player(this)
        player.buildModel()
        player.spawn()

        zombieManager = ZombieManager(this)
        hud = Hud(this)
        hud.init()

        // Touch control is created last, once everything it references exists.
        touch = TouchControl(this)
    }

    override fun simpleUpdate(tpf: Float) {
        elapsed += tpf

        if (player.dead) {
            updateTracers(tpf)
            hud.update(tpf)
            return
        }

        // Movement + aim
        player.update(tpf, touch.inForward, touch.inRight, touch.yawDelta)
        touch.endFrame()

        // Auto-fire at the crosshair (unlimited ammo)
        if (touch.firing) {
            fireTimer -= tpf
            if (fireTimer <= 0f) {
                val stats = GameConfig.WEAPON_STATS[GameConfig.selectedWeaponId]
                fireTimer = 1f / stats.fireRate
                val from = cam.getLocation().clone()
                val dir = cam.getDirection().clone().normalizeLocal()
                val hitAny = zombieManager.shoot(from, dir, stats)
                spawnTracer(from, dir, stats.range, hitAny)
            }
        }

        zombieManager.update(tpf)
        updateTracers(tpf)
        hud.update(tpf)
    }

    /** Simple smooth value noise for cloud patterns in the sky. */
    private fun skyNoise(x: Float, y: Float): Float {
        val xi = Math.floor(x.toDouble()).toInt()
        val yi = Math.floor(y.toDouble()).toInt()
        val xf = x - xi
        val yf = y - yi
        fun h(a: Int, b: Int): Float {
            var v = a * 374761393 + b * 668265263
            v = (v xor (v shr 13)) * 1274126177
            v = v xor (v shr 16)
            return (v and 0x7fffffff) / 2147483648f - 0.5f
        }
        val sx = xf * xf * (3f - 2f * xf)
        val sy = yf * yf * (3f - 2f * yf)
        val a = h(xi, yi); val b = h(xi + 1, yi)
        val c = h(xi, yi + 1); val d = h(xi + 1, yi + 1)
        return a + (b - a) * sx + (c - a) * sy + (a - b - c + d) * sx * sy
    }

    private fun addLights() {
        val sun = DirectionalLight().apply {
            setDirection(Vector3f(-0.5f, -0.8f, -0.4f).normalizeLocal())
            setColor(ColorRGBA(1f, 0.98f, 0.9f, 1f))
        }
        rootNode.addLight(sun)

        val fill = DirectionalLight().apply {
            setDirection(Vector3f(0.5f, -0.3f, 0.6f))
            setColor(ColorRGBA(0.5f, 0.6f, 0.8f, 1f))
        }
        rootNode.addLight(fill)

        val amb = AmbientLight().apply { setColor(ColorRGBA(0.5f, 0.5f, 0.52f, 1f)) }
        rootNode.addLight(amb)
    }

    private fun addSky() {
        val skyMat = Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
        val tex = Procedural.paintTexture(256, 256) { x, y, c ->
            val t = y / 255f
            val horizon = ColorRGBA(0.75f, 0.45f, 0.25f, 1f)   // warm horizon
            val zenith = ColorRGBA(0.10f, 0.22f, 0.45f, 1f)     // deep sky
            c.interpolateLocal(horizon, zenith, t.coerceIn(0f, 1f))
            // a few soft clouds (simple hash noise, no world dependency)
            val n = skyNoise(x * 0.012f, y * 0.012f + 40f)
            if (n > 0.10f) c.interpolateLocal(ColorRGBA(1f, 1f, 1f, 1f), (n - 0.10f).coerceIn(0f, 0.5f))
        }
        skyMat.setTexture("ColorMap", tex)
        skyMat.additionalRenderState.isDepthWrite = false

        val skyGeo = Geometry("sky", Sphere(32, 32, 1700f))
        skyGeo.material = skyMat
        skyGeo.setLocalTranslation(0f, 0f, 0f)
        skyGeo.modelBound = BoundingSphere(1800f, Vector3f.ZERO)
        rootNode.attachChild(skyGeo)

        skyGeo.setUserData("sky", true)
        // store for per-frame follow
        skyRef = skyGeo
    }

    private var skyRef: Geometry? = null

    private fun updateTracers(tpf: Float) {
        skyRef?.let { it.setLocalTranslation(cam.getLocation()) }
        val it = tracers.iterator()
        while (it.hasNext()) {
            val tr = it.next()
            tr.life -= tpf
            if (tr.life <= 0f) {
                tr.geo.removeFromParent()
                it.remove()
            }
        }
    }

    private fun spawnTracer(from: Vector3f, dir: Vector3f, range: Float, hit: Boolean) {
        val end = from.add(dir.mult(range))
        val line = Line(from, end)
        val m = Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
        m.setColor("Color", if (hit) ColorRGBA(1f, 0.9f, 0.2f, 1f) else ColorRGBA(0.7f, 0.7f, 0.7f, 1f))
        val g = Geometry("tracer", line)
        g.material = m
        rootNode.attachChild(g)
        tracers.add(Tracer(g, 0.08f))
    }

    /** Called by touch control when the player taps after dying — returns to the lobby. */
    fun quitFromGame() {
        this.stop(true)
    }

    override fun destroy() {
        super.destroy()
        if (::touch.isInitialized) inputManager.removeRawInputListener(touch)
    }
}
