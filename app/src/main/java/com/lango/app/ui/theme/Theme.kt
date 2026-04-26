package com.lango.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun LangoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LangoColorScheme,
        typography = LangoTypography,
        content = content
    )
}
