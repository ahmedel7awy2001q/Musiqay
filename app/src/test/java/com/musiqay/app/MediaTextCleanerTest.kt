package com.musiqay.app

import com.musiqay.app.util.cleanMediaArtist
import com.musiqay.app.util.cleanMediaTitle
import com.musiqay.app.util.cleanRadioStationName
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaTextCleanerTest {
    @Test fun removesLeadingWebsiteFromArtist() {
        assertEquals("Yasser AlDosry", cleanMediaArtist("www.tvQuran.com - Yasser AlDosry"))
    }

    @Test fun cleansFilenameStyleTitle() {
        assertEquals("سورة البقرة", cleanMediaTitle("www.example.com - سورة_البقرة.mp3"))
    }

    @Test fun removesDecorativeRadioDashes() {
        assertEquals("تراتيل قصيرة متميزة", cleanRadioStationName("---تراتيل قصيرة متميزة---"))
    }
}
