package game.logic

import game.model.LeaderboardEntry

/**
 * Pure leaderboard rules: ranking fastest runs and (de)serializing to a single string for
 * storage. No KorGE or storage types, so it is covered by fast unit tests.
 *
 * Entries are stored one per line as "timeMillis\tname". Names are restricted to letters and
 * spaces by the name-entry UI, so they never clash with the tab or newline separators.
 */
object Leaderboard {

    const val MAX_ENTRIES = 10

    /** The fastest [MAX_ENTRIES] runs after adding [entry], sorted ascending by time. */
    fun insert(entries: List<LeaderboardEntry>, entry: LeaderboardEntry): List<LeaderboardEntry> =
        (entries + entry).sortedBy { it.timeMillis }.take(MAX_ENTRIES)

    fun serialize(entries: List<LeaderboardEntry>): String =
        entries.joinToString("\n") { "${it.timeMillis}\t${it.name}" }

    fun deserialize(raw: String?): List<LeaderboardEntry> =
        raw?.lineSequence()?.mapNotNull(::parseLine)?.toList() ?: emptyList()

    private fun parseLine(line: String): LeaderboardEntry? {
        val (time, name) = line.split("\t", limit = 2).takeIf { it.size == 2 } ?: return null
        val millis = time.toIntOrNull() ?: return null
        return LeaderboardEntry(name, millis)
    }
}
