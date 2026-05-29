package game.view

import game.logic.formatTime
import game.model.GameState
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.korge.view.xy

/**
 * The left sidebar: a big TOTAL (accumulator) and TARGET, plus lives, score and the run timer,
 * all in the arcade pixel font. Balloons never spawn over this column, so it stays readable.
 */
class Hud(container: Container, width: Double, height: Double) {

    private val accView: Text
    private val targetView: Text
    private val statsView: Text
    private val timeView: Text

    init {
        with(container) {
            solidRect(width, height, RetroTheme.panel)
            solidRect(2.0, height, RetroTheme.cyan).xy(width - 2, 0.0)   // glowing divider
            text("TOTAL", textSize = 11.0, color = RetroTheme.dim, font = RetroTheme.font).xy(14, 14)
            accView = text("", textSize = 40.0, color = RetroTheme.green, font = RetroTheme.font).xy(14, 40)
            text("TARGET", textSize = 11.0, color = RetroTheme.dim, font = RetroTheme.font).xy(14, 150)
            targetView = text("", textSize = 34.0, color = RetroTheme.amber, font = RetroTheme.font).xy(14, 176)
            statsView = text("", textSize = 10.0, color = RetroTheme.text, font = RetroTheme.font).xy(14, 280)
            timeView = text("", textSize = 11.0, color = RetroTheme.cyan, font = RetroTheme.font).xy(14, 350)
        }
    }

    fun update(state: GameState, timeMillis: Int) {
        accView.text = "${state.accumulator}"
        targetView.text = "${state.target}"
        statsView.text = "LIVES  ${state.lives}\nSCORE  ${state.score}/25"
        timeView.text = "TIME ${formatTime(timeMillis)}"
    }
}
