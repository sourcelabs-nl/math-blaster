package game.audio

import korlibs.audio.sound.Sound
import korlibs.audio.sound.SoundChannel
import korlibs.audio.sound.toSound
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * A synthesized, looping chiptune: a driving square-wave lead over a steady square bass, in A
 * minor. Like [Sfx] it ships as code, not an asset file. Build once with [create] from a suspend
 * context, then [start] it after the first user gesture (browsers block audio until then).
 */
class Music private constructor(private val loop: Sound) {

    private var channel: SoundChannel? = null
    private var muted = false

    /** Begin looping forever. Safe to call repeatedly: only the first call starts playback. */
    fun start() {
        if (channel == null) channel = loop.playNoCancelForever()
        applyVolume()
    }

    /** Silence or restore the music without stopping the loop, so unmuting resumes in place. */
    fun setMuted(muted: Boolean) {
        this.muted = muted
        applyVolume()
    }

    private fun applyVolume() {
        channel?.volume = if (muted) 0.0 else 1.0
    }

    companion object {
        suspend fun create(): Music = Music(bufferToAudio(chiptune()).toSound())
    }
}

private const val BPM = 132.0
private val BEAT = 60.0 / BPM      // one quarter note, in seconds
private val EIGHTH = BEAT / 2

// Notes are semitone offsets from A4 (440 Hz); null is a rest. Four bars of A minor: Am F C E.
private val LEAD: List<Int?> = listOf(
    0, 3, 7, 3, 0, 3, 7, 10,        // Am arpeggio, climbing
    5, 8, 12, 8, 7, 3, 7, 3,        // F / Am
    0, 3, 7, 3, -2, 2, 5, 2,        // Am / Dm color
    7, 3, 0, 3, 7, 10, 12, null,    // E turnaround, rest to breathe before the loop
)
private val BASS: List<Int?> = listOf(
    -24, -24, -24, -24,             // A
    -16, -16, -16, -16,             // F
    -21, -21, -21, -21,             // C
    -17, -17, -17, -17,             // E
)

/** Render the lead and bass tracks and mix them into one loopable buffer. */
private fun chiptune(): FloatArray {
    val lead = renderTrack(LEAD, EIGHTH, volume = 0.22f, decayRate = 5.0)
    val bass = renderTrack(BASS, BEAT, volume = 0.20f, decayRate = 2.0)
    val length = maxOf(lead.size, bass.size)
    return FloatArray(length) { i -> lead.getOrElse(i) { 0f } + bass.getOrElse(i) { 0f } }
}

/** A square-wave voice: each note is a plucked tone with a fast attack and an exponential decay. */
private fun renderTrack(notes: List<Int?>, noteSeconds: Double, volume: Float, decayRate: Double): FloatArray {
    val perNote = (SAMPLE_RATE * noteSeconds).toInt()
    val buffer = FloatArray(perNote * notes.size)
    notes.forEachIndexed { index, semitone ->
        if (semitone == null) return@forEachIndexed
        val hz = noteHz(semitone)
        val start = index * perNote
        for (i in 0 until perNote) {
            val t = i.toDouble() / SAMPLE_RATE
            val square = if (sin(2 * PI * hz * t) >= 0) 1f else -1f
            buffer[start + i] = square * attack(t) * decay(t, decayRate) * volume
        }
    }
    return buffer
}

/** Equal-temperament frequency for a semitone offset from A4 (440 Hz). */
private fun noteHz(semitone: Int): Double = 440.0 * 2.0.pow(semitone / 12.0)

/** A 4 ms linear ramp from silence, so note onsets do not click. */
private fun attack(t: Double): Float = (t / 0.004).coerceAtMost(1.0).toFloat()
