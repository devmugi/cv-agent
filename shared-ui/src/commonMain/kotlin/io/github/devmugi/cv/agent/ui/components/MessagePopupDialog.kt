package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme

@Composable
fun MessagePopupDialog(
    content: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    color = ArcaneTheme.colors.primary
                )
            }
        },
        title = {
            Text(
                text = "Full Message",
                style = ArcaneTheme.typography.headlineLarge,
                color = ArcaneTheme.colors.text
            )
        },
        text = {
            Markdown(
                content = content,
                colors = markdownColor(text = ArcaneTheme.colors.text),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            )
        },
        containerColor = ArcaneTheme.colors.surface,
        modifier = modifier
    )
}
