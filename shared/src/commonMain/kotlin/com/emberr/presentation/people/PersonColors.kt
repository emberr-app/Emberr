package com.emberr.presentation.people

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.emberr.domain.model.PersonDetailKind
import com.emberr.ui.theme.LocalAppIsDark

private const val DEGREES_IN_A_CIRCLE = 360
private const val HUE_HASH_MULTIPLIER = 31
private const val AVATAR_SECOND_HUE_SHIFT = 38f
private const val GLOW_SECOND_HUE_SHIFT = 45f
private const val GLOW_THIRD_HUE_SHIFT = -25f
private const val KIND_TILE_LIGHT_END_SHARE_OF_KIND_COLOR = 0.68f

fun personHue(personNoteId: String): Int {
    val hash = personNoteId.fold(0) { runningHash, character -> runningHash * HUE_HASH_MULTIPLIER + character.code }
    return hash.mod(DEGREES_IN_A_CIRCLE)
}

data class PersonAvatarColors(
    val gradientStart: Color,
    val gradientEnd: Color,
    val initials: Color,
    val emojiBackground: Color
)

data class PersonGlowColors(
    val topLeftSpot: Color,
    val topRightSpot: Color,
    val lowerSpot: Color,
    val wash: Color,
    val avatarShadow: Color
)

data class KindTileColors(
    val gradientStart: Color,
    val gradientEnd: Color,
    val icon: Color
)

@Composable
fun personAvatarColors(personNoteId: String): PersonAvatarColors {
    val hue = personHue(personNoteId).toFloat()
    return if (LocalAppIsDark.current) {
        PersonAvatarColors(
            gradientStart = hslColor(hue, 0.85f, 0.44f),
            gradientEnd = hslColor(hue + AVATAR_SECOND_HUE_SHIFT, 0.72f, 0.32f),
            initials = hslColor(hue, 0.45f, 0.92f),
            emojiBackground = hslColor(hue, 0.65f, 0.22f)
        )
    } else {
        PersonAvatarColors(
            gradientStart = hslColor(hue, 0.85f, 0.84f),
            gradientEnd = hslColor(hue + AVATAR_SECOND_HUE_SHIFT, 0.72f, 0.70f),
            initials = hslColor(hue, 0.45f, 0.24f),
            emojiBackground = hslColor(hue, 0.65f, 0.94f)
        )
    }
}

@Composable
fun personGlowColors(personNoteId: String): PersonGlowColors {
    val hue = personHue(personNoteId).toFloat()
    val isDark = LocalAppIsDark.current
    val mainLightness = if (isDark) 0.34f else 0.86f
    val softLightness = if (isDark) 0.26f else 0.91f
    return PersonGlowColors(
        topLeftSpot = hslColor(hue, 0.80f, mainLightness),
        topRightSpot = hslColor(hue + GLOW_SECOND_HUE_SHIFT, 0.85f, softLightness),
        lowerSpot = hslColor(hue + GLOW_THIRD_HUE_SHIFT, 0.70f, softLightness),
        wash = hslColor(hue, 0.55f, mainLightness),
        avatarShadow = hslColor(hue, 0.60f, 0.35f, alpha = 0.55f)
    )
}

fun kindTileColors(kind: PersonDetailKind): KindTileColors {
    val kindColor = kindColor(kind)
    return KindTileColors(
        gradientStart = lerp(Color.White, kindColor, KIND_TILE_LIGHT_END_SHARE_OF_KIND_COLOR),
        gradientEnd = kindColor,
        icon = Color.White
    )
}

private fun kindColor(kind: PersonDetailKind): Color = when (kind) {
    PersonDetailKind.PHONE -> Color(0xFF2FBF64)
    PersonDetailKind.EMAIL -> Color(0xFF3478F6)
    PersonDetailKind.BIRTHDAY -> Color(0xFFF2559B)
    PersonDetailKind.ADDRESS -> Color(0xFFFF8A30)
    PersonDetailKind.COMPANY -> Color(0xFF6C5CE7)
    PersonDetailKind.ROLE -> Color(0xFF12B5A5)
    PersonDetailKind.WEBSITE -> Color(0xFF0EA5E9)
    PersonDetailKind.SOCIAL -> Color(0xFFA855F7)
    PersonDetailKind.RELATIONSHIP -> Color(0xFFF43F5E)
    PersonDetailKind.CUSTOM -> Color(0xFF64748B)
}

private fun hslColor(hue: Float, saturation: Float, lightness: Float, alpha: Float = 1f): Color =
    Color.hsl(hue.mod(DEGREES_IN_A_CIRCLE.toFloat()), saturation, lightness, alpha)
