package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Color palettes for premium themes
object ThemePreset {
    
    // 1. Infinity AMOLED Dark (Teal & Ocean Blue)
    private val AmoledDarkPrimary = Color(0xFF00E5FF)
    private val AmoledDarkSecondary = Color(0xFF00E676)
    private val AmoledDarkTertiary = Color(0xFF2979FF)
    
    // 2. Cyberpunk Neon (Hot Pink & Yellow)
    private val CyberpunkPrimary = Color(0xFFFF007F)
    private val CyberpunkSecondary = Color(0xFFCC00FF)
    private val CyberpunkTertiary = PatternColors.NeonYellow
    
    // 3. Royal Amethyst (Purple & Gold)
    private val RoyalPrimary = Color(0xFFD4AF37) // Gold
    private val RoyalSecondary = Color(0xFF9C27B0) // Violet
    private val RoyalTertiary = Color(0xFFE040FB) // Orchid
    
    // 4. Midnight Ocean (Deep Navy & Coral)
    private val OceanPrimary = Color(0xFF40C4FF)
    private val OceanSecondary = Color(0xFFFF5252)
    private val OceanTertiary = Color(0xFFFFAB40)

    // 5. Cosmic Forest (Evergreen & Mint)
    private val ForestPrimary = Color(0xFF69F0AE)
    private val ForestSecondary = Color(0xFF4CAF50)
    private val ForestTertiary = Color(0xFF81C784)

    fun getColorScheme(themeName: String, amoledDark: Boolean): ColorScheme {
        val baseScheme = when (themeName) {
            "Cyberpunk Neon" -> darkColorScheme(
                primary = CyberpunkPrimary,
                secondary = CyberpunkSecondary,
                tertiary = CyberpunkTertiary,
                background = Color(0xFF0F0B13),
                surface = Color(0xFF1B1525),
                onPrimary = Color.Black,
                onSecondary = Color.White,
                onBackground = Color(0xFFECE5F0),
                onSurface = Color(0xFFECE5F0)
            )
            "Royal Amethyst" -> darkColorScheme(
                primary = RoyalPrimary,
                secondary = RoyalSecondary,
                tertiary = RoyalTertiary,
                background = Color(0xFF120C1F),
                surface = Color(0xFF211738),
                onPrimary = Color.Black,
                onSecondary = Color.White,
                onBackground = Color(0xFFF1EBF9),
                onSurface = Color(0xFFF1EBF9)
            )
            "Midnight Ocean" -> darkColorScheme(
                primary = OceanPrimary,
                secondary = OceanSecondary,
                tertiary = OceanTertiary,
                background = Color(0xFF08121E),
                surface = Color(0xFF0E2034),
                onPrimary = Color.Black,
                onSecondary = Color.White,
                onBackground = Color(0xFFE1EFFC),
                onSurface = Color(0xFFE1EFFC)
            )
            "Cosmic Forest" -> darkColorScheme(
                primary = ForestPrimary,
                secondary = ForestSecondary,
                tertiary = ForestTertiary,
                background = Color(0xFF071B11),
                surface = Color(0xFF0D2D1E),
                onPrimary = Color.Black,
                onSecondary = Color.White,
                onBackground = Color(0xFFE4F5ED),
                onSurface = Color(0xFFE4F5ED)
            )
            "Bento Grid" -> darkColorScheme(
                primary = Color(0xFFD0BCFF),
                secondary = Color(0xFFCCC2DC),
                tertiary = Color(0xFFEFB8C8),
                background = Color(0xFF1C1B1F),
                surface = Color(0xFF2B2930),
                onPrimary = Color(0xFF381E72),
                onSecondary = Color(0xFF332D41),
                onTertiary = Color(0xFF492532),
                onBackground = Color(0xFFE6E1E5),
                onSurface = Color(0xFFE6E1E5),
                surfaceVariant = Color(0xFF49454F),
                onSurfaceVariant = Color(0xFFCAC4D0),
                outline = Color(0xFF938F99)
            )
            "Infinity AMOLED Dark" -> darkColorScheme(
                primary = AmoledDarkPrimary,
                secondary = AmoledDarkSecondary,
                tertiary = AmoledDarkTertiary,
                background = Color(0xFF0B141C),
                surface = Color(0xFF152636),
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color(0xFFE5F1FC),
                onSurface = Color(0xFFE5F1FC)
            )
            else -> darkColorScheme( // Fallback to Bento Grid by default!
                primary = Color(0xFFD0BCFF),
                secondary = Color(0xFFCCC2DC),
                tertiary = Color(0xFFEFB8C8),
                background = Color(0xFF1C1B1F),
                surface = Color(0xFF2B2930),
                onPrimary = Color(0xFF381E72),
                onSecondary = Color(0xFF332D41),
                onTertiary = Color(0xFF492532),
                onBackground = Color(0xFFE6E1E5),
                onSurface = Color(0xFFE6E1E5),
                surfaceVariant = Color(0xFF49454F),
                onSurfaceVariant = Color(0xFFCAC4D0),
                outline = Color(0xFF938F99)
            )
        }

        return if (amoledDark) {
            // Apply true black values for pure OLED black experience
            baseScheme.copy(
                background = Color.Black,
                surface = Color(0xFF0C0C0C),
                surfaceVariant = Color(0xFF161616),
                secondaryContainer = Color(0xFF1A1A1A)
            )
        } else {
            baseScheme
        }
    }
}

object PatternColors {
    val NeonYellow = Color(0xFFFFFF00)
}
