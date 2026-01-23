package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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

        // First row: Company, Product, Role, Period
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            overview.company?.let { company ->
                OverviewCard(
                    label = "COMPANY",
                    value = company,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            overview.product?.let { product ->
                OverviewCard(
                    label = "PRODUCT",
                    value = product,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            overview.role?.let { role ->
                OverviewCard(
                    label = "ROLE",
                    value = role,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            overview.period?.displayText?.let { period ->
                OverviewCard(
                    label = "PERIOD",
                    value = period,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        // Second row: Location (if present)
        overview.location?.let { location ->
            Spacer(modifier = Modifier.height(12.dp))
            OverviewCard(
                label = "LOCATION",
                value = location,
                modifier = Modifier.fillMaxWidth()
            )
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
