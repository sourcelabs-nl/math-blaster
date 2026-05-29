import game.logic.LevelConfigs
import game.logic.NumberGenerator
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class NumberGeneratorTest {

    @Test
    fun level1ValuesAreNeverZeroAndWithinRange() {
        val config = LevelConfigs.configFor(1)
        val random = Random(42)
        repeat(1000) {
            val value = NumberGenerator.randomValue(config, random)
            assertTrue(value != 0, "value must never be zero")
            assertTrue(value in -10..10, "value $value out of range")
        }
    }

    @Test
    fun level1TargetsAreSolvablePositiveNumbers() {
        val config = LevelConfigs.configFor(1)
        val random = Random(7)
        repeat(1000) {
            val target = NumberGenerator.randomTarget(config, random)
            assertTrue(target in 5..20, "target $target out of range")
        }
    }

    @Test
    fun level2UsesLargeDenominationNumbers() {
        val config = LevelConfigs.configFor(2)
        val allowed = setOf(5, 10, 25, 50, 100, 250, 500)
        val random = Random(99)
        repeat(1000) {
            val value = NumberGenerator.randomValue(config, random)
            assertTrue(abs(value) in allowed, "value $value not a level 2 denomination")
        }
    }

    @Test
    fun level2TargetsAreSolvableMultiplesOfFive() {
        val config = LevelConfigs.configFor(2)
        val random = Random(123)
        repeat(1000) {
            val target = NumberGenerator.randomTarget(config, random)
            assertTrue(target in 50..500 && target % 5 == 0, "target $target not a solvable level 2 target")
        }
    }
}
