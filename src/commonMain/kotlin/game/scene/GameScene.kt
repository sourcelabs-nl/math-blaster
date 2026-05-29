package game.scene

import game.audio.Music
import game.audio.Sfx
import game.logic.NumberGenerator
import game.logic.ShotOutcome
import game.logic.formatTime
import game.model.GameState
import game.model.LeaderboardEntry
import game.storage.LeaderboardStorage
import game.view.Hud
import game.view.IntroActions
import game.view.IntroViewData
import game.view.LeaderboardViewData
import game.view.RetroTheme
import game.view.balloon
import game.view.confirmView
import game.view.goodbyeView
import game.view.introView
import game.view.leaderboardView
import game.view.nameEntryView
import game.view.playerShip
import game.view.recenterAt
import game.view.starfield
import korlibs.event.Key
import korlibs.event.KeyEvent
import korlibs.image.color.RGBA
import korlibs.korge.input.MouseEvents
import korlibs.korge.input.keys
import korlibs.korge.input.mouse
import korlibs.korge.scene.Scene
import korlibs.korge.service.storage.storage
import korlibs.korge.view.Container
import korlibs.korge.view.SContainer
import korlibs.korge.view.SolidRect
import korlibs.korge.view.Text
import korlibs.korge.view.View
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.korge.view.xy
import kotlin.random.Random
import kotlin.time.DurationUnit

/**
 * Wires the game together: the intro, input, the per-frame loop, spawning, collisions, and the
 * win/leaderboard flow. Rules live in [GameState], rendering in the view package, audio in
 * [Sfx], persistence in [LeaderboardStorage]; this scene only orchestrates them through [phase].
 */
class GameScene : Scene() {

    // Named screenWidth/screenHeight (not width/height) on purpose: inside a `container { }`
    // builder the receiver is a View, whose own `width`/`height` would shadow these fields.
    private val screenWidth = 960.0
    private val screenHeight = 600.0
    private val hudWidth = 150.0          // left column reserved for the HUD (no balloons here)

    private val playerWidth = 40.0
    private val playerHeight = 28.0
    private val bulletSpeed = 460.0       // px/s, travels up
    private val enemySize = 46.0
    private val maxNameLength = 8         // balloon fall speed and spawn rate now come from the level config

    private val state = GameState()
    private val bullets = mutableListOf<SolidRect>()
    private val enemies = mutableListOf<Enemy>()
    private var spawnTimer = 0.0
    private var messageTimer = 0.0
    private var introCooldown = 0.0       // briefly ignores input so a stray load event can't auto-start
    private var quitFromPlaying = false   // where to return if the player cancels the quit prompt
    private var elapsedMs = 0.0
    private var phase = Phase.INTRO
    private var typedName = ""
    private var shownLevel = 1            // last level announced, so the level-up flash fires once
    private var muted = false
    private var pointerX: Double? = null  // latest pointer x while playing; null until the pointer steers
    private var tapStartedPlaying = false // a tap begun while playing fires the gun on release

    private lateinit var world: SContainer
    private lateinit var sfx: Sfx
    private lateinit var music: Music
    private lateinit var hud: Hud
    private lateinit var player: View
    private lateinit var message: Text
    private lateinit var hint: Text
    private lateinit var leaderboard: LeaderboardStorage
    private var overlay: Container? = null
    private var nameField: Text? = null

    private enum class Phase {
        INTRO, PLAYING, PAUSED, ENTERING_NAME, FINISHED, LEADERBOARD, CONFIRM_QUIT, QUIT,
    }

    private class Enemy(val view: View, val value: Int, val speedFactor: Double)

    override suspend fun SContainer.sceneMain() {
        world = this
        RetroTheme.load()
        sfx = Sfx.create()
        music = Music.create()
        leaderboard = LeaderboardStorage(views.storage)
        starfield(screenWidth, screenHeight)
        hud = Hud(this, hudWidth, screenHeight)
        player = playerShip(playerWidth, playerHeight)
            .xy((hudWidth + screenWidth - playerWidth) / 2, screenHeight - 40)
        message = text("", textSize = 20.0, color = RetroTheme.amber, font = RetroTheme.font)
        hint = text("Q QUIT\nL LEADERBOARD\nM MUTE", textSize = 8.0, color = RetroTheme.dim, font = RetroTheme.font)
            .xy(14, screenHeight - 52)   // tucked into the bottom of the left HUD bar
        refreshHud()
        showIntro()
        introCooldown = INTRO_INPUT_DELAY   // only on first load: ignore a stray pointer/key event

        keys {
            justDown(Key.SPACE) { if (phase == Phase.PLAYING) fire() }
            down { event -> onKeyDown(event) }
        }
        world.mouse {
            down { onPointerDown(it) }   // drag steers the ship, a tap fires: works with a mouse or on touch
            move { onPointerMove(it) }
            up { onPointerUp() }
        }
        addUpdater { dt ->
            val seconds = dt.toDouble(DurationUnit.SECONDS)
            if (introCooldown > 0) introCooldown -= seconds.coerceAtMost(0.05)   // clamp: a big first frame can't drain it
            step(seconds)
        }
    }

    private fun onKeyDown(event: KeyEvent) {
        if (event.key == Key.M) return toggleMute()   // mute works in any phase
        when (phase) {
            Phase.INTRO -> when (event.key) {
                Key.ENTER, Key.SPACE, Key.S -> startPlaying()
                Key.Q -> showQuitConfirm(fromPlaying = false)
                Key.L -> showFullLeaderboard()
                else -> {}
            }
            Phase.PLAYING -> when (event.key) {
                Key.Q -> showQuitConfirm(fromPlaying = true)
                Key.L -> pauseForLeaderboard()
                else -> {}
            }
            Phase.PAUSED -> resumePlaying()    // any key resumes from the in-game leaderboard
            Phase.CONFIRM_QUIT -> when (event.key) {
                Key.Y -> confirmQuit()
                Key.N, Key.ESCAPE -> cancelQuit()
                else -> {}
            }
            Phase.ENTERING_NAME -> editName(event)
            Phase.FINISHED -> if (event.key == Key.R) restart()
            Phase.LEADERBOARD -> showIntro()   // any key returns to the menu
            Phase.QUIT -> {}                   // terminal: nothing left to do
        }
    }

    private fun step(seconds: Double) {
        if (phase != Phase.PLAYING) return
        elapsedMs += seconds * 1000
        movePlayer(seconds)
        spawn(seconds)
        moveBullets(seconds)
        moveEnemies(seconds)
        checkHits()
        tickMessage(seconds)
        refreshHud()
    }

    private fun movePlayer(seconds: Double) {
        val delta = 300.0 * seconds
        val keys = views.input.keys
        when {
            keys[Key.LEFT] -> { player.x = (player.x - delta).coerceAtLeast(hudWidth); pointerX = null }
            keys[Key.RIGHT] -> { player.x = (player.x + delta).coerceAtMost(screenWidth - playerWidth); pointerX = null }
            else -> pointerX?.let { player.x = it - playerWidth / 2 }   // otherwise track the pointer
        }
    }

    private fun onPointerDown(event: MouseEvents) {
        if (phase != Phase.PLAYING || introCooldown > 0) return
        tapStartedPlaying = true
        aimAt(event.currentPosLocal.x)
    }

    private fun onPointerMove(event: MouseEvents) {
        if (phase == Phase.PLAYING) aimAt(event.currentPosLocal.x)
    }

    private fun onPointerUp() {
        if (tapStartedPlaying && phase == Phase.PLAYING) fire()
        tapStartedPlaying = false
    }

    /** Steer the ship so its center tracks the pointer x, clamped to the play area. */
    private fun aimAt(x: Double) {
        pointerX = x.coerceIn(hudWidth + playerWidth / 2, screenWidth - playerWidth / 2)
    }

    private fun fire() {
        val bullet = world.solidRect(4.0, 14.0, RetroTheme.amber)
            .xy(player.x + playerWidth / 2 - 2, player.y - 14)
        bullets.add(bullet)
        sfx.shoot()
    }

    private fun spawn(seconds: Double) {
        spawnTimer += seconds
        if (spawnTimer < state.config.spawnInterval) return
        spawnTimer = 0.0
        val value = NumberGenerator.randomValue(state.config)
        val x = Random.nextDouble(hudWidth, screenWidth - enemySize)
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
                hitsPlayer(enemy.view) -> { despawn(enemy, iter); onShipHit(); if (phase != Phase.PLAYING) return }
                enemy.view.y > screenHeight -> despawn(enemy, iter)   // fell past the ship: no penalty
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
            react(state.applyShot(hit.value))
        }
    }

    private fun react(outcome: ShotOutcome) {
        when (outcome) {
            ShotOutcome.CLEARED -> onRoundCleared()
            ShotOutcome.CONTINUE -> {}
        }
        refreshHud()
    }

    private fun onRoundCleared() {
        when {
            state.hasWon -> onWin()
            state.level > shownLevel -> { shownLevel = state.level; flash("LEVEL ${state.level}", RetroTheme.amber) }
            else -> flash("WIN!", RetroTheme.green)
        }
    }

    private fun toggleMute() {
        muted = !muted
        sfx.muted = muted
        music.setMuted(muted)
        flash(if (muted) "SOUND OFF" else "SOUND ON", RetroTheme.cyan)
    }

    private fun onShipHit() {
        state.loseLife()
        if (state.isGameOver) onGameOver() else flash("HIT! -1 LIFE", RetroTheme.magenta)
        refreshHud()
    }

    private fun onWin() {
        phase = Phase.ENTERING_NAME
        typedName = ""
        message.text = ""
        clearOverlay()
        overlay = world.container { nameField = nameEntryView(screenWidth, screenHeight, formatTime(elapsedMs.toInt())) }
    }

    // Reads the typed key, not event.character: KorGE only fills character on TYPE events, but
    // this handler runs on key-down, where it is empty. So we map the Key itself to a letter.
    private fun editName(event: KeyEvent) {
        when (event.key) {
            Key.BACKSPACE -> typedName = typedName.dropLast(1)
            Key.ENTER -> return confirmName()
            Key.SPACE -> appendNameChar(' ')
            else -> letterFor(event.key)?.let { appendNameChar(it) }
        }
        nameField?.apply { text = "${typedName}_"; recenterAt(screenWidth / 2) }
    }

    private fun appendNameChar(c: Char) {
        if (typedName.length < maxNameLength) typedName += c
    }

    /** The letter for an A..Z key (whose enum name is the single letter), or null otherwise. */
    private fun letterFor(key: Key): Char? =
        key.name.singleOrNull()?.takeIf { it in 'A'..'Z' }

    private fun confirmName() {
        val name = typedName.trim().ifEmpty { "ANON" }
        val entry = LeaderboardEntry(name, elapsedMs.toInt())
        phase = Phase.FINISHED
        showLeaderboard("YOU WIN!", leaderboard.save(entry), highlight = entry, footer = "PRESS R TO PLAY AGAIN")
    }

    private fun onGameOver() {
        phase = Phase.FINISHED
        message.text = ""
        showLeaderboard("GAME OVER", leaderboard.load(), highlight = null, footer = "PRESS R TO PLAY AGAIN")
    }

    private fun showFullLeaderboard() {
        phase = Phase.LEADERBOARD
        showLeaderboard("LEADERBOARD", leaderboard.load(), highlight = null, footer = "PRESS ANY KEY TO RETURN")
    }

    private fun startPlaying() {
        if (introCooldown > 0) return   // ignore the stray pointer/key event fired right on load
        clearOverlay()
        music.start()        // first user gesture: browsers allow audio to begin here
        phase = Phase.PLAYING
    }

    private fun restart() {
        bullets.forEach { it.removeFromParent() }
        enemies.forEach { it.view.removeFromParent() }
        bullets.clear()
        enemies.clear()
        spawnTimer = 0.0
        elapsedMs = 0.0
        shownLevel = 1
        pointerX = null
        tapStartedPlaying = false
        state.reset()
        message.text = ""
        phase = Phase.INTRO
        refreshHud()
        showIntro()
    }

    private fun showIntro() {
        clearOverlay()
        val actions = IntroActions(onOk = ::startPlaying, onQuit = { showQuitConfirm(fromPlaying = false) })
        val data = IntroViewData(screenWidth, screenHeight, state.target, leaderboard.load(), actions)
        overlay = world.container { introView(data) }
    }

    /** Pause and show the leaderboard mid-run; any key resumes (the timer is paused too). */
    private fun pauseForLeaderboard() {
        phase = Phase.PAUSED
        showLeaderboard("LEADERBOARD", leaderboard.load(), highlight = null, footer = "PRESS ANY KEY TO RESUME")
    }

    private fun resumePlaying() {
        clearOverlay()
        phase = Phase.PLAYING
    }

    private fun showQuitConfirm(fromPlaying: Boolean) {
        quitFromPlaying = fromPlaying
        phase = Phase.CONFIRM_QUIT
        clearOverlay()
        overlay = world.container { confirmView(screenWidth, screenHeight, "QUIT GAME?") }
    }

    private fun cancelQuit() {
        if (quitFromPlaying) resumePlaying() else showIntro()
    }

    private fun confirmQuit() {
        phase = Phase.QUIT
        clearOverlay()
        overlay = world.container { goodbyeView(screenWidth, screenHeight) }
        quit()   // closes the window on desktop; browsers keep the tab on the goodbye screen
    }

    private fun showLeaderboard(
        header: String,
        entries: List<LeaderboardEntry>,
        highlight: LeaderboardEntry?,
        footer: String,
    ) {
        clearOverlay()
        val data = LeaderboardViewData(screenWidth, screenHeight, header, entries, highlight, footer)
        overlay = world.container { leaderboardView(data) }
    }

    private fun clearOverlay() {
        overlay?.removeFromParent()
        overlay = null
        nameField = null
    }

    private fun quit() {
        views.gameWindow.close(0)
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

    private fun refreshHud() = hud.update(state, elapsedMs.toInt())

    /** Show a brief, colored status message centered in the play area. It fades on its own. */
    private fun flash(text: String, color: RGBA) {
        message.text = text
        message.color = color
        message.recenterAt((hudWidth + screenWidth) / 2)
        message.y = screenHeight / 2
        messageTimer = FLASH_SECONDS
    }

    /** Count down the active flash message and clear it when its time is up. */
    private fun tickMessage(seconds: Double) {
        if (messageTimer <= 0) return
        messageTimer -= seconds
        if (messageTimer <= 0) message.text = ""
    }

    private companion object {
        const val FLASH_SECONDS = 1.1
        const val INTRO_INPUT_DELAY = 0.5
        const val SPEED_RAMP_PER_SECOND = 0.8   // balloons gain this many px/s for each second played
        const val MIN_SPEED_FACTOR = 0.85       // per-balloon speed spread, kept modest
        const val MAX_SPEED_FACTOR = 1.2
    }
}
