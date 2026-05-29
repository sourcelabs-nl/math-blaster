package game.storage

import game.logic.Leaderboard
import game.model.LeaderboardEntry
import korlibs.korge.service.storage.IStorage

/**
 * Persists the leaderboard to KorGE local storage (the only persistence the competition allows).
 * All ranking lives in the pure [Leaderboard]; this class just reads and writes the string.
 */
class LeaderboardStorage(private val storage: IStorage) {

    fun load(): List<LeaderboardEntry> = Leaderboard.deserialize(storage.getOrNull(KEY))

    /** Add [entry], persist the trimmed top list, and return it for display. */
    fun save(entry: LeaderboardEntry): List<LeaderboardEntry> =
        Leaderboard.insert(load(), entry).also { storage[KEY] = Leaderboard.serialize(it) }

    private companion object {
        const val KEY = "leaderboard"
    }
}
