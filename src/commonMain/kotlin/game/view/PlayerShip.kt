package game.view

import korlibs.korge.view.Container
import korlibs.korge.view.View
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy

/**
 * A chunky pixel-art arcade fighter built from solid blocks (no sprite asset), pointing up. It
 * fits a [width] x [height] box anchored at its top-left, so the scene can keep using plain
 * x/y/width/height for movement and collision. Default size matches the scene's player box.
 */
fun Container.playerShip(width: Double = 40.0, height: Double = 28.0): View = container {
    val u = width / 10        // pixel unit; the ship is laid out on a 10-wide grid
    val hull = RetroTheme.cyan
    val trim = RetroTheme.text
    val thruster = RetroTheme.magenta

    solidRect(width, 3 * u, hull).xy(0.0, height - 3 * u)        // swept wings across the bottom
    solidRect(2 * u, 2 * u, hull).xy(0.0, height - 5 * u)        // left wingtip rising
    solidRect(2 * u, 2 * u, hull).xy(width - 2 * u, height - 5 * u)
    solidRect(3 * u, height, hull).xy(3.5 * u, 0.0)             // central body, full height
    solidRect(u, 2 * u, trim).xy(4.5 * u, 0.0)                  // bright nose tip
    solidRect(2 * u, 2 * u, RetroTheme.green).xy(4 * u, 3 * u)  // cockpit
    solidRect(u, 1.5 * u, thruster).xy(u, height - 1.5 * u)     // engine glow, left and right
    solidRect(u, 1.5 * u, thruster).xy(width - 2 * u, height - 1.5 * u)
}
