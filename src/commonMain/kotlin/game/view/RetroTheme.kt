package game.view

import korlibs.image.color.Colors
import korlibs.image.font.Font
import korlibs.image.font.readTtfFont
import korlibs.io.file.std.resourcesVfs

/**
 * The shared retro look: one chunky arcade pixel font (Press Start 2P) plus a limited
 * neon-on-black palette. Loaded once via [load] at startup, then read by every view builder so
 * the whole game speaks the same 8-bit visual language.
 */
object RetroTheme {

    lateinit var font: Font
        private set

    suspend fun load() {
        font = resourcesVfs["fonts/PressStart2P.ttf"].readTtfFont()
    }

    // Deep-space background, the same tone the HTML page paints behind the canvas.
    val space = Colors["#05060f"]
    val panel = Colors["#0b0e22"]        // HUD sidebar, slightly lifted from the void

    // Phosphor-bright foreground colors, kept few on purpose.
    val green = Colors["#39ff14"]        // the accumulator / "go" color
    val amber = Colors["#ffd166"]        // the target / highlight color
    val cyan = Colors["#4cc9f0"]         // ship, timers, positive numbers
    val magenta = Colors["#ff3b8d"]      // danger / negative numbers
    val dim = Colors["#7f86a8"]          // labels and secondary text
    val text = Colors["#e6e9ff"]         // primary readable text
    val star = Colors["#cfd6ff"]         // starfield
}
