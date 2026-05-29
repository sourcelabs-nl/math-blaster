package game.logic

/** Format a millisecond duration as seconds with one decimal, e.g. 12345 -> "12.3s". */
fun formatTime(millis: Int): String {
    val tenths = millis / 100
    return "${tenths / 10}.${tenths % 10}s"
}
