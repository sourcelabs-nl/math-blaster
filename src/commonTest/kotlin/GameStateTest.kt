import game.logic.ShotOutcome
import game.model.GameState
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameStateTest {

    @Test
    fun exactShotClearsAndScores() {
        val state = GameState(random = Random(1))
        val target = state.target
        assertEquals(ShotOutcome.CLEARED, state.applyShot(target))
        assertEquals(5, state.score)
        assertEquals(0, state.accumulator)
    }

    @Test
    fun overshootContinuesWithoutCostingALife() {
        val state = GameState(random = Random(1))
        val target = state.target
        assertEquals(ShotOutcome.CONTINUE, state.applyShot(target + 1))
        assertEquals(3, state.lives)
        assertEquals(target + 1, state.accumulator)
    }

    @Test
    fun partialShotContinues() {
        val state = GameState(random = Random(1))
        assertEquals(ShotOutcome.CONTINUE, state.applyShot(1))   // target is always >= 5
        assertEquals(1, state.accumulator)
    }

    @Test
    fun winsAfterClearingFiveRounds() {
        val state = GameState(random = Random(1))
        repeat(5) { state.applyShot(state.target) }   // each cleared round adds 5 points
        assertEquals(25, state.score)
        assertTrue(state.hasWon)
    }

    @Test
    fun losingTheLastLifeEndsTheGame() {
        val state = GameState(startingLives = 1, random = Random(1))
        state.loseLife()
        assertTrue(state.isGameOver)
    }
}
