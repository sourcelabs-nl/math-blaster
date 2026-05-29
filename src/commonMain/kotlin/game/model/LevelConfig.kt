package game.model

/**
 * Per-level rules gathered as a parameter object so a level's number set, target range, and
 * pacing travel together instead of as a long argument list. Pure data, no KorGE types.
 *
 * - [valueMagnitudes]: the unsigned values balloons can carry; a sign is applied per spawn.
 * - [targetPool]: the targets a round may pick, all solvable with [valueMagnitudes].
 * - [negativeChance]: how often a spawned value is negative.
 * - [baseEnemySpeed]/[maxEnemySpeed]: balloon fall speed at the start of a run and its cap.
 * - [spawnInterval]: seconds between balloon spawns.
 */
data class LevelConfig(
    val level: Int,
    val valueMagnitudes: List<Int>,
    val targetPool: List<Int>,
    val negativeChance: Double,
    val baseEnemySpeed: Double,
    val maxEnemySpeed: Double,
    val spawnInterval: Double,
)
