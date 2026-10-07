package com.musiqay.app

import com.musiqay.app.util.*
import org.junit.Assert.*
import org.junit.Test

class RadioRulesTest {
    @Test fun radioIdsAreSeparateFromLocalTracks() {
        assertTrue(isRadioMediaId("radio:quran"))
        assertFalse(isRadioMediaId("1234"))
        assertFalse(isRadioMediaId(null))
    }
    @Test fun duplicateOccurrenceAndProgressSurviveAddingWhileOnRadio() {
        val saved = MusicSession(listOf(5, 8, 5), 2, 37_000, true, 1)
        val edited = saved.append(9, next = true).append(10)
        assertEquals(listOf(5L, 8L, 5L, 9L, 10L), edited.ids)
        assertEquals(2, edited.index)
        assertEquals(37_000L, edited.positionMs)
        assertTrue(edited.shuffle)
        assertEquals(1, edited.repeat)
    }
    @Test fun missingCurrentTrackDoesNotTransferItsProgress() {
        val saved = MusicSession(listOf(5, 8, 9), 1, 37_000).available(setOf(5, 9))
        assertEquals(listOf(5L, 9L), saved.ids)
        assertEquals(9L, saved.currentId)
        assertEquals(0L, saved.positionMs)
    }
    @Test fun missingEarlierTrackKeepsSelectedDuplicate() {
        val saved = MusicSession(listOf(1, 5, 8, 5), 3, 37_000).available(setOf(5, 8))
        assertEquals(2, saved.index)
        assertEquals(37_000L, saved.positionMs)
    }
    @Test fun emptyQueueRemainsEmpty() {
        val saved = MusicSession().available(setOf(5, 8))
        assertTrue(saved.ids.isEmpty()); assertNull(saved.currentId)
    }
    @Test fun undoOnRadioPreservesOrRestoresCurrentOccurrence() {
        val saved = MusicSession(listOf(5, 8, 5), 2, 37_000)
        val restored = saved.insert(9, 1)
        assertEquals(listOf(5L, 9L, 8L, 5L), restored.ids)
        assertEquals(3, restored.index); assertEquals(37_000L, restored.positionMs)
        val currentRestored = saved.insert(9, 1, restorePositionMs = 2_500)
        assertEquals(9L, currentRestored.currentId); assertEquals(2_500L, currentRestored.positionMs)
    }
    @Test fun retriesStopAfterThreeAttempts() {
        assertEquals(3_000L, radioRetryDelayMs(0)); assertEquals(12_000L, radioRetryDelayMs(2))
        assertNull(radioRetryDelayMs(3)); assertNull(radioRetryDelayMs(-1))
    }
    @Test fun onlyPublicHttpAddressesAreAccepted() {
        assertNotNull(safeRadioUrl("http://radio.example.com:8000/stream"))
        assertNotNull(safeRadioUrl("https://radio.example.com/live.m3u8"))
        listOf("file:///sdcard/music.mp3", "content://media/audio/1", "http://localhost/", "http://192.168.1.1/", "http://127.0.0.1/", "https://user:pass@example.com/", "http://[::1]/").forEach {
            assertNull(it, safeRadioUrl(it))
        }
    }
    @Test fun expiringZenoRedirectIsReplacedWithStableMount() {
        assertEquals("https://stream.zeno.fm/example", preferredRadioStream("https://stream-159.zeno.fm/example?zt=expired", "https://stream-2.zeno.fm/example?zt=another"))
        assertEquals("https://radio.example.com/audio", preferredRadioStream("https://radio.example.com/live.pls", "https://radio.example.com/audio"))
    }
    @Test fun expiringRadiojarRedirectIsReplacedWithStableMount() {
        assertEquals("https://stream.radiojar.com/example", preferredRadioStream("http://n12.radiojar.com/example?rj-tok=expired", ""))
    }
    @Test fun curatedAliasesDoNotHideOtherStations() {
        assertEquals("nogoum", curatedRadioId("Nogoum FM", "https://stream.example.com/nogoum"))
        assertEquals("quran", curatedRadioId("إذاعة القرآن الكريم", "https://stream.radiojar.com/8s5u5tpdtwzuv"))
        assertEquals("radio9090", curatedRadioId("الراديو٩٠٩٠", "https://radio.example.com/live"))
        assertNull(curatedRadioId("Nogoum Mix 256", "https://stream.example.com/mix"))
    }
    @Test fun distinctMountsRemainDistinct() {
        assertEquals(canonicalRadioUrl("https://RADIO.example.com/live"), canonicalRadioUrl("http://radio.example.com/live/"))
        assertNotEquals(canonicalRadioUrl("https://radio.example.com/one"), canonicalRadioUrl("https://radio.example.com/two"))
    }
}
