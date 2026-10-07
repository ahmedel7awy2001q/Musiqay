package com.musiqay.app.util

private val controlChars = Regex("[\\p{Cc}\\p{Cf}]+")
private val repeatedWhitespace = Regex("\\s+")
private val audioExtension = Regex("(?i)\\.(mp3|m4a|aac|flac|wav|ogg|opus|wma)$")
private val leadingSite = Regex(
    "(?i)^(?:https?://)?(?:www\\.)?[a-z0-9.-]+\\.(?:com|net|org|info|fm|tv|me|co|io|eg)(?:/\\S*)?\\s*[-–—|:_•]*\\s*"
)
private val bracketedSite = Regex(
    "(?i)^\\[\\s*(?:https?://)?(?:www\\.)?[a-z0-9.-]+\\.(?:com|net|org|info|fm|tv|me|co|io|eg)[^]]*]\\s*[-–—|:_•]*\\s*"
)

private fun normalizeHumanText(raw: String?): String = raw.orEmpty()
    .replace(controlChars, " ")
    .replace('_', ' ')
    .replace(repeatedWhitespace, " ")
    .trim()

private fun stripSitePrefix(value: String): String {
    var clean = value.replace(bracketedSite, "").replace(leadingSite, "").trim()
    clean = clean.trim(' ', '-', '–', '—', '|', ':', '•', '_')
    return clean.replace(repeatedWhitespace, " ").trim()
}

fun cleanMediaTitle(raw: String?): String {
    val clean = stripSitePrefix(normalizeHumanText(raw).replace(audioExtension, ""))
    return clean.ifBlank { "بدون عنوان" }.take(180)
}

fun cleanMediaArtist(raw: String?): String {
    val normalized = normalizeHumanText(raw)
    if (normalized.equals("<unknown>", ignoreCase = true)) return "فنان غير معروف"
    val clean = stripSitePrefix(normalized)
    return clean.ifBlank { "فنان غير معروف" }.take(140)
}

fun cleanMediaAlbum(raw: String?): String {
    val normalized = normalizeHumanText(raw)
    if (normalized.equals("<unknown>", ignoreCase = true)) return "ألبوم غير معروف"
    val clean = stripSitePrefix(normalized)
    return clean.ifBlank { "ألبوم غير معروف" }.take(140)
}

fun cleanRadioStationName(raw: String?): String {
    val normalized = normalizeHumanText(raw)
    val clean = stripSitePrefix(normalized)
        .replace(Regex("[-–—_]{2,}"), " ")
        .trim(' ', '.', '،', ',', '؛', ';', '-', '–', '—', '_', '|', ':', '•')
        .replace(repeatedWhitespace, " ")
        .trim()
    return clean.take(150)
}
