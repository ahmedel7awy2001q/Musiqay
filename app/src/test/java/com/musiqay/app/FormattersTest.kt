package com.musiqay.app

import com.musiqay.app.util.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {
    @Test fun formatsDuration() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("1:05", formatDuration(65_000))
        assertEquals("1:02:03", formatDuration(3_723_000))
    }
    @Test fun formattingIsStableUnderArabicLocale() {
        val previous = java.util.Locale.getDefault()
        try { java.util.Locale.setDefault(java.util.Locale("ar", "EG")); assertEquals("7:59", formatDuration(479_000)); assertEquals("0:00", formatDuration(-1)) }
        finally { java.util.Locale.setDefault(previous) }
    }
}
