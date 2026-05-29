package game.view

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.Text
import korlibs.korge.view.View
import korlibs.korge.view.addUpdater
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import kotlin.time.DurationUnit

/** A translucent black sheet that dims the running game behind a full-screen overlay. */
fun Container.dimSheet(width: Double, height: Double): SolidRect =
    solidRect(width, height, Colors.BLACK).also { it.alpha = 0.82 }

/** A line of arcade-font text centered horizontally on [centerX], with its top at [y]. */
fun Container.centeredText(message: String, size: Double, color: RGBA, centerX: Double, y: Double): Text =
    text(message, textSize = size, color = color, font = RetroTheme.font)
        .also { it.recenterAt(centerX); it.y = y }

/** Re-center a (possibly changed) text on [centerX]. Use after updating [Text.text]. */
fun Text.recenterAt(centerX: Double): Text = also { it.x = centerX - it.width / 2 }

/** Toggle the view on and off on a fixed cadence: a retro blinking-text effect. */
fun <T : View> T.blink(onSeconds: Double = 0.6, offSeconds: Double = 0.3): T = apply {
    val cycle = onSeconds + offSeconds
    var elapsed = 0.0
    addUpdater { dt ->
        elapsed = (elapsed + dt.toDouble(DurationUnit.SECONDS)) % cycle
        visible = elapsed < onSeconds
    }
}

/** A yes/no confirmation overlay: a [question] with the Y / N key choices. */
fun Container.confirmView(width: Double, height: Double, question: String) {
    dimSheet(width, height)
    val cx = width / 2
    centeredText(question, 28.0, RetroTheme.green, cx, height * 0.40)
    centeredText("Y = YES      N = NO", 14.0, RetroTheme.text, cx, height * 0.52)
}

/** A terminal goodbye overlay shown after the player confirms quitting. */
fun Container.goodbyeView(width: Double, height: Double) {
    dimSheet(width, height)
    centeredText("THANKS FOR PLAYING!", 24.0, RetroTheme.amber, width / 2, height * 0.45)
}
