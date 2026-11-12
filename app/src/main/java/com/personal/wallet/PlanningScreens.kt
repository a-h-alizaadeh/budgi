package com.personal.wallet

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ───────────────────────────── شمارش معکوس سررسید ─────────────────────────────

/** تعداد روزهای تقویمی تا سررسید (منفی = گذشته) */
fun daysUntil(due: Long, now: Long = System.currentTimeMillis()): Int =
    Math.round((dayStart(0, due) - dayStart(0, now)) / 86_400_000.0).toInt()

fun dueLabel(days: Int): String {
    val text = when {
        days > 1 -> "$days روز مانده" + if (days >= 45) " (حدود ${days / 30} ماه)" else ""
        days == 1 -> "فردا"
        days == 0 -> "امروز"
        days == -1 -> "دیروز (معوق)"
        else -> "${-days} روز معوق"
    }
    return text.toPersianDigits()
}

fun dueColor(days: Int): Color = when {
    days < 0 -> Color(0xFFE53935)
    days <= 3 -> Color(0xFFFF9800)
    days <= 14 -> Color(0xFFFFCA28)
    else -> Color(0xFF1DB571)
}

// ───────────────────────────── هزینه‌های مورد انتظار ─────────────────────────────

@Composable
fun ExpectedExpensesScreen(
    items: List<ExpectedExpenseEntity>,
    fx: Money,
    onBack: () -> Unit,
    onOpen: (ExpectedExpenseEntity) -> Unit,
    onTogglePaid: (ExpectedExpenseEntity) -> Unit,
    onNew: () -> Unit
) {
    val unpaid = items.filter { !it.paid }.sortedBy { it.dueDate }
    val paid = items.filter { it.paid }.sortedByDescending { it.dueDate }
    val totalBase = unpaid.sumOf { fx.convert(it.amount, it.currency, fx.base) }
    val missing = unpaid.map { it.currency }.filter { !fx.hasRate(it) }.toSet()
    val next = unpaid.firstOrNull()

    Box(Modifier.fillMaxSize().background(WColors.Page)) {
        Column(Modifier.fillMaxSize()) {
            WalletAppBar(title = "هزینه‌های مورد انتظار", navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack)
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        Icons.Filled.DateRange, "هزینه‌ای ثبت نشده",
                        "قبض، قسط، اجاره یا هر پرداخت آینده را با دکمه + وارد کنید تا زمان باقی‌مانده تا سررسیدش را ببینید."
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                    item {
                        Column(
                            Modifier.padding(12.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(WColors.Card).padding(16.dp)
                        ) {
                            Text("جمع پرداخت‌های پیش‌رو (${fx.base})", color = WColors.Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(money(totalBase, fx.base), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                            if (next != null) {
                                val d = daysUntil(next.dueDate)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "نزدیک‌ترین: ${next.title} — ${dueLabel(d)}",
                                    color = dueColor(d), fontSize = 13.sp, fontWeight = FontWeight.Medium
                                )
                            }
                            if (missing.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text("نرخ تبدیل ${missing.joinToString("، ")} تعریف نشده؛ جمع دقیق نیست.", color = Color(0xFFFFB300), fontSize = 12.sp)
                            }
                        }
                    }
                    if (unpaid.isNotEmpty()) item { SectionHeader("پرداخت‌نشده") }
                    items(unpaid, key = { it.id }) { e -> ExpectedRow(e, onOpen, onTogglePaid) }
                    if (paid.isNotEmpty()) item { SectionHeader("پرداخت‌شده") }
                    items(paid, key = { it.id }) { e -> ExpectedRow(e, onOpen, onTogglePaid) }
                }
            }
        }
        LeftFab(onClick = onNew)
    }
}

@Composable
private fun ExpectedRow(e: ExpectedExpenseEntity, onOpen: (ExpectedExpenseEntity) -> Unit, onToggle: (ExpectedExpenseEntity) -> Unit) {
    val days = daysUntil(e.dueDate)
    Row(
        Modifier.fillMaxWidth().clickable { onOpen(e) }.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = e.paid, onCheckedChange = { onToggle(e) })
        Column(Modifier.weight(1f)) {
            Text(
                e.title, color = if (e.paid) WColors.Muted else Color.White, fontSize = 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text("سررسید: " + formatDate(e.dueDate), color = WColors.Muted, fontSize = 12.sp)
            if (e.note.isNotBlank()) Text(e.note, color = WColors.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(money(e.amount, e.currency), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (!e.paid) Text(dueLabel(days), color = dueColor(days), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            else Text("پرداخت شد", color = WColors.Income, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
    }
    Hairline(Modifier.padding(horizontal = 16.dp))
}

@Composable
fun ExpectedEditScreen(
    existing: ExpectedExpenseEntity?,
    defaultCurrency: String,
    onBack: () -> Unit,
    onSave: (ExpectedExpenseEntity) -> Unit,
    onDelete: (ExpectedExpenseEntity) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var currency by remember { mutableStateOf(existing?.currency ?: defaultCurrency) }
    var amountText by remember {
        mutableStateOf(existing?.let { minorToEditable(it.amount, currencyInfo(it.currency).decimals) } ?: "")
    }
    var due by remember { mutableStateOf(existing?.dueDate ?: (System.currentTimeMillis() + 7L * 86_400_000L)) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var paid by remember { mutableStateOf(existing?.paid ?: false) }
    var showDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(WColors.Page)) {
        WalletAppBar(
            title = if (existing == null) "هزینه‌ی جدید" else "ویرایش هزینه",
            navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack,
            actions = {
                if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, null, tint = Color.White) }
                IconButton(onClick = {
                    val v = amountText.replace(',', '.').toDoubleOrNull()
                    if (title.isBlank()) Toast.makeText(context, "عنوان را وارد کنید", Toast.LENGTH_SHORT).show()
                    else if (v == null || v <= 0.0) Toast.makeText(context, "مبلغ را وارد کنید", Toast.LENGTH_SHORT).show()
                    else onSave(
                        ExpectedExpenseEntity(
                            id = existing?.id ?: 0, title = title.trim(),
                            amount = majorToMinor(v, currencyInfo(currency).decimals), currency = currency,
                            dueDate = due, note = note.trim(), paid = paid
                        )
                    )
                }) { Icon(Icons.Filled.Check, null, tint = Color.White) }
            }
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            UnderlineField("عنوان", title, { title = it }, placeholder = "مثلاً قبض برق، قسط وام، اجاره")
            UnderlineField("مبلغ ($currency)", amountText, { amountText = it }, keyboardType = KeyboardType.Decimal)
            Column(Modifier.padding(horizontal = 16.dp)) {
                DropdownField(
                    "ارز", currency + " — " + currencyInfo(currency).name,
                    Currencies.map { it.code to (it.code + " — " + it.name) }, { currency = it }
                )
                Column(Modifier.fillMaxWidth().clickable { showDate = true }.padding(top = 12.dp)) {
                    Text(if (CalendarPrefs.jalali) "تاریخ سررسید (شمسی)" else "تاریخ سررسید", color = WColors.Muted, fontSize = 12.sp)
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatDate(due), color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ArrowDropDown, null, tint = Color(0xFFBDBDBD))
                    }
                    Hairline()
                }
                val d = daysUntil(due)
                if (!paid) Text(dueLabel(d), color = dueColor(d), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
            }
            UnderlineField("یادداشت", note, { note = it }, singleLine = false)
            Row(
                Modifier.fillMaxWidth().clickable { paid = !paid }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("پرداخت شده", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Switch(checked = paid, onCheckedChange = { paid = it })
            }
        }
    }
    DatePickerHost(showDate, due, { due = it; showDate = false }, { showDate = false })
    if (confirmDelete && existing != null) {
        ConfirmDialog("حذف هزینه", "«${existing.title}» حذف شود؟", "حذف", { confirmDelete = false; onDelete(existing) }, { confirmDelete = false })
    }
}

// ───────────────────────────── پس‌انداز ─────────────────────────────

@Composable
fun SavingsScreen(
    items: List<SavingEntity>,
    fx: Money,
    onBack: () -> Unit,
    onOpen: (SavingEntity) -> Unit,
    onNew: () -> Unit
) {
    val total = items.filter { it.amount > 0L }.sumOf { fx.convert(it.amount, it.currency, fx.base) }
    val missing = items.filter { it.amount > 0L }.map { it.currency }.filter { !fx.hasRate(it) }.toSet()

    Box(Modifier.fillMaxSize().background(WColors.Page)) {
        Column(Modifier.fillMaxSize()) {
            WalletAppBar(title = "پس‌انداز", navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack)
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        Icons.Filled.Savings, "پس‌اندازی ثبت نشده",
                        "هر پس‌انداز (طلا، سپرده، ارز، صندوق، پول نقد و…) را با مبلغ و محل نگهداری یادداشت کنید. لازم نیست حساب برنامه باشد."
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                    item {
                        Column(
                            Modifier.padding(12.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(WColors.Card).padding(16.dp)
                        ) {
                            Text("جمع پس‌اندازها (${fx.base})", color = WColors.Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(money(total, fx.base), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                            if (missing.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text("نرخ تبدیل ${missing.joinToString("، ")} تعریف نشده؛ جمع دقیق نیست.", color = Color(0xFFFFB300), fontSize = 12.sp)
                            }
                        }
                    }
                    items(items, key = { it.id }) { s ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpen(s) }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircleIcon(Icons.Filled.Savings, Color(0xFF00897B), size = 44.dp, iconSize = 24.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.title, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (s.holder.isNotBlank()) Text("نگهداری در: ${s.holder}", color = Color(0xFF9FB8B3), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (s.note.isNotBlank()) Text(s.note, color = WColors.Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (s.amount > 0L) Text(money(s.amount, s.currency), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(formatDate(s.updatedAt), color = WColors.Muted, fontSize = 11.sp)
                            }
                        }
                        Hairline(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
        LeftFab(onClick = onNew)
    }
}

@Composable
fun SavingEditScreen(
    existing: SavingEntity?,
    defaultCurrency: String,
    onBack: () -> Unit,
    onSave: (SavingEntity) -> Unit,
    onDelete: (SavingEntity) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var holder by remember { mutableStateOf(existing?.holder ?: "") }
    var currency by remember { mutableStateOf(existing?.currency ?: defaultCurrency) }
    var amountText by remember {
        mutableStateOf(existing?.takeIf { it.amount > 0L }?.let { minorToEditable(it.amount, currencyInfo(it.currency).decimals) } ?: "")
    }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(WColors.Page)) {
        WalletAppBar(
            title = if (existing == null) "پس‌انداز جدید" else "ویرایش پس‌انداز",
            navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack,
            actions = {
                if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, null, tint = Color.White) }
                IconButton(onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "عنوان را وارد کنید", Toast.LENGTH_SHORT).show()
                    } else {
                        val v = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
                        onSave(
                            SavingEntity(
                                id = existing?.id ?: 0, title = title.trim(), holder = holder.trim(),
                                amount = majorToMinor(v.coerceAtLeast(0.0), currencyInfo(currency).decimals),
                                currency = currency, note = note.trim(), updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }) { Icon(Icons.Filled.Check, null, tint = Color.White) }
            }
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            UnderlineField("عنوان", title, { title = it }, placeholder = "مثلاً سکه‌ی طلا، سپرده‌ی بلندمدت")
            UnderlineField("مبلغ ($currency) — اختیاری", amountText, { amountText = it }, keyboardType = KeyboardType.Decimal)
            Column(Modifier.padding(horizontal = 16.dp)) {
                DropdownField(
                    "ارز", currency + " — " + currencyInfo(currency).name,
                    Currencies.map { it.code to (it.code + " — " + it.name) }, { currency = it }
                )
            }
            UnderlineField("نگهداری در (حساب، کیف، صندوق، صرافی…)", holder, { holder = it }, placeholder = "هر متنی؛ لازم نیست حساب برنامه باشد")
            UnderlineField("یادداشت", note, { note = it }, singleLine = false)
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog("حذف پس‌انداز", "«${existing.title}» حذف شود؟", "حذف", { confirmDelete = false; onDelete(existing) }, { confirmDelete = false })
    }
}
