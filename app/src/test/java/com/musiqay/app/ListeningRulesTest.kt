package com.musiqay.app

import com.musiqay.app.util.*
import org.junit.Assert.*
import org.junit.Test

class ListeningRulesTest {
    @Test fun completedAndInvalidFilesDoNotResumeNearTheirEnd() {
        assertEquals(0L, resumePosition(57_001, 60_000))
        assertEquals(0L, resumePosition(-10, 60_000))
        assertEquals(0L, resumePosition(10_000, 0))
        assertEquals(25_000L, resumePosition(25_000, 60_000))
    }
    @Test fun seekingCannotLeaveTheFile() {
        assertEquals(0L, boundedSeek(5_000, -10_000, 120_000))
        assertEquals(120_000L, boundedSeek(115_000, 30_000, 120_000))
        assertEquals(35_000L, boundedSeek(5_000, 30_000, 120_000))
    }
    @Test fun displayTitlesPreserveStoredFilenamesWhileSurahDetectionRemainsAvailable() {
        val storedName = "من الروائع للمنشاوي سورة يوسف تلاوة جودة عالية.mp3"
        assertEquals(storedName, displayTitle(storedName))
        assertEquals("يوسف", detectedSurah(storedName))
        assertEquals("آل عمران", detectedSurah("سُورَة آل عمران"))
        assertEquals("ق", detectedSurah("سورة ق"))
        assertNull(detectedSurah("سورة قلب المدينة"))
        assertEquals("محاضرة طويلة.mp3", displayTitle("محاضرة طويلة.mp3"))
    }
    @Test fun radioRecitersAreNotMisclassifiedAsMusic() {
        assertEquals("قرآن", radioCategory("إذاعة ماهر المعيقلي", ""))
        assertEquals("قرآن", radioCategory("beautiful recitation", ""))
        assertEquals("موسيقى ومنوعات", radioCategory("Nile FM", "music"))
    }
    @Test fun emptyMetadataAndSourceAlbumsDoNotPolluteDisplay() {
        assertEquals("", displayArtist("فنان غير معروف"))
        assertEquals("", displayAlbum("SnapTube Audio"))
        assertEquals("جلسة مباشرة", displayAlbum("جلسة مباشرة"))
        assertEquals("إذاعة ماهر المعيقلي", cleanRadioStationName(".إذاعة ماهر المعيقلي."))
    }
}
