package com.personal.wallet

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ───────────────────────────── گارانتی‌ها ─────────────────────────────

@Composable
fun WarrantiesScreen(
    records: List<RecordEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onOpenRecord: (RecordEntity) -> Unit,
    onNew: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val items = remember(records, query) {
        records.filter { warrantyMonths(it.warranty) > 0 }
            .filter {
                query.isBlank() || it.note.contains(query, true) || it.categoryName.contains(query, true) ||
                    it.payee.contains(query, true)
            }
            .sortedBy { warrantyExpiry(it) }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF121212))) {
        Column(Modifier.fillMaxSize()) {
            if (searching) {
                Row(
                    Modifier.fillMaxWidth().background(WColors.AppBar).height(56.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBarColor(WColors.AppBar)
                    IconButton(onClick = { searching = false; query = "" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                    TextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        placeholder = { Text("جستجوی گارانتی", color = WColors.Muted) },
                        colors = transparentFieldColors(), modifier = Modifier.weight(1f)
                    )
                }
            } else {
                WalletAppBar(
                    title = "گارانتی‌ها", navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack,
                    actions = { IconButton(onClick = { searching = true }) { Icon(Icons.Filled.Search, null, tint = Color.White) } }
                )
            }
            if (items.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Box(Modifier.size(130.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.VerifiedUser, null, tint = Color(0xFFCFD8DC), modifier = Modifier.size(72.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("گارانتی‌هایتان را یک‌جا نگه دارید", color = Color(0xFFEDEDED), fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "همه‌ی گارانتی‌ها اینجا نمایش داده می‌شوند. برای افزودن اولین مورد روی دکمه + بزنید و در جزئیات تراکنش، مدت گارانتی را انتخاب کنید.",
                        color = WColors.Muted, fontSize = 14.sp, textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp)) {
                    items(items, key = { it.id }) { rec ->
                        val expiry = warrantyExpiry(rec)
                        val daysLeft = ((expiry - now) / 86_400_000L).toInt()
                        val cat = categories.firstOrNull { it.name == rec.categoryName }
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenRecord(rec) }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircleIcon(iconFor(cat?.iconKey ?: "category"), Color(cat?.colorHex ?: 0xFF78909C), size = 44.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(rec.note.ifBlank { rec.categoryName }, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "خرید: ${formatDate(rec.timestamp)} • ${labelOf(Warranties, rec.warranty)}",
                                    color = WColors.Muted, fontSize = 12.sp
                                )
                                Text("پایان: ${formatDate(expiry)}", color = WColors.Muted, fontSize = 12.sp)
                            }
                            Text(
                                if (daysLeft >= 0) "$daysLeft روز مانده".toPersianDigits() else "منقضی شده",
                                color = if (daysLeft >= 0) WColors.Income else WColors.Expense,
                                fontSize = 13.sp, fontWeight = FontWeight.Medium
                            )
                        }
                        Hairline(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onNew, containerColor = WColors.Blue, contentColor = Color.White,
            shape = RoundedCornerShape(16.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Filled.Add, null) }
    }
}

// ───────────────────────────── کارت‌های وفاداری ─────────────────────────────

@Composable
fun LoyaltyScreen(cards: List<LoyaltyCardEntity>, onBack: () -> Unit, onOpen: (LoyaltyCardEntity) -> Unit, onNew: () -> Unit) {
    Box(Modifier.fillMaxSize().background(WColors.Page)) {
        Column(Modifier.fillMaxSize()) {
            WalletAppBar(title = "کارت‌های وفاداری", navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack)
            if (cards.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(Icons.Filled.CardGiftcard, "کارتی ثبت نشده", "شماره‌ی کارت‌های باشگاه مشتریان و فروشگاه‌هایتان را اینجا نگه دارید. با + کارت جدید بسازید.")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(cards.chunked(2), key = { it.first().id }) { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { c ->
                                Column(
                                    Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(14.dp)).background(Color(c.colorHex))
                                        .clickable { onOpen(c) }.padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(c.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(c.number, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onNew, containerColor = WColors.Blue, contentColor = Color.White,
            shape = RoundedCornerShape(16.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Filled.Add, null) }
    }
}

@Composable
fun LoyaltyEditScreen(
    existing: LoyaltyCardEntity?, onBack: () -> Unit,
    onSave: (LoyaltyCardEntity) -> Unit, onDelete: (LoyaltyCardEntity) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var number by remember { mutableStateOf(existing?.number ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var color by remember { mutableStateOf(existing?.colorHex ?: CategoryPalette[0]) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(WColors.Page)) {
        WalletAppBar(
            title = if (existing == null) "کارت جدید" else "ویرایش کارت",
            navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack,
            actions = {
                if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, null, tint = Color.White) }
                IconButton(onClick = {
                    if (name.isBlank()) android.widget.Toast.makeText(context, "نام کارت را وارد کنید", android.widget.Toast.LENGTH_SHORT).show()
                    else onSave(LoyaltyCardEntity(existing?.id ?: 0, name.trim(), number.trim(), note.trim(), color))
                }) { Icon(Icons.Filled.Check, null, tint = Color.White) }
            }
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Box(
                Modifier.fillMaxWidth().padding(16.dp).height(130.dp).clip(RoundedCornerShape(16.dp)).background(Color(color)).padding(16.dp)
            ) {
                Text(name.ifBlank { "نام کارت" }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart))
                Text(number.ifBlank { "شماره کارت" }, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, modifier = Modifier.align(Alignment.BottomStart))
            }
            UnderlineField("نام فروشگاه / باشگاه", name, { name = it })
            UnderlineField("شماره کارت", number, { number = it })
            UnderlineField("یادداشت", note, { note = it })
            TextButton(onClick = { clipboard.setText(AnnotatedString(number)) }, modifier = Modifier.padding(horizontal = 8.dp)) {
                Icon(Icons.Filled.ContentCopy, null, tint = WColors.Blue)
                Spacer(Modifier.width(8.dp))
                Text("کپی شماره", color = WColors.Blue)
            }
            Spacer(Modifier.height(8.dp))
            Text("رنگ", color = WColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            Column(Modifier.padding(horizontal = 16.dp)) { ColorPicker(CategoryPalette, color) { color = it } }
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog("حذف کارت", "کارت «${existing.name}» حذف شود؟", "حذف", { confirmDelete = false; onDelete(existing) }, { confirmDelete = false })
    }
}

// ───────────────────────────── مکان‌ها ─────────────────────────────

@Composable
fun LocationsScreen(records: List<RecordEntity>, onBack: () -> Unit, onOpenPlace: (String) -> Unit) {
    val places = remember(records) {
        records.filter { it.place.isNotBlank() }
            .groupBy { it.place }
            .map { (p, list) -> Triple(p, list.size, list.maxOf { it.timestamp }) }
            .sortedByDescending { it.third }
    }
    Column(Modifier.fillMaxSize().background(WColors.Page)) {
        WalletAppBar(title = "مکان‌ها", navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack)
        if (places.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Filled.Place, "مکانی ثبت نشده", "در جزئیات هر تراکنش می‌توانید «مکان» را وارد کنید؛ اینجا فهرست آن‌ها را می‌بینید.")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(places, key = { it.first }) { (place, count, last) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenPlace(place) }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Place, null, tint = Color(0xFF90A4AE), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(place, color = Color.White, fontSize = 15.sp)
                            Text("آخرین: ${formatDate(last)}", color = WColors.Muted, fontSize = 12.sp)
                        }
                        Text("$count تراکنش".toPersianDigits(), color = WColors.Muted, fontSize = 13.sp)
                    }
                    Hairline(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

// ───────────────────────────── خروجی‌ها (مثل تصویر مرجع) ─────────────────────────────

@Composable
fun ExportsScreen(
    accounts: List<AccountEntity>,
    onBack: () -> Unit,
    onExport: (format: String, filter: ExportFilter) -> Unit
) {
    var accountId by remember { mutableStateOf(0) }
    var type by remember { mutableStateOf("BOTH") }
    var payment by remember { mutableStateOf("ALL") }
    var from by remember { mutableStateOf(dayStart(30)) }
    var to by remember { mutableStateOf(System.currentTimeMillis()) }
    var includeTransfers by remember { mutableStateOf(false) }
    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }

    val filter = ExportFilter(accountId, type, payment, from, to, includeTransfers)

    Column(Modifier.fillMaxSize().background(Color(0xFF242424)).verticalScroll(rememberScrollState())) {
        WalletAppBar(title = "خروجی‌ها", navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack)
        Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            DropdownField(
                "حساب", if (accountId == 0) "همه" else accounts.firstOrNull { it.id == accountId }?.name ?: "همه",
                listOf("0" to "همه") + accounts.map { it.id.toString() to it.name }, { accountId = it.toInt() }
            )
            DropdownField(
                "نوع", when (type) { "INCOME" -> "درآمد"; "EXPENSE" -> "هزینه"; else -> "هر دو" },
                listOf("BOTH" to "هر دو", "INCOME" to "درآمد", "EXPENSE" to "هزینه"), { type = it }
            )
            DropdownField(
                "نوع پرداخت", if (payment == "ALL") "همه" else labelOf(PaymentTypes, payment),
                listOf("ALL" to "همه") + PaymentTypes, { payment = it }, dim = payment == "ALL"
            )
            Column(Modifier.fillMaxWidth().clickable { pickFrom = true }.padding(top = 10.dp)) {
                Text("از تاریخ", color = WColors.Muted, fontSize = 12.sp)
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDate(from), color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ArrowDropDown, null, tint = Color(0xFFBDBDBD))
                }
                Hairline()
            }
            Column(Modifier.fillMaxWidth().clickable { pickTo = true }.padding(top = 10.dp)) {
                Text("تا تاریخ", color = WColors.Muted, fontSize = 12.sp)
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDate(to), color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ArrowDropDown, null, tint = Color(0xFFBDBDBD))
                }
                Hairline()
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("شامل انتقال بین حساب‌ها", color = WColors.Blue, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Checkbox(checked = includeTransfers, onCheckedChange = { includeTransfers = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExportButton("PDF", Color(0xFFF44336), Modifier.weight(1f)) { onExport("pdf", filter) }
                ExportButton("XLS", Color(0xFF4CAF50), Modifier.weight(1f)) { onExport("xlsx", filter) }
                ExportButton("CSV", Color(0xFF8BC34A), Modifier.weight(1f)) { onExport("csv", filter) }
            }
        }
    }

    DatePickerHost(pickFrom, from, { from = it; pickFrom = false }, { pickFrom = false })
    DatePickerHost(pickTo, to, { to = it; pickTo = false }, { pickTo = false })
}

@Composable
private fun ExportButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(60.dp).clip(RoundedCornerShape(16.dp)).background(color).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
}
