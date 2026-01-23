package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.domain.models.CVReference

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
        ArcaneSurface(
            variant = SurfaceVariant.Container,
            modifier = if (tooltipContent != null) {
                Modifier.clickable { showTooltip = !showTooltip }
            } else {
                Modifier
            }
        ) {
            Text(
                text = formatReferenceChipText(reference),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = ArcaneTheme.typography.labelSmall,
                color = ArcaneTheme.colors.text
            )
        }

        if (showTooltip && tooltipContent != null) {
            Popup(
                onDismissRequest = { showTooltip = false }
            ) {
                ArcaneSurface(
                    variant = SurfaceVariant.Container
                ) {
                    Text(
                        text = tooltipContent,
                        modifier = Modifier.padding(12.dp),
                        style = ArcaneTheme.typography.bodySmall,
                        color = ArcaneTheme.colors.text
                    )
                }
            }
        }
    }
}
