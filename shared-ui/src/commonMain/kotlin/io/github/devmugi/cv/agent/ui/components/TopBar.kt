package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme

fun buildTopBarTitle(): String = "<DH/> Denys Honcharenko CV"

@Composable
fun CVAgentTopBar(
    onCareerClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ArcaneSurface(
            modifier = Modifier.fillMaxWidth().statusBarsPadding(),
            showBorder = false
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "<DH/>",
                    style = ArcaneTheme.typography.headlineLarge,
                    color = ArcaneTheme.colors.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Denys Honcharenko CV",
                    style = ArcaneTheme.typography.headlineLarge,
                    color = ArcaneTheme.colors.text
                )
                Spacer(modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = onCareerClick) {
                    Text(text = "Career")
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ArcaneTheme.colors.surfaceContainerLow.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
