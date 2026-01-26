package io.github.devmugi.cv.agent.api.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import co.touchlab.kermit.Logger
import java.io.File

class AudioRecorder(private val context: Context) : AudioRecorderInterface {

    companion object {
        private const val TAG = "AudioRecorder"
        private const val SAMPLE_RATE_HZ = 16000
    }

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    override val isRecording: Boolean
        get() = recorder != null

    override fun startRecording(): Result<Unit> {
        if (isRecording) {
            Logger.w(TAG) { "Already recording" }
            return Result.failure(IllegalStateException("Already recording"))
        }

        return try {
            val file = File(context.cacheDir, "voice_recording_${System.currentTimeMillis()}.m4a")
            outputFile = file

            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(SAMPLE_RATE_HZ)
                setAudioChannels(1)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mediaRecorder
            Logger.d(TAG) { "Recording started: ${file.absolutePath}" }
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, e) { "Failed to start recording" }
            cleanup()
            Result.failure(e)
        }
    }

    override fun stopRecording(): Result<File> {
        val currentRecorder = recorder
        val currentFile = outputFile

        if (currentRecorder == null || currentFile == null) {
            Logger.w(TAG) { "Not currently recording" }
            return Result.failure(IllegalStateException("Not currently recording"))
        }

        return try {
            currentRecorder.stop()
            currentRecorder.release()
            recorder = null

            Logger.d(TAG) { "Recording stopped: ${currentFile.length()} bytes" }
            Result.success(currentFile)
        } catch (e: Exception) {
            Logger.e(TAG, e) { "Failed to stop recording" }
            cleanup()
            Result.failure(e)
        }
    }

    override fun cancelRecording() {
        Logger.d(TAG) { "Cancelling recording" }
        cleanup()
    }

    private fun cleanup() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
            // Ignore errors during cleanup
        }
        recorder = null

        outputFile?.delete()
        outputFile = null
    }
}
