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

// === Temoj ===

enum class TemoNomo(val etikedo: String) {
    RUGXA("Ruĝa Muzaiko"),
    ANTONIA("Antonia (Figma)"),
    VERDA("Verda Esperanto"),
}

// === Ruĝa Muzaiko-koloroj ===

private val Rugxo = Color(0xFFD0002F)
private val RugxoMalhela = Color(0xFF9F001E)
private val RugxoProfunda = Color(0xFF4F000E)
private val Kremo = Color(0xFFFAF6F2)
private val KarbonoRugxa = Color(0xFF1A1413)
private val Argento = Color(0xFFE8E2DD)

private val RugxaHela = lightColorScheme(
    primary = Rugxo, onPrimary = Color.White,
    primaryContainer = RugxoMalhela, onPrimaryContainer = Kremo,
    secondary = KarbonoRugxa, onSecondary = Kremo,
    secondaryContainer = Argento, onSecondaryContainer = KarbonoRugxa,
    tertiary = RugxoProfunda, onTertiary = Kremo,
    background = Kremo, onBackground = KarbonoRugxa,
    surface = Color.White, onSurface = KarbonoRugxa,
    surfaceVariant = Argento, onSurfaceVariant = KarbonoRugxa,
    outline = Argento, outlineVariant = Kremo,
    error = Rugxo, onError = Color.White,
)

private val RugxaMalhela = darkColorScheme(
    primary = Rugxo, onPrimary = Color.White,
    primaryContainer = RugxoProfunda, onPrimaryContainer = Kremo,
    secondary = Kremo, onSecondary = KarbonoRugxa,
    secondaryContainer = Color(0xFF3A3030), onSecondaryContainer = Kremo,
    tertiary = RugxoMalhela, onTertiary = Kremo,
    background = KarbonoRugxa, onBackground = Kremo,
    surface = Color(0xFF241D1B), onSurface = Kremo,
    surfaceVariant = Color(0xFF3A3030), onSurfaceVariant = Argento,
    outline = Color(0xFF3A3030), outlineVariant = KarbonoRugxa,
    error = Rugxo, onError = Color.White,
)

// === Antonia-koloroj (de Figma) ===

private val AntoniaPurpura = Color(0xFF2A1D40)
private val AntoniaRozeta = Color(0xFFEABDC5)
private val AntoniaGrizaBruna = Color(0xFF746E6F)
private val AntoniaKartFono = Color(0xFFF6F6F6)
private val AntoniaLokokupilo = Color(0xFFC4C4C4)
private val AntoniaEraro = Color(0xFFB00020)

private val AntoniaHela = lightColorScheme(
    primary = AntoniaPurpura, onPrimary = Color.White,
    primaryContainer = Color(0xFF32000A), onPrimaryContainer = AntoniaRozeta,
    secondary = AntoniaGrizaBruna, onSecondary = Color.White,
    secondaryContainer = AntoniaKartFono, onSecondaryContainer = AntoniaPurpura,
    tertiary = AntoniaRozeta, onTertiary = AntoniaPurpura,
    background = Color.White, onBackground = AntoniaPurpura,
    surface = Color.White, onSurface = AntoniaPurpura,
    surfaceVariant = AntoniaKartFono, onSurfaceVariant = AntoniaGrizaBruna,
    outline = AntoniaLokokupilo, outlineVariant = AntoniaKartFono,
    error = AntoniaEraro, onError = Color.White,
)

private val AntoniaMalhela = darkColorScheme(
    primary = AntoniaRozeta, onPrimary = AntoniaPurpura,
    primaryContainer = AntoniaPurpura, onPrimaryContainer = AntoniaRozeta,
    secondary = Color(0xFFB0AAB0), onSecondary = Color(0xFF1A1424),
    secondaryContainer = Color(0xFF2E2830), onSecondaryContainer = AntoniaRozeta,
    tertiary = AntoniaRozeta, onTertiary = AntoniaPurpura,
    background = Color(0xFF1A1424), onBackground = Color(0xFFE8E2E8),
    surface = Color(0xFF241D30), onSurface = Color(0xFFE8E2E8),
    surfaceVariant = Color(0xFF2E2830), onSurfaceVariant = Color(0xFFB0AAB0),
    outline = Color(0xFF2E2830), outlineVariant = Color(0xFF1A1424),
    error = AntoniaEraro, onError = Color.White,
)

// === Verda Esperanto-koloroj ===

private val Verdo = Color(0xFF009900)
private val VerdoMalhela = Color(0xFF007D13)
private val VerdoProfunda = Color(0xFF004D0C)
private val VerdoKremo = Color(0xFFF0EDEA)

private val VerdaHela = lightColorScheme(
    primary = Verdo, onPrimary = Color.White,
    primaryContainer = VerdoMalhela, onPrimaryContainer = Color.White,
    secondary = Color(0xFF1A1A1A), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0), onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = Color(0xFF4CAF50), onTertiary = Color(0xFF1A1A1A),
    background = VerdoKremo, onBackground = Color(0xFF1A1A1A),
    surface = Color.White, onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE0E0E0), onSurfaceVariant = Color(0xFF1A1A1A),
    outline = Color(0xFFE0E0E0), outlineVariant = VerdoKremo,
    error = Color(0xFFD0002F), onError = Color.White,
)

private val VerdaMalhela = darkColorScheme(
    primary = Verdo, onPrimary = Color.White,
    primaryContainer = VerdoProfunda, onPrimaryContainer = Color(0xFFE8F5E9),
    secondary = Color(0xFFB0BEC5), onSecondary = Color(0xFF1A2E1A),
    secondaryContainer = Color(0xFF2E3B2E), onSecondaryContainer = Color(0xFFE8F5E9),
    tertiary = Color(0xFF4CAF50), onTertiary = Color(0xFF1A2E1A),
    background = Color(0xFF1A2E1A), onBackground = Color(0xFFE8F5E9),
    surface = Color(0xFF243824), onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF2E3B2E), onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF2E3B2E), outlineVariant = Color(0xFF1A2E1A),
    error = Color(0xFFD0002F), onError = Color.White,
)

// === Elektilo ===

fun temuKolorskemo(temo: TemoNomo, malhela: Boolean): ColorScheme = when (temo) {
    TemoNomo.RUGXA -> if (malhela) RugxaMalhela else RugxaHela
    TemoNomo.ANTONIA -> if (malhela) AntoniaMalhela else AntoniaHela
    TemoNomo.VERDA -> if (malhela) VerdaMalhela else VerdaHela
}

@androidx.compose.runtime.Composable
fun muzaikoKolorskemo(malhela: Boolean = isSystemInDarkTheme()): ColorScheme =
    temuKolorskemo(TemoNomo.ANTONIA, malhela)

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
