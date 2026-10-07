package com.musiqay.app

import com.musiqay.app.util.*
import org.junit.Assert.*
import org.junit.Test

class LibraryRulesTest {
    @Test fun newFolderChoiceDoesNotHideNamesakes() {
        val hidden = setOf("path:Music/Favorites")
        assertTrue(isHiddenFolder("Music/Favorites", "Favorites", hidden))
        assertFalse(isHiddenFolder("Downloads/Favorites", "Favorites", hidden))
    }
    @Test fun newRootFolderChoiceDoesNotBecomeLegacyName() {
        val hidden = setOf("path:Music")
        assertTrue(isHiddenFolder("Music", "Music", hidden))
        assertFalse(isHiddenFolder("Downloads/Music", "Music", hidden))
    }
    @Test fun legacyHiddenFolderStillApplies() {
        assertTrue(isHiddenFolder("Recordings/Voice", "Voice", setOf("Voice")))
    }
    @Test fun arabicSearchIgnoresHarakatAndAlefVariants() {
        assertEquals(normalizeSearch("إبراهيم"), normalizeSearch("إِبْرَاهِيم"))
        assertEquals(normalizeSearch("اغاني"), normalizeSearch("أغاني"))
    }
    @Test fun latinSearchIgnoresAccentsAndCase() {
        assertEquals("beyonce", normalizeSearch("  Beyoncé  "))
    }
    @Test fun duplicateQueueRestoresSelectedOccurrence() {
        assertEquals(2, restoredQueueIndex(listOf(5, 8, 5), setOf(5, 8), 2, 5))
    }
    @Test fun missingFilesBeforeSelectedOccurrenceShiftIndex() {
        assertEquals(2, restoredQueueIndex(listOf(1, 5, 8, 5), setOf(5, 8), 3, 5))
    }
    @Test fun legacySessionFallsBackToMediaId() {
        assertEquals(1, restoredQueueIndex(listOf(5, 8), setOf(5, 8), -1, 8))
    }
    @Test fun missingCurrentFileSelectsNextSurvivor() {
        assertEquals(1, restoredQueueIndex(listOf(5, 8, 9), setOf(5, 9), 1, 8))
    }
    @Test fun allMissingOrEmptyQueuesAreSafe() {
        assertEquals(0, restoredQueueIndex(listOf(5), emptySet(), 0, 5))
        assertEquals(0, restoredQueueIndex(emptyList(), emptySet(), -1, -1))
    }
}
