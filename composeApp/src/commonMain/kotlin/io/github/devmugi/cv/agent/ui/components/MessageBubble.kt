package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import io.github.devmugi.cv.agent.agent.Message
import io.github.devmugi.cv.agent.agent.MessageRole
import io.github.devmugi.cv.agent.data.models.CVData

enum class MessageAlignment { START, END }
enum class MessageCornerShape { TOP_RIGHT_SMALL, TOP_LEFT_SMALL }

fun getMessageAlignment(role: MessageRole): MessageAlignment = when (role) {
    MessageRole.USER -> MessageAlignment.END
    MessageRole.ASSISTANT, MessageRole.SYSTEM -> MessageAlignment.START
}

fun getMessageCornerShape(role: MessageRole): MessageCornerShape = when (role) {
    MessageRole.USER -> MessageCornerShape.TOP_RIGHT_SMALL
    MessageRole.ASSISTANT, MessageRole.SYSTEM -> MessageCornerShape.TOP_LEFT_SMALL
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageBubble(
    message: Message,
    cvData: CVData? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    }
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = shape,
            color = backgroundColor,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isUser) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                } else {
                    Markdown(
                        content = message.content,
                        modifier = Modifier
                    )
                }

                if (message.references.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        message.references.forEach { reference ->
                            ReferenceChip(
                                reference = reference,
                                tooltipContent = getTooltipForReference(reference, cvData)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingMessageBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Markdown(
                content = content,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
