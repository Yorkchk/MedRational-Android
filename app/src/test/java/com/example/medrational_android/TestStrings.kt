package com.example.medrational_android

import com.example.medrational_android.ui.text.UiText
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The app's string resources read straight from res/values (French) and res/values-en (English),
 * so JVM tests can check the text a [UiText] shows without an Android Context.
 */
object TestStrings {

    // Gradle runs unit tests from the module directory
    val resDir = File("src/main/res")

    class Language(val locale: Locale, val strings: Map<String, String>, val plurals: Map<String, Map<String, String>>)

    val french by lazy { load("values", Locale.FRENCH) }
    val english by lazy { load("values-en", Locale.ENGLISH) }

    /** [text] as an English speaker sees it. */
    fun en(text: UiText?): String? = text?.let { resolve(it, english) }

    /** [text] as a French speaker sees it. */
    fun fr(text: UiText?): String? = text?.let { resolve(it, french) }

    fun resolve(text: UiText, language: Language): String = when (text) {
        is UiText.Res -> {
            val template = language.strings.getValue(resourceName(R.string::class.java, text.id))
            String.format(language.locale, template, *args(text.args, language))
        }
        is UiText.Plural -> {
            val forms = language.plurals.getValue(resourceName(R.plurals::class.java, text.id))
            // CLDR: French uses "one" for 0 and 1, English only for 1
            val one = if (language.locale.language == "fr") text.count in 0..1 else text.count == 1
            val template = forms.getValue(if (one) "one" else "other")
            String.format(language.locale, template, *args(text.args, language))
        }
    }

    private fun args(args: List<Any>, language: Language): Array<Any> =
        args.map { if (it is UiText) resolve(it, language) else it }.toTypedArray()

    private fun resourceName(type: Class<*>, id: Int): String =
        type.fields.first { it.getInt(null) == id }.name

    fun load(folder: String, locale: Locale): Language {
        val strings = mutableMapOf<String, String>()
        val plurals = mutableMapOf<String, Map<String, String>>()
        File(resDir, folder).listFiles { file -> file.extension == "xml" }.orEmpty().forEach { file ->
            val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
            root.elements("string").forEach { strings[it.getAttribute("name")] = unescape(it.textContent) }
            root.elements("plurals").forEach { element ->
                plurals[element.getAttribute("name")] = element.elements("item")
                    .associate { it.getAttribute("quantity") to unescape(it.textContent) }
            }
        }
        return Language(locale, strings, plurals)
    }

    private fun Element.elements(tag: String): List<Element> {
        val nodes = getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    // Android's escaping: surrounding quotes keep spaces, and \' \" \n \\ stand for themselves
    private fun unescape(raw: String): String {
        val text = if (raw.length >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) raw.substring(1, raw.length - 1) else raw
        return Regex("""\\(.)""").replace(text) { match ->
            when (val c = match.groupValues[1]) {
                "n" -> "\n"
                else -> c
            }
        }
    }
}
