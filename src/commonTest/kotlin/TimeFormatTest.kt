import game.logic.formatTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeFormatTest {

    @Test
    fun formatsMillisAsSecondsWithOneDecimal() {
        assertEquals("0.0s", formatTime(0))
        assertEquals("12.3s", formatTime(12345))
        assertEquals("9.0s", formatTime(9000))
    }
}
