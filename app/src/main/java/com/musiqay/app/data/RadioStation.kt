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
    val tags: String = "", val directoryId: String? = null
) {
    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId("radio:$id").setUri(streamUrl).setMimeType(mimeType)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(name).setArtist("بث مباشر • $category")
            .setArtworkUri(artworkUrl?.let(Uri::parse))
            .setExtras(Bundle().apply { putString("radio_station_id", id); putBoolean("spoken_audio", category == "قرآن") }).build()).build()
}

/** Curated public streams. Broadcaster links take priority over directory duplicates. */
object EgyptianRadio {
    val stations = listOf(
        RadioStation("quran", "القرآن الكريم من القاهرة", "تلاوات وبرامج دينية", "قرآن",
            "https://service.webvideocore.net/CL1olYogIrDWvwqiIKK7eCxOS4PStqG9DuEjAr2ZjZQtvS3d4y9r0cvRhvS17SGN/a_7a4vuubc6mo8.m3u8",
            "https://misrquran.gov.eg/", "application/x-mpegURL",
            radioArtwork(R.drawable.radio_quran), "قرآن"),
        RadioStation("nogoum", "نجوم FM", "أغاني وبرامج متنوعة", "نجوم",
            "https://audio.nrpstream.com/listen/nogoumfm/radio.mp3", "https://www.nogoumfm.net/",
            artworkUrl = radioArtwork(R.drawable.radio_nogoum)),
        RadioStation("radio9090", "الراديو 9090", "برامج وترفيه وأخبار", "9090",
            "https://9090streaming.mobtada.com/9090FMEGYPT", "https://www.9090.fm/",
            artworkUrl = radioArtwork(R.drawable.radio_radio9090)),
        RadioStation("nile", "نايل FM", "موسيقى وبرامج بالإنجليزية", "NILE",
            "https://audio.nrpstream.com/listen/nile_fm/radio.mp3", "https://www.nilefm.com/",
            artworkUrl = radioArtwork(R.drawable.radio_nile)),
        RadioStation("mega", "ميجا FM", "أغاني وبرامج • 92.7 FM", "ميجا",
            "https://megafm927.radioca.st/stream", "https://www.megafm927.com/ar"),
        RadioStation("shaaby", "شعبي FM 95", "أغاني شعبية وبرامج • 95 FM", "95",
            "https://radio95.radioca.st/stream", ""),
        RadioStation("hits", "راديو هيتس", "موسيقى وبرامج • 88.2 FM", "HITS",
            "https://radiohits882.radioca.st/;", "")
    )
}
