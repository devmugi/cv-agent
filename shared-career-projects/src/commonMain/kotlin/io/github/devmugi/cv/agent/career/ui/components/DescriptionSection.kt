package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.models.Description

private val AmberColor = Color(0xFFFFC107)

@Composable
fun DescriptionSection(
    description: Description,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(
            icon = Icons.Default.Inventory2,
            title = "Product Description"
        )

        Spacer(modifier = Modifier.height(12.dp))

        ArcaneSurface(
            variant = SurfaceVariant.Container,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                description.full?.let { fullDesc ->
                    Text(
                        text = fullDesc,
                        style = ArcaneTheme.typography.bodyMedium,
                        color = ArcaneTheme.colors.text
                    )
                }

                description.howItWorked?.takeIf { it.isNotEmpty() }?.let { steps ->
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "How It Worked",
                        style = ArcaneTheme.typography.labelLarge,
                        color = ArcaneTheme.colors.text,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    steps.forEachIndexed { index, step ->
                        HowItWorkedStep(
                            stepNumber = step.step ?: (index + 1),
                            title = step.title ?: "",
                            description = step.description ?: "",
                            isLast = index == steps.lastIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HowItWorkedStep(
    stepNumber: Int,
    title: String,
    description: String,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AmberColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber.toString(),
                    style = ArcaneTheme.typography.labelLarge,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(ArcaneTheme.colors.surfaceContainerLow)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = ArcaneTheme.typography.labelLarge,
                color = AmberColor,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = ArcaneTheme.typography.bodyMedium,
                color = ArcaneTheme.colors.textSecondary
            )
            if (!isLast) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
internal fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AmberColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            style = ArcaneTheme.typography.labelLarge,
            color = ArcaneTheme.colors.text,
            fontWeight = FontWeight.SemiBold
        )
    }
}
