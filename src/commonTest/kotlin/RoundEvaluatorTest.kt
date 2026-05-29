import game.logic.RoundEvaluator
import game.logic.ShotOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

class RoundEvaluatorTest {

    @Test
    fun exactHitClearsTheRound() {
        val result = RoundEvaluator.applyShot(accumulator = 4, target = 8, value = 4)
        assertEquals(ShotOutcome.CLEARED, result.outcome)
        assertEquals(8, result.accumulator)
    }

    @Test
    fun overshootContinuesAndKeepsTheAccumulator() {
        val result = RoundEvaluator.applyShot(accumulator = 9, target = 11, value = 3)
        assertEquals(ShotOutcome.CONTINUE, result.outcome)
        assertEquals(12, result.accumulator)   // overshoot to 12; a -1 shot would then clear
    }

    @Test
    fun partialProgressContinues() {
        val result = RoundEvaluator.applyShot(accumulator = 0, target = 8, value = 4)
        assertEquals(ShotOutcome.CONTINUE, result.outcome)
        assertEquals(4, result.accumulator)
    }

    @Test
    fun negativeShotCanCorrectTowardTarget() {
        val result = RoundEvaluator.applyShot(accumulator = 9, target = 8, value = -1)
        assertEquals(ShotOutcome.CLEARED, result.outcome)
        assertEquals(8, result.accumulator)
    }
}
