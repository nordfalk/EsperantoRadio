package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// === Muzaiko-koloroj ===

val MuzaikoRugo = Color(0xFFD0002F)
val MuzaikoRugoMalhela = Color(0xFF9F001E)
val MuzaikoRugoProfunda = Color(0xFF4F000E)
val MuzaikoKremo = Color(0xFFFAF6F2)
val MuzaikoKarbono = Color(0xFF1A1413)
val MuzaikoArgento = Color(0xFFE8E2DD)
val MuzaikoBlanko = Color(0xFFFFFFFF)
val MuzaikoMalhelaFono = Color(0xFF1A1413)
val MuzaikoMalhelaSurfaco = Color(0xFF241D1B)
val MuzaikoMalhelaMalaktiva = Color(0xFF3A3030)

// === Kolorskemoj ===

val MuzaikoHelaKolorskemo = lightColorScheme(
    primary = MuzaikoRugo,
    onPrimary = MuzaikoBlanko,
    primaryContainer = MuzaikoRugoMalhela,
    onPrimaryContainer = MuzaikoKremo,
    secondary = MuzaikoKarbono,
    onSecondary = MuzaikoKremo,
    secondaryContainer = MuzaikoArgento,
    onSecondaryContainer = MuzaikoKarbono,
    tertiary = MuzaikoRugoProfunda,
    onTertiary = MuzaikoKremo,
    background = MuzaikoKremo,
    onBackground = MuzaikoKarbono,
    surface = MuzaikoBlanko,
    onSurface = MuzaikoKarbono,
    surfaceVariant = MuzaikoArgento,
    onSurfaceVariant = MuzaikoKarbono,
    outline = MuzaikoArgento,
    outlineVariant = MuzaikoKremo,
    error = MuzaikoRugo,
    onError = MuzaikoBlanko,
)

val MuzaikoMalhelaKolorskemo = darkColorScheme(
    primary = MuzaikoRugo,
    onPrimary = MuzaikoBlanko,
    primaryContainer = MuzaikoRugoProfunda,
    onPrimaryContainer = MuzaikoKremo,
    secondary = MuzaikoKremo,
    onSecondary = MuzaikoKarbono,
    secondaryContainer = MuzaikoMalhelaMalaktiva,
    onSecondaryContainer = MuzaikoKremo,
    tertiary = MuzaikoRugoMalhela,
    onTertiary = MuzaikoKremo,
    background = MuzaikoMalhelaFono,
    onBackground = MuzaikoKremo,
    surface = MuzaikoMalhelaSurfaco,
    onSurface = MuzaikoKremo,
    surfaceVariant = MuzaikoMalhelaMalaktiva,
    onSurfaceVariant = MuzaikoArgento,
    outline = MuzaikoMalhelaMalaktiva,
    outlineVariant = MuzaikoKarbono,
    error = MuzaikoRugo,
    onError = MuzaikoBlanko,
)

// === Formoj ===

val MuzaikoFormoj = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

// === Tiparo ===

val MuzaikoTiparo = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 11.sp),
)

/**
 * elekto inter hela kaj malhela temo.
 */
@androidx.compose.runtime.Composable
fun muzaikoKolorskemo(malhela: Boolean = isSystemInDarkTheme()): ColorScheme =
    if (malhela) MuzaikoMalhelaKolorskemo else MuzaikoHelaKolorskemo
