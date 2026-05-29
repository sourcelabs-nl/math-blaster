package game.logic

import game.model.LevelConfig
import kotlin.random.Random

/** Pure generation of the numbers used by the game. No KorGE types, so it is unit-tested. */
object NumberGenerator {

    /** A shootable value: a magnitude from the level's set, made negative [negativeChance] of the time. */
    fun randomValue(config: LevelConfig, random: Random = Random.Default): Int {
        val magnitude = config.valueMagnitudes.random(random)
        return if (random.nextDouble() < config.negativeChance) -magnitude else magnitude
    }

    /** A target to accumulate to, drawn from the level's pool of solvable targets. */
    fun randomTarget(config: LevelConfig, random: Random = Random.Default): Int =
        config.targetPool.random(random)
}
