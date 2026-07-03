package com.example.spelltracker.ui.detail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.example.spelltracker.ui.theme.AppColors

/**
 * Минимальный HTML→[AnnotatedString] рендер для описания заклинания.
 *
 * Сложность O(N) по длине входа, без сторонних библиотек.
 *
 * Поддерживаемые теги:
 *   • `<p>` / `</p>` / `<br/>` — параграфы и переносы строк
 *   • `<strong>` / `<em>` — bold / italic
 *   • `<span class="saving_throw">` — золотой (брендовый) цвет
 *   • `<a>`, `<detail-tooltip>`, `<dice-roller>`, `<ul>`, `<li>` —
 *     контент читается как plain text; ссылки НЕ открываются
 *     (для UX используем [androidx.compose.foundation.text.ClickableText]
 *     если потребуется)
 *
 * HTML-entities:
 *   &nbsp; &laquo; &raquo; &mdash; &ndash; &times; &amp; &lt; &gt; &quot;
 *
 * НЕ поддерживаем намеренно: вложенные таблицы, изображения, JS, формы.
 */
fun parseSpellHtml(html: String): AnnotatedString {
    if (html.isBlank()) return AnnotatedString("")
    val builder = Builder()
    builder.run(html)
    return builder.build()
}

// ─── impl ────────────────────────────────────────────────────────────

private data class Format(val bold: Boolean = false, val italic: Boolean = false, val save: Boolean = false)

private class Builder {
    private val sb = androidx.compose.ui.text.AnnotatedString.Builder()
    private val stack = ArrayDeque<Format>().apply { addLast(Format()) }

    fun run(html: String) {
        var i = 0
        val n = html.length
        while (i < n) {
            val c = html[i]
            when {
                c == '<' -> {
                    val end = html.indexOf('>', i)
                    if (end == -1) {
                        sb.append(html.substring(i))
                        return
                    }
                    val raw = html.substring(i + 1, end).trim()
                    i = end + 1
                    handleTag(raw)
                }
                c == '&' -> {
                    val end = html.indexOf(';', i)
                    if (end == -1 || end - i > 8) {
                        emitChar('&')
                        i++
                    } else {
                        val entity = html.substring(i + 1, end)
                        val ch = entityToChar(entity)
                        if (ch != null) {
                            for (cc in ch) emitChar(cc)
                        } else {
                            sb.append("&$entity;")
                        }
                        i = end + 1
                    }
                }
                else -> {
                    emitChar(c)
                    i++
                }
            }
        }
    }

    fun build(): AnnotatedString = sb.toAnnotatedString()

    private fun handleTag(raw: String) {
        val lc = raw.lowercase()
        when {
            // Параграфы и переносы
            lc == "p" || lc.startsWith("p ") -> sb.append("\n\n")
            lc == "/p"                       -> sb.append("\n")
            lc == "br" || lc.startsWith("br ") || lc == "br/" || lc == "br /" -> sb.append("\n")
            // Bold / italic / saving throw span
            lc == "strong" || lc.startsWith("strong ") -> push { it.copy(bold = true) }
            lc == "/strong"                              -> pop()
            lc == "em" || lc.startsWith("em ")      -> push { it.copy(italic = true) }
            lc == "/em"                              -> pop()
            lc.startsWith("span") && lc.contains("saving_throw") ->
                push { it.copy(save = true) }
            lc == "/span"                           -> pop()
            // Любые закрывающие теги — pop (если есть что закрывать).
            // Покрывает </a>, </detail-tooltip>, </dice-roller>, </li>, </ul>, и т.д.
            lc.startsWith("/") -> pop()
            // Все остальные открывающие теги — no-op: текст внутри эмитится как есть.
            else -> { /* unknown open tag: ignore */ }
        }
    }

    private fun push(transform: (Format) -> Format) {
        stack.addLast(transform(stack.last()))
    }

    private fun pop() {
        if (stack.size > 1) stack.removeLast()
    }

    private fun emitChar(c: Char) {
        val f = stack.last()
        if (!f.bold && !f.italic && !f.save) {
            sb.append(c)
            return
        }
        val style = SpanStyle(
            fontWeight = if (f.bold) FontWeight.Bold else null,
            fontStyle = if (f.italic) FontStyle.Italic else null,
            color = if (f.save) AppColors.Gold else Color.Unspecified,
        )
        sb.withStyle(style) { append(c) }
    }
}

private fun entityToChar(entity: String): String? = when (entity.lowercase()) {
    "nbsp"   -> " "
    "laquo"  -> "«"
    "raquo"  -> "»"
    "mdash"  -> "—"
    "ndash"  -> "–"
    "times"  -> "×"
    "amp"    -> "&"
    "lt"     -> "<"
    "gt"     -> ">"
    "quot"   -> "\""
    "copy"   -> "©"
    "reg"    -> "®"
    else     -> null
}
