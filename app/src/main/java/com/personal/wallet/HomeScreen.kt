package com.personal.wallet

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    accounts: List<AccountEntity>,
    records: List<RecordEntity>,
    categories: List<CategoryEntity>,
    notes: List<NoteEntity>,
    fx: Money,
    selectedIds: Set<Int>,
    period: Period,
    hideAmounts: Boolean,
    onToggleHide: () -> Unit,
    onPeriodChange: (Period) -> Unit,
    onSelectOnly: (Int) -> Unit,
    onToggle: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenAccounts: () -> Unit,
    onAccountDetail: () -> Unit,
    onRecords: () -> Unit,
    onNewRecord: () -> Unit,
    onSpending: () -> Unit,
    onExport: () -> Unit,
    onCashflow: () -> Unit,
    onTemplates: () -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onNewNote: (Boolean) -> Unit,
    onCurrencyRates: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }

    Box(Modifier.fillMaxSize().background(WColors.Page)) {
        Column(Modifier.fillMaxSize()) {
            WalletAppBar(
                title = "خانه",
                navIcon = Icons.Filled.Menu,
                onNavClick = onOpenDrawer,
                actions = {
                    IconButton(onClick = onToggleHide) {
                        Icon(if (hideAmounts) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, tint = Color.White)
                    }
                }
            )
            // تب‌ها
            Row(Modifier.fillMaxWidth().background(WColors.AppBar)) {
                listOf("حساب‌ها", "یادداشت‌ها").forEachIndexed { i, label ->
                    Column(
                        Modifier.weight(1f).clickable { tab = i }.padding(top = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            label,
                            color = if (tab == i) Color.White else WColors.Muted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.width(56.dp).height(2.dp).background(if (tab == i) Color.White else Color.Transparent))
                    }
                }
            }

            if (tab == 0) {
                AccountsTab(
                    accounts, records, categories, fx, selectedIds, period,
                    onPeriodChange, onSelectOnly, onToggle, onSelectAll, onOpenAccounts,
                    onAccountDetail, onRecords, onSpending, onExport, onCashflow, onTemplates, onCurrencyRates
                )
            } else {
                NotesTab(notes, onOpenNote)
            }
        }

        if (tab == 0) {
            // یک لمس: ثبت تراکنش مستقیماً برای حساب انتخاب‌شده (بدون منوی میانی)
            LeftFab(onClick = onNewRecord)
        } else {
            LeftFab(onClick = {}) {
                ExpandableFab(
                    actions = listOf(
                        FabAction("چک‌لیست جدید", Icons.Filled.CheckCircle) { onNewNote(true) },
                        FabAction("یادداشت جدید", Icons.Filled.Edit) { onNewNote(false) }
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountsTab(
    accounts: List<AccountEntity>,
    records: List<RecordEntity>,
    categories: List<CategoryEntity>,
    fx: Money,
    selectedIds: Set<Int>,
    period: Period,
    onPeriodChange: (Period) -> Unit,
    onSelectOnly: (Int) -> Unit,
    onToggle: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onOpenAccounts: () -> Unit,
    onAccountDetail: () -> Unit,
    onRecords: () -> Unit,
    onSpending: () -> Unit,
    onExport: () -> Unit,
    onCashflow: () -> Unit,
    onTemplates: () -> Unit,
    onCurrencyRates: () -> Unit
) {
    var showHelp by remember { mutableStateOf(false) }
    val base = fx.base

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
        // ── حساب‌های من ──
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("حساب‌های من", color = Color.White, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF3C3C3C), RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenAccounts),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = WColors.Blue, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(10.dp))

        val cells: List<@Composable (Modifier) -> Unit> = buildList {
            accounts.forEach { acc ->
                add { m ->
                    AccountChip(
                        account = acc,
                        selected = acc.id in selectedIds,
                        modifier = m,
                        onClick = { onSelectOnly(acc.id) },
                        onLongClick = { onToggle(acc.id) }
                    )
                }
            }
        }
        cells.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { cell -> cell(Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (accounts.isEmpty()) {
            Text(
                "هنوز حسابی ندارید. برای ساخت حساب، منوی ☰ ← «حساب‌ها» را باز کنید یا همین‌جا بزنید.",
                color = WColors.Blue, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenAccounts).padding(16.dp)
            )
        }
        Text(
            "انتخاب همه",
            color = WColors.Blue,
            fontSize = 13.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onSelectAll)
                .padding(16.dp)
        )

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            PillButton("جزئیات حساب", null, onAccountDetail)
            PillButton("تراکنش‌ها", Icons.AutoMirrored.Filled.List, onRecords)
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).background(WColors.Section))

        // ── میانبرها ──
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickChip("هزینه‌ها", Icons.Filled.PieChart, Color(0xFF26C6DA), onSpending)
            QuickChip("خروجی", Icons.Filled.Share, Color(0xFFE0E0E0), onExport)
            QuickChip("جریان نقدی", Icons.Filled.BarChart, Color(0xFF26C6DA), onCashflow)
            QuickChip("قالب‌ها", Icons.Filled.ContentCopy, Color(0xFFE0E0E0), onTemplates)
            QuickChip("نرخ ارز", Icons.Filled.MonetizationOn, Color(0xFFFFB300), onCurrencyRates)
        }

        // ── جمع موجودی (به ارز پایه) ──
        val total = accounts.filter { it.id in selectedIds }.sumOf { fx.toBase(it.id, it.balance) }
        val missing = fx.missingRates(selectedIds)
        Column(
            Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(WColors.Card)
                .padding(16.dp)
        ) {
            Text("جمع موجودی حساب‌های انتخاب‌شده ($base)", color = WColors.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(money(total, base), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            if (missing.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "نرخ تبدیل ${missing.joinToString("، ")} تعریف نشده؛ جمع‌ها دقیق نیست. (نرخ ارز را بزنید)",
                    color = Color(0xFFFFB300), fontSize = 12.sp,
                    modifier = Modifier.clickable(onClick = onCurrencyRates)
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        // ── داشبورد ──
        val dash = remember(accounts, records, selectedIds, period, fx) {
            computeDashboard(accounts, records, selectedIds, period, fx)
        }
        Column(
            Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(WColors.Card)
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("داشبورد", color = Color.White, fontSize = 18.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("عملکرد ${period.label}", color = WColors.Muted, fontSize = 13.sp)
                }
                IconButton(onClick = { showHelp = true }) { Icon(Icons.Filled.Info, null, tint = WColors.Muted) }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().background(Color(0xFF262626)).padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text("همه‌ی مبالغ به ارز پایه ($base) تبدیل و نمایش داده می‌شوند.", color = WColors.Muted, fontSize = 12.sp)
            }

            val pagerState = rememberPagerState(pageCount = { 3 })
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                when (page) {
                    0 -> Row(
                        Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        GaugeBlock(
                            "روند موجودی", money(dash.balance, base), dash.balanceFraction,
                            percentText(dash.balanceChangePct) + " نسبت به ابتدای بازه", onRecords
                        )
                        GaugeBlock(
                            "جریان نقدی", money(dash.cashflow, base), dash.cashFraction,
                            if (dash.income + dash.expense == 0L) "بدون گردش در این بازه"
                            else (if (dash.cashflow >= 0) "پس‌انداز " else "کسری ") + String.format(Locale.US, "%.0f%%", kotlin.math.abs(dash.cashRatioPct)) + " از گردش",
                            onCashflow
                        )
                    }
                    1 -> Column(Modifier.fillMaxWidth().clickable(onClick = onCashflow).padding(16.dp)) {
                        Text("درآمد و هزینه", color = Color.White, fontSize = 15.sp)
                        Spacer(Modifier.height(14.dp))
                        val maxV = maxOf(dash.income, dash.expense, 1L).toFloat()
                        BarLine("درآمد", money(dash.income, base), dash.income / maxV, WColors.Income)
                        Spacer(Modifier.height(12.dp))
                        BarLine("هزینه", money(dash.expense, base), dash.expense / maxV, WColors.Expense)
                    }
                    else -> Column(Modifier.fillMaxWidth().clickable(onClick = onSpending).padding(16.dp)) {
                        Text("پرهزینه‌ترین دسته‌بندی‌ها", color = Color.White, fontSize = 15.sp)
                        Spacer(Modifier.height(12.dp))
                        if (dash.topCategories.isEmpty()) {
                            Text("در این بازه هزینه‌ای ثبت نشده است.", color = WColors.Muted, fontSize = 13.sp)
                        } else {
                            val top = dash.topCategories.first().second.toFloat().coerceAtLeast(1f)
                            dash.topCategories.forEach { (name, sum) ->
                                val c = categories.firstOrNull { it.name == name }
                                BarLine(name, money(sum, base), sum / top, Color(c?.colorHex ?: 0xFF78909C))
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.Center) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .padding(3.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (pagerState.currentPage == i) WColors.Blue else Color(0xFF555555))
                    )
                }
            }
            PeriodSelector(
                selected = period,
                onSelect = onPeriodChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)
            )
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            containerColor = WColors.Card,
            title = { Text("راهنمای داشبورد", color = Color.White) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "• روند موجودی: درصد تغییر مجموع موجودی حساب‌های انتخاب‌شده نسبت به ابتدای بازه. وسط گیج = بدون تغییر؛ سمت سبز = رشد؛ سمت قرمز = کاهش (دو سر گیج = ±۵۰٪).\n\n" +
                            "• جریان نقدی: (درآمد − هزینه) تقسیم بر (درآمد + هزینه). سبز یعنی بیشتر از خرج کرده‌اید پس‌انداز کرده‌اید؛ قرمز یعنی خرج بیشتر از درآمد بوده.\n\n" +
                            "• صفحه دوم: مقایسه‌ی درآمد و هزینه. صفحه سوم: پرهزینه‌ترین دسته‌بندی‌ها.\n\n" +
                            "• با لمس هر نمودار به صفحه‌ی جزئیاتش می‌روید. بازه را با 7D … 1Y عوض کنید. حساب‌هایی که انتخاب کرده‌اید محاسبه می‌شوند و ارزهای مختلف با نرخ‌های دستی به ارز پایه تبدیل می‌شوند.",
                        color = Color(0xFFD0D0D0), fontSize = 14.sp
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("متوجه شدم", color = WColors.Blue) } }
        )
    }
}

private fun percentText(p: Double): String =
    (if (p > 0.05) "+" else "") + String.format(Locale.US, "%.0f%%", p)

@Composable
private fun PillButton(text: String, icon: ImageVector?, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, Color(0xFF555555), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = WColors.Blue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = Color(0xFFEDEDED), fontSize = 13.sp)
    }
}

@Composable
private fun QuickChip(text: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF262628))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color(0xFFEDEDED), fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun GaugeBlock(title: String, valueText: String, fraction: Float, caption: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(158.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(4.dp)
    ) {
        Ltr { Gauge(fraction, Modifier.size(124.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(title, color = WColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(valueText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(caption, color = WColors.Muted, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun BarLine(label: String, valueText: String, fraction: Float, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFFE0E0E0), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(valueText, color = Color.White, fontSize = 13.sp, maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF333333))) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
