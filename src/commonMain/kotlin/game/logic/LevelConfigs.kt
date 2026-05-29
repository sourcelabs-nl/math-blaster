package game.logic

import game.model.LevelConfig

/**
 * The fixed set of difficulty levels. Level 1 is small signed numbers. Level 2 keeps the same
 * "add up to the target" loop but with larger denomination-style numbers, positive and negative,
 * larger targets, and a faster pace. Pure data, so it is covered by fast unit tests.
 */
object LevelConfigs {

    /** Rounds cleared in Level 1 before Level 2 begins. */
    const val ROUNDS_TO_LEVEL_2 = 2

    private val LEVEL_1 = LevelConfig(
        level = 1,
        valueMagnitudes = (1..10).toList(),
        targetPool = (5..20).toList(),
        negativeChance = 0.4,
        baseEnemySpeed = 50.0,
        maxEnemySpeed = 95.0,
        spawnInterval = 1.6,
    )

    private val LEVEL_2 = LevelConfig(
        level = 2,
        valueMagnitudes = listOf(5, 10, 25, 50, 100, 250, 500),
        targetPool = (50..500 step 5).toList(),
        negativeChance = 0.4,
        baseEnemySpeed = 68.0,
        maxEnemySpeed = 120.0,
        spawnInterval = 1.2,
    )

    fun configFor(level: Int): LevelConfig = if (level >= 2) LEVEL_2 else LEVEL_1

    /** Level 1 until [ROUNDS_TO_LEVEL_2] rounds are cleared, then Level 2. */
    fun levelForRoundsCleared(roundsCleared: Int): Int =
        if (roundsCleared >= ROUNDS_TO_LEVEL_2) 2 else 1
}
