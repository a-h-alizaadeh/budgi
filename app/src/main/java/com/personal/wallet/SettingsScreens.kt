package com.personal.wallet

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// ───────────────────────────── ابزار اثر انگشت ─────────────────────────────

fun canUseBiometric(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

fun showBiometricPrompt(activity: FragmentActivity, onSuccess: () -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
        }
    })
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("ورود به کیف پول")
        .setSubtitle("برای ادامه، اثر انگشت خود را تأیید کنید")
        .setNegativeButtonText("استفاده از رمز")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .build()
    prompt.authenticate(info)
}

// ───────────────────────────── امنیت (مثل تصویر مرجع) ─────────────────────────────

@Composable
fun SecurityScreen(settings: AppSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf("") } // "", "set", "confirm", "off"
    var firstPin by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(WColors.Page)) {
        WalletAppBar(title = "امنیت", navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack)
        SwitchRow(
            icon = Icons.Filled.Lock, iconTint = Color(0xFF2979FF),
            title = "رمز (PIN)", subtitle = "درخواست رمز ۴ رقمی هنگام ورود به برنامه",
            checked = settings.pinEnabled,
            onChange = { on -> dialog = if (on) "set" else "off" }
        )
        SwitchRow(
            icon = Icons.Filled.Check, iconTint = Color(0xFF2979FF),
            title = "اثر انگشت", subtitle = "درخواست اثر انگشت هنگام ورود به برنامه",
            checked = settings.fingerprint,
            onChange = { on ->
                if (!on) settings.updateFingerprint(false)
                else if (!settings.pinEnabled) Toast.makeText(context, "اول رمز (PIN) را فعال کنید", Toast.LENGTH_SHORT).show()
                else if (!canUseBiometric(context)) Toast.makeText(context, "در این گوشی اثر انگشت فعال/ثبت نشده است", Toast.LENGTH_LONG).show()
                else settings.updateFingerprint(true)
            }
        )
        Text(
            "رمز فقط روی همین گوشی ذخیره می‌شود (به‌صورت رمزنگاری یک‌طرفه). اگر رمز را فراموش کنید راهی برای بازیابی نیست؛ برای حذف قفل باید برنامه را پاک و دوباره نصب کنید (و داده‌ها از بین می‌روند).",
            color = WColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(16.dp)
        )
    }

    when (dialog) {
        "set" -> TextInputDialog(
            title = "رمز جدید", label = "۴ رقم", keyboardType = KeyboardType.NumberPassword, password = true,
            onConfirm = { t ->
                if (t.length == 4 && t.all { it.isDigit() }) { firstPin = t; dialog = "confirm" }
                else { Toast.makeText(context, "رمز باید دقیقاً ۴ رقم باشد", Toast.LENGTH_SHORT).show(); dialog = "" }
            },
            onDismiss = { dialog = "" }
        )
        "confirm" -> TextInputDialog(
            title = "تکرار رمز", label = "رمز را دوباره وارد کنید", keyboardType = KeyboardType.NumberPassword, password = true,
            onConfirm = { t ->
                if (t == firstPin) { settings.setPin(t); Toast.makeText(context, "رمز فعال شد", Toast.LENGTH_SHORT).show() }
                else Toast.makeText(context, "رمزها یکسان نیستند", Toast.LENGTH_SHORT).show()
                firstPin = ""; dialog = ""
            },
            onDismiss = { firstPin = ""; dialog = "" }
        )
        "off" -> TextInputDialog(
            title = "غیرفعال‌سازی رمز", label = "رمز فعلی", keyboardType = KeyboardType.NumberPassword, password = true,
            onConfirm = { t ->
                if (settings.checkPin(t)) settings.clearPin()
                else Toast.makeText(context, "رمز اشتباه است", Toast.LENGTH_SHORT).show()
                dialog = ""
            },
            onDismiss = { dialog = "" }
        )
    }
}

@Composable
fun SwitchRow(icon: ImageVector, iconTint: Color, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp)
            Text(subtitle, color = WColors.Muted, fontSize = 13.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ───────────────────────────── تنظیمات ─────────────────────────────

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onSecurity: () -> Unit,
    onRates: () -> Unit,
    onHelp: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(WColors.Page).verticalScroll(rememberScrollState())) {
        WalletAppBar(title = "تنظیمات", navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack)
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            DropdownField(
                "تقویم",
                if (settings.useJalali) "شمسی (هجری)" else "میلادی",
                listOf("jalali" to "شمسی (هجری)", "gregorian" to "میلادی"),
                { settings.updateJalali(it == "jalali") }
            )
        }
        Spacer(Modifier.height(8.dp))
        SwitchRow(
            Icons.Filled.VisibilityOff, WColors.Blue, "پنهان کردن مبالغ", "به‌جای مبلغ‌ها ستاره نمایش داده می‌شود",
            settings.hideAmounts
        ) { settings.updateHideAmounts(it) }
        NavRow(Icons.Filled.MonetizationOn, Color(0xFFFFB300), "ارز و نرخ تبدیل", "ارز پایه: ${settings.baseCurrency}", onRates)
        NavRow(Icons.Filled.Lock, Color(0xFF2979FF), "امنیت", if (settings.pinEnabled) "رمز فعال است" else "غیرفعال", onSecurity)
        NavRow(Icons.Filled.Info, Color(0xFFFFA000), "راهنما", "آموزش بخش‌های برنامه", onHelp)
        Text("نسخه ۳٫۰ • داده‌ها فقط روی همین گوشی ذخیره می‌شوند.", color = WColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun NavRow(icon: ImageVector, tint: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp)
            Text(subtitle, color = WColors.Muted, fontSize = 13.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = WColors.Muted)
    }
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WColors.Card,
        title = { Text("راهنما", color = Color.White) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "• صفحه اصلی: روی هر حساب بزنید تا فقط همان انتخاب شود؛ لمس طولانی = افزودن/حذف از انتخاب. «انتخاب همه» همه‌ی حساب‌ها را برمی‌گرداند.\n\n" +
                        "• ثبت تراکنش: دکمه‌ی + پایین‌سمت‌چپ، مستقیماً تراکنش جدید را برای حساب انتخاب‌شده باز می‌کند. مبلغ را با صفحه‌کلید بزنید؛ حساب/دسته‌بندی را از پایین مبلغ عوض کنید. نوع تراکنش (درآمد/هزینه/انتقال) از تب‌های بالا، و دکمه‌های «توضیحات» و «قالب‌ها» از نوار زیر مبلغ.\n\n" +
                        "• انتقال بین دو حساب با ارزهای مختلف: مبلغ مقصد از نرخ‌ها محاسبه می‌شود و با لمس قابل ویرایش است.\n\n" +
                        "• دسته‌بندی‌ها دوسطحی‌اند: دسته‌ی مادر و زیرمجموعه. در ثبت تراکنش فقط زیرمجموعه (یا دسته‌ی بدون زیرمجموعه) انتخاب می‌شود.\n\n" +
                        "• ترتیب حساب‌ها: منوی ☰ ← حساب‌ها، روی یک حساب نگه دارید و بکشید. افزودن حساب هم همان‌جاست.\n\n" +
                        "• هزینه‌های مورد انتظار: قبض/قسط/اجاره با تاریخ سررسید و شمارش معکوس. پس‌انداز: یادداشت پس‌اندازها با مبلغ و محل نگهداری (متن آزاد).\n\n" +
                        "• نرخ ارز دستی است: ۱ واحد هر ارز چند واحد ارز پایه است. بدون نرخ، جمع‌های چندارزی دقیق نیستند.\n\n" +
                        "• یادداشت‌ها: تب «یادداشت‌ها» در صفحه اصلی؛ متنی یا چک‌لیست، با رنگ و پین.\n\n" +
                        "• قبل از حذف یا نصب مجدد برنامه حتماً از «خروجی» فایل بگیرید.",
                    color = Color(0xFFD0D0D0), fontSize = 14.sp
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن", color = WColors.Blue) } }
    )
}

// ───────────────────────────── ارز و نرخ تبدیل ─────────────────────────────

@Composable
fun CurrencyRatesScreen(
    base: String,
    rates: Map<String, Double>,
    usedCurrencies: Set<String>,
    onBack: () -> Unit,
    onSetRate: (String, Double) -> Unit,
    onDeleteRate: (String) -> Unit,
    onChangeBase: (String) -> Unit
) {
    var editing by remember { mutableStateOf<String?>(null) }
    var pendingBase by remember { mutableStateOf<String?>(null) }
    val list = remember(base, usedCurrencies) {
        Currencies.filter { it.code != base }.sortedByDescending { it.code in usedCurrencies }
    }

    Column(Modifier.fillMaxSize().background(WColors.Page)) {
        WalletAppBar(title = "ارز و نرخ تبدیل", navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack)
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            DropdownField(
                "ارز پایه (همه‌ی جمع‌ها به این ارز تبدیل می‌شوند)",
                base + " — " + currencyInfo(base).name,
                Currencies.map { it.code to (it.code + " — " + it.name) },
                { if (it != base) pendingBase = it }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "برای هر ارز بنویسید «۱ واحد آن چند $base است». این نرخ‌ها دستی‌اند و خودکار به‌روز نمی‌شوند.",
                color = WColors.Muted, fontSize = 12.sp
            )
        }
        SectionHeader("نرخ‌ها")
        LazyColumn(Modifier.fillMaxSize()) {
            items(list, key = { it.code }) { cur ->
                val rate = rates[cur.code]
                val used = cur.code in usedCurrencies
                Row(
                    Modifier.fillMaxWidth().clickable { editing = cur.code }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(cur.code + " — " + cur.name, color = Color.White, fontSize = 15.sp)
                        if (used && rate == null) Text("در حساب‌ها استفاده شده ولی نرخ ندارد", color = Color(0xFFFFB300), fontSize = 12.sp)
                    }
                    Text(
                        if (rate == null) "تعریف نشده" else "۱ = " + java.text.DecimalFormat("#,##0.####").format(rate) + " $base",
                        color = if (rate == null) WColors.Muted else Color(0xFFE0E0E0), fontSize = 13.sp
                    )
                }
                Hairline(Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    editing?.let { code ->
        TextInputDialog(
            title = "نرخ $code",
            label = "۱ $code = ? $base",
            initial = rates[code]?.let { java.text.DecimalFormat("0.######").format(it) } ?: "",
            keyboardType = KeyboardType.Decimal,
            onConfirm = { t ->
                val v = t.replace(',', '.').toDoubleOrNull()
                if (t.isBlank()) onDeleteRate(code) else if (v != null && v > 0.0) onSetRate(code, v)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
    pendingBase?.let { code ->
        ConfirmDialog(
            title = "تغییر ارز پایه",
            text = "با تغییر ارز پایه به $code، نرخ‌های ثبت‌شده پاک می‌شوند (چون نسبت به ارز پایه‌ی قبلی بودند) و باید دوباره وارد شوند. مبالغ حساب‌ها و تراکنش‌ها تغییر نمی‌کند.",
            confirmText = "تغییر بده",
            onConfirm = { onChangeBase(code); pendingBase = null },
            onDismiss = { pendingBase = null }
        )
    }
}
