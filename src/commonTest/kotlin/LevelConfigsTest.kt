import game.logic.LevelConfigs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LevelConfigsTest {

    @Test
    fun startsOnLevelOne() {
        assertEquals(1, LevelConfigs.levelForRoundsCleared(0))
    }

    @Test
    fun advancesToLevelTwoAfterEnoughRounds() {
        assertEquals(2, LevelConfigs.levelForRoundsCleared(LevelConfigs.ROUNDS_TO_LEVEL_2))
    }

    @Test
    fun levelTwoUsesLargerNumbersAndFasterPaceThanLevelOne() {
        val one = LevelConfigs.configFor(1)
        val two = LevelConfigs.configFor(2)
        assertTrue(two.valueMagnitudes.max() > one.valueMagnitudes.max(), "level 2 numbers should be larger")
        assertTrue(two.baseEnemySpeed >= one.baseEnemySpeed, "level 2 should not be slower")
        assertTrue(two.spawnInterval <= one.spawnInterval, "level 2 should not spawn slower")
    }
}
