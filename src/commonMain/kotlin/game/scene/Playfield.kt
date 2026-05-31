package game.scene

import game.audio.Sfx
import game.logic.NumberGenerator
import game.logic.ShotOutcome
import game.model.GameState
import game.view.RetroTheme
import game.view.balloon
import game.view.playerShip
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.View
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import kotlin.random.Random

/** Where the playfield sits inside the window: the play column to the right of the HUD. */
data class PlayArea(
    val width: Double,
    val height: Double,
    val hudWidth: Double,
)

/**
 * The live simulation: the player ship, the bullets, the falling numbers, their movement and
 * collisions. It owns those views and the [GameState] mutations they cause, reporting back to the
 * scene through [onShot] (a number was shot) and [onShipHit] (a number reached the ship). The
 * scene keeps the phase machine, input routing, HUD, and overlays.
 */
class Playfield(
    private val world: Container,
    private val area: PlayArea,
    private val state: GameState,
    private val sfx: Sfx,
    private val onShot: (ShotOutcome) -> Unit,
    private val onShipHit: () -> Boolean,   // returns true if the run continues after the hit
) {
    private val playerWidth = 40.0
    private val playerHeight = 28.0
    private val bulletSpeed = 460.0   // px/s, travels up
    private val enemySize = 46.0

    private val player: View = world.playerShip(playerWidth, playerHeight)
    private val bullets = mutableListOf<SolidRect>()
    private val enemies = mutableListOf<Enemy>()
    private var spawnTimer = 0.0
    private var pointerX: Double? = null   // latest pointer x while playing; null until the pointer steers

    var elapsedMs = 0.0
        private set

    private class Enemy(val view: View, val value: Int, val speedFactor: Double)

    init {
        centerPlayer()
    }

    /** Advance one frame: steer the ship, spawn, move shots and numbers, resolve collisions. */
    fun update(seconds: Double, leftHeld: Boolean, rightHeld: Boolean) {
        elapsedMs += seconds * 1000
        movePlayer(seconds, leftHeld, rightHeld)
        spawn(seconds)
        moveBullets(seconds)
        moveEnemies(seconds)
        checkHits()
    }

    /** Steer the ship so its center tracks the pointer x, clamped to the play area. */
    fun aimAt(x: Double) {
        pointerX = x.coerceIn(area.hudWidth + playerWidth / 2, area.width - playerWidth / 2)
    }

    fun fire() {
        val bullet = world.solidRect(4.0, 14.0, RetroTheme.amber)
            .xy(player.x + playerWidth / 2 - 2, player.y - 14)
        bullets.add(bullet)
        sfx.shoot()
    }

    /** Clear all entities and recenter the ship for a fresh run. */
    fun reset() {
        bullets.forEach { it.removeFromParent() }
        enemies.forEach { it.view.removeFromParent() }
        bullets.clear()
        enemies.clear()
        spawnTimer = 0.0
        elapsedMs = 0.0
        pointerX = null
        centerPlayer()
    }

    private fun centerPlayer() {
        player.xy((area.hudWidth + area.width - playerWidth) / 2, area.height - 40)
    }

    private fun movePlayer(seconds: Double, leftHeld: Boolean, rightHeld: Boolean) {
        val delta = 300.0 * seconds
        when {
            leftHeld -> { player.x = (player.x - delta).coerceAtLeast(area.hudWidth); pointerX = null }
            rightHeld -> { player.x = (player.x + delta).coerceAtMost(area.width - playerWidth); pointerX = null }
            else -> pointerX?.let { player.x = it - playerWidth / 2 }   // otherwise track the pointer
        }
    }

    private fun spawn(seconds: Double) {
        spawnTimer += seconds
        if (spawnTimer < state.config.spawnInterval) return
        spawnTimer = 0.0
        val value = NumberGenerator.randomValue(state.config)
        val x = Random.nextDouble(area.hudWidth, area.width - enemySize)
        val speedFactor = Random.nextDouble(MIN_SPEED_FACTOR, MAX_SPEED_FACTOR)   // a little drift-speed variety per balloon
        enemies.add(Enemy(world.balloon(value, enemySize, x), value, speedFactor))
    }

    private fun moveBullets(seconds: Double) {
        val iter = bullets.iterator()
        while (iter.hasNext()) {
            val bullet = iter.next()
            bullet.y -= bulletSpeed * seconds
            if (bullet.y + 14 < 0) {
                bullet.removeFromParent()
                iter.remove()
            }
        }
    }

    /** Balloons drift a little faster the longer the run lasts, easing up to the level's cap. */
    private fun currentEnemySpeed(): Double =
        (state.config.baseEnemySpeed + elapsedMs / 1000.0 * SPEED_RAMP_PER_SECOND)
            .coerceAtMost(state.config.maxEnemySpeed)

    private fun moveEnemies(seconds: Double) {
        val baseSpeed = currentEnemySpeed()
        val iter = enemies.iterator()
        while (iter.hasNext()) {
            val enemy = iter.next()
            enemy.view.y += baseSpeed * enemy.speedFactor * seconds
            when {
                hitsPlayer(enemy.view) -> { despawn(enemy, iter); if (!onShipHit()) return }
                enemy.view.y > area.height -> despawn(enemy, iter)   // fell past the ship: no penalty
            }
        }
    }

    private fun checkHits() {
        val iter = bullets.iterator()
        while (iter.hasNext()) {
            val bullet = iter.next()
            val hit = enemies.firstOrNull { overlaps(bullet, it.view) } ?: continue
            bullet.removeFromParent()
            iter.remove()
            hit.view.removeFromParent()
            enemies.remove(hit)
            sfx.pop()
            onShot(state.applyShot(hit.value))
        }
    }

    private fun despawn(enemy: Enemy, iter: MutableIterator<Enemy>) {
        enemy.view.removeFromParent()
        iter.remove()
    }

    private fun hitsPlayer(enemy: View): Boolean =
        enemy.x < player.x + playerWidth && enemy.x + enemySize > player.x &&
            enemy.y < player.y + playerHeight && enemy.y + enemySize > player.y

    private fun overlaps(bullet: SolidRect, enemy: View): Boolean =
        bullet.x < enemy.x + enemySize && bullet.x + 4 > enemy.x &&
            bullet.y < enemy.y + enemySize && bullet.y + 14 > enemy.y

    private companion object {
        const val SPEED_RAMP_PER_SECOND = 0.8   // balloons gain this many px/s for each second played
        const val MIN_SPEED_FACTOR = 0.85       // per-balloon speed spread, kept modest
        const val MAX_SPEED_FACTOR = 1.2
    }
}
