package game.view

import korlibs.korge.view.Container
import korlibs.korge.view.Text

/**
 * Win overlay: shows the finishing time and a live field for the player's name. Returns the
 * [Text] that displays the typed name so the scene can update it as keys are pressed.
 */
fun Container.nameEntryView(width: Double, height: Double, timeText: String): Text {
    dimSheet(width, height)
    val cx = width / 2
    centeredText("YOU WIN!", 28.0, RetroTheme.green, cx, height * 0.20)
    centeredText("TIME $timeText", 16.0, RetroTheme.amber, cx, height * 0.32)
    centeredText("TYPE YOUR NAME, THEN ENTER", 9.0, RetroTheme.text, cx, height * 0.46)
    return centeredText("_", 22.0, RetroTheme.cyan, cx, height * 0.54)
}
