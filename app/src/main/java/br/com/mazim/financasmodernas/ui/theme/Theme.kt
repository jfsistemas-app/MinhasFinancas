package br.com.mazim.financasmodernas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AppColors = lightColorScheme(
    primary = Emerald,
    onPrimary = CardSurface,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = Emerald,
    secondary = Teal,
    onSecondary = CardSurface,
    secondaryContainer = TealContainer,
    background = WarmBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFF13221E),
    surface = CardSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFF13221E),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEBF3EF),
    outline = androidx.compose.ui.graphics.Color(0xFF90A59C),
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)

@Composable
fun FinancasModernasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = Typography(),
        shapes = AppShapes,
        content = content,
    )
}
