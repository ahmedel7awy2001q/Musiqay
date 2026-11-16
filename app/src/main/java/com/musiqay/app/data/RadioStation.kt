package com.musiqay.app.data

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.musiqay.app.BuildConfig
import com.musiqay.app.R

private fun radioArtwork(id: Int): String = "android.resource://" + BuildConfig.APPLICATION_ID + "/" + id

data class RadioStation(
    val id: String, val name: String, val description: String, val badge: String,
    val streamUrl: String, val website: String, val mimeType: String? = "audio/mpeg",
    val artworkUrl: String? = null, val category: String = "موسيقى ومنوعات",
    val tags: String = "", val directoryId: String? = null,
    val featured: Boolean = false, val priority: Int = 0,
    val sourceLabel: String? = null,
    val fallbackStreamUrl: String? = null,
    val isLive: Boolean = true
) {
    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId("radio:$id").setUri(streamUrl).setMimeType(mimeType)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(name)
            .setArtist((if (isLive) "بث مباشر" else "تسجيل") + " • " + category)
            .setArtworkUri(artworkUrl?.let(Uri::parse))
            .setExtras(Bundle().apply {
                putString("radio_station_id", id)
                putBoolean("radio_is_live", isLive)
                putBoolean("spoken_audio", category == "قرآن" || category == "أذكار ورقية" || category.contains("ابتهالات"))
                fallbackStreamUrl?.let { putString("fallback_stream_url", it) }
            }).build()).build()
}

/** Curated public streams. Broadcaster links take priority over directory duplicates. */
object EgyptianRadio {
    val stations = listOf(
        RadioStation("ruqyah", "الرقية الشرعية", "رقية شرعية وتحصين على مدار الساعة", "رقية",
            "https://qurango.net/radio/roqiah", "https://qurango.net/",
            artworkUrl = "https://www.nourelquran.com/files/pic/reader_20260618204619_2035.jpg",
            category = "أذكار ورقية", featured = true, priority = 100, sourceLabel = "Quran Stations"),
        RadioStation("athkar-sabah", "أذكار الصباح", "أذكار الصباح بث متواصل", "صباح",
            "https://qurango.net/radio/athkar_sabah", "https://qurango.net/",
            artworkUrl = "https://www.e-quran.com/files/image/radio.jpg",
            category = "أذكار ورقية", featured = true, priority = 90, sourceLabel = "Quran Stations"),
        RadioStation("athkar-masa", "أذكار المساء", "أذكار المساء بث متواصل", "مساء",
            "https://qurango.net/radio/athkar_masa", "https://qurango.net/",
            artworkUrl = "https://www.e-quran.com/files/image/radio.jpg",
            category = "أذكار ورقية", featured = true, priority = 85, sourceLabel = "Quran Stations"),
        RadioStation("sakeenah", "آيات السكينة", "آيات مختارة للسكينة والطمأنينة", "سكينة",
            "https://qurango.net/radio/sakeenah", "https://qurango.net/",
            artworkUrl = "https://www.e-quran.com/files/image/radio.jpg",
            category = "أذكار ورقية", featured = true, priority = 80, sourceLabel = "Quran Stations"),
        RadioStation(
            "quran", "إذاعة القرآن الكريم من القاهرة", "البث المباشر لإذاعة القرآن الكريم المصرية", "قرآن",
            "https://stream.radiojar.com/8s5u5tpdtwzuv",
            "https://misrquran.gov.eg/",
            artworkUrl = radioArtwork(R.drawable.radio_quran),
            category = "قرآن",
            featured = true,
            priority = 95,
            sourceLabel = "إذاعة القرآن الكريم المصرية",
            fallbackStreamUrl = "https://service.webvideocore.net/CL1olYogIrDWvwqiIKK7eCxOS4PStqG9DuEjAr2ZjZQtvS3d4y9r0cvRhvS17SGN/a_7a4vuubc6mo8.m3u8"
        ),
        RadioStation(
            "nogoum", "نجوم FM", "أغاني وبرامج متنوعة", "نجوم",
            "https://stream.zeno.fm/qb1zvsykm98uv",
            "https://www.nogoumfm.net/",
            artworkUrl = radioArtwork(R.drawable.radio_nogoum),
            featured = true,
            priority = 75,
            sourceLabel = "Nogoum FM",
            fallbackStreamUrl = "https://audio.nrpstream.com/listen/nogoumfm/radio.mp3"
        ),
        RadioStation(
            "radio9090", "الراديو 9090", "برامج وترفيه وأخبار", "9090",
            "https://9090streaming.mobtada.com/9090FMEGYPT",
            "https://www.9090.fm/",
            artworkUrl = radioArtwork(R.drawable.radio_radio9090),
            category = "أخبار ورياضة",
            featured = true,
            priority = 72,
            sourceLabel = "الراديو 9090",
            fallbackStreamUrl = "http://9090streaming.mobtada.com/9090FMEGYPT"
        ),
        RadioStation(
            "nile", "نايل FM", "موسيقى وبرامج بالإنجليزية • 104.2 FM", "NILE",
            "https://audio.nrpstream.com/listen/nile_fm/radio.mp3",
            "https://www.nilefm.com/",
            artworkUrl = radioArtwork(R.drawable.radio_nile),
            featured = true,
            priority = 70,
            sourceLabel = "Nile FM"
        ),
        RadioStation(
            "mega", "ميجا FM", "أغاني وبرامج • 92.7 FM", "ميجا",
            "https://megafm927.radioca.st/stream",
            "https://www.megafm927.com/ar",
            priority = 66,
            sourceLabel = "Mega FM 92.7"
        ),
        RadioStation(
            "shaaby", "شعبي FM 95", "أغاني شعبية وبرامج • 95 FM", "95",
            "https://radio95.radioca.st/;",
            "",
            priority = 64,
            sourceLabel = "Sha3by FM",
            fallbackStreamUrl = "https://radio95.radioca.st/stream"
        ),
        RadioStation(
            "hits", "راديو هيتس 88.2", "موسيقى وبرامج • 88.2 FM", "HITS",
            "https://radiohits882.radioca.st/;",
            "https://radiohits882.com/",
            priority = 63,
            sourceLabel = "Radio Hits 88.2"
        ),
        RadioStation(
            "onsport", "ON Sport FM", "رياضة وأخبار وتحليلات مباشرة", "SPORT",
            "https://carina.streamerr.co:2020/stream/OnSportFM",
            "https://onsportfm.com/",
            category = "أخبار ورياضة",
            featured = true,
            priority = 74,
            sourceLabel = "ON Sport FM"
        )
    )
}
