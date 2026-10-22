package uk.co.fenixprint.stanalyser

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import kotlin.math.*

/**
 * Mic-based live analysis. Android does not generally allow arbitrary third-party
 * capture of Spotify's internal PCM output. Mic permission is required.
 */
class AudioBands(private val context: Context) {
    private var recording = false
    private var worker: Thread? = null
    @Volatile var levels: FloatArray = FloatArray(10) { 0.2f }
        private set

    fun start() {
        if (recording || context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return
        recording = true
        worker = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val rate = 22050
            val n = 2048
            val size = max(AudioRecord.getMinBufferSize(rate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT), n * 4)
            var recorder: AudioRecord? = null
            try {
                recorder = AudioRecord(MediaRecorder.AudioSource.MIC, rate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size)
                if (recorder.state != AudioRecord.STATE_INITIALIZED) return@Thread
                recorder.startRecording()
                val samples = ShortArray(n)
                val frequencies = doubleArrayOf(65.0, 100.0, 160.0, 250.0, 400.0,
                    630.0, 1000.0, 1600.0, 2500.0, 4000.0)
                val smooth = FloatArray(10)
                while (recording) {
                    val count = recorder.read(samples, 0, n)
                    if (count <= 0) continue
                    val out = FloatArray(10)
                    frequencies.forEachIndexed { i, frequency ->
                        // Goertzel frequency-bin energy, with neighbouring bins for breadth.
                        var power = 0.0
                        for (factor in doubleArrayOf(0.85, 1.0, 1.17)) {
                            val omega = 2.0 * PI * frequency * factor / rate
                            val coeff = 2.0 * cos(omega)
                            var s1 = 0.0; var s2 = 0.0
                            for (j in 0 until count) {
                                val window = 0.5 - 0.5 * cos(2 * PI * j / max(1, count - 1))
                                val s0 = (samples[j] / 32768.0) * window + coeff * s1 - s2
                                s2 = s1; s1 = s0
                            }
                            power += sqrt(max(0.0, s1 * s1 + s2 * s2 - coeff * s1 * s2))
                        }
                        val value = (ln(1.0 + power * 2.0) / 6.5).toFloat().coerceIn(0f, 1f)
                        smooth[i] = if (value > smooth[i]) smooth[i] * .42f + value * .58f
                                    else smooth[i] * .83f + value * .17f
                        out[i] = smooth[i]
                    }
                    levels = out
                }
            } catch (_: Exception) {
            } finally {
                try { recorder?.stop() } catch (_: Exception) {}
                recorder?.release()
            }
        }.also { it.start() }
    }
    fun stop() { recording = false; worker?.interrupt(); worker = null }
}
