package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.components.controls.ArcaneButton
import io.github.devmugi.arcane.design.components.controls.ArcaneButtonStyle
import io.github.devmugi.arcane.design.components.controls.ArcaneTextField
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface

fun isSendButtonEnabled(text: String, isLoading: Boolean): Boolean {
    return text.isNotBlank() && !isLoading
}

@Composable
fun MessageInput(
    onSend: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val canSend = isSendButtonEnabled(text, isLoading)

    ArcaneSurface(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .alpha(if (isLoading) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArcaneTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = "Ask about my experience...",
                enabled = !isLoading,
                modifier = Modifier.weight(1f).testTag("message_input_field")
            )

            ArcaneButton(
                onClick = {
                    if (canSend) {
                        onSend(text)
                        text = ""
                    }
                },
                style = ArcaneButtonStyle.Primary,
                enabled = canSend,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(48.dp)
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}
