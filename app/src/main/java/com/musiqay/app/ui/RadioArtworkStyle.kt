package com.musiqay.app.ui

import androidx.compose.ui.graphics.Color

// Preserve contrast for the broadcasters' original transparent logos.
fun radioLogoBackground(stationId: String?): Color =
    if (stationId == "nogoum" || stationId == "quran") Color(0xFF145C3D) else Color.White
