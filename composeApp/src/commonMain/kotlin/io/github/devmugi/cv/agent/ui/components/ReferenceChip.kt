package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import io.github.devmugi.cv.agent.data.models.CVReference
import io.github.devmugi.cv.agent.ui.theme.ReferenceChipBg

fun formatReferenceChipText(reference: CVReference): String {
    return if (reference.label.isNotEmpty()) {
        "${reference.type}: ${reference.label}"
    } else {
        reference.type
    }
}

@Composable
fun ReferenceChip(
    reference: CVReference,
    tooltipContent: String? = null,
    modifier: Modifier = Modifier
) {
    var showTooltip by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { if (tooltipContent != null) showTooltip = !showTooltip },
            shape = MaterialTheme.shapes.small,
            color = ReferenceChipBg
        ) {
            Text(
                text = formatReferenceChipText(reference),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (showTooltip && tooltipContent != null) {
            Popup(
                onDismissRequest = { showTooltip = false }
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = tooltipContent,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
