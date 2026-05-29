package game.audio

import korlibs.audio.sound.AudioData
import korlibs.audio.sound.AudioSample
import korlibs.audio.sound.AudioSamples

/**
 * Tiny software synthesizer shared by [Sfx] and [Music], so the game ships with zero audio
 * asset files: every sound is generated from waveforms at startup. All helpers are pure math.
 */
// 48 kHz matches the Web Audio context rate used by current browsers. Matching it avoids KorGE's
// WasmJS resampler, which otherwise reads one sample past the buffer end on every audio callback
// (a flood of "array element access out of bounds" runtime errors while any sound plays).
internal const val SAMPLE_RATE = 48000

// Stereo on purpose: KorGE's WasmJS audio mixer reads two channels, so a mono buffer makes its
// ScriptProcessorNode callback read out of bounds on every frame. We write the same value to both.
private const val CHANNELS = 2

/** Build a stereo [AudioData] of [durationMs] from a per-sample amplitude in -1f..1f. */
internal fun synth(durationMs: Int, amplitude: (t: Double) -> Float): AudioData {
    val count = SAMPLE_RATE * durationMs / 1000
    val samples = AudioSamples(CHANNELS, count)
    for (i in 0 until count) {
        val t = i.toDouble() / SAMPLE_RATE
        val sample = AudioSample(amplitude(t).coerceIn(-1f, 1f))
        samples[0, i] = sample
        samples[1, i] = sample
    }
    return AudioData(SAMPLE_RATE, samples)
}

/** Wrap a ready-made amplitude buffer (one float per sample) as stereo [AudioData]. */
internal fun bufferToAudio(buffer: FloatArray): AudioData {
    val samples = AudioSamples(CHANNELS, buffer.size)
    for (i in buffer.indices) {
        val sample = AudioSample(buffer[i].coerceIn(-1f, 1f))
        samples[0, i] = sample
        samples[1, i] = sample
    }
    return AudioData(SAMPLE_RATE, samples)
}

/** Linear frequency glide from [fromHz] to [toHz] across [overMs], then held at [toHz]. */
internal fun sweep(t: Double, fromHz: Double, toHz: Double, overMs: Double): Double {
    val progress = (t / (overMs / 1000.0)).coerceAtMost(1.0)
    return fromHz + (toHz - fromHz) * progress
}

/** Exponential fade-out envelope: 1.0 at t=0, decaying faster for larger [rate]. */
internal fun decay(t: Double, rate: Double): Float = kotlin.math.exp(-t * rate).toFloat()
