package com.personal.wallet

/** نوشتن و خواندن CSV (با پشتیبانی از گیومه، ویرگول و خط جدید داخل سلول) */
object Csv {
    fun write(headers: List<String>, rows: List<List<Any>>): String {
        fun esc(v: Any): String = "\"" + v.toString().replace("\"", "\"\"") + "\""
        val sb = StringBuilder()
        sb.append(headers.joinToString(",") { esc(it) }).append("\n")
        for (r in rows) sb.append(r.joinToString(",") { esc(it) }).append("\n")
        return sb.toString()
    }

    fun parse(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        val s = text.removePrefix("\uFEFF")
        while (i < s.length) {
            val ch = s[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < s.length && s[i + 1] == '"') { cur.append('"'); i++ } else inQuotes = false
                } else cur.append(ch)
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> { row.add(cur.toString()); cur.setLength(0) }
                    '\r' -> {}
                    '\n' -> {
                        row.add(cur.toString()); cur.setLength(0)
                        if (row.any { it.isNotEmpty() }) rows.add(row)
                        row = ArrayList()
                    }
                    else -> cur.append(ch)
                }
            }
            i++
        }
        if (cur.isNotEmpty() || row.isNotEmpty()) {
            row.add(cur.toString())
            if (row.any { it.isNotEmpty() }) rows.add(row)
        }
        return rows
    }
}
