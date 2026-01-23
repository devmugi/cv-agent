package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.models.Technologies

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TechnologiesSection(
    technologies: Technologies,
    modifier: Modifier = Modifier
) {
    val hasPrimary = !technologies.primary.isNullOrEmpty()
    val hasSecondary = !technologies.secondary.isNullOrEmpty()
    val hasTools = !technologies.tools.isNullOrEmpty()

    Column(modifier = modifier) {
        SectionHeader(
            icon = Icons.Default.Code,
            title = "Technologies"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Primary technologies (Core Stack)
        if (hasPrimary) {
            TechnologyGroupLabel(label = "Core Stack")
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                technologies.primary?.forEach { tech ->
                    tech.name?.let {
                        TechnologyTag(
                            text = it,
                            category = TechnologyCategory.PRIMARY
                        )
                    }
                }
            }
        }

        // Secondary technologies (Frameworks & Libraries)
        if (hasSecondary) {
            if (hasPrimary) Spacer(modifier = Modifier.height(16.dp))
            TechnologyGroupLabel(label = "Frameworks & Libraries")
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                technologies.secondary?.forEach { tech ->
                    tech.name?.let {
                        TechnologyTag(
                            text = it,
                            category = TechnologyCategory.SECONDARY
                        )
                    }
                }
            }
        }

        // Tools
        if (hasTools) {
            if (hasPrimary || hasSecondary) Spacer(modifier = Modifier.height(16.dp))
            TechnologyGroupLabel(label = "Tools & Infrastructure")
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                technologies.tools?.forEach { tool ->
                    TechnologyTag(
                        text = tool,
                        category = TechnologyCategory.TOOL
                    )
                }
            }
        }
    }
}

@Composable
private fun TechnologyGroupLabel(label: String) {
    Text(
        text = label,
        style = ArcaneTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = ArcaneTheme.colors.textSecondary
    )
}
