package game.audio

import korlibs.audio.sound.AudioData
import korlibs.audio.sound.Sound
import korlibs.audio.sound.toSound
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Synthesized retro sound effects, so the game needs no audio asset files. Build once with
 * [create] from a suspend context, then trigger from anywhere (the plays are fire-and-forget).
 *
 * The waveforms are deliberately lo-fi: a buzzy square-wave laser and a noise-burst explosion,
 * the kind of crunchy blips an 8-bit arcade machine would make. See [synth] in Synth.kt.
 */
class Sfx private constructor(
    private val shootSound: Sound,
    private val popSound: Sound,
) {
    fun shoot() { shootSound.playNoCancel() }

    fun pop() { popSound.playNoCancel() }

    companion object {
        suspend fun create(): Sfx = Sfx(
            shootSound = laserBlast().toSound(),
            popSound = explosion().toSound(),
        )

        /** A descending square-wave "pew": classic arcade laser blast. */
        private fun laserBlast(): AudioData = synth(durationMs = 130) { t ->
            val freq = sweep(t, fromHz = 1200.0, toHz = 300.0, overMs = 130.0)
            val square = if (sin(2 * PI * freq * t) >= 0) 1f else -1f
            square * decay(t, rate = 18.0) * 0.35f
        }

        /** A noise burst with a low rumble that fades fast: retro explosion. */
        private fun explosion(): AudioData {
            val random = Random(0)   // fixed seed so the build is deterministic
            return synth(durationMs = 300) { t ->
                val noise = random.nextFloat() * 2f - 1f
                val rumble = sin(2 * PI * sweep(t, fromHz = 120.0, toHz = 40.0, overMs = 300.0) * t)
                (noise * 0.6f + rumble.toFloat() * 0.4f) * decay(t, rate = 11.0) * 0.6f
            }
        }
    }
}
