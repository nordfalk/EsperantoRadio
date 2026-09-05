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

// === Esperanto-verdaj koloroj ===
// Inspiro: Esperanto-flago (#009900), muzaiko.info fono (#f0edea), TEJO verdo (#007D13)

val EsperantoVerdo = Color(0xFF009900)       // Esperanto-flago verdo — chefa akcento
val VerdoMalhela = Color(0xFF007D13)         // TEJO-verdo — emfazo, premata
val VerdoProfunda = Color(0xFF004D0C)        // Malhela temo, profundeco
val VerdoHela = Color(0xFFE8F5E9)            // Hela verda fono por kartoj
val VerdoMeza = Color(0xFF4CAF50)            // Meza verdo por malaktivaj akcentoj

// Fono de muzaiko.info
val MuzaikoKremo = Color(0xFFF0EDEA)         // muzaiko.info fono (varma kremo)
val Karbono = Color(0xFF1A1A1A)              // Teksto, malhela fono
val Argento = Color(0xFFE0E0E0)              // Dividiloj, malaktivaj

// Kontrasta ruĝo (de Muzaiko-emblemo) por eraroj kaj avertmarkoj
val MuzikoRugo = Color(0xFFD0002F)          // Muzaiko-emblemo ruĝo

// === Kolorskemoj ===

val EsperantoHelaKolorskemo = lightColorScheme(
    primary = EsperantoVerdo,
    onPrimary = Color.White,
    primaryContainer = VerdoMalhela,
    onPrimaryContainer = Color.White,
    secondary = Karbono,
    onSecondary = Color.White,
    secondaryContainer = Argento,
    onSecondaryContainer = Karbono,
    tertiary = VerdoMeza,
    onTertiary = Karbono,
    background = MuzaikoKremo,
    onBackground = Karbono,
    surface = Color.White,
    onSurface = Karbono,
    surfaceVariant = Argento,
    onSurfaceVariant = Karbono,
    outline = Argento,
    outlineVariant = MuzaikoKremo,
    error = MuzikoRugo,
    onError = Color.White,
)

val EsperantoMalhelaKolorskemo = darkColorScheme(
    primary = EsperantoVerdo,
    onPrimary = Color.White,
    primaryContainer = VerdoProfunda,
    onPrimaryContainer = VerdoHela,
    secondary = Color(0xFFB0BEC5),
    onSecondary = Karbono,
    secondaryContainer = Color(0xFF2E3B2E),
    onSecondaryContainer = VerdoHela,
    tertiary = VerdoMeza,
    onTertiary = Karbono,
    background = Color(0xFF1A2E1A),
    onBackground = Color(0xFFE8F5E9),
    surface = Color(0xFF243824),
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF2E3B2E),
    onSurfaceVariant = Argento,
    outline = Color(0xFF2E3B2E),
    outlineVariant = Color(0xFF1A2E1A),
    error = MuzikoRugo,
    onError = Color.White,
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
    if (malhela) EsperantoMalhelaKolorskemo else EsperantoHelaKolorskemo
