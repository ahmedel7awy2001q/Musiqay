package com.musiqay.app

import com.musiqay.app.util.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {
    @Test fun formatsDuration() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("1:05", formatDuration(65_000))
        assertEquals("62:03", formatDuration(3_723_000))
    }
}
