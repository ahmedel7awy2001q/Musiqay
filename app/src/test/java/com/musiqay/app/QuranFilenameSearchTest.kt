package com.musiqay.app

import com.musiqay.app.util.displayTitle
import com.musiqay.app.util.filenameMatchesSurahNumber
import com.musiqay.app.util.filenameMatchesSurahQuery
import com.musiqay.app.util.surahNumberForQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranFilenameSearchTest {
    @Test
    fun resolvesSurahNameToMushafOrder() {
        assertEquals(2, surahNumberForQuery("سورة البقرة"))
        assertEquals(18, surahNumberForQuery("الكهف"))
        assertEquals(114, surahNumberForQuery("سورة الناس"))
    }

    @Test
    fun matchesPaddedAndUnpaddedSurahNumbersOnlyAsTokens() {
        assertTrue(filenameMatchesSurahNumber("018.mp3", 18))
        assertTrue(filenameMatchesSurahNumber("18.mp3", 18))
        assertTrue(filenameMatchesSurahNumber("0018 - recitation.mp3", 18))
        assertFalse(filenameMatchesSurahNumber("118.mp3", 18))
        assertFalse(filenameMatchesSurahNumber("180.mp3", 18))
    }

    @Test
    fun surahQueryMatchesNameOrNumberInFilename() {
        assertTrue(filenameMatchesSurahQuery("سورة الكهف - ياسر.mp3", "سورة الكهف"))
        assertTrue(filenameMatchesSurahQuery("018.mp3", "سورة الكهف"))
        assertTrue(filenameMatchesSurahQuery("18.mp3", "الكهف"))
        assertFalse(filenameMatchesSurahQuery("019.mp3", "سورة الكهف"))
    }

    @Test
    fun displayTitleDoesNotRewriteStoredFilename() {
        assertEquals("سورة الكهف - ياسر.mp3", displayTitle("سورة الكهف - ياسر.mp3"))
        assertEquals("018.mp3", displayTitle("018.mp3"))
    }
}
