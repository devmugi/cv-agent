package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.models.Overview

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewSection(
    overview: Overview,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(
            icon = Icons.Default.Info,
            title = "Overview"
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            overview.company?.let { company ->
                OverviewCard(
                    label = "COMPANY",
                    value = company,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            overview.product?.let { product ->
                OverviewCard(
                    label = "PRODUCT",
                    value = product,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            overview.role?.let { role ->
                OverviewCard(
                    label = "ROLE",
                    value = role,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            overview.period?.displayText?.let { period ->
                OverviewCard(
                    label = "PERIOD",
                    value = period,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            overview.location?.let { location ->
                OverviewCard(
                    label = "LOCATION",
                    value = location,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

@Composable
private fun OverviewCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ArcaneSurface(
        variant = SurfaceVariant.Container,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = ArcaneTheme.typography.labelSmall,
                color = ArcaneTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = ArcaneTheme.typography.bodyMedium,
                color = ArcaneTheme.colors.text,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
