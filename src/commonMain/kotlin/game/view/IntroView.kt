package game.view

import game.logic.formatTime
import game.model.LeaderboardEntry
import korlibs.image.color.RGBA
import korlibs.korge.input.onClick
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy

/** Buttons the pre-game intro offers. ENTER triggers [onOk]. */
class IntroActions(val onOk: () -> Unit, val onQuit: () -> Unit)

/** Everything the pre-game overlay needs: screen size, the first target, best times, buttons. */
data class IntroViewData(
    val width: Double,
    val height: Double,
    val target: Int,
    val entries: List<LeaderboardEntry>,
    val actions: IntroActions,
)

/**
 * Pre-game overlay laid out in two columns: the briefing and buttons on the left, the
 * best-times leaderboard on the right.
 */
fun Container.introView(data: IntroViewData) {
    dimSheet(data.width, data.height)
    centeredText("MATH BLASTER", 28.0, RetroTheme.green, data.width / 2, 50.0)
    briefing(data)
    leaderboardColumn(data)
}

private fun Container.briefing(data: IntroViewData) {
    val cx = data.width * 0.34
    centeredText("BLAST TILL YOU REACH", 16.0, RetroTheme.dim, cx, 150.0)   // aligned with the LEADERBOARD title
    centeredText("${data.target}", 60.0, RetroTheme.amber, cx, 195.0).blink()
    centeredText("SHOOT NUMBERS TO ADD THEM UP", 9.0, RetroTheme.text, cx, 300.0)
    centeredText("NEGATIVES CORRECT AN OVERSHOOT", 9.0, RetroTheme.cyan, cx, 325.0)
    centeredText("ARROWS MOVE    SPACE FIRES", 9.0, RetroTheme.text, cx, 350.0)
    centeredText("ON TOUCH: DRAG TO MOVE    TAP TO FIRE", 9.0, RetroTheme.dim, cx, 370.0)
    button("START", RetroTheme.green, cx, 405.0, data.actions.onOk)
    button("QUIT", RetroTheme.magenta, cx, 470.0, data.actions.onQuit)
}

private fun Container.leaderboardColumn(data: IntroViewData) {
    val cx = data.width * 0.72
    centeredText("LEADERBOARD", 16.0, RetroTheme.dim, cx, 150.0)
    if (data.entries.isEmpty()) {
        centeredText("NO TIMES YET", 11.0, RetroTheme.text, cx, 200.0)
        return
    }
    data.entries.forEachIndexed { index, entry ->
        val rank = "${index + 1}".padStart(2)
        val line = "$rank ${entry.name.padEnd(8)} ${formatTime(entry.timeMillis)}"
        centeredText(line, 11.0, RetroTheme.text, cx, 195.0 + index * 26)
    }
}

private fun Container.button(label: String, color: RGBA, centerX: Double, y: Double, onPress: () -> Unit) {
    val w = 150.0
    val h = 44.0
    container {
        solidRect(w, h, color)
        val text = centeredText(label, 16.0, RetroTheme.space, w / 2, h / 2 - 8)
        underlineFirstLetter(text, label.length)
        xy(centerX - w / 2, y)
        onClick { onPress() }
    }
}

/** Underline just the first glyph: the retro way to mark its keyboard shortcut (O, Q). */
private fun Container.underlineFirstLetter(label: Text, charCount: Int) {
    val glyphWidth = label.width / charCount
    solidRect(glyphWidth * 0.75, 3.0, RetroTheme.space).xy(label.x + glyphWidth * 0.125, label.y + 18)
}
