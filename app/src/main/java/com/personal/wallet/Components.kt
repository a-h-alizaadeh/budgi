package com.personal.wallet

import android.app.Activity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// ───────────────────────────── ساختار پایه ─────────────────────────────

/** بخش‌هایی که باید چپ‌به‌راست بمانند (اعداد، صفحه‌کلید، نمودارها) */
@Composable
fun Ltr(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)
}

/** رنگ نوار وضعیت سیستم را با رنگ اپ‌بار همان صفحه هماهنگ می‌کند (مثل اپ مرجع) */
@Composable
fun StatusBarColor(color: Color) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) window.statusBarColor = color.toArgb()
        }
    }
}

@Composable
fun WalletAppBar(
    title: String,
    modifier: Modifier = Modifier,
    background: Color = WColors.AppBar,
    navIcon: ImageVector? = null,
    onNavClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    StatusBarColor(background)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (navIcon != null) {
            IconButton(onClick = onNavClick) { Icon(navIcon, contentDescription = null, tint = Color.White) }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
        actions()
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = WColors.Line) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun SectionHeader(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(WColors.Section)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text, color = WColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CircleIcon(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    shape: Shape = CircleShape
) {
    Box(
        modifier = modifier.size(size).clip(shape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(150.dp).clip(CircleShape).background(Color(0xFF262626)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color(0xFFE0E0E0), modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = WColors.Muted, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

// ───────────────────────────── دکمه شناور بازشونده ─────────────────────────────

data class FabAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** دکمه‌ی + ثابت در پایین‌سمت‌چپ صفحه (همیشه سمت چپ فیزیکی، حتی در چیدمان راست‌به‌چپ) */
@Composable
fun LeftFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {
        FloatingActionButton(
            onClick = onClick,
            containerColor = WColors.Blue,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = null) }
    }
) {
    Ltr {
        Box(modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomStart) { content() }
    }
}

@Composable
fun ExpandableFab(actions: List<FabAction>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (expanded) {
            actions.forEach { a ->
                Row(
                    modifier = Modifier
                        .shadow(3.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFD7E3FF))
                        .clickable {
                            expanded = false
                            a.onClick()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(a.icon, null, tint = Color(0xFF1B1B1B), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(a.label, color = Color(0xFF1B1B1B), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = WColors.Blue,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(if (expanded) Icons.Filled.Close else Icons.Filled.Add, contentDescription = null)
        }
    }
}

// ───────────────────────────── چیپ حساب (صفحه اصلی) ─────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountChip(
    account: AccountEntity,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(70.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(account.colorHex) else WColors.ChipGray)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(iconFor(account.iconKey), null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                account.name,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            money(account.balance, account.currency),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

// ───────────────────────────── نمودارها ─────────────────────────────

@Composable
fun Gauge(fraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.11f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(stroke / 2f, stroke / 2f)
        val colors = listOf(Color(0xFFF44336), Color(0xFFFF9800), Color(0xFF19B26B))
        colors.forEachIndexed { i, c ->
            drawArc(
                color = c,
                startAngle = 135f + i * 90f,
                sweepAngle = 88f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )
        }
        val f = fraction.coerceIn(0f, 1f)
        val angle = Math.toRadians((135f + 270f * f).toDouble())
        val center = Offset(size.width / 2f, size.height / 2f)
        val len = size.minDimension * 0.31f
        val end = Offset(
            center.x + (cos(angle) * len).toFloat(),
            center.y + (sin(angle) * len).toFloat()
        )
        drawLine(color = Color.White, start = center, end = end, strokeWidth = size.minDimension * 0.04f, cap = StrokeCap.Round)
        drawCircle(color = Color(0xFF454545), radius = size.minDimension * 0.075f, center = center)
    }
}

@Composable
fun LineChart(values: List<Long>, modifier: Modifier = Modifier, color: Color = Color(0xFF2196F3)) {
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val minV = values.min()
        val maxV = values.max()
        val w = size.width
        val h = size.height
        val padTop = h * 0.08f
        val padBottom = h * 0.04f
        val stepX = if (values.size > 1) w / (values.size - 1) else w

        fun yOf(v: Long): Float {
            if (maxV == minV) return h - padBottom
            val ratio = (v - minV).toFloat() / (maxV - minV).toFloat()
            return padTop + (h - padTop - padBottom) * (1f - ratio)
        }

        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            if (i == 0) path.moveTo(x, yOf(v)) else path.lineTo(x, yOf(v))
        }
        if (maxV != minV) {
            val fill = Path()
            fill.addPath(path)
            fill.lineTo(w, h)
            fill.lineTo(0f, h)
            fill.close()
            drawPath(fill, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent)))
        }
        drawPath(path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun DonutChart(slices: List<Pair<Float, Color>>, modifier: Modifier = Modifier, strokeWidth: Dp = 26.dp) {
    Canvas(modifier) {
        val sw = strokeWidth.toPx()
        val arcSize = Size(size.width - sw, size.height - sw)
        val topLeft = Offset(sw / 2f, sw / 2f)
        val total = slices.sumOf { it.first.toDouble() }.toFloat()
        if (total <= 0f) {
            drawArc(color = Color(0xFF333333), startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = sw))
            return@Canvas
        }
        var start = -90f
        slices.forEach { (v, c) ->
            val sweep = 360f * v / total
            drawArc(color = c, startAngle = start, sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f), useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = sw))
            start += sweep
        }
    }
}

/** میله‌های دوتایی (درآمد/هزینه) برای هر ماه */
@Composable
fun PairBarChart(incomes: List<Long>, expenses: List<Long>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val n = incomes.size
        if (n == 0) return@Canvas
        val maxV = (incomes + expenses).maxOrNull()?.coerceAtLeast(1L) ?: 1L
        val slot = size.width / n
        val barW = slot * 0.26f
        val baseY = size.height
        for (i in 0 until n) {
            val cx = slot * i + slot / 2f
            val ih = size.height * (incomes[i].toFloat() / maxV.toFloat())
            val eh = size.height * (expenses[i].toFloat() / maxV.toFloat())
            drawRect(color = Color(0xFF1DB571), topLeft = Offset(cx - barW - 2f, baseY - ih), size = Size(barW, ih))
            drawRect(color = Color(0xFFE53935), topLeft = Offset(cx + 2f, baseY - eh), size = Size(barW, eh))
        }
    }
}

// ───────────────────────────── انتخاب بازه (7D 30D 12W 6M 1Y) ─────────────────────────────

@Composable
fun PeriodSelector(selected: Period, onSelect: (Period) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, Color(0xFF555555), shape)
            .height(40.dp)
    ) {
        Period.values().forEachIndexed { index, p ->
            if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF555555)))
            val sel = p == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (sel) WColors.Blue else Color.Transparent)
                    .clickable { onSelect(p) },
                contentAlignment = Alignment.Center
            ) {
                Text(p.short, color = if (sel) Color.White else Color(0xFFE0E0E0), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ───────────────────────────── ردیف تراکنش ─────────────────────────────

@Composable
fun RecordRow(
    rec: RecordEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onClick: () -> Unit
) {
    val isTransfer = rec.type == "TRANSFER"
    val cat = categories.firstOrNull { it.name == rec.categoryName }
    val icon = if (isTransfer) Icons.Filled.SwapHoriz else iconFor(cat?.iconKey ?: "category")
    val color = if (isTransfer) Color(0xFF546E7A) else Color(cat?.colorHex ?: 0xFF78909C)
    val account = accounts.firstOrNull { it.id == rec.accountId }
    val accName = account?.name ?: "؟"
    val currency = account?.currency ?: "IRR"
    val targetName = accounts.firstOrNull { it.id == rec.targetAccountId }?.name ?: "؟"

    val title = if (isTransfer) "انتقال" else rec.categoryName
    val subtitle = if (isTransfer) "$accName ← $targetName" else accName
    val amountText = when (rec.type) {
        "EXPENSE" -> "-" + money(rec.amount, currency)
        else -> money(rec.amount, currency)
    }
    val amountColor = when (rec.type) {
        "EXPENSE" -> WColors.Expense
        "INCOME" -> WColors.Income
        else -> Color(0xFFE0E0E0)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.size(44.dp)) {
            CircleIcon(icon, color, modifier = Modifier.align(Alignment.TopStart).padding(top = 2.dp, end = 2.dp), size = 40.dp)
            if (rec.status != "Uncleared") {
                Box(
                    Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(WColors.Page)
                        .padding(1.5.dp)
                        .clip(CircleShape)
                        .background(if (rec.status == "Reconciled") Color(0xFF2196F3) else Color(0xFF19B26B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = WColors.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (rec.note.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text("\"${rec.note}\"", color = WColors.Muted, fontSize = 11.sp, fontStyle = FontStyle.Italic, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(amountText, color = amountColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(formatDate(rec.timestamp), color = WColors.Muted, fontSize = 12.sp)
        }
    }
}

// ───────────────────────────── دیالوگ‌ها و فیلدها ─────────────────────────────

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WColors.Card,
        title = { Text(title, color = Color.White) },
        text = { Text(text, color = Color(0xFFBDBDBD)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText, color = WColors.Expense) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = WColors.Muted) } }
    )
}

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initial: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WColors.Card,
        title = { Text(title, color = Color.White) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label, color = WColors.Muted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = WColors.Blue,
                    cursorColor = WColors.Blue
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }) { Text("تأیید", color = WColors.Blue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = WColors.Muted) } }
    )
}

/** فیلد متنی بدون پس‌زمینه؛ با پیش‌فرض بدون خط زیرین (برای نوار جستجو) */
@Composable
fun transparentFieldColors(
    focusedIndicator: Color = Color.Transparent,
    unfocusedIndicator: Color = Color.Transparent
): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = focusedIndicator,
    unfocusedIndicatorColor = unfocusedIndicator,
    cursorColor = WColors.Blue,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)

@Composable
fun UnderlineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(label, color = WColors.Muted, fontSize = 12.sp)
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF6E6E6E), fontSize = 18.sp) },
            singleLine = singleLine,
            textStyle = TextStyle(fontSize = 18.sp, color = Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = transparentFieldColors(WColors.Blue, WColors.Line),
            modifier = Modifier.fillMaxWidth().offset(x = (-16).dp)
        )
    }
}

@Composable
fun DropdownField(
    label: String,
    valueText: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    dim: Boolean = false
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { open = true }
                .padding(top = 10.dp)
        ) {
            Text(label, color = WColors.Muted, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(valueText, color = if (dim) WColors.Muted else Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, null, tint = Color(0xFFBDBDBD))
            }
            Hairline()
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (key, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(key)
                        open = false
                    }
                )
            }
        }
    }
}

/** انتخاب‌گر رنگ (دایره‌های رنگی ۶ تایی در هر ردیف) */
@Composable
fun ColorPicker(colors: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    colors.chunked(6).forEach { row ->
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            row.forEach { c ->
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color(c)).clickable { onPick(c) },
                    contentAlignment = Alignment.Center
                ) {
                    if (c == selected) Icon(Icons.Filled.Check, null, tint = Color.White)
                }
            }
            repeat(6 - row.size) { Spacer(Modifier.size(40.dp)) }
        }
    }
}

/** انتخاب‌گر آیکون؛ آیکون‌ها در گروه‌های موضوعی نمایش داده می‌شوند */
@Composable
fun IconPicker(selectedKey: String, tint: Color, onPick: (String) -> Unit) {
    val selected = canonicalIconKey(selectedKey)
    IconGroups.forEach { (title, keys) ->
        Text(title, color = WColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
        keys.chunked(6).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { key ->
                    val sel = key == selected
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (sel) tint else Color(0xFF2B2B2B))
                            .clickable { onPick(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(iconFor(key), null, tint = if (sel) Color.White else Color(0xFFBDBDBD), modifier = Modifier.size(22.dp))
                    }
                }
                repeat(6 - row.size) { Spacer(Modifier.size(44.dp)) }
            }
        }
    }
}
