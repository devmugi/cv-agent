package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.devmugi.cv.agent.career.models.Technologies

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TechnologiesSection(
    technologies: Technologies,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(
            icon = Icons.Default.Code,
            title = "Technologies"
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Primary technologies
            technologies.primary?.forEach { tech ->
                tech.name?.let { TechnologyTag(text = it) }
            }

            // Secondary technologies
            technologies.secondary?.forEach { tech ->
                tech.name?.let { TechnologyTag(text = it) }
            }

            // Tools
            technologies.tools?.forEach { tool ->
                TechnologyTag(text = tool)
            }
        }
    }
}
