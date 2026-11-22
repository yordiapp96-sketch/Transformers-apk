package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Zero-asset procedural 16-bit retro synth SFX & looping multi-channel chiptune BGM engine.
 * Synthesizes crisp PCM SFX and two seamless looping BGM themes (Sector & Boss) in memory.
 */
class RetroAudioEngine {

    enum class Sfx {
        SHOOT,
        CHARGE_READY,
        CHARGE_BEAM,
        SLASH,
        PARRY,
        JUMP,
        DOUBLE_JUMP,
        TRANSFORM_TO_VEHICLE,
        TRANSFORM_TO_MECH,
        BOOST,
        HIT,
        EXPLOSION,
        PICKUP,
        RELIC,
        CHECKPOINT,
        BOSS_ALERT,
        DAMAGE,
        VICTORY,
    }

    enum class BgmTrack {
        SECTOR,
        BOSS,
    }

    @Volatile
    var isMuted: Boolean = false
        set(value) {
            field = value
            sfxMuted = value
            bgmMuted = value
        }

    @Volatile
    var sfxMuted: Boolean = false

    @Volatile
    var bgmMuted: Boolean = false
        set(value) {
            field = value
            if (value) stopBgm()
        }

    private val sampleRate = 22050
    private val sfxExecutor = Executors.newFixedThreadPool(3)
    private val clips: Map<Sfx, ShortArray> = buildClips()

    // BGM state
    private val bgmLock = Any()
    private val sectorBgmPcm: ShortArray by lazy { synthBgmLoop(isBoss = false) }
    private val bossBgmPcm: ShortArray by lazy { synthBgmLoop(isBoss = true) }
    private var activeBgmTrack: AudioTrack? = null
    private var currentTrackKind: BgmTrack? = null
    private var bgmPlaying = false

    private fun buildClips(): Map<Sfx, ShortArray> = mapOf(
        Sfx.SHOOT to synthSweep(0.085f, 1150f, 240f, wave = Wave.SQUARE, vol = 0.24f),
        Sfx.CHARGE_READY to synthArpeggio(floatArrayOf(660f, 990f, 1320f), 0.035f, 0.20f),
        Sfx.CHARGE_BEAM to synthSweep(0.18f, 1480f, 180f, wave = Wave.SAW, vol = 0.30f),
        Sfx.SLASH to synthSlash(),
        Sfx.PARRY to synthArpeggio(floatArrayOf(1046.5f, 1568.0f), 0.045f, 0.28f),
        Sfx.JUMP to synthSweep(0.11f, 240f, 640f, wave = Wave.SQUARE, vol = 0.22f),
        Sfx.DOUBLE_JUMP to synthSweep(0.14f, 420f, 980f, wave = Wave.SAW, vol = 0.23f),
        Sfx.TRANSFORM_TO_VEHICLE to synthTransform(toVehicle = true),
        Sfx.TRANSFORM_TO_MECH to synthTransform(toVehicle = false),
        Sfx.BOOST to synthBoost(),
        Sfx.HIT to synthSweep(0.065f, 340f, 110f, wave = Wave.NOISE_MIX, vol = 0.28f),
        Sfx.EXPLOSION to synthExplosion(0.32f),
        Sfx.PICKUP to synthArpeggio(floatArrayOf(587.3f, 784.0f, 1174.7f), 0.048f, 0.24f),
        Sfx.RELIC to synthArpeggio(floatArrayOf(523.3f, 659.3f, 784.0f, 1046.5f, 1318.5f), 0.065f, 0.28f),
        Sfx.CHECKPOINT to synthArpeggio(floatArrayOf(440f, 554.4f, 659.3f, 880f), 0.075f, 0.26f),
        Sfx.BOSS_ALERT to synthBossSiren(),
        Sfx.DAMAGE to synthSweep(0.18f, 190f, 65f, wave = Wave.SAW, vol = 0.34f),
        Sfx.VICTORY to synthArpeggio(floatArrayOf(523.3f, 659.3f, 784f, 1046.5f, 987.8f, 1046.5f), 0.095f, 0.30f),
    )

    fun play(sfx: Sfx) {
        if (sfxMuted) return
        val pcm = clips[sfx] ?: return
        sfxExecutor.execute {
            runCatching {
                val byteLen = pcm.size * 2
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(byteLen)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                val durationMs = (pcm.size * 1000L) / sampleRate + 35L
                Thread.sleep(durationMs)
                track.stop()
                track.release()
            }
        }
    }

    fun updateBgmState(shouldPlay: Boolean, isBoss: Boolean) {
        if (!shouldPlay || bgmMuted) {
            stopBgm()
            return
        }
        val desired = if (isBoss) BgmTrack.BOSS else BgmTrack.SECTOR
        synchronized(bgmLock) {
            if (bgmPlaying && currentTrackKind == desired) return
            stopBgmLocked()
            startBgmLocked(desired)
        }
    }

    fun stopBgm() {
        synchronized(bgmLock) {
            stopBgmLocked()
        }
    }

    private fun startBgmLocked(kind: BgmTrack) {
        runCatching {
            val pcm = if (kind == BgmTrack.BOSS) bossBgmPcm else sectorBgmPcm
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.setLoopPoints(0, pcm.size, -1)
            track.play()
            activeBgmTrack = track
            currentTrackKind = kind
            bgmPlaying = true
        }
    }

    private fun stopBgmLocked() {
        val existing = activeBgmTrack
        activeBgmTrack = null
        currentTrackKind = null
        bgmPlaying = false
        if (existing != null) {
            runCatching {
                existing.stop()
                existing.release()
            }
        }
    }

    fun release() {
        stopBgm()
        runCatching { sfxExecutor.shutdownNow() }
    }

    // ==================== MULTI-CHANNEL CHIPTUNE BGM SYNTHESIS ====================

    private fun midiToHz(midi: Int): Float = (440.0 * 2.0.pow((midi - 69) / 12.0)).toFloat()

    private fun synthBgmLoop(isBoss: Boolean): ShortArray {
        val bpm = if (isBoss) 156f else 138f
        val stepDur = 60f / bpm / 2f // 8th-note grid
        val steps = 32 // 4 bars of 8th notes -> crisp loop
        val samplesPerStep = (sampleRate * stepDur).toInt()
        val totalSamples = steps * samplesPerStep
        val out = ShortArray(totalSamples)

        // Driving minor-key cyberpunk lead & bass MIDI sequences (0 = rest)
        val leadMidi = if (isBoss) {
            intArrayOf(
                64, 0, 67, 71, 72, 0, 71, 67,
                64, 67, 71, 76, 75, 0, 71, 67,
                60, 0, 64, 67, 69, 0, 67, 64,
                62, 66, 69, 74, 71, 0, 69, 66,
            )
        } else {
            intArrayOf(
                69, 0, 72, 76, 0, 74, 72, 69,
                67, 0, 71, 74, 0, 72, 71, 67,
                65, 0, 69, 72, 0, 71, 69, 65,
                64, 67, 71, 76, 74, 72, 71, 67,
            )
        }

        val bassMidi = if (isBoss) {
            intArrayOf(
                40, 40, 52, 40, 40, 40, 52, 43,
                40, 40, 52, 40, 47, 43, 40, 38,
                36, 36, 48, 36, 36, 36, 48, 39,
                38, 38, 50, 38, 43, 42, 40, 38,
            )
        } else {
            intArrayOf(
                45, 45, 57, 45, 45, 45, 57, 48,
                43, 43, 55, 43, 43, 43, 55, 47,
                41, 41, 53, 41, 41, 41, 53, 45,
                40, 40, 52, 40, 43, 43, 47, 40,
            )
        }

        var leadPhase = 0.0
        var bassPhase = 0.0
        val rng = Random(if (isBoss) 777 else 333)

        for (step in 0 until steps) {
            val lNote = leadMidi[step]
            val bNote = bassMidi[step]
            val lFreq = if (lNote > 0) midiToHz(lNote) else 0f
            val bFreq = if (bNote > 0) midiToHz(bNote) else 0f
            val isKick = step % 4 == 0
            val isSnare = step % 4 == 2
            val isHat = step % 2 == 1

            for (i in 0 until samplesPerStep) {
                val t = i.toFloat() / samplesPerStep

                // Channel 1: Pulse/Square Lead with gentle pluck envelope
                var leadSample = 0f
                if (lFreq > 0f) {
                    leadPhase += 2.0 * PI * lFreq / sampleRate
                    val duty = if (isBoss) 0.25 else 0.5
                    val cycle = (leadPhase / (2.0 * PI)) % 1.0
                    val sq = if (cycle < duty) 1f else -1f
                    val env = exp(-t * 2.6f)
                    leadSample = sq * env * 0.11f
                }

                // Channel 2: Driving Saw/Triangle Bass
                var bassSample = 0f
                if (bFreq > 0f) {
                    bassPhase += 2.0 * PI * bFreq / sampleRate
                    val saw = ((bassPhase / (2.0 * PI)) % 1.0).toFloat() * 2f - 1f
                    val env = exp(-t * 1.8f)
                    bassSample = saw * env * 0.13f
                }

                // Channel 3: Crisp Chiptune Drums
                var drumSample = 0f
                if (isKick && t < 0.35f) {
                    val kFreq = 135f * exp(-t * 10f) + 42f
                    drumSample += sin(2.0 * PI * kFreq * (t * stepDur)).toFloat() * exp(-t * 9f) * 0.14f
                }
                if (isSnare && t < 0.45f) {
                    drumSample += (rng.nextFloat() * 2f - 1f) * exp(-t * 8f) * 0.09f
                } else if (isHat && t < 0.20f) {
                    drumSample += (rng.nextFloat() * 2f - 1f) * exp(-t * 16f) * 0.045f
                }

                val mixed = (leadSample + bassSample + drumSample).coerceIn(-0.9f, 0.9f)
                out[step * samplesPerStep + i] = (mixed * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out
    }

    private enum class Wave { SQUARE, SAW, SINE, NOISE_MIX }

    private fun synthSweep(
        durationSec: Float,
        fStart: Float,
        fEnd: Float,
        wave: Wave,
        vol: Float,
    ): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        var phase = 0.0
        val rng = Random(42)
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = fStart + (fEnd - fStart) * t
            phase += 2.0 * PI * freq / sampleRate
            val env = (1f - t) * (1f - t * 0.3f)
            val raw = when (wave) {
                Wave.SQUARE -> if (sin(phase) >= 0) 1f else -1f
                Wave.SAW -> ((phase / (2.0 * PI)) % 1.0).toFloat() * 2f - 1f
                Wave.SINE -> sin(phase).toFloat()
                Wave.NOISE_MIX -> {
                    val sq = if (sin(phase) >= 0) 0.6f else -0.6f
                    val nz = (rng.nextFloat() * 2f - 1f) * 0.6f
                    sq + nz
                }
            }
            out[i] = (raw * env * vol * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthTransform(toVehicle: Boolean): ShortArray {
        val steps = if (toVehicle) {
            floatArrayOf(680f, 340f, 820f, 260f, 540f)
        } else {
            floatArrayOf(260f, 560f, 340f, 840f, 620f)
        }
        val stepDur = 0.042f
        val samplesPerStep = (sampleRate * stepDur).toInt()
        val out = ShortArray(steps.size * samplesPerStep)
        var phase = 0.0
        for (s in steps.indices) {
            val baseFreq = steps[s]
            for (i in 0 until samplesPerStep) {
                val localT = i.toFloat() / samplesPerStep
                val freq = baseFreq * (1f + (if (toVehicle) -0.18f else 0.18f) * localT)
                phase += 2.0 * PI * freq / sampleRate
                val sq = if (sin(phase) >= 0) 1f else -1f
                val env = (1f - localT * 0.35f) * 0.28f
                out[s * samplesPerStep + i] =
                    (sq * env * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
            }
        }
        return out
    }

    private fun synthSlash(): ShortArray {
        val count = (sampleRate * 0.13f).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        val rng = Random(99)
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = 1400f * exp(-t * 3.5f) + 180f
            phase += 2.0 * PI * freq / sampleRate
            val tone = sin(phase).toFloat() * 0.55f + (rng.nextFloat() * 2f - 1f) * 0.45f
            val env = (1f - t) * 0.30f
            out[i] = (tone * env * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthBoost(): ShortArray {
        val count = (sampleRate * 0.22f).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        val rng = Random(7)
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = 160f + 420f * t
            phase += 2.0 * PI * freq / sampleRate
            val saw = ((phase / (2.0 * PI)) % 1.0).toFloat() * 2f - 1f
            val nz = rng.nextFloat() * 2f - 1f
            val env = (1f - t * 0.75f) * 0.26f
            out[i] = ((saw * 0.5f + nz * 0.5f) * env * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthExplosion(durationSec: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        val rng = Random(1337)
        var lp = 0f
        for (i in 0 until count) {
            val t = i.toFloat() / count
            phase += 2.0 * PI * (95f * (1f - t * 0.6f)) / sampleRate
            val sub = sin(phase).toFloat() * 0.45f
            val white = rng.nextFloat() * 2f - 1f
            lp += 0.22f * (white - lp)
            val env = exp(-t * 4.5f) * 0.36f
            out[i] = ((sub + lp * 0.85f) * env * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthArpeggio(notes: FloatArray, noteDur: Float, vol: Float): ShortArray {
        val perNote = (sampleRate * noteDur).toInt()
        val out = ShortArray(notes.size * perNote)
        var phase = 0.0
        for (n in notes.indices) {
            for (i in 0 until perNote) {
                val t = i.toFloat() / perNote
                phase += 2.0 * PI * notes[n] / sampleRate
                val sq = if (sin(phase) >= 0) 0.7f else -0.7f
                val sine = sin(phase).toFloat() * 0.3f
                val env = (1f - t * 0.5f) * vol
                out[n * perNote + i] =
                    ((sq + sine) * env * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
            }
        }
        return out
    }

    private fun synthBossSiren(): ShortArray {
        val count = (sampleRate * 0.45f).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = 140f + 90f * sin(t * 6.0 * PI).toFloat()
            phase += 2.0 * PI * freq / sampleRate
            val saw = ((phase / (2.0 * PI)) % 1.0).toFloat() * 2f - 1f
            val env = (1f - t * 0.4f) * 0.30f
            out[i] = (saw * env * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }
}
