import game.logic.NumberGenerator
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class NumberGeneratorTest {

    @Test
    fun valuesAreNeverZeroAndWithinRange() {
        val random = Random(42)
        repeat(1000) {
            val value = NumberGenerator.randomValue(random)
            assertTrue(value != 0, "value must never be zero")
            assertTrue(value in -10..10, "value $value out of range")
        }
    }

    @Test
    fun targetsAreSolvablePositiveNumbers() {
        val random = Random(7)
        repeat(1000) {
            val target = NumberGenerator.randomTarget(random)
            assertTrue(target in 5..20, "target $target out of range")
        }
    }
}
