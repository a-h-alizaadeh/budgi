package com.personal.wallet

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** رنگ‌ها از روی ویدیوی اپ مرجع (Wallet by BudgetBakers) نمونه‌برداری شده‌اند. */
object WColors {
    val AppBar = Color(0xFF232323)
    val Page = Color(0xFF1B1B1B)
    val Card = Color(0xFF212121)
    val Section = Color(0xFF0F0F0F)
    val Line = Color(0xFF303030)
    val Blue = Color(0xFF5E97F6)
    val Muted = Color(0xFF9E9E9E)
    val Income = Color(0xFF1DB571)
    val Expense = Color(0xFFE53935)
    val ChipGray = Color(0xFF6D6D6D)
    val Keypad = Color(0xFF1B1B1B)
    val KeypadSide = Color(0xFF0F0F0F)
    val KeyText = Color(0xFFB0B0B0)
}

@Composable
fun WalletTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = WColors.Blue,
            onPrimary = Color.White,
            background = WColors.Page,
            onBackground = Color.White,
            surface = WColors.Card,
            onSurface = Color.White,
            surfaceVariant = WColors.Card,
            onSurfaceVariant = Color(0xFFBDBDBD)
        ),
        content = content
    )
}

// ───────────────────────────── قالب‌بندی ─────────────────────────────

/** تاریخ با توجه به تنظیم تقویم: شمسی «۹ آبان ۱۴۰۵» یا میلادی «Oct 31, 2026» */
fun formatDate(ts: Long): String {
    return if (CalendarPrefs.jalali) {
        val j = Jalali.fromMillis(ts)
        "${j.day} ${Jalali.monthNames[j.month - 1]} ${j.year}".toPersianDigits()
    } else {
        SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(ts))
    }
}

fun formatShortDate(ts: Long): String {
    return if (CalendarPrefs.jalali) {
        val j = Jalali.fromMillis(ts)
        "${j.day} ${Jalali.monthNames[j.month - 1]}".toPersianDigits()
    } else {
        SimpleDateFormat("MMM d", Locale.US).format(Date(ts))
    }
}

fun formatTime(ts: Long): String {
    return if (CalendarPrefs.jalali) {
        SimpleDateFormat("HH:mm", Locale.US).format(Date(ts)).toPersianDigits()
    } else {
        SimpleDateFormat("h:mm a", Locale.US).format(Date(ts))
    }
}

fun formatIsoDate(ts: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ts))

fun formatJalaliNumeric(ts: Long): String {
    val j = Jalali.fromMillis(ts)
    return "%04d/%02d/%02d".format(Locale.US, j.year, j.month, j.day)
}

// ───────────────────────────── لیست‌های انتخابی (مقدار ذخیره‌شده → برچسب نمایشی) ─────────────────────────────

val PaymentTypes = listOf(
    "Cash" to "نقد",
    "Debit card" to "کارت بانکی",
    "Credit card" to "کارت اعتباری",
    "Bank transfer" to "انتقال بانکی",
    "Voucher" to "کوپن / ژتون",
    "Mobile payment" to "پرداخت موبایلی",
    "Web payment" to "پرداخت اینترنتی"
)

val RecordStatuses = listOf(
    "Cleared" to "انجام‌شده",
    "Uncleared" to "در انتظار",
    "Reconciled" to "تطبیق‌شده"
)

val Warranties = listOf(
    "None" to "ندارد",
    "6 months" to "۶ ماه",
    "1 year" to "۱ سال",
    "2 years" to "۲ سال",
    "3 years" to "۳ سال",
    "5 years" to "۵ سال",
    "10 years" to "۱۰ سال"
)

val AccountTypes = listOf(
    "Cash" to "نقد",
    "Bank account" to "حساب بانکی",
    "Credit card" to "کارت اعتباری",
    "Savings" to "پس‌انداز",
    "Investment" to "سرمایه‌گذاری"
)

fun labelOf(options: List<Pair<String, String>>, key: String): String =
    options.firstOrNull { it.first == key }?.second ?: key

// ───────────────────────────── آیکون‌ها ─────────────────────────────

/** آیکون‌ها به‌صورت گروه‌بندی‌شده؛ برای افزودن آیکون جدید فقط یک خط به گروه مناسب اضافه کنید. */
private val iconDefs: List<Pair<String, List<Pair<String, ImageVector>>>> by lazy {
    listOf(
        "غذا و خرید" to listOf(
            "restaurant" to Icons.Filled.Restaurant, "fastfood" to Icons.Filled.Fastfood,
            "cafe" to Icons.Filled.LocalCafe, "bar" to Icons.Filled.LocalBar, "cake" to Icons.Filled.Cake,
            "grocery" to Icons.Filled.LocalGroceryStore, "shopping_bag" to Icons.Filled.ShoppingBag,
            "store" to Icons.Filled.Storefront, "clothes" to Icons.Filled.Checkroom, "gift" to Icons.Filled.CardGiftcard
        ),
        "خانه و قبوض" to listOf(
            "home" to Icons.Filled.Home, "apartment" to Icons.Filled.Apartment, "electric" to Icons.Filled.Lightbulb,
            "fire" to Icons.Filled.LocalFireDepartment, "wifi" to Icons.Filled.Wifi, "phone" to Icons.Filled.Smartphone,
            "tv" to Icons.Filled.Tv, "furniture" to Icons.Filled.Chair, "kitchen" to Icons.Filled.Kitchen,
            "laundry" to Icons.Filled.LocalLaundryService, "build" to Icons.Filled.Build, "handyman" to Icons.Filled.Handyman
        ),
        "حمل‌ونقل و سفر" to listOf(
            "bus" to Icons.Filled.DirectionsBus, "subway" to Icons.Filled.Subway, "taxi" to Icons.Filled.LocalTaxi,
            "car" to Icons.Filled.DirectionsCar, "bike" to Icons.Filled.DirectionsBike, "moto" to Icons.Filled.TwoWheeler,
            "gas" to Icons.Filled.LocalGasStation, "parking" to Icons.Filled.LocalParking, "flight" to Icons.Filled.Flight,
            "hotel" to Icons.Filled.Hotel, "beach" to Icons.Filled.BeachAccess
        ),
        "سلامت و ورزش" to listOf(
            "medical" to Icons.Filled.MedicalServices, "hospital" to Icons.Filled.LocalHospital,
            "pharmacy" to Icons.Filled.LocalPharmacy, "fitness" to Icons.Filled.FitnessCenter,
            "soccer" to Icons.Filled.SportsSoccer, "spa" to Icons.Filled.Spa, "pets" to Icons.Filled.Pets,
            "child" to Icons.Filled.ChildCare
        ),
        "آموزش و کار" to listOf(
            "school" to Icons.Filled.School, "book" to Icons.Filled.MenuBook, "language" to Icons.Filled.Language,
            "science" to Icons.Filled.Science, "edit" to Icons.Filled.Edit, "brush" to Icons.Filled.Brush,
            "work" to Icons.Filled.Work, "computer" to Icons.Filled.Computer, "print" to Icons.Filled.Print
        ),
        "سرگرمی" to listOf(
            "games" to Icons.Filled.SportsEsports, "movie" to Icons.Filled.Movie, "theater" to Icons.Filled.Theaters,
            "music" to Icons.Filled.MusicNote, "headphones" to Icons.Filled.Headphones,
            "camera" to Icons.Filled.CameraAlt, "park" to Icons.Filled.Park
        ),
        "مالی" to listOf(
            "coins" to Icons.Filled.MonetizationOn, "payments" to Icons.Filled.Payments, "atm" to Icons.Filled.LocalAtm,
            "card" to Icons.Filled.CreditCard, "bank" to Icons.Filled.AccountBalance,
            "wallet" to Icons.Filled.AccountBalanceWallet, "savings" to Icons.Filled.Savings,
            "trending" to Icons.Filled.TrendingUp, "receipt" to Icons.Filled.Receipt, "sell" to Icons.Filled.Sell
        ),
        "سایر" to listOf(
            "person" to Icons.Filled.Person, "group" to Icons.Filled.Group, "favorite" to Icons.Filled.Favorite,
            "eco" to Icons.Filled.Eco, "security" to Icons.Filled.Security, "key" to Icons.Filled.VpnKey,
            "public" to Icons.Filled.Public, "star" to Icons.Filled.Star, "category" to Icons.Filled.Category
        )
    )
}

val IconRegistry: Map<String, ImageVector> by lazy { iconDefs.flatMap { it.second }.toMap() }

/** گروه‌ها و کلید آیکون‌ها به همان ترتیب نمایش در انتخاب‌گر */
val IconGroups: List<Pair<String, List<String>>> by lazy { iconDefs.map { (title, list) -> title to list.map { it.first } } }

/** کلیدهای قدیمیِ حذف‌شده (تکراری) به معادل جدیدشان می‌روند تا داده‌های نسخه‌ی قبل درست نمایش داده شوند. */
private val IconAliases = mapOf(
    "book2" to "book", "redeem" to "gift", "money" to "coins", "cart" to "shopping_bag", "train" to "subway"
)

fun canonicalIconKey(key: String): String = IconAliases[key] ?: key

fun iconFor(key: String): ImageVector = IconRegistry[canonicalIconKey(key)] ?: Icons.Filled.Category
