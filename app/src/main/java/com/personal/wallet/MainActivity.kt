package com.personal.wallet

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** صفحه‌های برنامه؛ پیش‌نویس تراکنش در خود صفحه‌ها نگهداری می‌شود تا هنگام رفتن به انتخاب دسته/حساب از بین نرود. */
sealed class Screen {
    object Home : Screen()
    data class AccountDetail(val accountId: Int) : Screen()
    data class Records(val accountIds: Set<Int>, val query: String = "") : Screen()
    data class AddRecord(val draft: RecordDraft) : Screen()
    data class RecordDetails(val draft: RecordDraft) : Screen()
    data class CategoryPicker(val draft: RecordDraft) : Screen()
    data class AccountPicker(val draft: RecordDraft, val forTarget: Boolean) : Screen()
    object Categories : Screen()
    data class CategoryEdit(val categoryId: Int?, val parentId: Int?) : Screen()
    object Accounts : Screen()
    data class AccountEdit(val accountId: Int?) : Screen()
    object Templates : Screen()
    object Spending : Screen()
    object Cashflow : Screen()
    data class NoteEdit(val noteId: Int?, val isList: Boolean) : Screen()
    object Settings : Screen()
    object Security : Screen()
    object CurrencyRates : Screen()
    object Warranties : Screen()
    object Loyalty : Screen()
    data class LoyaltyEdit(val cardId: Int?) : Screen()
    object Locations : Screen()
    object Exports : Screen()
    object ExpectedExpenses : Screen()
    data class ExpectedEdit(val id: Int?) : Screen()
    object Savings : Screen()
    data class SavingEdit(val id: Int?) : Screen()
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(applicationContext)
        val repo = WalletRepository(database)
        val settings = AppSettings(applicationContext)
        setContent {
            // کل برنامه راست‌به‌چپ است؛ اعداد، صفحه‌کلید و نمودارها با Ltr{} چپ‌به‌راست می‌مانند.
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalHideAmounts provides settings.hideAmounts
            ) {
                WalletTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = WColors.Page, contentColor = Color.White) {
                        var unlocked by remember { mutableStateOf(!settings.pinEnabled) }
                        if (!unlocked) {
                            LockScreen(settings) { unlocked = true }
                        } else {
                            WalletApp(repo, settings)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WalletApp(repo: WalletRepository, settings: AppSettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val accounts by repo.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by repo.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val records by repo.records.collectAsStateWithLifecycle(initialValue = emptyList())
    val templates by repo.templates.collectAsStateWithLifecycle(initialValue = emptyList())
    val notes by repo.notes.collectAsStateWithLifecycle(initialValue = emptyList())
    val cards by repo.cards.collectAsStateWithLifecycle(initialValue = emptyList())
    val rateRows by repo.rates.collectAsStateWithLifecycle(initialValue = emptyList())
    val expected by repo.expected.collectAsStateWithLifecycle(initialValue = emptyList())
    val savings by repo.savings.collectAsStateWithLifecycle(initialValue = emptyList())

    val base = settings.baseCurrency
    val fx = remember(accounts, rateRows, base) { Money(accounts, rateRows.associate { it.code to it.rate }, base) }

    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val current = stack.last()

    var rawSelected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var period by remember { mutableStateOf(Period.D30) }
    var showHelp by remember { mutableStateOf(false) }
    var othersOpen by remember { mutableStateOf(false) }

    // انتخاب معتبر: فقط حساب‌هایی که هنوز وجود دارند؛ اگر خالی بود یعنی همه
    val selectedIds: Set<Int> = remember(rawSelected, accounts) {
        val valid = rawSelected.filter { id -> accounts.any { it.id == id } }.toSet()
        if (valid.isEmpty()) accounts.map { it.id }.toSet() else valid
    }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun finishDraft(draft: RecordDraft) {
        val idx = stack.indexOfLast { it is Screen.AddRecord && it.draft === draft }
        if (idx > 0) {
            while (stack.size > idx) stack.removeAt(stack.lastIndex)
        } else {
            pop()
        }
    }

    // ── خروجی (PDF / XLSX / CSV) و ورودی CSV؛ با سیستم فایل اندروید، بدون نیاز به مجوز ──
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    fun writePending(uri: Uri?) {
        val data = pendingBytes
        pendingBytes = null
        if (uri == null || data == null) return
        try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
            toast("فایل ذخیره شد")
        } catch (e: Exception) {
            toast("ذخیره فایل ناموفق بود")
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { writePending(it) }
    val xlsxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { writePending(it) }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { writePending(it) }

    fun doExport(format: String, list: List<RecordEntity>) {
        if (list.isEmpty()) { toast("تراکنشی برای خروجی وجود ندارد"); return }
        val snapshotAccounts = accounts
        scope.launch {
            val bytes = withContext(Dispatchers.Default) {
                when (format) {
                    "pdf" -> buildPdfBytes("گزارش تراکنش‌ها", list, snapshotAccounts)
                    "xlsx" -> buildXlsxBytes(list, snapshotAccounts)
                    else -> buildCsvBytes(list, snapshotAccounts)
                }
            }
            pendingBytes = bytes
            when (format) {
                "pdf" -> pdfLauncher.launch("wallet-export.pdf")
                "xlsx" -> xlsxLauncher.launch("wallet-export.xlsx")
                else -> csvLauncher.launch("wallet-export.csv")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) } ?: ""
                    }
                    val r = importCsv(text, accounts, categories, base, repo)
                    toast("${r.imported} تراکنش وارد شد، ${r.skipped} ردیف نادیده گرفته شد، ${r.newAccounts} حساب جدید")
                } catch (e: Exception) {
                    toast("خواندن فایل ناموفق بود")
                }
            }
        }
    }

    fun startDraft(type: String, warranty: String = "None") {
        if (accounts.isEmpty()) { toast("ابتدا یک حساب بسازید"); return }
        // حساب مربوطه: صفحه‌ی جزئیات حساب، یا حساب انتخاب‌شده در صفحه‌ی اصلی/تراکنش‌ها
        val pref = (current as? Screen.AccountDetail)?.accountId
            ?: (current as? Screen.Records)?.accountIds?.singleOrNull()
            ?: selectedIds.firstOrNull()
        push(Screen.AddRecord(newDraft(type, accounts, categories, pref, warranty)))
    }
    fun startFromTemplate(t: TemplateEntity) {
        if (accounts.none { it.id == t.accountId }) { toast("حساب این قالب دیگر وجود ندارد"); return }
        push(Screen.AddRecord(draftFromTemplate(t, accounts)))
    }
    fun saveDraft(draft: RecordDraft) {
        val rec = draft.toEntity(accounts, fx)
        if (accounts.none { it.id == rec.accountId }) { toast("حساب را انتخاب کنید"); return }
        if (rec.amount <= 0L) { toast("مبلغ را وارد کنید"); return }
        if (rec.type == "TRANSFER" && (rec.targetAccountId == null || rec.targetAccountId == rec.accountId)) {
            toast("حساب مبدأ و مقصد باید متفاوت باشند"); return
        }
        scope.launch { repo.saveRecord(rec, draft.editing) }
        finishDraft(draft)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = current is Screen.Home,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF111111), modifier = Modifier.width(300.dp)) {
                Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
                    fun go(s: Screen?) {
                        scope.launch { drawerState.close() }
                        while (stack.size > 1) stack.removeAt(stack.lastIndex)
                        if (s != null) push(s)
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("مدیریت مالی شخصی", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    Hairline(Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(8.dp))
                    DrawerItem("خانه", Icons.Filled.Home, Color(0xFF5E97F6)) { go(null) }
                    DrawerItem("تراکنش‌ها", Icons.AutoMirrored.Filled.List, Color(0xFF26C6DA)) { go(Screen.Records(accounts.map { it.id }.toSet())) }
                    DrawerItem("حساب‌ها", Icons.Filled.AccountBalanceWallet, Color(0xFF66BB6A)) { go(Screen.Accounts) }
                    DrawerItem("دسته‌بندی‌ها", Icons.Filled.Category, Color(0xFFAB47BC)) { go(Screen.Categories) }
                    DrawerItem("قالب‌ها", Icons.Filled.ContentCopy, Color(0xFF90A4AE)) { go(Screen.Templates) }
                    DrawerItem("هزینه‌ها", Icons.Filled.PieChart, Color(0xFF26C6DA)) { go(Screen.Spending) }
                    DrawerItem("جریان نقدی", Icons.Filled.BarChart, Color(0xFF26C6DA)) { go(Screen.Cashflow) }
                    DrawerItem("هزینه‌های مورد انتظار", Icons.Filled.DateRange, Color(0xFFFF7043)) { go(Screen.ExpectedExpenses) }
                    DrawerItem("پس‌انداز", Icons.Filled.Savings, Color(0xFF00BFA5)) { go(Screen.Savings) }
                    Hairline(Modifier.padding(vertical = 6.dp))
                    DrawerItem("گارانتی‌ها", Icons.Filled.VerifiedUser, Color(0xFFFFA000)) { go(Screen.Warranties) }
                    DrawerItem("کارت‌های وفاداری", Icons.Filled.CardGiftcard, Color(0xFFF44336)) { go(Screen.Loyalty) }
                    DrawerItem("ارز و نرخ تبدیل", Icons.Filled.MonetizationOn, Color(0xFF5E97F6)) { go(Screen.CurrencyRates) }
                    DrawerItem("سایر", Icons.Filled.MoreHoriz, Color(0xFF90A4AE), trailing = {
                        Icon(if (othersOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = WColors.Muted)
                    }) { othersOpen = !othersOpen }
                    if (othersOpen) {
                        DrawerItem("ورود (CSV)", Icons.Filled.FileDownload, Color(0xFFB0BEC5), indent = true) {
                            scope.launch { drawerState.close() }
                            importLauncher.launch(arrayOf("*/*"))
                        }
                        DrawerItem("خروجی‌ها", Icons.Filled.FileUpload, Color(0xFFB0BEC5), indent = true) { go(Screen.Exports) }
                        DrawerItem("مکان‌ها", Icons.Filled.Place, Color(0xFFB0BEC5), indent = true) { go(Screen.Locations) }
                    }
                    Hairline(Modifier.padding(vertical = 6.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable { settings.updateHideAmounts(!settings.hideAmounts) }.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("پنهان کردن مبالغ", color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Switch(checked = settings.hideAmounts, onCheckedChange = { settings.updateHideAmounts(it) })
                    }
                    Hairline(Modifier.padding(vertical = 6.dp))
                    DrawerItem("امنیت", Icons.Filled.Lock, Color(0xFF2979FF)) { go(Screen.Security) }
                    DrawerItem("راهنما", Icons.Filled.Info, Color(0xFFFFA000)) {
                        scope.launch { drawerState.close() }
                        showHelp = true
                    }
                    DrawerItem("تنظیمات", Icons.Filled.Settings, Color(0xFF66BB6A)) { go(Screen.Settings) }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    ) {
        Box(Modifier.fillMaxSize().background(WColors.Page)) {
            when (val s = current) {
                is Screen.Home -> HomeScreen(
                    accounts = accounts, records = records, categories = categories, notes = notes,
                    fx = fx, selectedIds = selectedIds, period = period, hideAmounts = settings.hideAmounts,
                    onToggleHide = { settings.updateHideAmounts(!settings.hideAmounts) },
                    onPeriodChange = { period = it },
                    onSelectOnly = { rawSelected = setOf(it) },
                    onToggle = { id ->
                        val next = if (id in selectedIds) selectedIds - id else selectedIds + id
                        rawSelected = if (next.isEmpty()) setOf(id) else next
                    },
                    onSelectAll = { rawSelected = accounts.map { it.id }.toSet() },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenAccounts = { push(Screen.Accounts) },
                    onAccountDetail = {
                        val id = selectedIds.singleOrNull()
                        if (id == null) toast("برای دیدن جزئیات، فقط یک حساب را انتخاب کنید") else push(Screen.AccountDetail(id))
                    },
                    onRecords = { push(Screen.Records(selectedIds)) },
                    onNewRecord = { startDraft("EXPENSE") },
                    onSpending = { push(Screen.Spending) },
                    onExport = { push(Screen.Exports) },
                    onCashflow = { push(Screen.Cashflow) },
                    onTemplates = { push(Screen.Templates) },
                    onOpenNote = { push(Screen.NoteEdit(it.id, it.isList)) },
                    onNewNote = { isList -> push(Screen.NoteEdit(null, isList)) },
                    onCurrencyRates = { push(Screen.CurrencyRates) }
                )

                is Screen.AccountDetail -> {
                    val acc = accounts.firstOrNull { it.id == s.accountId }
                    if (acc == null) {
                        LaunchedEffect(Unit) { pop() }
                    } else {
                        AccountDetailScreen(
                            account = acc, records = records, accounts = accounts, categories = categories,
                            onBack = { pop() },
                            onEdit = { push(Screen.AccountEdit(acc.id)) },
                            onSetBalance = { v -> scope.launch { repo.setBalance(acc.id, v) } },
                            onOpenRecord = { push(Screen.AddRecord(draftFromRecord(it, accounts))) },
                            onAllRecords = { push(Screen.Records(setOf(acc.id))) },
                            onNewRecord = { startDraft("EXPENSE") }
                        )
                    }
                }

                is Screen.Records -> RecordsScreen(
                    fx = fx, initialQuery = s.query,
                    accountIds = s.accountIds, accounts = accounts, records = records, categories = categories,
                    onBack = { pop() },
                    onOpenRecord = { push(Screen.AddRecord(draftFromRecord(it, accounts))) },
                    onExport = { doExport("csv", it) },
                    onNewRecord = { startDraft("EXPENSE") }
                )

                is Screen.AddRecord -> AddRecordScreen(
                    draft = s.draft, accounts = accounts, templates = templates, fx = fx,
                    onClose = { pop() },
                    onSave = { saveDraft(s.draft) },
                    onDelete = {
                        s.draft.editing?.let { rec -> scope.launch { repo.deleteRecord(rec) } }
                        finishDraft(s.draft)
                    },
                    onOpenDetails = { push(Screen.RecordDetails(s.draft)) },
                    onPickAccount = { push(Screen.AccountPicker(s.draft, false)) },
                    onPickTarget = { push(Screen.AccountPicker(s.draft, true)) },
                    onPickCategory = { push(Screen.CategoryPicker(s.draft)) },
                    onApplyTemplate = { t ->
                        val dec = currencyInfo(accounts.firstOrNull { it.id == t.accountId }?.currency ?: "IRR").decimals
                        s.draft.type = t.type
                        s.draft.accountId = t.accountId
                        s.draft.targetAccountId = t.targetAccountId
                        s.draft.categoryName = t.categoryName
                        s.draft.expression = minorToEditable(t.amount, dec)
                        s.draft.note = t.note
                    }
                )

                is Screen.RecordDetails -> RecordDetailsScreen(
                    draft = s.draft, accounts = accounts,
                    onBack = { pop() },
                    onSave = { saveDraft(s.draft) },
                    onSaveTemplate = { name ->
                        val d = s.draft
                        scope.launch {
                            repo.saveTemplate(
                                TemplateEntity(
                                    name = name, type = d.type, amount = d.amountMinor(accounts), accountId = d.accountId,
                                    targetAccountId = d.targetAccountId, categoryName = d.categoryName, note = d.note.trim()
                                )
                            )
                        }
                        toast("قالب ذخیره شد")
                    }
                )

                is Screen.CategoryPicker -> CategoryPickerScreen(
                    categories = categories, records = records,
                    onBack = { pop() },
                    onSettings = { push(Screen.Categories) },
                    onPick = { c -> s.draft.categoryName = c.name; pop() }
                )

                is Screen.AccountPicker -> AccountsScreen(
                    title = "حساب", accounts = accounts, onBack = { pop() },
                    onPick = { acc ->
                        if (s.forTarget) {
                            s.draft.targetAccountId = acc.id
                            s.draft.targetOverride = null
                        } else {
                            s.draft.accountId = acc.id
                            if (s.draft.targetAccountId == acc.id) {
                                s.draft.targetAccountId = accounts.firstOrNull { it.id != acc.id }?.id
                            }
                            s.draft.targetOverride = null
                        }
                        pop()
                    },
                    onEdit = { push(Screen.AccountEdit(it.id)) },
                    onAdd = { push(Screen.AccountEdit(null)) }
                )

                is Screen.Categories -> CategoriesScreen(
                    categories = categories, onBack = { pop() },
                    onEdit = { push(Screen.CategoryEdit(it.id, null)) },
                    onAdd = { parent -> push(Screen.CategoryEdit(null, parent)) }
                )

                is Screen.CategoryEdit -> CategoryEditScreen(
                    existing = categories.firstOrNull { it.id == s.categoryId },
                    categories = categories, presetParentId = s.parentId,
                    onBack = { pop() },
                    onSave = { cat, oldName -> scope.launch { repo.saveCategory(cat, oldName) }; pop() },
                    onDelete = { list -> scope.launch { repo.deleteCategoryTree(list) }; pop() }
                )

                is Screen.Accounts -> AccountsScreen(
                    title = "حساب‌ها", accounts = accounts, onBack = { pop() },
                    onPick = null,
                    onEdit = { push(Screen.AccountEdit(it.id)) },
                    onAdd = { push(Screen.AccountEdit(null)) },
                    onReorder = { ids -> scope.launch { repo.reorderAccounts(ids) } }
                )

                is Screen.AccountEdit -> AccountEditScreen(
                    existing = accounts.firstOrNull { it.id == s.accountId },
                    hasRecords = records.any { it.accountId == s.accountId || it.targetAccountId == s.accountId },
                    defaultCurrency = base,
                    onBack = { pop() },
                    onSave = { acc -> scope.launch { repo.saveAccount(acc) }; pop() },
                    onDelete = { acc ->
                        scope.launch { repo.deleteAccount(acc) }
                        pop()
                        if (stack.lastOrNull().let { it is Screen.AccountDetail && it.accountId == acc.id }) pop()
                        rawSelected = rawSelected - acc.id
                    }
                )

                is Screen.Templates -> TemplatesScreen(
                    templates = templates, accounts = accounts, onBack = { pop() },
                    onUse = { startFromTemplate(it) },
                    onDelete = { t -> scope.launch { repo.deleteTemplate(t) } }
                )

                is Screen.Spending -> SpendingScreen(fx, selectedIds, records, categories) { pop() }
                is Screen.Cashflow -> CashflowScreen(fx, selectedIds, records) { pop() }

                is Screen.NoteEdit -> NoteEditScreen(
                    existing = notes.firstOrNull { it.id == s.noteId },
                    newIsList = s.isList,
                    onSave = { n -> scope.launch { repo.saveNote(n) }; pop() },
                    onDelete = { n -> scope.launch { repo.deleteNote(n) }; pop() },
                    onClose = { pop() }
                )

                is Screen.Settings -> SettingsScreen(
                    settings = settings, onBack = { pop() },
                    onSecurity = { push(Screen.Security) },
                    onRates = { push(Screen.CurrencyRates) },
                    onHelp = { showHelp = true }
                )
                is Screen.Security -> SecurityScreen(settings) { pop() }
                is Screen.CurrencyRates -> CurrencyRatesScreen(
                    base = base,
                    rates = rateRows.associate { it.code to it.rate },
                    usedCurrencies = accounts.map { it.currency }.toSet(),
                    onBack = { pop() },
                    onSetRate = { code, rate -> scope.launch { repo.saveRate(code, rate) } },
                    onDeleteRate = { code -> scope.launch { repo.deleteRate(code) } },
                    onChangeBase = { code ->
                        settings.updateBaseCurrency(code)
                        scope.launch { repo.clearRates() }
                    }
                )

                is Screen.Warranties -> WarrantiesScreen(
                    records = records, accounts = accounts, categories = categories,
                    onBack = { pop() },
                    onOpenRecord = { push(Screen.AddRecord(draftFromRecord(it, accounts))) },
                    onNew = { startDraft("EXPENSE", "1 year") }
                )

                is Screen.Loyalty -> LoyaltyScreen(
                    cards = cards, onBack = { pop() },
                    onOpen = { push(Screen.LoyaltyEdit(it.id)) },
                    onNew = { push(Screen.LoyaltyEdit(null)) }
                )
                is Screen.LoyaltyEdit -> LoyaltyEditScreen(
                    existing = cards.firstOrNull { it.id == s.cardId },
                    onBack = { pop() },
                    onSave = { c -> scope.launch { repo.saveCard(c) }; pop() },
                    onDelete = { c -> scope.launch { repo.deleteCard(c) }; pop() }
                )

                is Screen.Locations -> LocationsScreen(
                    records = records, onBack = { pop() },
                    onOpenPlace = { place -> push(Screen.Records(accounts.map { it.id }.toSet(), place)) }
                )

                is Screen.ExpectedExpenses -> ExpectedExpensesScreen(
                    items = expected, fx = fx, onBack = { pop() },
                    onOpen = { push(Screen.ExpectedEdit(it.id)) },
                    onTogglePaid = { e -> scope.launch { repo.saveExpected(e.copy(paid = !e.paid)) } },
                    onNew = { push(Screen.ExpectedEdit(null)) }
                )
                is Screen.ExpectedEdit -> ExpectedEditScreen(
                    existing = expected.firstOrNull { it.id == s.id }, defaultCurrency = base,
                    onBack = { pop() },
                    onSave = { e -> scope.launch { repo.saveExpected(e) }; pop() },
                    onDelete = { e -> scope.launch { repo.deleteExpected(e) }; pop() }
                )
                is Screen.Savings -> SavingsScreen(
                    items = savings, fx = fx, onBack = { pop() },
                    onOpen = { push(Screen.SavingEdit(it.id)) },
                    onNew = { push(Screen.SavingEdit(null)) }
                )
                is Screen.SavingEdit -> SavingEditScreen(
                    existing = savings.firstOrNull { it.id == s.id }, defaultCurrency = base,
                    onBack = { pop() },
                    onSave = { x -> scope.launch { repo.saveSaving(x) }; pop() },
                    onDelete = { x -> scope.launch { repo.deleteSaving(x) }; pop() }
                )

                is Screen.Exports -> ExportsScreen(
                    accounts = accounts, onBack = { pop() },
                    onExport = { format, filter -> doExport(format, applyExportFilter(records, filter)) }
                )
            }
        }
    }

    if (showHelp) HelpDialog { showHelp = false }
}

@Composable
fun DrawerItem(
    text: String,
    icon: ImageVector,
    tint: Color,
    indent: Boolean = false,
    trailing: @Composable () -> Unit = {},
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = if (indent) 40.dp else 20.dp, end = 20.dp, top = 13.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(18.dp))
        Text(text, color = if (indent) Color(0xFFBDBDBD) else Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        trailing()
    }
}
