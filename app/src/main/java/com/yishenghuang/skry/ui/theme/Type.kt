package com.yishenghuang.skry.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val SkrySans = FontFamily.SansSerif

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = SkrySans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.8).sp,
        color = SkryColors.OnBackground
    ),
    headlineLarge = TextStyle(
        fontFamily = SkrySans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.56).sp,
        color = SkryColors.OnBackground
    ),
    headlineMedium = TextStyle(
        fontFamily = SkrySans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.44).sp,
        color = SkryColors.OnBackground
    ),
    titleLarge = TextStyle(
        fontFamily = SkrySans,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.36).sp,
        color = SkryColors.OnBackground
    ),
    titleMedium = TextStyle(
        fontFamily = SkrySans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
        color = SkryColors.OnBackground
    ),
    bodyLarge = TextStyle(
        fontFamily = SkrySans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        color = SkryColors.OnBackground
    ),
    bodyMedium = TextStyle(
        fontFamily = SkrySans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        color = SkryColors.OnSurfaceMuted
    ),
    labelLarge = TextStyle(
        fontFamily = SkrySans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        color = SkryColors.OnBackground
    ),
    labelMedium = TextStyle(
        fontFamily = SkrySans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        color = SkryColors.TagForeground
    ),
    labelSmall = TextStyle(
        fontFamily = SkrySans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp,
        color = SkryColors.Accent
    )
)
