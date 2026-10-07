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

fun displayTitle(title: String): String = detectedSurah(title)?.let { "سورة $it" } ?: title
