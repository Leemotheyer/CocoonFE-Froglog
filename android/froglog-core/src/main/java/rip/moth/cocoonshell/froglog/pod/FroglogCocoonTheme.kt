package rip.moth.cocoonshell.froglog.pod

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun FroglogCocoonTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    val (gradStart, gradEnd) = FroglogPodChrome.breezeGradient()
    val background = Brush.verticalGradient(
        colors = listOf(Color(gradStart.toULong()), Color(gradEnd.toULong())),
    )
    MaterialTheme(colorScheme = scheme) {
        Box(modifier = Modifier.fillMaxSize().background(background)) {
            content()
        }
    }
}
