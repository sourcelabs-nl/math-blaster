package game.model

import game.logic.NumberGenerator
import game.logic.RoundEvaluator
import game.logic.ShotOutcome
import kotlin.random.Random

/**
 * Pure, mutable game state and the rules that change it. No KorGE types, so the scoring,
 * busting, and target logic are covered by fast unit tests.
 */
class GameState(
    private val startingLives: Int = 3,
    private val random: Random = Random.Default,
) {
    var accumulator = 0; private set
    var target = 0; private set
    var lives = startingLives; private set
    var score = 0; private set

    val isGameOver: Boolean get() = lives <= 0

    /** True once enough rounds are cleared to win: the timed run is complete. */
    val hasWon: Boolean get() = score >= WIN_SCORE

    init {
        newTarget()
    }

    /** Apply a shot of [value] to the accumulator and return what happened. */
    fun applyShot(value: Int): ShotOutcome {
        val result = RoundEvaluator.applyShot(accumulator, target, value)
        accumulator = result.accumulator
        if (result.outcome == ShotOutcome.CLEARED) clearRound()
        return result.outcome
    }

    /** A falling number reached the ship. */
    fun loseLife() {
        lives--
    }

    fun reset() {
        accumulator = 0
        lives = startingLives
        score = 0
        newTarget()
    }

    private fun clearRound() {
        score += POINTS_PER_ROUND
        accumulator = 0
        newTarget()
    }

    private fun newTarget() {
        target = NumberGenerator.randomTarget(random)
    }

    private companion object {
        const val POINTS_PER_ROUND = 5
        const val WIN_SCORE = 25
    }
}
