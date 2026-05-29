package game.view

import game.logic.formatTime
import game.model.LeaderboardEntry
import korlibs.korge.view.Container

/** Parameters for the full-screen leaderboard overlay. */
data class LeaderboardViewData(
    val width: Double,
    val height: Double,
    val header: String,
    val entries: List<LeaderboardEntry>,
    val highlight: LeaderboardEntry?,
    val footer: String,
)

/** Full-screen overlay: a header, the ranked fastest runs, and a footer hint. */
fun Container.leaderboardView(data: LeaderboardViewData) {
    dimSheet(data.width, data.height)
    val cx = data.width / 2
    centeredText(data.header, 24.0, RetroTheme.green, cx, data.height * 0.10)
    centeredText("FASTEST TO 25", 10.0, RetroTheme.dim, cx, data.height * 0.18)
    rows(data, cx)
    centeredText(data.footer, 10.0, RetroTheme.text, cx, data.height * 0.90)
}

private fun Container.rows(data: LeaderboardViewData, centerX: Double) {
    if (data.entries.isEmpty()) {
        centeredText("NO TIMES YET - BE THE FIRST!", 10.0, RetroTheme.text, centerX, data.height * 0.45)
        return
    }
    data.entries.forEachIndexed { index, entry ->
        val color = if (entry == data.highlight) RetroTheme.amber else RetroTheme.text
        val rank = "${index + 1}".padStart(2)
        val line = "$rank ${entry.name.padEnd(8)} ${formatTime(entry.timeMillis)}"
        centeredText(line, 12.0, color, centerX, data.height * 0.26 + index * 24)
    }
}
