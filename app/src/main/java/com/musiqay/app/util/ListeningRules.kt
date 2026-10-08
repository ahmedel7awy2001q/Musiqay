package com.musiqay.app.util

/** Never resume a completed recording or seek beyond its currently available duration. */
fun resumePosition(positionMs: Long, durationMs: Long): Long {
    if (durationMs <= 0 || positionMs < 5_000 || positionMs >= durationMs - 3_000) return 0
    return positionMs.coerceIn(0, durationMs)
}

fun boundedSeek(positionMs: Long, deltaMs: Long, durationMs: Long): Long =
    (positionMs.coerceAtLeast(0) + deltaMs.coerceIn(-60_000, 60_000)).coerceIn(0, durationMs.coerceAtLeast(0))

val PlaybackSpeeds = listOf(.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

fun displayArtist(value: String): String = value.takeUnless {
    it.isBlank() || it in setOf("فنان غير معروف", "غير معروف", "<unknown>")
}.orEmpty()

fun displayAlbum(value: String): String = value.takeUnless {
    it.isBlank() || it == "ألبوم غير معروف" ||
        it.lowercase(java.util.Locale.ROOT) in setOf("snaptube audio", "snaptube", "youtube", "unknown")
}.orEmpty()

private val surahNames = ("الفاتحة,البقرة,آل عمران,النساء,المائدة,الأنعام,الأعراف,الأنفال,التوبة,يونس,هود,يوسف,الرعد,إبراهيم,الحجر,النحل,الإسراء,الكهف,مريم,طه,الأنبياء,الحج,المؤمنون,النور,الفرقان,الشعراء,النمل,القصص,العنكبوت,الروم,لقمان,السجدة,الأحزاب,سبأ,فاطر,يس,الصافات,ص,الزمر,غافر,فصلت,الشورى,الزخرف,الدخان,الجاثية,الأحقاف,محمد,الفتح,الحجرات,ق,الذاريات,الطور,النجم,القمر,الرحمن,الواقعة,الحديد,المجادلة,الحشر,الممتحنة,الصف,الجمعة,المنافقون,التغابن,الطلاق,التحريم,الملك,القلم,الحاقة,المعارج,نوح,الجن,المزمل,المدثر,القيامة,الإنسان,المرسلات,النبأ,النازعات,عبس,التكوير,الانفطار,المطففين,الانشقاق,البروج,الطارق,الأعلى,الغاشية,الفجر,البلد,الشمس,الليل,الضحى,الشرح,التين,العلق,القدر,البينة,الزلزلة,العاديات,القارعة,التكاثر,العصر,الهمزة,الفيل,قريش,الماعون,الكوثر,الكافرون,النصر,المسد,الإخلاص,الفلق,الناس").split(',')
private val surahPatterns = surahNames.sortedByDescending { it.length }.map {
    it to Regex("(?:^|\\s)سور[ةه]\\s+${Regex.escape(normalizeSearch(it))}(?=$|[^\\p{L}\\p{N}])")
}

/** Only recognize an explicit, known surah name; duration alone never implies Quran. */
fun detectedSurah(title: String): String? {
    val text = normalizeSearch(title)
    return surahPatterns.firstOrNull { it.second.containsMatchIn(text) }?.first
}

private fun normalizeArabicDigits(value: String): String = buildString(value.length) {
    value.forEach { char ->
        append(
            when (char) {
                in '٠'..'٩' -> ('0'.code + (char.code - '٠'.code)).toChar()
                in '۰'..'۹' -> ('0'.code + (char.code - '۰'.code)).toChar()
                else -> char
            }
        )
    }
}

/** Resolve an exact surah-name query, with or without the word "سورة", to its Mushaf order. */
fun surahNumberForQuery(query: String): Int? {
    val normalized = normalizeSearch(query)
        .replaceFirst(Regex("^سور[ةه]\\s+"), "")
        .trim()
    if (normalized.isBlank()) return null
    val index = surahNames.indexOfFirst { normalizeSearch(it) == normalized }
    return index.takeIf { it >= 0 }?.plus(1)
}

/**
 * Match a surah number only when the *whole stored file name* clearly represents that number.
 *
 * Accepted examples for Al-Kahf (18):
 * - 18.mp3
 * - 018.mp3
 * - mp3.018
 * - MP3_018.mp3
 *
 * Deliberately rejected:
 * - 52-18-1.mp3
 * - Hashr.From.18.To.The.End.mp3
 * - phone-like IDs containing 18
 *
 * This prevents a surah-name search from flooding results with unrelated files that merely
 * contain the same digits somewhere in their name.
 */
fun filenameMatchesSurahNumber(fileName: String, surahNumber: Int): Boolean {
    if (surahNumber !in 1..114) return false
    val latinDigits = normalizeArabicDigits(fileName).trim()
    val withoutExtension = latinDigits.replace(
        Regex("(?i)\\.(mp3|m4a|aac|flac|wav|ogg|opus|amr|wma)$"),
        ""
    ).trim()
    val normalized = withoutExtension.lowercase(java.util.Locale.ROOT)
    val number = surahNumber.toString()
    return Regex("^(?:mp3[._ -]?)?0*${number}$").matches(normalized)
}

/**
 * A surah-name search matches only the file name itself or the surah's numeric Mushaf order.
 * Artist/album/folder metadata cannot accidentally pull unrelated files into these results.
 */
fun filenameMatchesSurahQuery(fileName: String, query: String): Boolean {
    val number = surahNumberForQuery(query) ?: return false
    val name = surahNames[number - 1]
    return normalizeSearch(fileName).contains(normalizeSearch(name)) ||
        filenameMatchesSurahNumber(fileName, number)
}

/** UI titles remain exactly as stored on the phone; Quran inference is search/grouping only. */
fun displayTitle(title: String): String = title
