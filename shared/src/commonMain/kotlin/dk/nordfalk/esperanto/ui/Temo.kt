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

// === Antonia-koloroj (de Figma-dizajno "Muzaiko — Antonia") ===

val AntoniaPurpura = Color(0xFF2A1D40)      // titoloj, priskriboj
val AntoniaRuhaPurpura = Color(0xFF32000A)   // kanalnomo (tre malhela)
val AntoniaGrizaBruna = Color(0xFF746E6F)    // kanalnomoj en kartoj
val AntoniaKartFono = Color(0xFFF6F6F6)      // kartfono
val AntoniaLokokupilo = Color(0xFFC4C4C4)    // bildlokokupilo
val AntoniaRozeta = Color(0xFFEABDC5)        // favorato, aktiva langeto, ludbutono
val AntoniaEraro = Color(0xFFB00020)        // eraroj
val AntoniaBlanko = Color(0xFFFFFFFF)        // fono
val AntoniaNigra = Color(0xFF000000)         // teksto
val AntoniaMalhelaFono = Color(0xFF1A1424)   // malhela temo fono
val AntoniaMalhelaSurfaco = Color(0xFF241D30) // malhela temo surfaco

// === Kolorskemoj ===

val MuzaikoHelaKolorskemo = lightColorScheme(
    primary = AntoniaPurpura,
    onPrimary = AntoniaBlanko,
    primaryContainer = AntoniaRuhaPurpura,
    onPrimaryContainer = AntoniaRozeta,
    secondary = AntoniaGrizaBruna,
    onSecondary = AntoniaBlanko,
    secondaryContainer = AntoniaKartFono,
    onSecondaryContainer = AntoniaPurpura,
    tertiary = AntoniaRozeta,
    onTertiary = AntoniaPurpura,
    background = AntoniaBlanko,
    onBackground = AntoniaPurpura,
    surface = AntoniaBlanko,
    onSurface = AntoniaPurpura,
    surfaceVariant = AntoniaKartFono,
    onSurfaceVariant = AntoniaGrizaBruna,
    outline = AntoniaLokokupilo,
    outlineVariant = AntoniaKartFono,
    error = AntoniaEraro,
    onError = AntoniaBlanko,
)

val MuzaikoMalhelaKolorskemo = darkColorScheme(
    primary = AntoniaRozeta,
    onPrimary = AntoniaPurpura,
    primaryContainer = AntoniaPurpura,
    onPrimaryContainer = AntoniaRozeta,
    secondary = Color(0xFFB0AAB0),
    onSecondary = AntoniaMalhelaFono,
    secondaryContainer = Color(0xFF2E2830),
    onSecondaryContainer = AntoniaRozeta,
    tertiary = AntoniaRozeta,
    onTertiary = AntoniaPurpura,
    background = AntoniaMalhelaFono,
    onBackground = Color(0xFFE8E2E8),
    surface = AntoniaMalhelaSurfaco,
    onSurface = Color(0xFFE8E2E8),
    surfaceVariant = Color(0xFF2E2830),
    onSurfaceVariant = Color(0xFFB0AAB0),
    outline = Color(0xFF2E2830),
    outlineVariant = AntoniaMalhelaFono,
    error = AntoniaEraro,
    onError = AntoniaBlanko,
)

// === Formoj ===

val MuzaikoFormoj = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(15.dp),
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
