package com.emberr.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

const val FULL_THEME_STRENGTH = 100

private val EmberrLightColorScheme = lightColorScheme(
    primary          = CharcoalNoir,
    onPrimary        = CloudVeil,
    secondary        = CharcoalNoir,
    onSecondary      = CloudVeil,
    background       = Color.White,
    onBackground     = Color.Black,
    surface          = CloudVeil,
    onSurface        = CharcoalNoir,
    outline          = UrbanFog,
    surfaceVariant = CloudVeil,
    surfaceContainerHigh = Color(0xFFD8D8D8)
)

private val EmberrDarkColorScheme = darkColorScheme(
    primary          = CloudVeil,
    onPrimary        = CharcoalNoir,
    secondary        = CloudVeil,
    onSecondary      = CharcoalNoir,
    background       = Color.Black,
    onBackground     = Color.White,
    surface          = IroncladGrey,
    onSurface        = CloudVeil,
    outline          = UrbanFog,
    surfaceVariant = Color(0xFF363636),
    surfaceContainerHigh = Color(0xFF363636)
)

private val SoftLightColorScheme = lightColorScheme(
    primary          = Color(0xFF1C1C1C),
    onPrimary        = Color(0xFFF2F2F2),
    secondary        = Color(0xFF1C1C1C),
    onSecondary      = Color(0xFFF2F2F2),
    background       = Color(0xFFF2F2F2),
    onBackground     = Color(0xFF1C1C1C),
    surface          = Color(0xFFE6E6E6),
    onSurface        = Color(0xFF1C1C1C),
    outline          = UrbanFog,
    outlineVariant   = Color(0xFFCFCFCF),
    onSurfaceVariant = Color(0xFF4A4A4A),
    surfaceVariant = Color(0xFFE6E6E6),
    surfaceContainerHigh = Color(0xFFD4D4D4)
)

private val SoftDarkColorScheme = darkColorScheme(
    primary          = Color(0xFFE6E6E6),
    onPrimary        = Color(0xFF101010),
    secondary        = Color(0xFFE6E6E6),
    onSecondary      = Color(0xFF101010),
    background       = Color(0xFF101010),
    onBackground     = Color(0xFFE6E6E6),
    surface          = Color(0xFF1C1C1C),
    onSurface        = Color(0xFFE6E6E6),
    outline          = Color(0xFF8A8A8A),
    outlineVariant   = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBDBDBD),
    surfaceVariant = Color(0xFF313131),
    surfaceContainerHigh = Color(0xFF313131)
)

private val HighContrastLightColorScheme = lightColorScheme(
    primary          = Color.Black,
    onPrimary        = Color.White,
    secondary        = Color(0xFF0040C0),
    onSecondary      = Color.White,
    background       = Color.White,
    onBackground     = Color.Black,
    surface          = Color(0xFFEBEBEB),
    onSurface        = Color.Black,
    outline          = Color(0xFF3D3D3D),
    outlineVariant   = Color(0xFF8A8A8A),
    onSurfaceVariant = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFEBEBEB),
    surfaceContainerHigh = Color(0xFFD6D6D6)
)

private val HighContrastDarkColorScheme = darkColorScheme(
    primary          = Color.White,
    onPrimary        = Color.Black,
    secondary        = Color(0xFFFFD600),
    onSecondary      = Color.Black,
    background       = Color.Black,
    onBackground     = Color.White,
    surface          = Color(0xFF141414),
    onSurface        = Color.White,
    outline          = Color(0xFFC2C2C2),
    outlineVariant   = Color(0xFF7A7A7A),
    onSurfaceVariant = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF2E2E2E),
    surfaceContainerHigh = Color(0xFF2E2E2E)
)

private val TerracottaLightColorScheme = lightColorScheme(
    primary          = Color(0xFF2B1D14),
    onPrimary        = Color(0xFFFFFBF7),
    secondary        = Color(0xFFE0662B),
    onSecondary      = Color.White,
    background       = Color(0xFFFFFBF7),
    onBackground     = Color(0xFF2B1D14),
    surface          = Color(0xFFF5EDE6),
    onSurface        = Color(0xFF2B1D14),
    outline          = Color(0xFF8C7A6E),
    outlineVariant   = Color(0xFFDCCFC4),
    onSurfaceVariant = Color(0xFF5A4A3F),
    surfaceVariant = Color(0xFFF5EDE6),
    surfaceContainerHigh = Color(0xFFE5D8CD)
)

private val TerracottaDarkColorScheme = darkColorScheme(
    primary          = Color(0xFFF3E6DA),
    onPrimary        = Color(0xFF16110D),
    secondary        = Color(0xFFF08A4B),
    onSecondary      = Color(0xFF16110D),
    background       = Color(0xFF16110D),
    onBackground     = Color(0xFFF3E6DA),
    surface          = Color(0xFF241C16),
    onSurface        = Color(0xFFF3E6DA),
    outline          = Color(0xFF8F7F72),
    outlineVariant   = Color(0xFF4A3D33),
    onSurfaceVariant = Color(0xFFD6C6B8),
    surfaceVariant = Color(0xFF3A2E25),
    surfaceContainerHigh = Color(0xFF3A2E25)
)

private val PaperLightColorScheme = lightColorScheme(
    primary          = Color(0xFF3B2F22),
    onPrimary        = Color(0xFFF6F0E3),
    secondary        = Color(0xFF8A6A3F),
    onSecondary      = Color(0xFFF6F0E3),
    background       = Color(0xFFF6F0E3),
    onBackground     = Color(0xFF3B2F22),
    surface          = Color(0xFFECE3D0),
    onSurface        = Color(0xFF3B2F22),
    outline          = Color(0xFF8E7F68),
    outlineVariant   = Color(0xFFD6CAB2),
    onSurfaceVariant = Color(0xFF5E5040),
    surfaceVariant = Color(0xFFECE3D0),
    surfaceContainerHigh = Color(0xFFDDD1B9)
)

private val PaperDarkColorScheme = darkColorScheme(
    primary          = Color(0xFFE8DCC4),
    onPrimary        = Color(0xFF1E1A14),
    secondary        = Color(0xFFC49A62),
    onSecondary      = Color(0xFF1E1A14),
    background       = Color(0xFF1E1A14),
    onBackground     = Color(0xFFE8DCC4),
    surface          = Color(0xFF2B251D),
    onSurface        = Color(0xFFE8DCC4),
    outline          = Color(0xFF9A8C74),
    outlineVariant   = Color(0xFF4E4538),
    onSurfaceVariant = Color(0xFFD2C5AC),
    surfaceVariant = Color(0xFF40382C),
    surfaceContainerHigh = Color(0xFF40382C)
)

private val NordLightColorScheme = lightColorScheme(
    primary          = Color(0xFF2E3440),
    onPrimary        = Color(0xFFECEFF4),
    secondary        = Color(0xFF5E81AC),
    onSecondary      = Color(0xFFECEFF4),
    background       = Color(0xFFECEFF4),
    onBackground     = Color(0xFF2E3440),
    surface          = Color(0xFFD8DEE9),
    onSurface        = Color(0xFF2E3440),
    outline          = Color(0xFF7B8394),
    outlineVariant   = Color(0xFFBCC3D0),
    onSurfaceVariant = Color(0xFF4C566A),
    surfaceVariant = Color(0xFFD8DEE9),
    surfaceContainerHigh = Color(0xFFCBD2DE)
)

private val NordDarkColorScheme = darkColorScheme(
    primary          = Color(0xFFE5E9F0),
    onPrimary        = Color(0xFF2E3440),
    secondary        = Color(0xFF88C0D0),
    onSecondary      = Color(0xFF2E3440),
    background       = Color(0xFF2E3440),
    onBackground     = Color(0xFFECEFF4),
    surface          = Color(0xFF3B4252),
    onSurface        = Color(0xFFE5E9F0),
    outline          = Color(0xFF8D96A8),
    outlineVariant   = Color(0xFF4C566A),
    onSurfaceVariant = Color(0xFFD8DEE9),
    surfaceVariant = Color(0xFF434C5E),
    surfaceContainerHigh = Color(0xFF434C5E)
)

private val ForestLightColorScheme = lightColorScheme(
    primary          = Color(0xFF1F2A1F),
    onPrimary        = Color(0xFFF3F5F0),
    secondary        = Color(0xFF5B7F4A),
    onSecondary      = Color(0xFFF3F5F0),
    background       = Color(0xFFF3F5F0),
    onBackground     = Color(0xFF1F2A1F),
    surface          = Color(0xFFE6EBE1),
    onSurface        = Color(0xFF1F2A1F),
    outline          = Color(0xFF7A8575),
    outlineVariant   = Color(0xFFCAD3C2),
    onSurfaceVariant = Color(0xFF465246),
    surfaceVariant = Color(0xFFE6EBE1),
    surfaceContainerHigh = Color(0xFFD3DBCC)
)

private val ForestDarkColorScheme = darkColorScheme(
    primary          = Color(0xFFDCE5D6),
    onPrimary        = Color(0xFF121712),
    secondary        = Color(0xFF8DB57A),
    onSecondary      = Color(0xFF121712),
    background       = Color(0xFF121712),
    onBackground     = Color(0xFFDCE5D6),
    surface          = Color(0xFF1E261E),
    onSurface        = Color(0xFFDCE5D6),
    outline          = Color(0xFF84907F),
    outlineVariant   = Color(0xFF3F4C3F),
    onSurfaceVariant = Color(0xFFC6D1BF),
    surfaceVariant = Color(0xFF334033),
    surfaceContainerHigh = Color(0xFF334033)
)

private val RosePineDawnColorScheme = lightColorScheme(
    primary          = Color(0xFF464261),
    onPrimary        = Color(0xFFFAF4ED),
    secondary        = Color(0xFFD7827E),
    onSecondary      = Color(0xFFFAF4ED),
    background       = Color(0xFFFAF4ED),
    onBackground     = Color(0xFF464261),
    surface          = Color(0xFFF2E9E1),
    onSurface        = Color(0xFF464261),
    outline          = Color(0xFF9893A5),
    outlineVariant   = Color(0xFFCECACD),
    onSurfaceVariant = Color(0xFF797593),
    surfaceVariant = Color(0xFFF2E9E1),
    surfaceContainerHigh = Color(0xFFDFDAD9)
)

private val RosePineMainColorScheme = darkColorScheme(
    primary          = Color(0xFFE0DEF4),
    onPrimary        = Color(0xFF191724),
    secondary        = Color(0xFFEBBCBA),
    onSecondary      = Color(0xFF191724),
    background       = Color(0xFF191724),
    onBackground     = Color(0xFFE0DEF4),
    surface          = Color(0xFF26233A),
    onSurface        = Color(0xFFE0DEF4),
    outline          = Color(0xFF908CAA),
    outlineVariant   = Color(0xFF403D52),
    onSurfaceVariant = Color(0xFF908CAA),
    surfaceVariant = Color(0xFF403D52),
    surfaceContainerHigh = Color(0xFF403D52)
)

private val CatppuccinLatteColorScheme = lightColorScheme(
    primary          = Color(0xFF4C4F69),
    onPrimary        = Color(0xFFEFF1F5),
    secondary        = Color(0xFF8839EF),
    onSecondary      = Color(0xFFEFF1F5),
    background       = Color(0xFFEFF1F5),
    onBackground     = Color(0xFF4C4F69),
    surface          = Color(0xFFE6E9EF),
    onSurface        = Color(0xFF4C4F69),
    outline          = Color(0xFF8C8FA1),
    outlineVariant   = Color(0xFFCCD0DA),
    onSurfaceVariant = Color(0xFF5C5F77),
    surfaceVariant = Color(0xFFE6E9EF),
    surfaceContainerHigh = Color(0xFFDCE0E8)
)

private val CatppuccinMochaColorScheme = darkColorScheme(
    primary          = Color(0xFFCDD6F4),
    onPrimary        = Color(0xFF1E1E2E),
    secondary        = Color(0xFFCBA6F7),
    onSecondary      = Color(0xFF1E1E2E),
    background       = Color(0xFF1E1E2E),
    onBackground     = Color(0xFFCDD6F4),
    surface          = Color(0xFF313244),
    onSurface        = Color(0xFFCDD6F4),
    outline          = Color(0xFF7F849C),
    outlineVariant   = Color(0xFF45475A),
    onSurfaceVariant = Color(0xFFBAC2DE),
    surfaceVariant = Color(0xFF45475A),
    surfaceContainerHigh = Color(0xFF45475A)
)

private val CatppuccinFrappeColorScheme = darkColorScheme(
    primary          = Color(0xFFC6D0F5),
    onPrimary        = Color(0xFF303446),
    secondary        = Color(0xFFCA9EE6),
    onSecondary      = Color(0xFF303446),
    background       = Color(0xFF303446),
    onBackground     = Color(0xFFC6D0F5),
    surface          = Color(0xFF414559),
    onSurface        = Color(0xFFC6D0F5),
    outline          = Color(0xFF838BA7),
    outlineVariant   = Color(0xFF51576D),
    onSurfaceVariant = Color(0xFFB5BFE2),
    surfaceVariant = Color(0xFF51576D),
    surfaceContainerHigh = Color(0xFF51576D)
)

private val TokyoNightLightColorScheme = lightColorScheme(
    primary          = Color(0xFF343B58),
    onPrimary        = Color(0xFFE1E2E7),
    secondary        = Color(0xFF2E7DE9),
    onSecondary      = Color(0xFFE1E2E7),
    background       = Color(0xFFE1E2E7),
    onBackground     = Color(0xFF343B58),
    surface          = Color(0xFFD0D5E3),
    onSurface        = Color(0xFF343B58),
    outline          = Color(0xFF848CB5),
    outlineVariant   = Color(0xFFA8AECB),
    onSurfaceVariant = Color(0xFF6172B0),
    surfaceVariant = Color(0xFFD0D5E3),
    surfaceContainerHigh = Color(0xFFC4C8DA)
)

private val TokyoNightDarkColorScheme = darkColorScheme(
    primary          = Color(0xFFC0CAF5),
    onPrimary        = Color(0xFF1A1B26),
    secondary        = Color(0xFF7AA2F7),
    onSecondary      = Color(0xFF1A1B26),
    background       = Color(0xFF1A1B26),
    onBackground     = Color(0xFFC0CAF5),
    surface          = Color(0xFF292E42),
    onSurface        = Color(0xFFC0CAF5),
    outline          = Color(0xFF737AA2),
    outlineVariant   = Color(0xFF3B4261),
    onSurfaceVariant = Color(0xFFA9B1D6),
    surfaceVariant = Color(0xFF414868),
    surfaceContainerHigh = Color(0xFF414868)
)

private val GruvboxLightColorScheme = lightColorScheme(
    primary          = Color(0xFF3C3836),
    onPrimary        = Color(0xFFFBF1C7),
    secondary        = Color(0xFFAF3A03),
    onSecondary      = Color(0xFFFBF1C7),
    background       = Color(0xFFFBF1C7),
    onBackground     = Color(0xFF3C3836),
    surface          = Color(0xFFF2E5BC),
    onSurface        = Color(0xFF3C3836),
    outline          = Color(0xFF928374),
    outlineVariant   = Color(0xFFD5C4A1),
    onSurfaceVariant = Color(0xFF7C6F64),
    surfaceVariant = Color(0xFFF2E5BC),
    surfaceContainerHigh = Color(0xFFEBDBB2)
)

private val GruvboxDarkColorScheme = darkColorScheme(
    primary          = Color(0xFFEBDBB2),
    onPrimary        = Color(0xFF282828),
    secondary        = Color(0xFFFE8019),
    onSecondary      = Color(0xFF282828),
    background       = Color(0xFF282828),
    onBackground     = Color(0xFFEBDBB2),
    surface          = Color(0xFF32302F),
    onSurface        = Color(0xFFEBDBB2),
    outline          = Color(0xFF928374),
    outlineVariant   = Color(0xFF504945),
    onSurfaceVariant = Color(0xFFA89984),
    surfaceVariant = Color(0xFF3C3836),
    surfaceContainerHigh = Color(0xFF3C3836)
)

private val EverforestLightColorScheme = lightColorScheme(
    primary          = Color(0xFF5C6A72),
    onPrimary        = Color(0xFFFDF6E3),
    secondary        = Color(0xFF8DA101),
    onSecondary      = Color(0xFFFDF6E3),
    background       = Color(0xFFFDF6E3),
    onBackground     = Color(0xFF5C6A72),
    surface          = Color(0xFFE6E2CC),
    onSurface        = Color(0xFF5C6A72),
    outline          = Color(0xFF939F91),
    outlineVariant   = Color(0xFFBDC3AF),
    onSurfaceVariant = Color(0xFF829181),
    surfaceVariant = Color(0xFFE6E2CC),
    surfaceContainerHigh = Color(0xFFE0DCC7)
)

private val EverforestDarkColorScheme = darkColorScheme(
    primary          = Color(0xFFD3C6AA),
    onPrimary        = Color(0xFF2D353B),
    secondary        = Color(0xFFA7C080),
    onSecondary      = Color(0xFF2D353B),
    background       = Color(0xFF2D353B),
    onBackground     = Color(0xFFD3C6AA),
    surface          = Color(0xFF343F44),
    onSurface        = Color(0xFFD3C6AA),
    outline          = Color(0xFF859289),
    outlineVariant   = Color(0xFF475258),
    onSurfaceVariant = Color(0xFF9DA9A0),
    surfaceVariant = Color(0xFF3D484D),
    surfaceContainerHigh = Color(0xFF3D484D)
)

class ThemeColorSchemes(val light: ColorScheme, val dark: ColorScheme)

enum class ColorThemePreference(
    val displayName: String,
    val lightColorScheme: ColorScheme,
    val darkColorScheme: ColorScheme
) {
    EMBERR("Emberr", EmberrLightColorScheme, EmberrDarkColorScheme),
    SOFT("Soft", SoftLightColorScheme, SoftDarkColorScheme),
    WALLPAPER("Wallpaper", EmberrLightColorScheme, EmberrDarkColorScheme),
    HIGH_CONTRAST("High Contrast", HighContrastLightColorScheme, HighContrastDarkColorScheme),
    TERRACOTTA("Terracotta", TerracottaLightColorScheme, TerracottaDarkColorScheme),
    PAPER("Paper", PaperLightColorScheme, PaperDarkColorScheme),
    NORD("Nord", NordLightColorScheme, NordDarkColorScheme),
    FOREST("Forest", ForestLightColorScheme, ForestDarkColorScheme),
    ROSE("Rosé Pine", RosePineDawnColorScheme, RosePineMainColorScheme),
    CATPPUCCIN_FRAPPE("Catppuccin Frappé", CatppuccinLatteColorScheme, CatppuccinFrappeColorScheme),
    CATPPUCCIN("Catppuccin Mocha", CatppuccinLatteColorScheme, CatppuccinMochaColorScheme),
    TOKYO_NIGHT("Tokyo Night", TokyoNightLightColorScheme, TokyoNightDarkColorScheme),
    GRUVBOX("Gruvbox", GruvboxLightColorScheme, GruvboxDarkColorScheme),
    EVERFOREST("Everforest", EverforestLightColorScheme, EverforestDarkColorScheme);

    fun colorSchemeFor(
        darkTheme: Boolean,
        themedTextAndIcons: Boolean,
        strengthPercent: Int = FULL_THEME_STRENGTH,
        sourceColorSchemes: ThemeColorSchemes = ThemeColorSchemes(lightColorScheme, darkColorScheme)
    ): ColorScheme {
        val themedColorScheme = if (darkTheme) sourceColorSchemes.dark else sourceColorSchemes.light
        val neutralColorScheme = if (darkTheme) EmberrDarkColorScheme else EmberrLightColorScheme
        val textAndIconColorScheme = if (themedTextAndIcons) themedColorScheme else neutralColorScheme
        val strength = strengthPercent.coerceIn(0, FULL_THEME_STRENGTH) / FULL_THEME_STRENGTH.toFloat()

        fun blend(neutralColor: Color, themedColor: Color): Color = when {
            neutralColor == themedColor || strength >= 1f -> themedColor
            strength <= 0f -> neutralColor
            else -> lerp(neutralColor, themedColor, strength)
        }

        return themedColorScheme.copy(
            primary = blend(neutralColorScheme.primary, textAndIconColorScheme.primary),
            onPrimary = blend(neutralColorScheme.onPrimary, textAndIconColorScheme.onPrimary),
            secondary = blend(neutralColorScheme.secondary, themedColorScheme.secondary),
            onSecondary = blend(neutralColorScheme.onSecondary, themedColorScheme.onSecondary),
            background = blend(neutralColorScheme.background, themedColorScheme.background),
            onBackground = blend(neutralColorScheme.onBackground, textAndIconColorScheme.onBackground),
            surface = blend(neutralColorScheme.surface, themedColorScheme.surface),
            onSurface = blend(neutralColorScheme.onSurface, textAndIconColorScheme.onSurface),
            outline = blend(neutralColorScheme.outline, textAndIconColorScheme.outline),
            outlineVariant = blend(neutralColorScheme.outlineVariant, themedColorScheme.outlineVariant),
            onSurfaceVariant = blend(neutralColorScheme.onSurfaceVariant, textAndIconColorScheme.onSurfaceVariant),
            surfaceVariant = blend(neutralColorScheme.surfaceVariant, themedColorScheme.surfaceVariant),
            surfaceContainerHigh = blend(neutralColorScheme.surfaceContainerHigh, themedColorScheme.surfaceContainerHigh)
        )
    }
}

fun colorThemePreferenceNamed(name: String): ColorThemePreference =
    ColorThemePreference.entries.firstOrNull { it.name == name } ?: ColorThemePreference.EMBERR
