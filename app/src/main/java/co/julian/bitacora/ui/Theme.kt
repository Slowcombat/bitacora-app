package co.julian.bitacora.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val Claro = lightColorScheme(
    primary = Color(0xFF2952C9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E9FB),
    onPrimaryContainer = Color(0xFF18212E),
    secondary = Color(0xFF7A4CC2),
    secondaryContainer = Color(0xFFEFE7FB),
    onSecondaryContainer = Color(0xFF18212E),
    tertiary = Color(0xFF1F8A5B),
    background = Color(0xFFEEF1F5),
    onBackground = Color(0xFF18212E),
    surface = Color.White,
    onSurface = Color(0xFF18212E),
    surfaceVariant = Color(0xFFE7EAF0),
    onSurfaceVariant = Color(0xFF5B6576),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F8FB),
    outline = Color(0xFFC9D0DB),
    outlineVariant = Color(0xFFD6DCE5),
    error = Color(0xFFB3261E),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF7F9CF5),
    onPrimary = Color(0xFF10151D),
    primaryContainer = Color(0xFF1F2B48),
    onPrimaryContainer = Color(0xFFE6EBF2),
    secondary = Color(0xFFB596EC),
    secondaryContainer = Color(0xFF2E2442),
    onSecondaryContainer = Color(0xFFE6EBF2),
    tertiary = Color(0xFF5CC795),
    background = Color(0xFF10151D),
    onBackground = Color(0xFFE6EBF2),
    surface = Color(0xFF18202B),
    onSurface = Color(0xFFE6EBF2),
    surfaceVariant = Color(0xFF232C39),
    onSurfaceVariant = Color(0xFF9AA5B6),
    surfaceContainer = Color(0xFF18202B),
    surfaceContainerLow = Color(0xFF141B24),
    outline = Color(0xFF3A4555),
    outlineVariant = Color(0xFF2A3443),
    error = Color(0xFFF2B8B5),
)

/** Colores por tipo de bloque: (principal, fondo suave). */
class ColoresTipo(private val oscuro: Boolean) {
    fun principal(tipo: String): Color = when (tipo) {
        "uni" -> if (oscuro) Color(0xFFE3B062) else Color(0xFFB7791F)
        "study" -> if (oscuro) Color(0xFF7F9CF5) else Color(0xFF2952C9)
        "eng" -> if (oscuro) Color(0xFFB596EC) else Color(0xFF7A4CC2)
        "gym" -> if (oscuro) Color(0xFF5CC795) else Color(0xFF1F8A5B)
        "work" -> if (oscuro) Color(0xFF9AA5B6) else Color(0xFF5B6576)
        else -> if (oscuro) Color(0xFF7D8798) else Color(0xFF8A93A3)
    }

    fun suave(tipo: String): Color = when (tipo) {
        "uni" -> if (oscuro) Color(0xFF3A2C16) else Color(0xFFFBF0DC)
        "study" -> if (oscuro) Color(0xFF1F2B48) else Color(0xFFE2E9FB)
        "eng" -> if (oscuro) Color(0xFF2E2442) else Color(0xFFEFE7FB)
        "gym" -> if (oscuro) Color(0xFF16362A) else Color(0xFFDDF3E8)
        "work" -> if (oscuro) Color(0xFF232C39) else Color(0xFFE7EAF0)
        else -> if (oscuro) Color(0xFF1D2530) else Color(0xFFF1F3F6)
    }

    val bien: Color get() = principal("gym")
    val alerta: Color get() = principal("uni")
}

val LocalColoresTipo = staticCompositionLocalOf { ColoresTipo(false) }

@Composable
fun BitacoraTheme(content: @Composable () -> Unit) {
    val oscuro = isSystemInDarkTheme()
    CompositionLocalProvider(LocalColoresTipo provides ColoresTipo(oscuro)) {
        MaterialTheme(colorScheme = if (oscuro) Oscuro else Claro, content = content)
    }
}
