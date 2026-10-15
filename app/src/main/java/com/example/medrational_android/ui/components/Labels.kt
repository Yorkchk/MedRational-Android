package com.example.medrational_android.ui.components

/**
 * Up to two initials for a monogram tile ("Médecine interne" gives "MI"), from the first letter or
 * digit of the first two words. Accents are kept, symbols and emoji skipped. Null when the name
 * has neither, so the caller shows an icon instead.
 */
fun monogram(name: String?): String? {
    if (name.isNullOrBlank()) return null
    val initials = name.trim()
        .split(Regex("\\s+"))
        .mapNotNull { word -> word.firstLetterOrDigit() }
        .take(2)
    return initials.takeIf { it.isNotEmpty() }?.joinToString("") { it.uppercase() }
}

private fun String.firstLetterOrDigit(): String? {
    var index = 0
    while (index < length) {
        val codePoint = codePointAt(index)
        if (Character.isLetterOrDigit(codePoint)) return String(Character.toChars(codePoint))
        index += Character.charCount(codePoint)
    }
    return null
}

/**
 * The first name to greet the user with ("Bonjour, Salma"), from the stored full name. Null when
 * there is no usable name, including when only an email was stored.
 */
fun greetingName(storedName: String?): String? {
    val first = storedName?.trim()?.split(Regex("\\s+"))?.firstOrNull()
    return first?.takeIf { it.isNotBlank() && '@' !in it }
}
