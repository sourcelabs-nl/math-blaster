package game.logic

/**
 * Pure game-rule logic: no KorGE types, so it is covered by fast unit tests.
 *
 * The player shoots signed numbers that add to a running [accumulator]. Landing on the
 * [target] exactly clears the round. Overshooting is allowed and costs nothing: shoot a
 * negative number to come back down toward the target.
 */
enum class ShotOutcome { CLEARED, CONTINUE }

data class ShotResult(val outcome: ShotOutcome, val accumulator: Int)

object RoundEvaluator {

    /**
     * Apply a shot of [value] to the current [accumulator] aiming for [target].
     *
     * - [CLEARED] when the new accumulator equals the target.
     * - [CONTINUE] otherwise, including overshoot: the accumulator simply keeps the new value.
     */
    fun applyShot(accumulator: Int, target: Int, value: Int): ShotResult {
        val next = accumulator + value
        val outcome = if (next == target) ShotOutcome.CLEARED else ShotOutcome.CONTINUE
        return ShotResult(outcome, next)
    }
}
