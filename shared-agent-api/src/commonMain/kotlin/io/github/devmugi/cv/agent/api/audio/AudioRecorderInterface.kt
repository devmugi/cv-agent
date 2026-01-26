package io.github.devmugi.cv.agent.api.audio

import java.io.File

interface AudioRecorderInterface {
    val isRecording: Boolean
    fun startRecording(): Result<Unit>
    fun stopRecording(): Result<File>
    fun cancelRecording()
}
