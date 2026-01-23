package io.github.devmugi.cv.agent.career.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.components.controls.ArcaneButtonStyle
import io.github.devmugi.arcane.design.components.controls.ArcaneTextButton
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.models.CareerProject

@Composable
fun CareerProjectTimelineInfo(
    project: CareerProject,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ArcaneSurface(
        variant = SurfaceVariant.Container,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = project.name,
                style = ArcaneTheme.typography.headlineMedium,
                color = ArcaneTheme.colors.text
            )

            Text(
                text = project.companyName,
                style = ArcaneTheme.typography.labelMedium,
                color = ArcaneTheme.colors.primary
            )

            Text(
                text = project.description,
                style = ArcaneTheme.typography.bodyMedium,
                color = ArcaneTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArcaneTextButton(
                    text = "Details",
                    onClick = onDetailsClick,
                    style = ArcaneButtonStyle.Outlined()
                )
            }
        }
    }
}
