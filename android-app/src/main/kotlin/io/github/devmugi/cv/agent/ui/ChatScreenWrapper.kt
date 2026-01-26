package io.github.devmugi.cv.agent.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastState
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.agent.VoiceInputController
import io.github.devmugi.cv.agent.agent.VoiceInputState
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.domain.models.ChatState

@Composable
fun ChatScreenWrapper(
    chatState: ChatState,
    viewModel: ChatViewModel,
    voiceController: VoiceInputController?,
    toastState: ArcaneToastState,
    analytics: Analytics,
    modifier: Modifier = Modifier,
    onNavigateToCareerTimeline: () -> Unit = {},
    onNavigateToProject: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val voiceState by voiceController?.state?.collectAsState()
        ?: remember { mutableStateOf(VoiceInputState.Idle) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PermissionChecker.PERMISSION_GRANTED
        )
    }

    var pendingRecordingStart by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted && pendingRecordingStart) {
            voiceController?.startRecording()
        }
        pendingRecordingStart = false
        if (!granted) {
            toastState.show("Microphone permission required for voice input")
        }
    }

    // Handle voice input errors
    LaunchedEffect(voiceState) {
        if (voiceState is VoiceInputState.Error) {
            toastState.show((voiceState as VoiceInputState.Error).message)
            voiceController?.clearError()
        }
    }

    ChatScreen(
        state = chatState,
        toastState = toastState,
        onSendMessage = viewModel::sendMessage,
        analytics = analytics,
        modifier = modifier,
        onSuggestionClick = viewModel::onSuggestionClicked,
        onCopyMessage = viewModel::onMessageCopied,
        onShareMessage = { /* TODO */ },
        onLikeMessage = viewModel::onMessageLiked,
        onDislikeMessage = viewModel::onMessageDisliked,
        onRegenerateMessage = viewModel::onRegenerateClicked,
        onClearHistory = viewModel::clearHistory,
        onNavigateToCareerTimeline = onNavigateToCareerTimeline,
        onNavigateToProject = onNavigateToProject,
        // Voice input
        isRecording = voiceState is VoiceInputState.Recording,
        isTranscribing = voiceState is VoiceInputState.Transcribing,
        onRecordingStart = {
            voiceController?.startRecording()
        },
        onRecordingStop = {
            voiceController?.stopRecordingAndTranscribe { transcribedText ->
                if (transcribedText.isNotBlank()) {
                    viewModel.sendMessage(transcribedText)
                }
            }
        },
        onRequestMicPermission = {
            pendingRecordingStart = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        hasMicPermission = hasMicPermission
    )
}
