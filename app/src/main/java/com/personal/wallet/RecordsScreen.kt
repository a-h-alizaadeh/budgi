package com.personal.wallet

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun dayLabel(ts: Long): String {
    val today = dayStart(0)
    return when {
        ts >= today -> "امروز"
        ts >= dayStart(1) -> "دیروز"
        else -> formatDate(ts)
    }
}

@Composable
fun RecordsScreen(
    fx: Money,
    initialQuery: String,
    accountIds: Set<Int>,
    accounts: List<AccountEntity>,
    records: List<RecordEntity>,
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onOpenRecord: (RecordEntity) -> Unit,
    onExport: (List<RecordEntity>) -> Unit,
    onNewRecord: () -> Unit
) {
    var period by remember { mutableStateOf(Period.Y1) }
    var searching by remember { mutableStateOf(initialQuery.isNotEmpty()) }
    var query by remember { mutableStateOf(initialQuery) }
    var typeFilter by remember { mutableStateOf("ALL") }
    var menuOpen by remember { mutableStateOf(false) }

    val from = remember(period) { dayStart(period.days - 1) }
    val filtered = remember(records, accountIds, period, query, typeFilter) {
        records.filter { r ->
            r.timestamp >= from &&
                recordTouches(r, accountIds) &&
                (typeFilter == "ALL" || r.type == typeFilter) &&
                (query.isBlank() ||
                    r.categoryName.contains(query, ignoreCase = true) ||
                    r.note.contains(query, ignoreCase = true) ||
                    r.payee.contains(query, ignoreCase = true) ||
                    r.place.contains(query, ignoreCase = true) ||
                    r.labels.contains(query, ignoreCase = true))
        }
    }
    val common = fx.commonCurrency(accountIds)
    val sumCode = common ?: fx.base
    val conv: Conv = if (common != null) IdentityConv else ({ id: Int, v: Long -> fx.toBase(id, v) })
    val sum = filtered.sumOf { recordDelta(it, accountIds, conv) }
    val grouped = remember(filtered) { filtered.groupBy { dayStart(0, it.timestamp) }.toList().sortedByDescending { it.first } }

    Box(Modifier.fillMaxSize().background(WColors.Page)) {
        Column(Modifier.fillMaxSize()) {
            if (searching) {
                Row(
                    Modifier.fillMaxWidth().background(WColors.AppBar).height(56.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBarColor(WColors.AppBar)
                    IconButton(onClick = { searching = false; query = "" }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("جستجو در دسته‌بندی، یادداشت، برچسب، مکان…", color = WColors.Muted, fontSize = 14.sp) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = Color.White),
                        colors = transparentFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                WalletAppBar(
                    title = "تراکنش‌ها",
                    navIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavClick = onBack,
                    actions = {
                        IconButton(onClick = { searching = true }) { Icon(Icons.Filled.Search, null, tint = Color.White) }
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, null, tint = Color.White) }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                listOf(
                                    "ALL" to "همه تراکنش‌ها",
                                    "EXPENSE" to "فقط هزینه‌ها",
                                    "INCOME" to "فقط درآمدها",
                                    "TRANSFER" to "فقط انتقال‌ها"
                                ).forEach { (k, l) ->
                                    DropdownMenuItem(
                                        text = { Text(if (typeFilter == k) "✓ $l" else l) },
                                        onClick = { typeFilter = k; menuOpen = false }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("خروجی CSV") },
                                    onClick = { menuOpen = false; onExport(filtered) }
                                )
                            }
                        }
                    }
                )
            }

            Row(
                Modifier.fillMaxWidth().background(WColors.Section).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(period.label, color = WColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                val signText = if (sum < 0) "-" else ""
                Text("∑ $signText" + money(kotlin.math.abs(sum), sumCode), color = WColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            if (filtered.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.List,
                        title = "لیست تراکنش‌ها",
                        message = "هیچ تراکنشی با فیلترهای انتخاب‌شده پیدا نشد. بازه یا فیلترها را تغییر دهید."
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 96.dp)) {
                    grouped.forEach { (day, list) ->
                        item(key = "h$day") {
                            Row(
                                Modifier.fillMaxWidth().background(WColors.AppBar).padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(dayLabel(day), color = Color(0xFFE0E0E0), fontSize = 13.sp, modifier = Modifier.weight(1f))
                                val d = list.sumOf { recordDelta(it, accountIds, conv) }
                                Text((if (d < 0) "-" else "") + money(kotlin.math.abs(d), sumCode), color = WColors.Muted, fontSize = 12.sp)
                            }
                        }
                        items(list, key = { it.id }) { rec ->
                            RecordRow(rec, accounts, categories) { onOpenRecord(rec) }
                            Hairline(Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }

            // پنل انتخاب بازه در پایین صفحه (مثل اپ مرجع)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(Color(0xFF262626))
                    .padding(top = 8.dp, bottom = 18.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFBDBDBD)))
                Spacer(Modifier.height(14.dp))
                PeriodSelector(period, { period = it }, Modifier.fillMaxWidth())
            }
        }

        // دکمه + بالای پنل بازه قرار می‌گیرد تا روی آن نیفتد
        LeftFab(onClick = onNewRecord, modifier = Modifier.padding(bottom = 80.dp))
    }
}
