package io.github.akrishna87.daybook

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContract
import io.github.akrishna87.daybook.model.Recording
import io.github.akrishna87.daybook.model.newId
import java.io.File

/**
 * Voice in Daybook: voice notes recorded into a note (kept as .m4a files in the app's own storage,
 * so they never leave the phone), and speech typed into search, Quick Add and notes by the phone's
 * speech recogniser.
 */
object Voice {
    fun dir(context: Context): File = File(context.filesDir, "voice").apply { mkdirs() }

    fun file(context: Context, recording: Recording): File = File(dir(context), recording.file)

    fun deleteFiles(context: Context, recordings: List<Recording>) {
        for (r in recordings) runCatching { file(context, r).delete() }
    }

    /** "0:07", "1:23", "1:02:03". */
    fun duration(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}

/** Records one voice note at a time. */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var current: File? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    /** Elapsed time of the recording in progress. */
    fun elapsedMs(): Long = if (recorder == null) 0L else SystemClock.elapsedRealtime() - startedAt

    /** Starts recording; false if the microphone couldn't be opened. */
    fun start(): Boolean {
        if (recorder != null) return true
        val f = File(Voice.dir(context), "voice-${newId()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            current = f
            startedAt = SystemClock.elapsedRealtime()
            true
        } catch (e: Exception) {
            runCatching { r.release() }
            f.delete()
            false
        }
    }

    /** Stops and returns the new voice note, or null if nothing usable was recorded. */
    fun stop(): Recording? {
        val r = recorder ?: return null
        val f = current
        val elapsed = elapsedMs()
        recorder = null
        current = null
        val ok = runCatching { r.stop() }.isSuccess
        runCatching { r.release() }
        if (f == null) return null
        if (!ok || !f.exists() || f.length() == 0L) {
            f.delete()
            return null
        }
        return Recording(file = f.name, durationMs = elapsed)
    }

    /** Stops and throws the recording away. */
    fun cancel() {
        val f = current
        stop()
        f?.delete()
    }
}

/** Plays voice notes, one at a time. */
class VoicePlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    var playingId: String? = null
        private set

    fun positionMs(): Long = runCatching { player?.currentPosition?.toLong() }.getOrNull() ?: 0L

    /** Plays [recording], or stops it if it's already playing. Returns false if it couldn't play. */
    fun toggle(recording: Recording, onDone: () -> Unit): Boolean {
        if (playingId == recording.id) {
            stop()
            return true
        }
        stop()
        val f = Voice.file(context, recording)
        if (!f.exists()) return false
        val p = MediaPlayer()
        return try {
            p.setDataSource(f.absolutePath)
            p.setOnCompletionListener {
                stop()
                onDone()
            }
            p.prepare()
            p.start()
            player = p
            playingId = recording.id
            true
        } catch (e: Exception) {
            runCatching { p.release() }
            false
        }
    }

    fun stop() {
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = null
        playingId = null
    }
}

/**
 * Asks the phone's speech recogniser for what was said. The result is the best guess, or null
 * if nothing was heard or the user backed out. No microphone permission is needed: the
 * recogniser app does the listening.
 */
class SpeechToText : ActivityResultContract<String, String?>() {
    override fun createIntent(context: Context, input: String): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, input)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode != Activity.RESULT_OK) return null
        return intent?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    }

    companion object {
        /** Launches the recogniser; false when the phone has no speech recogniser. */
        fun launch(launcher: androidx.activity.result.ActivityResultLauncher<String>, prompt: String): Boolean = try {
            launcher.launch(prompt)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
