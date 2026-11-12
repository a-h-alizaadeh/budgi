package com.personal.wallet

import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ───────────────────────────── فیلتر خروجی (مثل تصویر مرجع) ─────────────────────────────

data class ExportFilter(
    val accountId: Int,          // ۰ = همه
    val type: String,            // BOTH, INCOME, EXPENSE
    val payment: String,         // ALL یا کلید نوع پرداخت
    val from: Long,
    val to: Long,
    val includeTransfers: Boolean
)

fun applyExportFilter(records: List<RecordEntity>, f: ExportFilter): List<RecordEntity> {
    val start = dayStart(0, f.from)
    val endExclusive = dayStart(-1, f.to)
    return records.filter { r ->
        r.timestamp in start until endExclusive &&
            (f.accountId == 0 || r.accountId == f.accountId || r.targetAccountId == f.accountId) &&
            (f.payment == "ALL" || r.paymentType == f.payment) &&
            when (r.type) {
                "TRANSFER" -> f.includeTransfers
                "INCOME" -> f.type == "BOTH" || f.type == "INCOME"
                else -> f.type == "BOTH" || f.type == "EXPENSE"
            }
    }.sortedBy { it.timestamp }
}

private fun typeLabel(t: String) = when (t) {
    "INCOME" -> "درآمد"
    "EXPENSE" -> "هزینه"
    else -> "انتقال"
}

private fun majorOf(minor: Long, currency: String): Double =
    java.math.BigDecimal.valueOf(minor, currencyInfo(currency).decimals).toDouble()

// ───────────────────────────── CSV (ستون‌های انگلیسی؛ قابل بازگشت با «ورود») ─────────────────────────────

val CsvHeaders = listOf(
    "date", "date_jalali", "time", "type", "account", "currency", "target_account", "category",
    "amount", "target_amount", "note", "payee", "labels", "payment_type", "status", "warranty", "place"
)

fun buildCsvBytes(records: List<RecordEntity>, accounts: List<AccountEntity>): ByteArray {
    val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
    val rows = records.map { r ->
        val acc = accounts.firstOrNull { it.id == r.accountId }
        val tgt = accounts.firstOrNull { it.id == r.targetAccountId }
        val cur = acc?.currency ?: "IRR"
        listOf<Any>(
            formatIsoDate(r.timestamp), formatJalaliNumeric(r.timestamp), timeFmt.format(Date(r.timestamp)),
            r.type, acc?.name ?: "", cur, tgt?.name ?: "", r.categoryName,
            minorToEditable(r.amount, currencyInfo(cur).decimals),
            if (r.type == "TRANSFER" && tgt != null) minorToEditable(effectiveTarget(r), currencyInfo(tgt.currency).decimals) else "",
            r.note, r.payee, r.labels, r.paymentType, r.status, r.warranty, r.place
        )
    }
    return ("\uFEFF" + Csv.write(CsvHeaders, rows)).toByteArray(Charsets.UTF_8)
}

// ───────────────────────────── XLSX (ستون‌های فارسی، راست‌به‌چپ) ─────────────────────────────

fun buildXlsxBytes(records: List<RecordEntity>, accounts: List<AccountEntity>): ByteArray {
    val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
    val headers = listOf(
        "تاریخ میلادی", "تاریخ شمسی", "ساعت", "نوع", "حساب", "ارز", "حساب مقصد", "دسته‌بندی",
        "مبلغ", "مبلغ مقصد", "یادداشت", "دریافت/پرداخت‌کننده", "برچسب‌ها", "نوع پرداخت", "وضعیت", "گارانتی", "مکان"
    )
    val rows = records.map { r ->
        val acc = accounts.firstOrNull { it.id == r.accountId }
        val tgt = accounts.firstOrNull { it.id == r.targetAccountId }
        val cur = acc?.currency ?: "IRR"
        listOf<Any>(
            formatIsoDate(r.timestamp), formatJalaliNumeric(r.timestamp), timeFmt.format(Date(r.timestamp)),
            typeLabel(r.type), acc?.name ?: "", cur, tgt?.name ?: "", r.categoryName,
            majorOf(r.amount, cur),
            if (r.type == "TRANSFER" && tgt != null) majorOf(effectiveTarget(r), tgt.currency) else "",
            r.note, r.payee, r.labels, labelOf(PaymentTypes, r.paymentType), labelOf(RecordStatuses, r.status),
            labelOf(Warranties, r.warranty), r.place
        )
    }
    return Xlsx.build("تراکنش‌ها", headers, rows, true)
}

// ───────────────────────────── PDF (با PdfDocument خود اندروید؛ بدون کتابخانه) ─────────────────────────────

fun buildPdfBytes(title: String, records: List<RecordEntity>, accounts: List<AccountEntity>): ByteArray {
    val pageW = 842
    val pageH = 595
    val margin = 28f
    val rowH = 22f
    val headers = listOf("تاریخ", "حساب", "دسته‌بندی", "نوع", "مبلغ", "یادداشت")
    val widths = floatArrayOf(110f, 110f, 150f, 60f, 150f, 206f) // مجموع = 786
    val rows = records.map { r ->
        val acc = accounts.firstOrNull { it.id == r.accountId }
        val cur = acc?.currency ?: "IRR"
        val sign = if (r.type == "EXPENSE") "-" else ""
        listOf(
            formatDate(r.timestamp), acc?.name ?: "", r.categoryName, typeLabel(r.type),
            sign + formatMoney(r.amount, cur), r.note
        )
    }

    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = AColor.BLACK; textAlign = Paint.Align.RIGHT }
    val bold = Paint(text).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = AColor.WHITE }
    val titlePaint = Paint(text).apply { textSize = 16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val headBg = Paint().apply { color = AColor.rgb(33, 33, 33) }
    val zebra = Paint().apply { color = AColor.rgb(244, 244, 244) }

    fun fit(s: String, maxW: Float): String {
        if (text.measureText(s) <= maxW) return s
        var end = s.length
        while (end > 1 && text.measureText(s.substring(0, end) + "…") > maxW) end--
        return s.substring(0, end) + "…"
    }

    val doc = PdfDocument()
    var pageNo = 0
    var page: PdfDocument.Page? = null
    var y = 0f

    fun newPage() {
        page?.let { doc.finishPage(it) }
        pageNo++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
        val c = page!!.canvas
        y = margin
        if (pageNo == 1) {
            c.drawText(title, pageW - margin, y + 14f, titlePaint)
            y += 30f
            c.drawText("تعداد: ${records.size}   |   ${formatDate(System.currentTimeMillis())}", pageW - margin, y + 4f, text)
            y += 18f
        }
        c.drawRect(margin, y, pageW - margin, y + rowH, headBg)
        var x = pageW - margin
        headers.forEachIndexed { i, h ->
            c.drawText(h, x - 6f, y + 15f, bold)
            x -= widths[i]
        }
        y += rowH
    }

    newPage()
    rows.forEachIndexed { idx, row ->
        if (y + rowH > pageH - margin) newPage()
        val c = page!!.canvas
        if (idx % 2 == 1) c.drawRect(margin, y, pageW - margin, y + rowH, zebra)
        var x = pageW - margin
        row.forEachIndexed { i, cell ->
            c.drawText(fit(cell, widths[i] - 12f), x - 6f, y + 15f, text)
            x -= widths[i]
        }
        y += rowH
    }
    page?.let { doc.finishPage(it) }
    val out = ByteArrayOutputStream()
    doc.writeTo(out)
    doc.close()
    return out.toByteArray()
}

// ───────────────────────────── ورود از CSV (همان قالب خروجی CSV برنامه) ─────────────────────────────

data class ImportResult(val imported: Int, val skipped: Int, val newAccounts: Int)

suspend fun importCsv(
    text: String,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    baseCurrency: String,
    repo: WalletRepository
): ImportResult {
    val rows = Csv.parse(text)
    if (rows.size < 2) return ImportResult(0, 0, 0)
    val header = rows[0].map { it.trim().lowercase() }
    fun col(name: String) = header.indexOf(name)
    val iDate = col("date"); val iTime = col("time"); val iType = col("type"); val iAcc = col("account")
    val iCur = col("currency"); val iTgt = col("target_account"); val iCat = col("category")
    val iAmt = col("amount"); val iTAmt = col("target_amount"); val iNote = col("note")
    val iPayee = col("payee"); val iLabels = col("labels"); val iPay = col("payment_type")
    val iStatus = col("status"); val iWarr = col("warranty"); val iPlace = col("place")
    if (iDate < 0 || iType < 0 || iAcc < 0 || iAmt < 0 || iCat < 0) return ImportResult(0, rows.size - 1, 0)

    val accMap = HashMap<String, AccountEntity>()
    accounts.forEach { accMap[it.name.trim().lowercase()] = it }
    val catNames = categories.map { it.name.trim().lowercase() }.toMutableSet()
    var created = 0
    var okCount = 0
    var skipped = 0
    val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    suspend fun ensureAccount(name: String, currency: String): AccountEntity? {
        val key = name.trim().lowercase()
        if (key.isEmpty()) return null
        accMap[key]?.let { return it }
        val acc = AccountEntity(
            name = name.trim(), balance = 0L,
            colorHex = AccountPalette[(accMap.size) % AccountPalette.size],
            currency = currency
        )
        val id = repo.saveAccount(acc)
        val saved = acc.copy(id = id)
        accMap[key] = saved
        created++
        return saved
    }

    fun cell(r: List<String>, i: Int): String = if (i >= 0 && i < r.size) r[i].trim() else ""

    for (r in rows.drop(1)) {
        val type = cell(r, iType).uppercase()
        if (type != "INCOME" && type != "EXPENSE" && type != "TRANSFER") { skipped++; continue }
        val cur = cell(r, iCur).ifEmpty { baseCurrency }
        val acc = ensureAccount(cell(r, iAcc), cur)
        val amount = cell(r, iAmt).replace(',', '.').toDoubleOrNull()
        val ts = try { dateFmt.parse(cell(r, iDate) + " " + cell(r, iTime).ifEmpty { "12:00" })?.time } catch (e: Exception) { null }
        if (acc == null || amount == null || amount <= 0.0 || ts == null) { skipped++; continue }
        val tgt = if (type == "TRANSFER") ensureAccount(cell(r, iTgt), cur) else null
        if (type == "TRANSFER" && (tgt == null || tgt.id == acc.id)) { skipped++; continue }
        val catName = if (type == "TRANSFER") TRANSFER_CATEGORY else cell(r, iCat).ifEmpty { "عمومی" }
        if (type != "TRANSFER" && catNames.add(catName.lowercase())) {
            repo.saveCategory(CategoryEntity(name = catName, colorHex = CategoryPalette[catNames.size % CategoryPalette.size]), null)
        }
        val tAmt = cell(r, iTAmt).replace(',', '.').toDoubleOrNull()
        val rec = RecordEntity(
            amount = majorToMinor(amount, currencyInfo(acc.currency).decimals),
            type = type, accountId = acc.id, targetAccountId = tgt?.id, categoryName = catName,
            note = cell(r, iNote), timestamp = ts, payee = cell(r, iPayee), labels = cell(r, iLabels),
            paymentType = cell(r, iPay).ifEmpty { "Cash" }, status = cell(r, iStatus).ifEmpty { "Cleared" },
            warranty = cell(r, iWarr).ifEmpty { "None" }, place = cell(r, iPlace),
            targetAmount = if (tAmt != null && tgt != null && tgt.currency != acc.currency)
                majorToMinor(tAmt, currencyInfo(tgt.currency).decimals) else 0L
        )
        repo.saveRecord(rec, null)
        okCount++
    }
    return ImportResult(okCount, skipped, created)
}
