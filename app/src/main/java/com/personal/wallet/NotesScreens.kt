package com.personal.wallet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val NoteColors = listOf(
    0xFF2D2E31, 0xFF77172E, 0xFF692B17, 0xFF7C4A03, 0xFF264D3B, 0xFF0C625D,
    0xFF256377, 0xFF284255, 0xFF472E5B, 0xFF6C394F, 0xFF4B443A, 0xFF232427
)

// ───────────────────────────── قالب‌بندی یادداشت ─────────────────────────────

private val VazirFamily = FontFamily(Font(R.font.vazirmatn_regular, FontWeight.Normal), Font(R.font.vazirmatn_bold, FontWeight.Bold))
private val ShabnamFamily = FontFamily(Font(R.font.shabnam_regular, FontWeight.Normal), Font(R.font.shabnam_bold, FontWeight.Bold))
private val GandomFamily = FontFamily(Font(R.font.gandom_regular, FontWeight.Normal))

/** کلید ذخیره‌شده در دیتابیس → نام نمایشی */
val NoteFontOptions = listOf(
    "default" to "پیش‌فرض سیستم",
    "vazir" to "وزیرمتن",
    "shabnam" to "شبنم",
    "gandom" to "گندم",
    "serif" to "سریف (لاتین)",
    "mono" to "تایپ‌رایتر (لاتین)"
)

fun noteFontFamily(key: String): FontFamily = when (key) {
    "vazir" -> VazirFamily
    "shabnam" -> ShabnamFamily
    "gandom" -> GandomFamily
    "serif" -> FontFamily.Serif
    "mono" -> FontFamily.Monospace
    else -> FontFamily.Default
}

/** رنگ متن؛ ۰ یعنی رنگ پیش‌فرض (سفید) */
val NoteTextColors = listOf(0L, 0xFFFFE082, 0xFFFFAB91, 0xFFA5D6A7, 0xFF81D4FA, 0xFFF48FB1, 0xFFCE93D8)

data class NoteFormat(
    val fontSize: Int = 16,
    val fontFamily: String = "default",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    val align: String = "start",
    val textColor: Long = 0L
) {
    fun toTextStyle(sizeOverride: Float? = null, extraStrike: Boolean = false, dim: Boolean = false): TextStyle {
        val decos = ArrayList<TextDecoration>()
        if (underline) decos.add(TextDecoration.Underline)
        if (strike || extraStrike) decos.add(TextDecoration.LineThrough)
        val base = if (textColor == 0L) Color.White else Color(textColor)
        return TextStyle(
            fontSize = (sizeOverride ?: fontSize.toFloat()).sp,
            fontFamily = noteFontFamily(fontFamily),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (decos.isEmpty()) TextDecoration.None else TextDecoration.combine(decos),
            textAlign = when (align) {
                "center" -> TextAlign.Center
                "end" -> TextAlign.End
                else -> TextAlign.Start
            },
            color = if (dim) base.copy(alpha = 0.55f) else base
        )
    }
}

fun NoteEntity.format() = NoteFormat(fontSize, fontFamily, bold, italic, underline, strike, align, textColor)

/** «۰|متن» یا «۱|متن» برای هر آیتم چک‌لیست */
fun parseChecklist(body: String): List<Pair<Boolean, String>> =
    body.split("\n").filter { it.isNotEmpty() }.map { line ->
        val checked = line.startsWith("1|")
        (checked) to (if (line.length >= 2 && line[1] == '|') line.substring(2) else line)
    }

fun encodeChecklist(items: List<Pair<Boolean, String>>): String =
    items.filter { it.second.isNotBlank() }.joinToString("\n") { (if (it.first) "1|" else "0|") + it.second.replace("\n", " ") }

// ───────────────────────────── تب یادداشت‌ها (مثل Google Keep) ─────────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NotesTab(notes: List<NoteEntity>, onOpen: (NoteEntity) -> Unit, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(notes, query) {
        if (query.isBlank()) notes else notes.filter {
            it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true)
        }
    }
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF2A2A2A))
                .padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null, tint = WColors.Muted)
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("جستجوی یادداشت‌ها", color = WColors.Muted, fontSize = 14.sp) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = Color.White),
                colors = transparentFieldColors(),
                modifier = Modifier.weight(1f)
            )
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    Icons.Filled.Edit, "یادداشتی وجود ندارد",
                    "با دکمه + یک یادداشت متنی یا چک‌لیست بسازید. یادداشت‌ها فقط روی همین گوشی ذخیره می‌شوند."
                )
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 96.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { n -> NoteCard(n) { onOpen(n) } }
            }
        }
    }
}

@Composable
private fun NoteCard(n: NoteEntity, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(n.colorHex))
            .border(1.dp, Color(0x33FFFFFF), shape)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        if (n.pinned) {
            Icon(Icons.Filled.Star, null, tint = Color(0xFFE0E0E0), modifier = Modifier.size(16.dp).align(Alignment.End))
        }
        if (n.title.isNotBlank()) {
            Text(n.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
        }
        val fmt = n.format()
        val cardSize = (n.fontSize * 0.8f).coerceIn(11f, 19f)
        if (n.isList) {
            val items = parseChecklist(n.body)
            items.take(6).forEach { (checked, text) ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                    Icon(
                        if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank, null,
                        tint = Color(0xFFD0D0D0), modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text,
                        style = fmt.toTextStyle(sizeOverride = cardSize, extraStrike = checked, dim = checked),
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (items.size > 6) Text("+ ${items.size - 6} مورد دیگر", color = WColors.Muted, fontSize = 12.sp)
        } else if (n.body.isNotBlank()) {
            Text(n.body, style = fmt.toTextStyle(sizeOverride = cardSize), maxLines = 9, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(8.dp))
        Text(formatDate(n.updatedAt), color = Color(0xFFBDBDBD), fontSize = 10.sp)
    }
}

// ───────────────────────────── ویرایشگر یادداشت (با نوار قالب‌بندی) ─────────────────────────────

@Composable
fun NoteEditScreen(
    existing: NoteEntity?,
    newIsList: Boolean,
    onSave: (NoteEntity) -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onClose: () -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var body by remember { mutableStateOf(if (existing?.isList == true) "" else existing?.body ?: "") }
    val isList = existing?.isList ?: newIsList
    val items = remember {
        mutableStateListOf<Pair<Boolean, String>>().also { l ->
            if (isList) {
                l.addAll(parseChecklist(existing?.body ?: ""))
                if (l.isEmpty()) l.add(false to "")
            }
        }
    }
    var color by remember { mutableStateOf(existing?.colorHex ?: NoteColors[0]) }
    var pinned by remember { mutableStateOf(existing?.pinned ?: false) }
    var fmt by remember { mutableStateOf(existing?.format() ?: NoteFormat()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showColors by remember { mutableStateOf(false) }
    var showFormat by remember { mutableStateOf(false) }

    fun saveAndClose() {
        val content = if (isList) encodeChecklist(items.toList()) else body.trim()
        if (title.isBlank() && content.isBlank()) {
            if (existing != null) onDelete(existing) else onClose()
        } else {
            onSave(
                NoteEntity(
                    id = existing?.id ?: 0, title = title.trim(), body = content,
                    colorHex = color, pinned = pinned, isList = isList, updatedAt = System.currentTimeMillis(),
                    fontSize = fmt.fontSize, fontFamily = fmt.fontFamily, bold = fmt.bold, italic = fmt.italic,
                    underline = fmt.underline, strike = fmt.strike, align = fmt.align, textColor = fmt.textColor
                )
            )
        }
    }

    BackHandler { saveAndClose() }

    val bg = Color(color)
    val bodyStyle = fmt.toTextStyle()
    Column(Modifier.fillMaxSize().background(bg)) {
        WalletAppBar(
            title = if (isList) "چک‌لیست" else "یادداشت",
            background = bg,
            navIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavClick = { saveAndClose() },
            actions = {
                IconButton(onClick = { showFormat = !showFormat }) {
                    Icon(Icons.Filled.TextFields, null, tint = if (showFormat) Color(0xFFFFD54F) else Color.White)
                }
                IconButton(onClick = { pinned = !pinned }) {
                    Icon(Icons.Filled.Star, null, tint = if (pinned) Color(0xFFFFD54F) else Color(0x88FFFFFF))
                }
                IconButton(onClick = { showColors = !showColors }) { Icon(Icons.Filled.Palette, null, tint = Color.White) }
                if (existing != null) {
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, null, tint = Color.White) }
                }
            }
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            TextField(
                value = title, onValueChange = { title = it },
                placeholder = { Text("عنوان", color = Color(0xFFBDBDBD), fontSize = 20.sp) },
                textStyle = TextStyle(
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    fontFamily = noteFontFamily(fmt.fontFamily)
                ),
                colors = transparentFieldColors(), modifier = Modifier.fillMaxWidth()
            )
            if (isList) {
                items.forEachIndexed { i, (checked, text) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { items[i] = it to text },
                            colors = CheckboxDefaults.colors(checkedColor = WColors.Blue, uncheckedColor = Color(0xFFD0D0D0))
                        )
                        TextField(
                            value = text, onValueChange = { items[i] = checked to it },
                            placeholder = { Text("مورد جدید", color = Color(0xFFBDBDBD)) },
                            textStyle = fmt.toTextStyle(extraStrike = checked, dim = checked),
                            colors = transparentFieldColors(), modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { if (items.size > 1) items.removeAt(i) else items[0] = false to "" }) {
                            Icon(Icons.Filled.Close, null, tint = Color(0xFFBDBDBD))
                        }
                    }
                }
                TextButton(onClick = { items.add(false to "") }) {
                    Icon(Icons.Filled.Add, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("افزودن مورد", color = Color.White)
                }
            } else {
                TextField(
                    value = body, onValueChange = { body = it },
                    placeholder = { Text("یادداشت…", color = Color(0xFFBDBDBD)) },
                    textStyle = bodyStyle,
                    colors = transparentFieldColors(),
                    keyboardOptions = KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp)
                )
            }
        }
        if (showFormat) {
            NoteFormatBar(fmt) { fmt = it }
        }
        if (showColors) {
            Row(
                Modifier.fillMaxWidth().background(Color(0x33000000)).horizontalScroll(rememberScrollState()).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NoteColors.forEach { c ->
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(Color(c))
                            .border(if (c == color) 2.dp else 1.dp, if (c == color) Color.White else Color(0x55FFFFFF), CircleShape)
                            .clickable { color = c },
                        contentAlignment = Alignment.Center
                    ) { if (c == color) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "حذف یادداشت", text = "این یادداشت حذف شود؟", confirmText = "حذف",
            onConfirm = { confirmDelete = false; onDelete(existing) },
            onDismiss = { confirmDelete = false }
        )
    }
}

/** نوار ابزار قالب‌بندی: بولد، ایتالیک، زیرخط، خط‌خورده، اندازه، تراز، فونت و رنگ متن */
@Composable
private fun NoteFormatBar(fmt: NoteFormat, onChange: (NoteFormat) -> Unit) {
    var fontMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(Color(0x55000000)).padding(vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FmtToggle(Icons.Filled.FormatBold, fmt.bold) { onChange(fmt.copy(bold = !fmt.bold)) }
            FmtToggle(Icons.Filled.FormatItalic, fmt.italic) { onChange(fmt.copy(italic = !fmt.italic)) }
            FmtToggle(Icons.Filled.FormatUnderlined, fmt.underline) { onChange(fmt.copy(underline = !fmt.underline)) }
            FmtToggle(Icons.Filled.FormatStrikethrough, fmt.strike) { onChange(fmt.copy(strike = !fmt.strike)) }
            Box(Modifier.width(1.dp).height(24.dp).background(Color(0x55FFFFFF)))
            // اندازه‌ی متن
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).clickable { onChange(fmt.copy(fontSize = (fmt.fontSize - 2).coerceAtLeast(12))) },
                contentAlignment = Alignment.Center
            ) { Text("A−", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            Text(fmt.fontSize.toString().toPersianDigits(), color = Color.White, fontSize = 14.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).clickable { onChange(fmt.copy(fontSize = (fmt.fontSize + 2).coerceAtMost(36))) },
                contentAlignment = Alignment.Center
            ) { Text("A+", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            Box(Modifier.width(1.dp).height(24.dp).background(Color(0x55FFFFFF)))
            // تراز: در چیدمان راست‌به‌چپ «start» یعنی راست
            FmtToggle(Icons.Filled.FormatAlignRight, fmt.align == "start") { onChange(fmt.copy(align = "start")) }
            FmtToggle(Icons.Filled.FormatAlignCenter, fmt.align == "center") { onChange(fmt.copy(align = "center")) }
            FmtToggle(Icons.Filled.FormatAlignLeft, fmt.align == "end") { onChange(fmt.copy(align = "end")) }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box {
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x33FFFFFF)).clickable { fontMenu = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("فونت: " + labelOf(NoteFontOptions, fmt.fontFamily), color = Color.White, fontSize = 13.sp)
                    Icon(Icons.Filled.ArrowDropDown, null, tint = Color.White)
                }
                DropdownMenu(expanded = fontMenu, onDismissRequest = { fontMenu = false }) {
                    NoteFontOptions.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label, fontFamily = noteFontFamily(key)) },
                            onClick = { onChange(fmt.copy(fontFamily = key)); fontMenu = false }
                        )
                    }
                }
            }
            NoteTextColors.forEach { c ->
                val sel = fmt.textColor == c
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(if (c == 0L) Color.White else Color(c))
                        .border(if (sel) 2.dp else 1.dp, if (sel) Color(0xFFFFD54F) else Color(0x66000000), CircleShape)
                        .clickable { onChange(fmt.copy(textColor = c)) }
                )
            }
        }
    }
}

@Composable
private fun FmtToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
            .background(if (active) Color(0x55FFFFFF) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = Color.White) }
}
