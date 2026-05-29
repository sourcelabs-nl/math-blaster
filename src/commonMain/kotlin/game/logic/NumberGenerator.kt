package game.logic

import kotlin.random.Random

/** Pure generation of the numbers used by the game. No KorGE types, so it is unit-tested. */
object NumberGenerator {

    /** A shootable value: 60% positive (1..10), 40% negative (-10..-1). Never zero. */
    fun randomValue(random: Random = Random.Default): Int =
        if (random.nextDouble() < 0.6) random.nextInt(1, 11) else -random.nextInt(1, 11)

    /** A target to accumulate to. Any positive target is solvable with the available numbers. */
    fun randomTarget(random: Random = Random.Default): Int = random.nextInt(5, 21)
}
