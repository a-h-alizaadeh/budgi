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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

// ───────────────────────────── قالب‌ها ─────────────────────────────

@Composable
fun TemplatesScreen(
    templates: List<TemplateEntity>,
    accounts: List<AccountEntity>,
    onBack: () -> Unit,
    onUse: (TemplateEntity) -> Unit,
    onDelete: (TemplateEntity) -> Unit
) {
    var toDelete by remember { mutableStateOf<TemplateEntity?>(null) }
    Column(Modifier.fillMaxSize().background(WColors.Page)) {
        WalletAppBar(title = "قالب‌ها", navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack)
        if (templates.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    Icons.Filled.ContentCopy, "قالبی وجود ندارد",
                    "در صفحه جزئیات هر تراکنش می‌توانید آن را به‌عنوان قالب ذخیره کنید تا بعداً با یک لمس ثبت شود."
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(templates, key = { it.id }) { t ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onUse(t) }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircleIcon(Icons.Filled.ContentCopy, Color(0xFF546E7A), size = 40.dp, iconSize = 20.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.name, color = Color.White, fontSize = 15.sp)
                            Text(t.summary(accounts), color = WColors.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { toDelete = t }) { Icon(Icons.Filled.Delete, null, tint = WColors.Muted) }
                    }
                    Hairline(Modifier.padding(start = 72.dp))
                }
            }
        }
    }
    toDelete?.let { t ->
        ConfirmDialog(
            title = "حذف قالب",
            text = "قالب «${t.name}» حذف شود؟",
            confirmText = "حذف",
            onConfirm = { onDelete(t); toDelete = null },
            onDismiss = { toDelete = null }
        )
    }
}

// ───────────────────────────── هزینه‌ها (نمودار دونات) ─────────────────────────────

@Composable
fun SpendingScreen(
    fx: Money,
    accountIds: Set<Int>,
    records: List<RecordEntity>,
    categories: List<CategoryEntity>,
    onBack: () -> Unit
) {
    var period by remember { mutableStateOf(Period.D30) }
    val from = remember(period) { dayStart(period.days - 1) }
    val rows = remember(records, accountIds, period, fx) {
        records.filter { it.type == "EXPENSE" && it.timestamp >= from && it.accountId in accountIds }
            .groupBy { it.categoryName }
            .map { (k, v) -> k to v.sumOf { fx.toBase(it.accountId, it.amount) } }
            .sortedByDescending { it.second }
    }
    val total = rows.sumOf { it.second }
    fun colorOf(name: String): Color = Color(categories.firstOrNull { it.name == name }?.colorHex ?: 0xFF78909C)

    Column(Modifier.fillMaxSize().background(WColors.Page)) {
        WalletAppBar(title = "هزینه‌ها", navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                PeriodSelector(period, { period = it }, Modifier.fillMaxWidth().padding(16.dp))
                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    DonutChart(rows.map { it.second.toFloat() to colorOf(it.first) }, Modifier.size(220.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("جمع هزینه (${fx.base})", color = WColors.Muted, fontSize = 12.sp)
                        Text(money(total, fx.base), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                SectionHeader("به تفکیک دسته‌بندی")
            }
            if (rows.isEmpty()) {
                item { Text("در این بازه هزینه‌ای ثبت نشده است.", color = WColors.Muted, modifier = Modifier.padding(24.dp)) }
            } else {
                items(rows, key = { it.first }) { (name, sum) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(colorOf(name)))
                        Spacer(Modifier.width(12.dp))
                        Text(name, color = Color(0xFFE0E0E0), fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(String.format(java.util.Locale.US, "%.1f%%", sum * 100.0 / total.coerceAtLeast(1L)), color = WColors.Muted, fontSize = 12.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(money(sum, fx.base), color = Color.White, fontSize = 13.sp)
                    }
                    Hairline(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

// ───────────────────────────── جریان نقدی ماهانه ─────────────────────────────

@Composable
fun CashflowScreen(fx: Money, accountIds: Set<Int>, records: List<RecordEntity>, onBack: () -> Unit) {
    val months = remember(records, accountIds, fx) {
        (5 downTo 0).map { back ->
            val start = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); add(Calendar.MONTH, -back)
            }
            val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            val inMonth = records.filter { it.accountId in accountIds && it.timestamp >= start.timeInMillis && it.timestamp < end.timeInMillis }
            val label = (start.get(Calendar.MONTH) + 1).toString() + "/" + (start.get(Calendar.YEAR) % 100)
            Triple(
                label,
                inMonth.filter { it.type == "INCOME" }.sumOf { fx.toBase(it.accountId, it.amount) },
                inMonth.filter { it.type == "EXPENSE" }.sumOf { fx.toBase(it.accountId, it.amount) }
            )
        }
    }
    Column(Modifier.fillMaxSize().background(WColors.Page)) {
        WalletAppBar(title = "جریان نقدی", navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend("درآمد", WColors.Income)
                Legend("هزینه", WColors.Expense)
            }
            Ltr {
                Column {
                    PairBarChart(months.map { it.second }, months.map { it.third }, Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 16.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                        months.forEach { m -> Text(m.first, color = WColors.Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionHeader("خلاصه ماهانه (${fx.base})")
            months.reversed().forEach { (label, inc, exp) ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.width(56.dp))
                    Column(Modifier.weight(1f)) {
                        Text("+" + money(inc, fx.base), color = WColors.Income, fontSize = 12.sp)
                        Text("-" + money(exp, fx.base), color = WColors.Expense, fontSize = 12.sp)
                    }
                    val net = inc - exp
                    Text((if (net < 0) "-" else "") + money(kotlin.math.abs(net), fx.base), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Hairline(Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun Legend(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color(0xFFE0E0E0), fontSize = 13.sp)
    }
}
