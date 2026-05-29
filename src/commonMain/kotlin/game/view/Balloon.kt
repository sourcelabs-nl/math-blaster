package game.view

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.View
import korlibs.korge.view.align.centerOn
import korlibs.korge.view.circle
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.korge.view.xy

/**
 * Builds a balloon-shaped falling number at column [x] (its top starts just above the screen)
 * and returns the container view. Positive numbers are cyan, negatives neon magenta.
 */
fun Container.balloon(value: Int, size: Double, x: Double): View {
    val radius = size / 2
    val color: RGBA = if (value >= 0) RetroTheme.cyan else RetroTheme.magenta
    val label = if (value >= 0) "+$value" else "$value"
    val labelSize = if (label.length >= 4) 9.0 else 13.0   // shrink so big Level 2 numbers fit the balloon
    return container {
        val body = circle(radius, color)
        solidRect(2.0, 14.0, color).xy(radius - 1.0, size)   // little string under the balloon
        text(label, textSize = labelSize, color = RetroTheme.space, font = RetroTheme.font) { centerOn(body) }
        xy(x, -size)
    }
}
