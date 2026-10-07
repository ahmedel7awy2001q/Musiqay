package com.musiqay.app.util

import java.net.URI
import java.util.Locale

fun safeRadioUrl(value: String): String? = runCatching {
    val uri = URI(value.trim())
    val host = uri.host?.lowercase(Locale.ROOT) ?: return null
    if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https") || uri.userInfo != null) return null
    if (host == "localhost" || host.endsWith(".local") || host.endsWith(".localhost")) return null
    val digits = host.split('.').mapNotNull(String::toIntOrNull)
    if (digits.size == 4 && (digits[0] in setOf(0, 10, 127) ||
        digits[0] == 169 && digits[1] == 254 || digits[0] == 192 && digits[1] == 168 ||
        digits[0] == 172 && digits[1] in 16..31)) return null
    if (host.contains(':') && (host.contains("::1") || host.startsWith("[fc") ||
        host.startsWith("[fd") || host.startsWith("[fe80") || host.startsWith("[::ffff:"))) return null
    uri.toASCIIString()
}.getOrNull()

private fun String.isRadioHost(domain: String): Boolean = this == domain || endsWith(".$domain")

/** Use stable mounts instead of expiring directory redirect/listener tokens. */
fun preferredRadioStream(original: String, resolved: String): String? {
    val source = safeRadioUrl(original)
    val uri = source?.let { URI(it) }
    val host = uri?.host?.lowercase(Locale.ROOT)
    if (host?.isRadioHost("zeno.fm") == true && uri.path.isNotBlank())
        return "https://stream.zeno.fm${uri.path}"
    if (host?.isRadioHost("radiojar.com") == true && uri.path.isNotBlank())
        return "https://stream.radiojar.com${uri.path}"
    return safeRadioUrl(resolved) ?: source
}

fun canonicalRadioUrl(url: String): String = runCatching {
    val uri = URI(url)
    "${uri.host?.lowercase(Locale.ROOT)}:${uri.port}${uri.path.orEmpty().trimEnd('/')}${uri.query?.let { "?$it" }.orEmpty()}"
}.getOrDefault(url)

fun curatedRadioId(name: String, url: String): String? {
    val compact = normalizeRadioSearch(name).replace(Regex("[^\\p{L}\\p{N}]"), "")
    return when {
        url.contains("8s5u5tpdtwzuv") -> "quran"
        compact in setOf("nogoumfm", "nogoumfm1006", "نجومfm", "نجومافام") -> "nogoum"
        compact.contains("9090") || url.contains("9090FMEGYPT") -> "radio9090"
        compact in setOf("nilefm", "نايلfm", "نايلافام", "افامالنيل") -> "nile"
        else -> null
    }
}
fun normalizeRadioSearch(value: String): String = normalizeSearch(value.map { char ->
    val digit = Character.digit(char, 10)
    if (digit >= 0) digit.digitToChar() else char
}.joinToString(""))
fun radioCategory(name: String, tags: String): String {
    val term = normalizeSearch("$name $tags")
    return when {
        listOf("قران", "quran", "islam", "تلاو", "recitation", "المنشاوي", "الحصري", "العفاسي", "المعيقلي", "الدوسري", "السديس", "الشريم", "عبد الباسط", "عبدالباسط").any(term::contains) -> "قرآن"
        listOf("sport", "رياض", "news", "اخبار", "العرب", "البرنامج العام").any(term::contains) -> "أخبار ورياضة"
        else -> "موسيقى ومنوعات"
    }
}
