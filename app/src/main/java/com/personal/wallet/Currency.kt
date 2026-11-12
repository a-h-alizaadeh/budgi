package com.personal.wallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import java.text.NumberFormat
import java.util.Locale

data class CurrencyInfo(val code: String, val name: String, val decimals: Int)

val Currencies = listOf(
    CurrencyInfo("IRR", "ریال ایران", 0),
    CurrencyInfo("IRT", "تومان ایران", 0),
    CurrencyInfo("AFN", "افغانی افغانستان", 2),
    CurrencyInfo("USD", "دلار آمریکا", 2),
    CurrencyInfo("EUR", "یورو", 2),
    CurrencyInfo("GBP", "پوند انگلیس", 2),
    CurrencyInfo("AED", "درهم امارات", 2),
    CurrencyInfo("TRY", "لیر ترکیه", 2),
    CurrencyInfo("CAD", "دلار کانادا", 2),
    CurrencyInfo("AUD", "دلار استرالیا", 2),
    CurrencyInfo("CHF", "فرانک سوئیس", 2),
    CurrencyInfo("JPY", "ین ژاپن", 0),
    CurrencyInfo("CNY", "یوان چین", 2),
    CurrencyInfo("INR", "روپیه هند", 2),
    CurrencyInfo("PKR", "روپیه پاکستان", 2),
    CurrencyInfo("SAR", "ریال عربستان", 2),
    CurrencyInfo("RUB", "روبل روسیه", 2),
    CurrencyInfo("USDT", "تتر", 2)
)

fun currencyInfo(code: String): CurrencyInfo = Currencies.firstOrNull { it.code == code } ?: CurrencyInfo(code, code, 2)

fun pow10(d: Int): Long {
    var r = 1L
    repeat(d) { r *= 10L }
    return r
}

/** مبلغ (واحد کوچک ارز) را با جداکننده هزارگان و کد ارز نمایش می‌دهد؛ مثل «USD 1,234.50» */
fun formatMoney(minor: Long, code: String): String = code + " " + formatNumber(minor, code)

fun formatNumber(minor: Long, code: String): String {
    val d = currencyInfo(code).decimals
    val nf = NumberFormat.getNumberInstance(Locale.US)
    nf.minimumFractionDigits = d
    nf.maximumFractionDigits = d
    return nf.format(java.math.BigDecimal.valueOf(minor, d))
}

/** رشته قابل ویرایش (بدون جداکننده): 123450 با ۲ رقم اعشار → «1234.5» */
fun minorToEditable(minor: Long, decimals: Int): String {
    if (decimals == 0) return minor.toString()
    val neg = minor < 0
    val abs = Math.abs(minor)
    val p = pow10(decimals)
    val intPart = abs / p
    val frac = (abs % p).toString().padStart(decimals, '0').trimEnd('0')
    val body = if (frac.isEmpty()) intPart.toString() else "$intPart.$frac"
    return if (neg) "-$body" else body
}

fun majorToMinor(v: Double, decimals: Int): Long = Math.round(v * pow10(decimals).toDouble())

/** همه‌ی ارزها را به «ارز پایه» تبدیل می‌کند؛ نرخ‌ها دستی هستند (۱ واحد ارز = X واحد ارز پایه) */
class Money(
    val accounts: List<AccountEntity>,
    val rates: Map<String, Double>,
    val base: String
) {
    private val byId = accounts.associateBy { it.id }

    fun currencyOf(accountId: Int): String = byId[accountId]?.currency ?: base

    fun rate(code: String): Double = if (code == base) 1.0 else (rates[code] ?: 1.0)

    fun hasRate(code: String): Boolean = code == base || rates.containsKey(code)

    fun convert(minor: Long, from: String, to: String): Long {
        if (from == to) return minor
        val major = minor.toDouble() / pow10(currencyInfo(from).decimals).toDouble()
        val inTo = major * rate(from) / rate(to)
        return majorToMinor(inTo, currencyInfo(to).decimals)
    }

    fun toBase(accountId: Int, minor: Long): Long = convert(minor, currencyOf(accountId), base)

    /** ارز مشترک همه‌ی حساب‌های انتخاب‌شده؛ اگر متفاوت باشند null */
    fun commonCurrency(ids: Set<Int>): String? {
        val set = ids.mapNotNull { byId[it]?.currency }.toSet()
        return if (set.size == 1) set.first() else null
    }

    /** ارزهایی که در حساب‌های انتخاب‌شده هست ولی نرخ تبدیل ندارد */
    fun missingRates(ids: Set<Int>): Set<String> =
        ids.mapNotNull { byId[it]?.currency }.filter { !hasRate(it) }.toSet()
}

/** مبالغ مخفی‌شده با سوییچ «پنهان کردن مبالغ» */
val LocalHideAmounts = compositionLocalOf { false }

@Composable
fun money(minor: Long, code: String): String =
    if (LocalHideAmounts.current) "$code ••••" else formatMoney(minor, code)
