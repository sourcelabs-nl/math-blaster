import game.logic.Leaderboard
import game.model.LeaderboardEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class LeaderboardTest {

    @Test
    fun insertSortsByFastestTime() {
        val existing = listOf(LeaderboardEntry("ANA", 5000), LeaderboardEntry("BOB", 9000))
        val result = Leaderboard.insert(existing, LeaderboardEntry("CAT", 7000))
        assertEquals(listOf("ANA", "CAT", "BOB"), result.map { it.name })
    }

    @Test
    fun insertKeepsOnlyTheTopTen() {
        val full = (1..10).map { LeaderboardEntry("P$it", it * 1000) }
        val result = Leaderboard.insert(full, LeaderboardEntry("FAST", 500))
        assertEquals(10, result.size)
        assertEquals("FAST", result.first().name)
        assertEquals(false, result.any { it.timeMillis == 10000 })   // slowest dropped
    }

    @Test
    fun serializeRoundTrips() {
        val entries = listOf(LeaderboardEntry("ANA", 5000), LeaderboardEntry("BOB LEE", 9000))
        assertEquals(entries, Leaderboard.deserialize(Leaderboard.serialize(entries)))
    }

    @Test
    fun deserializeOfNullOrGarbageIsEmpty() {
        assertEquals(emptyList(), Leaderboard.deserialize(null))
        assertEquals(emptyList(), Leaderboard.deserialize("not-a-number\tname"))
    }
}
