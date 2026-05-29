package game.view

import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.DurationUnit

/** One drifting star: nearer stars (higher depth) are bigger, brighter, and fall faster. */
private class Star(
    val view: SolidRect,
    val speed: Double,
    val baseAlpha: Double,
    val twinkleRate: Double,
    val phase: Double,
)

/**
 * A drifting, twinkling starfield for the deep-space backdrop. It animates itself every frame
 * (parallax drift downward plus a sine twinkle) and recycles stars to the top as they fall off.
 */
fun Container.starfield(width: Double, height: Double, count: Int = 90): Container = container {
    val random = Random.Default
    val stars = List(count) {
        val depth = random.nextDouble()
        val size = 1.0 + depth * 1.5
        val star = solidRect(size, size, RetroTheme.star)
            .xy(random.nextDouble(width), random.nextDouble(height))
        Star(
            view = star,
            speed = 8.0 + depth * 42.0,
            baseAlpha = 0.25 + depth * 0.65,
            twinkleRate = 1.5 + random.nextDouble() * 3.0,
            phase = random.nextDouble() * 2 * PI,
        )
    }
    var time = 0.0
    addUpdater { dt ->
        val seconds = dt.toDouble(DurationUnit.SECONDS)
        time += seconds
        for (star in stars) {
            star.view.y += star.speed * seconds
            if (star.view.y > height) {
                star.view.y = 0.0
                star.view.x = random.nextDouble(width)
            }
            star.view.alpha = (star.baseAlpha * (0.55 + 0.45 * sin(time * star.twinkleRate + star.phase)))
                .coerceIn(0.0, 1.0)
        }
    }
}
