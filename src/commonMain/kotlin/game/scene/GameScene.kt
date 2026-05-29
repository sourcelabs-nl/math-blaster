package game.scene

import game.audio.Music
import game.audio.Sfx
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
import game.view.confirmView
import game.view.goodbyeView
import game.view.introView
import game.view.leaderboardView
import game.view.nameEntryView
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
import korlibs.korge.view.Text
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.text
import korlibs.korge.view.xy
import kotlin.time.DurationUnit

/**
 * Wires the game together: the intro, input routing, the per-frame loop, and the
 * win/leaderboard flow. The live simulation (ship, bullets, falling numbers, collisions) lives
 * in [Playfield]; rules live in [GameState], rendering in the view package, audio in [Sfx],
 * persistence in [LeaderboardStorage]. This scene only orchestrates them through [phase].
 */
class GameScene : Scene() {

    // Named screenWidth/screenHeight (not width/height) on purpose: inside a `container { }`
    // builder the receiver is a View, whose own `width`/`height` would shadow these fields.
    private val screenWidth = 960.0
    private val screenHeight = 600.0
    private val hudWidth = 150.0          // left column reserved for the HUD (no balloons here)
    private val maxNameLength = 8

    private val state = GameState()
    private var messageTimer = 0.0
    private var introCooldown = 0.0       // briefly ignores input so a stray load event can't auto-start
    private var quitFromPlaying = false   // where to return if the player cancels the quit prompt
    private var phase = Phase.INTRO
    private var typedName = ""
    private var shownLevel = 1            // last level announced, so the level-up flash fires once
    private var muted = false
    private var tapStartedPlaying = false // a tap begun while playing fires the gun on release

    private lateinit var world: SContainer
    private lateinit var sfx: Sfx
    private lateinit var music: Music
    private lateinit var hud: Hud
    private lateinit var playfield: Playfield
    private lateinit var message: Text
    private lateinit var hint: Text
    private lateinit var leaderboard: LeaderboardStorage
    private var overlay: Container? = null
    private var nameField: Text? = null

    private enum class Phase {
        INTRO, PLAYING, PAUSED, ENTERING_NAME, FINISHED, LEADERBOARD, CONFIRM_QUIT, QUIT,
    }

    override suspend fun SContainer.sceneMain() {
        world = this
        RetroTheme.load()
        sfx = Sfx.create()
        music = Music.create()
        leaderboard = LeaderboardStorage(views.storage)
        starfield(screenWidth, screenHeight)
        hud = Hud(this, hudWidth, screenHeight)
        playfield = Playfield(world, PlayArea(screenWidth, screenHeight, hudWidth), state, sfx, ::react, ::onShipHit)
        message = text("", textSize = 20.0, color = RetroTheme.amber, font = RetroTheme.font)
        hint = text("Q QUIT\nL LEADERBOARD\nM MUTE", textSize = 8.0, color = RetroTheme.dim, font = RetroTheme.font)
            .xy(14, screenHeight - 52)   // tucked into the bottom of the left HUD bar
        refreshHud()
        showIntro()
        introCooldown = INTRO_INPUT_DELAY   // only on first load: ignore a stray pointer/key event

        keys {
            justDown(Key.SPACE) { if (phase == Phase.PLAYING) playfield.fire() }
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
        val keys = views.input.keys
        playfield.update(seconds, keys[Key.LEFT], keys[Key.RIGHT])
        tickMessage(seconds)
        refreshHud()
    }

    private fun onPointerDown(event: MouseEvents) {
        if (phase != Phase.PLAYING || introCooldown > 0) return
        tapStartedPlaying = true
        playfield.aimAt(event.currentPosLocal.x)
    }

    private fun onPointerMove(event: MouseEvents) {
        if (phase == Phase.PLAYING) playfield.aimAt(event.currentPosLocal.x)
    }

    private fun onPointerUp() {
        if (tapStartedPlaying && phase == Phase.PLAYING) playfield.fire()
        tapStartedPlaying = false
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

    /** Lose a life when a number reaches the ship. Returns true if the run continues. */
    private fun onShipHit(): Boolean {
        state.loseLife()
        if (state.isGameOver) { onGameOver(); refreshHud(); return false }
        flash("HIT! -1 LIFE", RetroTheme.magenta)
        refreshHud()
        return true
    }

    private fun onWin() {
        phase = Phase.ENTERING_NAME
        typedName = ""
        message.text = ""
        clearOverlay()
        overlay = world.container { nameField = nameEntryView(screenWidth, screenHeight, formatTime(playfield.elapsedMs.toInt())) }
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
        val entry = LeaderboardEntry(name, playfield.elapsedMs.toInt())
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
        playfield.reset()
        shownLevel = 1
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

    private fun refreshHud() = hud.update(state, playfield.elapsedMs.toInt())

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
    }
}
