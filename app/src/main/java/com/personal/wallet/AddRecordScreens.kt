package com.personal.wallet

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private fun darken(c: Color, f: Float = 0.82f): Color = Color(c.red * f, c.green * f, c.blue * f, 1f)
private fun lighten(c: Color, f: Float = 0.18f): Color =
    Color(c.red + (1f - c.red) * f, c.green + (1f - c.green) * f, c.blue + (1f - c.blue) * f, 1f)

// ───────────────────────────── صفحه اصلی افزودن / ویرایش تراکنش ─────────────────────────────

@Composable
fun AddRecordScreen(
    draft: RecordDraft,
    accounts: List<AccountEntity>,
    templates: List<TemplateEntity>,
    fx: Money,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onOpenDetails: () -> Unit,
    onPickAccount: () -> Unit,
    onPickTarget: () -> Unit,
    onPickCategory: () -> Unit,
    onApplyTemplate: (TemplateEntity) -> Unit
) {
    var showTemplates by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }

    val account = accounts.firstOrNull { it.id == draft.accountId }
    val target = accounts.firstOrNull { it.id == draft.targetAccountId }
    val header = if (account != null) Color(account.colorHex) else Color(0xFFF38A00)
    val top = darken(header)
    val isTransfer = draft.type == "TRANSFER"
    val srcCur = draft.sourceCurrency(accounts)
    val tgtCur = draft.targetCurrency(accounts)
    val decimals = currencyInfo(srcCur).decimals

    Column(Modifier.fillMaxSize().background(WColors.Page)) {
        WalletAppBar(
            title = "",
            background = top,
            navIcon = Icons.Filled.Close,
            onNavClick = onClose,
            actions = {
                if (draft.editing != null) {
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, null, tint = Color.White) }
                }
                IconButton(onClick = onSave) { Icon(Icons.Filled.Check, null, tint = Color.White) }
            }
        )
        // تب‌ها
        Row(Modifier.fillMaxWidth().height(48.dp)) {
            listOf("INCOME" to "درآمد", "EXPENSE" to "هزینه", "TRANSFER" to "انتقال").forEachIndexed { i, (key, label) ->
                if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(top))
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (draft.type == key) top else header)
                        .clickable { draft.type = key },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        // مبلغ (اعداد چپ‌به‌راست می‌مانند)
        Box(Modifier.fillMaxWidth().weight(1f).background(header)) {
            Ltr {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(bottom = 40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when (draft.type) {
                            "INCOME" -> "+"
                            "EXPENSE" -> "–"
                            else -> ""
                        },
                        color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Light
                    )
                    Spacer(Modifier.weight(1f))
                    val shown = prettyExpression(draft.expression)
                    Text(
                        shown,
                        color = Color.White,
                        fontSize = when {
                            shown.length <= 8 -> 64.sp
                            shown.length <= 12 -> 46.sp
                            shown.length <= 17 -> 34.sp
                            else -> 26.sp
                        },
                        fontWeight = FontWeight.Light,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(srcCur, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Light)
                }
            }
            // حساب / دسته‌بندی (راست‌چین)
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 12.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PickerLabel(if (isTransfer) "از حساب" else "حساب", account?.name ?: "انتخاب", onPickAccount, Modifier.weight(1f))
                if (isTransfer) {
                    PickerLabel("به حساب", target?.name ?: "انتخاب", onPickTarget, Modifier.weight(1f))
                } else {
                    PickerLabel("دسته‌بندی", draft.categoryName, onPickCategory, Modifier.weight(1f))
                }
            }
        }
        // نوار پایین مبلغ: «توضیحات» سمت راست، «قالب‌ها» (یا مبلغ مقصد در انتقال) کنارش
        Row(Modifier.fillMaxWidth().height(52.dp)) {
            BarButton("توضیحات", Icons.Filled.Edit, lighten(header), Modifier.weight(1f), onOpenDetails)
            Box(Modifier.width(1.dp).fillMaxHeight().background(top))
            if (isTransfer) {
                val tMinor = draft.targetMinor(accounts, fx)
                val differs = srcCur != tgtCur
                Row(
                    Modifier
                        .weight(1.7f)
                        .fillMaxHeight()
                        .background(Color(0xFFDCE3E8))
                        .clickable(enabled = differs) { showTargetDialog = true }
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "مبلغ مقصد ~ " + formatMoney(tMinor, tgtCur) + if (differs) " (ویرایش)" else "",
                        color = Color(0xFF5F6B72), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                BarButton("قالب‌ها", Icons.Filled.ContentCopy, lighten(header), Modifier.weight(1f)) { showTemplates = true }
            }
        }
        Ltr { Keypad(draft, decimals) }
    }

    if (showTemplates) {
        AlertDialog(
            onDismissRequest = { showTemplates = false },
            containerColor = WColors.Card,
            title = { Text("قالب‌ها", color = Color.White) },
            text = {
                if (templates.isEmpty()) {
                    Text(
                        "هنوز قالبی ذخیره نشده است. در صفحه جزئیات تراکنش می‌توانید آن را به‌عنوان قالب ذخیره کنید.",
                        color = WColors.Muted, fontSize = 14.sp
                    )
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        templates.forEach { t ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { showTemplates = false; onApplyTemplate(t) }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(t.name, color = Color.White, fontSize = 15.sp)
                                Text(t.summary(accounts), color = WColors.Muted, fontSize = 12.sp)
                            }
                            Hairline()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTemplates = false }) { Text("بستن", color = WColors.Blue) } }
        )
    }

    if (showTargetDialog) {
        TextInputDialog(
            title = "مبلغ حساب مقصد",
            label = "مبلغ به $tgtCur",
            initial = minorToEditable(draft.targetMinor(accounts, fx), currencyInfo(tgtCur).decimals),
            keyboardType = KeyboardType.Decimal,
            onConfirm = { t ->
                val v = t.replace(',', '.').toDoubleOrNull()
                if (v != null && v > 0.0) draft.targetOverride = majorToMinor(v, currencyInfo(tgtCur).decimals)
                showTargetDialog = false
            },
            onDismiss = { showTargetDialog = false }
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "حذف تراکنش",
            text = "این تراکنش حذف می‌شود و موجودی حساب‌ها به حالت قبل برمی‌گردد.",
            confirmText = "حذف",
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun BarButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxHeight().background(bg).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** عنوان کوچک + مقدار، راست‌چین */
@Composable
private fun PickerLabel(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ───────────────────────────── صفحه‌کلید (بزرگ‌تر) ─────────────────────────────

@Composable
private fun Keypad(draft: RecordDraft, decimals: Int) {
    fun typeDigit(d: String) {
        val e = draft.expression
        // سقف طول عدد، برای جلوگیری از سرریز (Overflow) مقدارهای Long
        if (e.split('+', '-', '*', '/').last().length >= 12 || e.length >= 40) return
        draft.expression = if (e == "0") d else e + d
    }

    fun typeOperator(op: String) {
        val e = draft.expression
        val last = e.lastOrNull()
        draft.expression = when {
            last == null -> "0"
            last in "+-*/" -> e.dropLast(1) + op
            last == '.' -> e + "0" + op
            else -> e + op
        }
    }

    fun typeDot() {
        val e = draft.expression
        val lastNumber = e.split('+', '-', '*', '/').last()
        if (!lastNumber.contains('.')) draft.expression = if (e.isEmpty()) "0." else "$e."
    }

    fun backspace() {
        val e = draft.expression
        draft.expression = if (e.length > 1) e.dropLast(1) else "0"
    }

    fun equalsKey() {
        draft.expression = formatResult(evaluateExpression(draft.expression), decimals)
    }

    val keyColor = WColors.KeyText
    Row(Modifier.fillMaxWidth().height(344.dp)) {
        Column(Modifier.weight(3f).fillMaxHeight().background(WColors.Keypad)) {
            val rows = listOf(listOf("7", "8", "9"), listOf("4", "5", "6"), listOf("1", "2", "3"), listOf(".", "0", "⌫"))
            rows.forEach { row ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    row.forEach { k ->
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    when (k) {
                                        "." -> typeDot()
                                        "⌫" -> backspace()
                                        else -> typeDigit(k)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (k == "⌫") {
                                Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = keyColor, modifier = Modifier.size(32.dp))
                            } else {
                                Text(k, color = keyColor, fontSize = 36.sp, fontWeight = FontWeight.Light)
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().background(WColors.KeypadSide)) {
            listOf("÷" to "/", "×" to "*", "−" to "-", "+" to "+", "=" to "=").forEach { (label, op) ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable { if (op == "=") equalsKey() else typeOperator(op) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = keyColor, fontSize = 30.sp, fontWeight = FontWeight.Light)
                }
            }
        }
    }
}

// ───────────────────────────── صفحه جزئیات تراکنش (یادداشت، تاریخ شمسی، برچسب…) ─────────────────────────────

@Composable
fun RecordDetailsScreen(
    draft: RecordDraft,
    accounts: List<AccountEntity>,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSaveTemplate: (String) -> Unit
) {
    val context = LocalContext.current
    val account = accounts.firstOrNull { it.id == draft.accountId }
    val header = if (account != null) darken(Color(account.colorHex)) else Color(0xFFCB7E00)
    var showLabelDialog by remember { mutableStateOf(false) }
    var showPlaceDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    val currency = draft.sourceCurrency(accounts)

    val cal = Calendar.getInstance().also { it.timeInMillis = draft.timestamp }

    Column(Modifier.fillMaxSize().background(Color(0xFF222222))) {
        Row(
            Modifier.fillMaxWidth().background(header).height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBarColor(header)
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text(
                formatMoney(draft.amountMinor(accounts), currency),
                color = Color.White, fontSize = 20.sp, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSave) { Icon(Icons.Filled.Check, null, tint = Color.White) }
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
            UnderlineField("یادداشت", draft.note, { draft.note = it }, placeholder = "توضیحات", singleLine = false)

            // برچسب‌ها
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("برچسب‌ها", color = WColors.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    draft.labels.forEach { l ->
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF3A3A3A))
                                .clickable { draft.labels = draft.labels - l }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(l, color = Color.White, fontSize = 13.sp)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Filled.Close, null, tint = WColors.Muted, modifier = Modifier.size(14.dp))
                        }
                    }
                    AddPill("افزودن برچسب") { showLabelDialog = true }
                }
                Spacer(Modifier.height(8.dp))
                Hairline()
            }

            UnderlineField(
                if (draft.type == "INCOME") "پرداخت‌کننده" else "دریافت‌کننده",
                draft.payee, { draft.payee = it }
            )

            // تاریخ و ساعت
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1.4f).clickable { showDate = true }.padding(top = 10.dp)) {
                    Text(if (CalendarPrefs.jalali) "تاریخ (شمسی)" else "تاریخ", color = WColors.Muted, fontSize = 12.sp)
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatDate(draft.timestamp), color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ArrowDropDown, null, tint = Color(0xFFBDBDBD))
                    }
                    Hairline()
                }
                Column(
                    Modifier.weight(1f).clickable {
                        TimePickerDialog(
                            context,
                            { _, h, min ->
                                val c = Calendar.getInstance()
                                c.timeInMillis = draft.timestamp
                                c.set(Calendar.HOUR_OF_DAY, h)
                                c.set(Calendar.MINUTE, min)
                                draft.timestamp = c.timeInMillis
                            },
                            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), CalendarPrefs.jalali
                        ).show()
                    }.padding(top = 10.dp)
                ) {
                    Text("ساعت", color = WColors.Muted, fontSize = 12.sp)
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(draft.timestamp), color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ArrowDropDown, null, tint = Color(0xFFBDBDBD))
                    }
                    Hairline()
                }
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                DropdownField("نوع پرداخت", labelOf(PaymentTypes, draft.paymentType), PaymentTypes, { draft.paymentType = it })
                DropdownField("گارانتی", labelOf(Warranties, draft.warranty), Warranties, { draft.warranty = it }, dim = draft.warranty == "None")
                DropdownField("وضعیت", labelOf(RecordStatuses, draft.status), RecordStatuses, { draft.status = it })
            }

            // مکان
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("مکان", color = WColors.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                if (draft.place.isBlank()) {
                    AddPill("افزودن مکان") { showPlaceDialog = true }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Place, null, tint = WColors.Blue)
                        Spacer(Modifier.width(8.dp))
                        Text(draft.place, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f).clickable { showPlaceDialog = true })
                        IconButton(onClick = { draft.place = "" }) { Icon(Icons.Filled.Close, null, tint = WColors.Muted) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Hairline()
            }

            // ذخیره به عنوان قالب
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                OutlinedButton(
                    onClick = {
                        if (draft.amountMinor(accounts) <= 0L) {
                            Toast.makeText(context, "اول مبلغ را وارد کنید", Toast.LENGTH_SHORT).show()
                        } else showTemplateDialog = true
                    },
                    border = BorderStroke(1.dp, Color(0xFF555555))
                ) {
                    Icon(Icons.Filled.ContentCopy, null, tint = WColors.Blue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ذخیره به‌عنوان قالب", color = Color.White)
                }
            }
        }
    }

    DatePickerHost(
        show = showDate,
        initialMillis = draft.timestamp,
        onPicked = { draft.timestamp = it; showDate = false },
        onDismiss = { showDate = false }
    )

    if (showLabelDialog) {
        TextInputDialog(
            title = "برچسب جدید",
            label = "نام برچسب",
            onConfirm = { t ->
                if (t.isNotBlank() && t !in draft.labels) draft.labels = draft.labels + t.replace(",", " ")
                showLabelDialog = false
            },
            onDismiss = { showLabelDialog = false }
        )
    }
    if (showPlaceDialog) {
        TextInputDialog(
            title = "مکان",
            label = "نام مکان یا آدرس",
            initial = draft.place,
            onConfirm = { t -> draft.place = t; showPlaceDialog = false },
            onDismiss = { showPlaceDialog = false }
        )
    }
    if (showTemplateDialog) {
        TextInputDialog(
            title = "ذخیره قالب",
            label = "نام قالب (مثلاً کرایه تاکسی)",
            onConfirm = { t ->
                if (t.isNotBlank()) onSaveTemplate(t)
                showTemplateDialog = false
            },
            onDismiss = { showTemplateDialog = false }
        )
    }
}

@Composable
private fun AddPill(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF555555), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.AddCircle, null, tint = WColors.Blue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontSize = 13.sp)
    }
}

// ───────────────────────────── انتخاب دسته‌بندی (دو سطح: مادر ← زیرمجموعه) ─────────────────────────────

/**
 * فقط دسته‌های «برگ» قابل انتخاب‌اند: دسته‌ی مادر (که زیرمجموعه دارد) با لمس باز/بسته می‌شود
 * و انتخاب نمی‌شود؛ باید یکی از زیرمجموعه‌هایش را انتخاب کرد. دسته‌ی اصلی بدون زیرمجموعه مستقیماً قابل انتخاب است.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryPickerScreen(
    categories: List<CategoryEntity>,
    records: List<RecordEntity>,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onPick: (CategoryEntity) -> Unit
) {
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var collapsed by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val parentIds = remember(categories) { categories.mapNotNull { it.parentId }.toSet() }
    val frequent = remember(categories, records) {
        val counts = records.groupingBy { it.categoryName }.eachCount()
        val leaves = categories.filter { it.id !in parentIds }
        leaves.filter { counts.containsKey(it.name) }
            .sortedByDescending { counts[it.name] ?: 0 }
            .ifEmpty { leaves }
            .take(8)
    }
    val nodes = remember(categories, collapsed, query) {
        if (query.isBlank()) flattenTree(categories, parentIds - collapsed)
        else categories.filter { it.id !in parentIds && it.name.contains(query, ignoreCase = true) }.map { CategoryNode(it, 0, false) }
    }

    Column(Modifier.fillMaxSize().background(WColors.Page)) {
        if (searching) {
            Row(
                Modifier.fillMaxWidth().background(WColors.AppBar).height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBarColor(WColors.AppBar)
                IconButton(onClick = { searching = false; query = "" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("جستجوی دسته‌بندی", color = WColors.Muted) },
                    singleLine = true,
                    colors = transparentFieldColors(),
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            WalletAppBar(
                title = "دسته‌بندی",
                navIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavClick = onBack,
                actions = {
                    IconButton(onClick = { searching = true }) { Icon(Icons.Filled.Search, null, tint = Color.White) }
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, null, tint = Color.White) }
                }
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            if (query.isBlank()) {
                item { SectionHeader("پرتکرارترین‌ها") }
                item {
                    val pages = frequent.chunked(4)
                    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                                val pageItems = pages.getOrNull(page) ?: emptyList<CategoryEntity>()
                                pageItems.forEach { c ->
                                    Column(
                                        Modifier.weight(1f).clickable { onPick(c) }.padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircleIcon(iconFor(c.iconKey), Color(c.colorHex), size = 52.dp, iconSize = 28.dp)
                                        Spacer(Modifier.height(6.dp))
                                        Text(c.name, color = Color(0xFFE0E0E0), fontSize = 12.sp, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                repeat(4 - pageItems.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        if (pages.size > 1) {
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                                repeat(pages.size) { i ->
                                    Box(
                                        Modifier.padding(3.dp).size(7.dp).clip(CircleShape)
                                            .background(if (pagerState.currentPage == i) WColors.Blue else Color(0xFF555555))
                                    )
                                }
                            }
                        }
                    }
                }
                item { SectionHeader("همه دسته‌بندی‌ها (مادر ← زیرمجموعه)") }
            }
            items(nodes, key = { it.category.id }) { node ->
                val isParent = node.hasChildren
                val open = node.category.id !in collapsed
                CategoryRow(
                    c = node.category,
                    depth = node.depth,
                    trailing = {
                        if (isParent) {
                            Icon(
                                if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                null, tint = WColors.Muted, modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    },
                    subtitle = if (query.isNotBlank()) categoryPath(categories, node.category) else null,
                    onClick = {
                        if (isParent) {
                            collapsed = if (open) collapsed + node.category.id else collapsed - node.category.id
                        } else {
                            onPick(node.category)
                        }
                    }
                )
            }
        }
    }
}

/** ردیف دسته‌بندی با تورفتگی بر اساس عمق درخت (آیکون‌ها کمی درشت‌تر) */
@Composable
fun CategoryRow(
    c: CategoryEntity,
    depth: Int = 0,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {},
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = (16 + depth * 22).dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIcon(
            iconFor(c.iconKey), Color(c.colorHex),
            size = if (depth == 0) 46.dp else 40.dp,
            iconSize = if (depth == 0) 26.dp else 22.dp
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(c.name, color = Color(0xFFE0E0E0), fontSize = if (depth == 0) 15.sp else 14.sp)
            if (subtitle != null) Text(subtitle, color = WColors.Muted, fontSize = 11.sp)
        }
        Box(Modifier.defaultMinSize(minHeight = 48.dp), contentAlignment = Alignment.Center) { trailing() }
    }
    Hairline(Modifier.padding(horizontal = 16.dp))
}
