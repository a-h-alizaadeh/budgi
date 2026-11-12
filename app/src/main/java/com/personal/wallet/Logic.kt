package com.personal.wallet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

// ───────────────────────────── بازه‌های زمانی ─────────────────────────────

enum class Period(val short: String, val days: Int, val label: String) {
    D7("7D", 7, "۷ روز گذشته"),
    D30("30D", 30, "۳۰ روز گذشته"),
    W12("12W", 84, "۱۲ هفته گذشته"),
    M6("6M", 182, "۶ ماه گذشته"),
    Y1("1Y", 365, "۱ سال گذشته")
}

/** شروع روزِ «daysAgo» روز قبل از [now] (۰ = همان روز) */
fun dayStart(daysAgo: Int, now: Long = System.currentTimeMillis()): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = now
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    c.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return c.timeInMillis
}

// ───────────────────────────── اثر تراکنش روی مجموعه‌ای از حساب‌ها ─────────────────────────────

/** تابع تبدیل: (شناسه حساب، مبلغ به واحد کوچک ارز همان حساب) → مبلغ در ارز نمایش */
typealias Conv = (Int, Long) -> Long

val IdentityConv: Conv = { _, v -> v }

fun recordDelta(rec: RecordEntity, ids: Set<Int>, conv: Conv): Long {
    return when (rec.type) {
        "INCOME" -> if (rec.accountId in ids) conv(rec.accountId, rec.amount) else 0L
        "EXPENSE" -> if (rec.accountId in ids) -conv(rec.accountId, rec.amount) else 0L
        else -> {
            var d = 0L
            if (rec.accountId in ids) d -= conv(rec.accountId, rec.amount)
            val t = rec.targetAccountId
            if (t != null && t in ids) d += conv(t, effectiveTarget(rec))
            d
        }
    }
}

fun recordTouches(rec: RecordEntity, ids: Set<Int>): Boolean {
    if (rec.accountId in ids) return true
    val t = rec.targetAccountId
    return t != null && t in ids
}

/** موجودی پایان هر روز در «days» روز اخیر (آخرین نقطه = موجودی همین الان). یک بار پیمایش به عقب: O(N + days) */
fun balanceSeries(
    currentBalance: Long,
    records: List<RecordEntity>,
    ids: Set<Int>,
    days: Int,
    conv: Conv,
    now: Long = System.currentTimeMillis()
): List<Long> {
    val relevant = records
        .map { it.timestamp to recordDelta(it, ids, conv) }
        .filter { it.second != 0L }
        .sortedByDescending { it.first }
    val out = LongArray(days)
    var running = currentBalance
    var idx = 0
    for (daysAgo in 0 until days) {
        val boundary = if (daysAgo == 0) now else dayStart(daysAgo - 1, now)
        while (idx < relevant.size && relevant[idx].first > boundary) {
            running -= relevant[idx].second
            idx++
        }
        out[days - 1 - daysAgo] = running
    }
    return out.toList()
}

// ───────────────────────────── داشبورد صفحه اصلی ─────────────────────────────

/**
 * تعریف نمودارهای داشبورد (همه به «ارز پایه» تبدیل می‌شوند):
 *  • گیج «روند موجودی»: درصد تغییر مجموع موجودی حساب‌های انتخاب‌شده نسبت به ابتدای بازه (عقربه: ±۵۰٪ = دو سر گیج)
 *  • گیج «جریان نقدی»: (درآمد − هزینه) تقسیم بر (درآمد + هزینه) در بازه؛ سبز = پس‌انداز، قرمز = بیش‌خرج
 *  • صفحه دوم: مقایسه درآمد و هزینه • صفحه سوم: پرهزینه‌ترین دسته‌بندی‌ها
 */
data class DashboardData(
    val balance: Long,
    val balanceChangePct: Double,
    val balanceFraction: Float,
    val cashflow: Long,
    val cashRatioPct: Double,
    val cashFraction: Float,
    val income: Long,
    val expense: Long,
    val topCategories: List<Pair<String, Long>>
)

fun computeDashboard(
    accounts: List<AccountEntity>,
    records: List<RecordEntity>,
    ids: Set<Int>,
    period: Period,
    money: Money,
    now: Long = System.currentTimeMillis()
): DashboardData {
    val conv: Conv = { id, v -> money.toBase(id, v) }
    val balance = accounts.filter { it.id in ids }.sumOf { money.toBase(it.id, it.balance) }
    val series = balanceSeries(balance, records, ids, period.days, conv, now)
    val start = series.firstOrNull() ?: balance
    val change = if (start == 0L) 0.0 else (balance - start).toDouble() / abs(start.toDouble())
    val balanceFraction = (0.5 + change.coerceIn(-0.5, 0.5)).toFloat()

    val from = dayStart(period.days - 1, now)
    val inPeriod = records.filter { it.timestamp >= from && it.accountId in ids }
    val income = inPeriod.filter { it.type == "INCOME" }.sumOf { money.toBase(it.accountId, it.amount) }
    val expense = inPeriod.filter { it.type == "EXPENSE" }.sumOf { money.toBase(it.accountId, it.amount) }
    val net = income - expense
    val total = income + expense
    val ratio = if (total > 0L) net.toDouble() / total.toDouble() else 0.0
    val cashFraction = (0.5 + 0.5 * ratio).toFloat()

    val top = inPeriod.filter { it.type == "EXPENSE" }
        .groupBy { it.categoryName }
        .map { (k, v) -> k to v.sumOf { money.toBase(it.accountId, it.amount) } }
        .sortedByDescending { it.second }
        .take(5)

    return DashboardData(balance, change * 100.0, balanceFraction, net, ratio * 100.0, cashFraction, income, expense, top)
}

// ───────────────────────────── ماشین‌حساب ─────────────────────────────

fun evaluateExpression(raw: String): Double {
    val s = raw.trim().trimEnd('+', '-', '*', '/', '×', '÷', '.')
    if (s.isEmpty()) return 0.0
    val nums = ArrayList<Double>()
    val ops = ArrayList<Char>()
    var cur = StringBuilder()
    for (ch in s) {
        when (ch) {
            '+', '-', '*', '/', '×', '÷' -> {
                nums.add(cur.toString().toDoubleOrNull() ?: 0.0)
                cur = StringBuilder()
                ops.add(
                    when (ch) {
                        '×' -> '*'
                        '÷' -> '/'
                        else -> ch
                    }
                )
            }
            else -> cur.append(ch)
        }
    }
    nums.add(cur.toString().toDoubleOrNull() ?: 0.0)

    val n2 = ArrayList<Double>()
    val o2 = ArrayList<Char>()
    n2.add(nums[0])
    for (i in ops.indices) {
        val op = ops[i]
        val next = nums[i + 1]
        if (op == '*') {
            n2[n2.lastIndex] = n2[n2.lastIndex] * next
        } else if (op == '/') {
            if (next != 0.0) n2[n2.lastIndex] = n2[n2.lastIndex] / next
        } else {
            o2.add(op)
            n2.add(next)
        }
    }
    var r = n2[0]
    for (i in o2.indices) {
        r = if (o2[i] == '+') r + n2[i + 1] else r - n2[i + 1]
    }
    return r
}

/** نتیجه «=»: عدد گرد‌شده به تعداد اعشار ارز، بدون جداکننده */
fun formatResult(v: Double, decimals: Int): String {
    if (v <= 0.0 || v.isNaN() || v.isInfinite()) return "0"
    return BigDecimal(v).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

/** نمایش خوانا: جداکننده هزارگان و علامت‌های ریاضی */
fun prettyExpression(e: String): String {
    val grouped = Regex("(\\d+)(\\.\\d*)?").replace(e) { m ->
        val intPart = m.groupValues[1].toLongOrNull()
        val frac = m.groupValues[2]
        val g = if (intPart != null) NumberFormat.getNumberInstance(Locale.US).format(intPart) else m.groupValues[1]
        g + frac
    }
    return grouped.replace("*", "×").replace("/", "÷")
}

// ───────────────────────────── دسته‌بندی‌های درختی ─────────────────────────────

data class CategoryNode(val category: CategoryEntity, val depth: Int, val hasChildren: Boolean)

private fun rootsOf(categories: List<CategoryEntity>): List<CategoryEntity> {
    val ids = categories.map { it.id }.toSet()
    return categories.filter { it.parentId == null || it.parentId !in ids }
}

fun childrenOf(categories: List<CategoryEntity>, id: Int): List<CategoryEntity> =
    categories.filter { it.parentId == id }

/** همه‌ی فرزندان (در هر عمق) */
fun descendantsOf(categories: List<CategoryEntity>, id: Int): List<CategoryEntity> {
    val out = ArrayList<CategoryEntity>()
    val queue = ArrayDeque<Int>()
    queue.add(id)
    val seen = HashSet<Int>()
    while (queue.isNotEmpty()) {
        val cur = queue.removeFirst()
        if (!seen.add(cur)) continue
        for (c in childrenOf(categories, cur)) {
            out.add(c)
            queue.add(c.id)
        }
    }
    return out
}

/** درخت را به لیست تخت تبدیل می‌کند؛ فقط شاخه‌های باز ([expanded]) نمایش داده می‌شوند. null = همه باز */
fun flattenTree(categories: List<CategoryEntity>, expanded: Set<Int>?): List<CategoryNode> {
    val out = ArrayList<CategoryNode>()
    val seen = HashSet<Int>()
    fun walk(c: CategoryEntity, depth: Int) {
        if (!seen.add(c.id)) return
        val kids = childrenOf(categories, c.id)
        out.add(CategoryNode(c, depth, kids.isNotEmpty()))
        if (kids.isNotEmpty() && (expanded == null || c.id in expanded)) kids.forEach { walk(it, depth + 1) }
    }
    rootsOf(categories).forEach { walk(it, 0) }
    return out
}

/** مسیر کامل، مثل «غذا › رستوران» */
fun categoryPath(categories: List<CategoryEntity>, c: CategoryEntity): String {
    val names = ArrayList<String>()
    var cur: CategoryEntity? = c
    var guard = 0
    while (cur != null && guard++ < 20) {
        names.add(0, cur.name)
        val p = cur.parentId
        cur = if (p == null) null else categories.firstOrNull { it.id == p }
    }
    return names.joinToString(" › ")
}

// ───────────────────────────── گارانتی ─────────────────────────────

fun warrantyMonths(key: String): Int = when (key) {
    "6 months" -> 6
    "1 year" -> 12
    "2 years" -> 24
    "3 years" -> 36
    "5 years" -> 60
    "10 years" -> 120
    else -> 0
}

fun warrantyExpiry(rec: RecordEntity): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = rec.timestamp
    c.add(Calendar.MONTH, warrantyMonths(rec.warranty))
    return c.timeInMillis
}

// ───────────────────────────── پیش‌نویس تراکنش (برای افزودن/ویرایش) ─────────────────────────────

const val TRANSFER_CATEGORY = "انتقال بین‌بانکی"

class RecordDraft(
    val editing: RecordEntity?,
    type: String,
    accountId: Int,
    targetAccountId: Int?,
    categoryName: String,
    expression: String
) {
    var type by mutableStateOf(type)
    var accountId by mutableStateOf(accountId)
    var targetAccountId by mutableStateOf(targetAccountId)
    var categoryName by mutableStateOf(categoryName)
    var expression by mutableStateOf(expression)
    /** مبلغ دستی حساب مقصد (فقط وقتی ارز دو حساب فرق دارد) */
    var targetOverride by mutableStateOf<Long?>(null)
    var note by mutableStateOf("")
    var payee by mutableStateOf("")
    var labels by mutableStateOf<List<String>>(emptyList())
    var paymentType by mutableStateOf("Cash")
    var status by mutableStateOf("Cleared")
    var warranty by mutableStateOf("None")
    var place by mutableStateOf("")
    var timestamp by mutableStateOf(System.currentTimeMillis())

    fun sourceCurrency(accounts: List<AccountEntity>): String =
        accounts.firstOrNull { it.id == accountId }?.currency ?: "IRR"

    fun targetCurrency(accounts: List<AccountEntity>): String =
        accounts.firstOrNull { it.id == targetAccountId }?.currency ?: sourceCurrency(accounts)

    fun amountMinor(accounts: List<AccountEntity>): Long =
        majorToMinor(evaluateExpression(expression), currencyInfo(sourceCurrency(accounts)).decimals).coerceAtLeast(0L)

    /** مبلغی که به حساب مقصد واریز می‌شود (برای انتقال بین دو ارز مختلف از نرخ‌ها محاسبه می‌شود) */
    fun targetMinor(accounts: List<AccountEntity>, money: Money): Long {
        val amount = amountMinor(accounts)
        val from = sourceCurrency(accounts)
        val to = targetCurrency(accounts)
        if (from == to) return amount
        return targetOverride ?: money.convert(amount, from, to)
    }

    fun toEntity(accounts: List<AccountEntity>, money: Money): RecordEntity {
        val isTransfer = type == "TRANSFER"
        val differs = isTransfer && sourceCurrency(accounts) != targetCurrency(accounts)
        return RecordEntity(
            id = editing?.id ?: 0,
            amount = amountMinor(accounts),
            type = type,
            accountId = accountId,
            targetAccountId = if (isTransfer) targetAccountId else null,
            categoryName = if (isTransfer) TRANSFER_CATEGORY else categoryName,
            note = note.trim(),
            timestamp = timestamp,
            payee = payee.trim(),
            labels = labels.joinToString(","),
            paymentType = paymentType,
            status = status,
            warranty = warranty,
            place = place.trim(),
            targetAmount = if (differs) targetMinor(accounts, money) else 0L
        )
    }
}

fun draftFromRecord(rec: RecordEntity, accounts: List<AccountEntity>): RecordDraft {
    val srcDec = currencyInfo(accounts.firstOrNull { it.id == rec.accountId }?.currency ?: "IRR").decimals
    val d = RecordDraft(rec, rec.type, rec.accountId, rec.targetAccountId, rec.categoryName, minorToEditable(rec.amount, srcDec))
    d.note = rec.note
    d.payee = rec.payee
    d.labels = rec.labels.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    d.paymentType = rec.paymentType
    d.status = rec.status
    d.warranty = rec.warranty
    d.place = rec.place
    d.timestamp = rec.timestamp
    if (rec.targetAmount > 0L) d.targetOverride = rec.targetAmount
    return d
}

fun draftFromTemplate(t: TemplateEntity, accounts: List<AccountEntity>): RecordDraft {
    val dec = currencyInfo(accounts.firstOrNull { it.id == t.accountId }?.currency ?: "IRR").decimals
    val d = RecordDraft(null, t.type, t.accountId, t.targetAccountId, t.categoryName, minorToEditable(t.amount, dec))
    d.note = t.note
    return d
}

fun newDraft(
    type: String,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    preferredAccountId: Int?,
    warranty: String = "None"
): RecordDraft {
    val from = accounts.firstOrNull { it.id == preferredAccountId } ?: accounts.firstOrNull()
    val target = accounts.firstOrNull { it.id != from?.id }
    val d = RecordDraft(
        editing = null,
        type = type,
        accountId = from?.id ?: 0,
        targetAccountId = target?.id,
        categoryName = categories.firstOrNull { it.parentId == null }?.name ?: "عمومی",
        expression = "0"
    )
    d.warranty = warranty
    return d
}

fun TemplateEntity.summary(accounts: List<AccountEntity>): String {
    val acc = accounts.firstOrNull { it.id == accountId }
    return "$categoryName • ${acc?.name ?: ""} • ${formatMoney(amount, acc?.currency ?: "IRR")}"
}
