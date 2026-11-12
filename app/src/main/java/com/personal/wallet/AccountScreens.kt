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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// ───────────────────────────── جزئیات حساب ─────────────────────────────

@Composable
fun AccountDetailScreen(
    account: AccountEntity,
    records: List<RecordEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSetBalance: (Long) -> Unit,
    onOpenRecord: (RecordEntity) -> Unit,
    onAllRecords: () -> Unit,
    onNewRecord: () -> Unit
) {
    var showBalanceDialog by remember { mutableStateOf(false) }
    val ids = remember(account.id) { setOf(account.id) }
    val series = remember(account, records) { balanceSeries(account.balance, records, ids, 30, IdentityConv) }
    val start = series.firstOrNull() ?: account.balance
    val pct = if (start == 0L) 0.0 else (account.balance - start).toDouble() / abs(start.toDouble()) * 100.0
    val mine = remember(records, account.id) { records.filter { recordTouches(it, ids) }.take(10) }
    val decimals = currencyInfo(account.currency).decimals

    Box(Modifier.fillMaxSize().background(WColors.Page)) {
        Column(Modifier.fillMaxSize()) {
            WalletAppBar(
                title = "جزئیات حساب",
                navIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavClick = onBack,
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, null, tint = Color.White) }
                }
            )
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                item {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircleIcon(
                                icon = iconFor(account.iconKey),
                                color = Color(account.colorHex),
                                size = 60.dp,
                                iconSize = 34.dp,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(Modifier.width(20.dp))
                            Column {
                                Text(account.name, color = Color.White, fontSize = 17.sp)
                                Text(
                                    labelOf(AccountTypes, account.type) + " • " + account.currency,
                                    color = WColors.Muted, fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                            Column(Modifier.weight(1f)) {
                                Text("امروز", color = WColors.Muted, fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(money(account.balance, account.currency), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color(0xFF2A3A5C))
                                            .clickable { showBalanceDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Edit, null, tint = WColors.Blue, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("۳۰ روز اخیر", color = WColors.Muted, fontSize = 12.sp)
                                val pctText = (if (pct > 0) "+" else "") + String.format(java.util.Locale.US, "%.0f", pct) + "%"
                                Text(pctText, color = Color.White, fontSize = 20.sp)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Ltr {
                            Column {
                                LineChart(series, Modifier.fillMaxWidth().height(170.dp))
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    val now = System.currentTimeMillis()
                                    listOf(29, 20, 10, 0).forEach { d ->
                                        Text(
                                            if (d == 0) "امروز" else formatShortDate(dayStart(d, now)),
                                            color = WColors.Muted, fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(8.dp).background(WColors.Section))
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("آخرین تراکنش‌ها", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        Text("آخرین تراکنش‌هایی که ثبت کرده‌ام", color = WColors.Muted, fontSize = 13.sp)
                    }
                    Hairline()
                }
                if (mine.isEmpty()) {
                    item {
                        Text("هنوز تراکنشی برای این حساب ثبت نشده است.", color = WColors.Muted, fontSize = 14.sp, modifier = Modifier.padding(24.dp))
                    }
                } else {
                    items(mine, key = { it.id }) { rec ->
                        RecordRow(rec, accounts, categories) { onOpenRecord(rec) }
                        Hairline(Modifier.padding(horizontal = 16.dp))
                    }
                    item {
                        TextButton(onClick = onAllRecords, modifier = Modifier.padding(8.dp)) {
                            Text("مشاهده همه تراکنش‌ها", color = WColors.Blue)
                        }
                    }
                }
            }
        }
        LeftFab(onClick = onNewRecord)
    }

    if (showBalanceDialog) {
        TextInputDialog(
            title = "اصلاح موجودی",
            label = "موجودی جدید (${account.currency})",
            initial = minorToEditable(account.balance, decimals),
            keyboardType = KeyboardType.Decimal,
            onConfirm = { text ->
                val v = text.replace(',', '.').toDoubleOrNull()
                if (v != null) onSetBalance(majorToMinor(v, decimals))
                showBalanceDialog = false
            },
            onDismiss = { showBalanceDialog = false }
        )
    }
}

// ───────────────────────────── فهرست حساب‌ها (مدیریت با جابه‌جایی، یا انتخاب) ─────────────────────────────

/**
 * [onPick] != null → حالت انتخاب (برای تراکنش)؛ در غیر این صورت حالت مدیریت.
 * در حالت مدیریت با «نگه داشتن و کشیدن» ترتیب حساب‌ها عوض می‌شود و در دیتابیس ذخیره می‌گردد.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountsScreen(
    title: String,
    accounts: List<AccountEntity>,
    onBack: () -> Unit,
    onPick: ((AccountEntity) -> Unit)?,
    onEdit: (AccountEntity) -> Unit,
    onAdd: () -> Unit,
    onReorder: (List<Int>) -> Unit = {}
) {
    val manage = onPick == null
    val local = remember { mutableStateListOf<AccountEntity>().also { it.addAll(accounts) } }
    var draggedId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val listState = rememberLazyListState()

    // هماهنگ‌سازی با دیتابیس؛ در حال کشیدن دست نمی‌زنیم
    LaunchedEffect(accounts) {
        if (draggedId == null) {
            local.clear()
            local.addAll(accounts)
        }
    }

    Column(Modifier.fillMaxSize().background(WColors.Page)) {
        WalletAppBar(
            title = title,
            navIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavClick = onBack,
            actions = { IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, null, tint = Color.White) } }
        )
        if (manage && local.size > 1) {
            Text(
                "برای تغییر ترتیب، روی هر حساب نگه دارید و بکشید.",
                color = WColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 32.dp)) {
            items(local, key = { it.id }) { acc ->
                val dragged = draggedId == acc.id
                val dragModifier = if (manage) {
                    Modifier.pointerInput(acc.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggedId = acc.id; dragOffset = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount.y
                                val infos = listState.layoutInfo.visibleItemsInfo
                                val info = infos.firstOrNull { it.key == acc.id }
                                if (info != null) {
                                    val center = info.offset + dragOffset + info.size / 2f
                                    val target = infos.firstOrNull { it.key != acc.id && center >= it.offset && center <= it.offset + it.size }
                                    if (target != null) {
                                        val from = local.indexOfFirst { it.id == acc.id }
                                        val to = local.indexOfFirst { it.id == target.key }
                                        if (from >= 0 && to >= 0 && from != to) {
                                            local.add(to, local.removeAt(from))
                                            dragOffset += (info.offset - target.offset).toFloat()
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                draggedId = null
                                dragOffset = 0f
                                onReorder(local.map { it.id })
                            },
                            onDragCancel = { draggedId = null; dragOffset = 0f }
                        )
                    }
                } else Modifier
                Row(
                    Modifier
                        .then(if (dragged) Modifier else Modifier.animateItemPlacement())
                        .zIndex(if (dragged) 1f else 0f)
                        .graphicsLayer { translationY = if (dragged) dragOffset else 0f }
                        .fillMaxWidth()
                        .background(if (dragged) Color(0xFF2C2C2C) else WColors.Page)
                        .then(dragModifier)
                        .clickable { if (onPick != null) onPick(acc) else onEdit(acc) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleIcon(iconFor(acc.iconKey), Color(acc.colorHex), size = 44.dp, iconSize = 26.dp, shape = RoundedCornerShape(8.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(acc.name, color = Color.White, fontSize = 15.sp)
                        Text(labelOf(AccountTypes, acc.type) + " • " + acc.currency, color = WColors.Muted, fontSize = 13.sp)
                    }
                    if (onPick != null) {
                        IconButton(onClick = { onEdit(acc) }) { Icon(Icons.Filled.Edit, null, tint = WColors.Muted) }
                    } else {
                        Text(money(acc.balance, acc.currency), color = Color(0xFFE0E0E0), fontSize = 13.sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.DragHandle, null, tint = WColors.Muted)
                    }
                }
                Hairline(Modifier.padding(horizontal = 16.dp))
            }
            item {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onAdd).padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, null, tint = WColors.Blue, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("افزودن حساب", color = WColors.Blue, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ───────────────────────────── ویرایش / ساخت حساب (ارز، آیکون، رنگ) ─────────────────────────────

@Composable
fun AccountEditScreen(
    existing: AccountEntity?,
    hasRecords: Boolean,
    defaultCurrency: String,
    onBack: () -> Unit,
    onSave: (AccountEntity) -> Unit,
    onDelete: (AccountEntity) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: "Cash") }
    var currency by remember { mutableStateOf(existing?.currency ?: defaultCurrency) }
    var balanceText by remember {
        mutableStateOf(existing?.let { minorToEditable(it.balance, currencyInfo(it.currency).decimals) } ?: "0")
    }
    var color by remember { mutableStateOf(existing?.colorHex ?: AccountPalette[1]) }
    var iconKey by remember { mutableStateOf(existing?.iconKey ?: "coins") }
    var confirmDelete by remember { mutableStateOf(false) }
    val currencyLocked = existing != null && hasRecords

    Column(Modifier.fillMaxSize().background(WColors.Page)) {
        WalletAppBar(
            title = if (existing == null) "حساب جدید" else "ویرایش حساب",
            navIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavClick = onBack,
            actions = {
                if (existing != null) {
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, null, tint = Color.White) }
                }
                IconButton(onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "نام حساب را وارد کنید", Toast.LENGTH_SHORT).show()
                    } else {
                        val d = currencyInfo(currency).decimals
                        val bal = majorToMinor(balanceText.replace(',', '.').toDoubleOrNull() ?: 0.0, d)
                        onSave(
                            AccountEntity(
                                id = existing?.id ?: 0,
                                name = name.trim(),
                                type = type,
                                balance = bal,
                                colorHex = color,
                                sortOrder = existing?.sortOrder ?: 0,
                                currency = currency,
                                iconKey = iconKey
                            )
                        )
                    }
                }) { Icon(Icons.Filled.Check, null, tint = Color.White) }
            }
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                CircleIcon(iconFor(iconKey), Color(color), size = 76.dp, iconSize = 42.dp, shape = RoundedCornerShape(14.dp))
            }
            UnderlineField("نام حساب", name, { name = it }, placeholder = "مثلاً ملی")
            Column(Modifier.padding(horizontal = 16.dp)) {
                DropdownField("نوع حساب", labelOf(AccountTypes, type), AccountTypes, { type = it })
                if (currencyLocked) {
                    Text("ارز (چون برای این حساب تراکنش ثبت شده، قابل تغییر نیست)", color = WColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                    Text(currency + " — " + currencyInfo(currency).name, color = Color(0xFFE0E0E0), fontSize = 17.sp, modifier = Modifier.padding(vertical = 8.dp))
                    Hairline()
                } else {
                    DropdownField(
                        "ارز", currency + " — " + currencyInfo(currency).name,
                        Currencies.map { it.code to (it.code + " — " + it.name) }, { currency = it }
                    )
                }
            }
            UnderlineField(
                label = (if (existing == null) "موجودی اولیه" else "موجودی فعلی") + " ($currency)",
                value = balanceText,
                onValueChange = { balanceText = it },
                keyboardType = KeyboardType.Decimal
            )
            Spacer(Modifier.height(12.dp))
            Text("رنگ", color = WColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            Column(Modifier.padding(horizontal = 16.dp)) { ColorPicker(AccountPalette, color) { color = it } }
            Spacer(Modifier.height(12.dp))
            Text("آیکون", color = WColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { IconPicker(iconKey, Color(color)) { iconKey = it } }
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "حذف حساب",
            text = "حساب «${existing.name}» همراه با تمام تراکنش‌هایش حذف می‌شود. این کار قابل بازگشت نیست.",
            confirmText = "حذف",
            onConfirm = {
                confirmDelete = false
                onDelete(existing)
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
