package com.personal.wallet

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

/** مبلغ واریزی به حساب مقصد در یک انتقال (اگر ارزها فرق کنند، «targetAmount» جدا ثبت می‌شود) */
fun effectiveTarget(rec: RecordEntity): Long = if (rec.targetAmount > 0L) rec.targetAmount else rec.amount

/**
 * تمام عملیات‌هایی که روی موجودی حساب‌ها اثر می‌گذارند اینجا و داخل یک تراکنش دیتابیس انجام می‌شوند
 * تا موجودی حساب‌ها و لیست تراکنش‌ها هیچ‌وقت ناسازگار نشوند.
 */
class WalletRepository(private val db: AppDatabase) {
    private val dao = db.walletDao()

    val accounts: Flow<List<AccountEntity>> = dao.getAllAccounts()
    val categories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val records: Flow<List<RecordEntity>> = dao.getAllRecords()
    val templates: Flow<List<TemplateEntity>> = dao.getAllTemplates()
    val notes: Flow<List<NoteEntity>> = dao.getAllNotes()
    val cards: Flow<List<LoyaltyCardEntity>> = dao.getAllCards()
    val rates: Flow<List<CurrencyRateEntity>> = dao.getAllRates()
    val expected: Flow<List<ExpectedExpenseEntity>> = dao.getAllExpected()
    val savings: Flow<List<SavingEntity>> = dao.getAllSavings()

    // ───── تراکنش‌ها ─────

    /** [old] != null یعنی ویرایش تراکنش: اثر قبلی برگردانده می‌شود و اثر جدید اعمال می‌شود. */
    suspend fun saveRecord(newRec: RecordEntity, old: RecordEntity?) {
        db.withTransaction {
            if (old != null) {
                applyEffect(old, -1)
                applyEffect(newRec, 1)
                dao.updateRecord(newRec)
            } else {
                applyEffect(newRec, 1)
                dao.insertRecord(newRec)
            }
            Unit
        }
    }

    suspend fun deleteRecord(rec: RecordEntity) {
        db.withTransaction {
            applyEffect(rec, -1)
            dao.deleteRecord(rec)
        }
    }

    private suspend fun applyEffect(rec: RecordEntity, direction: Int) {
        val amount = rec.amount * direction
        when (rec.type) {
            "EXPENSE" -> adjust(rec.accountId, -amount)
            "INCOME" -> adjust(rec.accountId, amount)
            "TRANSFER" -> {
                adjust(rec.accountId, -amount)
                val target = rec.targetAccountId
                if (target != null) adjust(target, effectiveTarget(rec) * direction)
            }
        }
    }

    private suspend fun adjust(accountId: Int, delta: Long) {
        val acc = dao.getAccount(accountId) ?: return
        dao.updateAccount(acc.copy(balance = acc.balance + delta))
    }

    // ───── حساب‌ها ─────

    /** شناسه حساب را برمی‌گرداند */
    suspend fun saveAccount(account: AccountEntity): Int {
        return if (account.id == 0) {
            val next = (dao.maxAccountSort() ?: 0) + 1
            dao.insertAccount(account.copy(sortOrder = next)).toInt()
        } else {
            dao.updateAccount(account)
            account.id
        }
    }

    /** ترتیب جدید نمایش حساب‌ها؛ فهرست شناسه‌ها به همان ترتیب دلخواه */
    suspend fun reorderAccounts(idsInOrder: List<Int>) {
        db.withTransaction {
            idsInOrder.forEachIndexed { index, id -> dao.setAccountOrder(id, index + 1) }
        }
    }

    suspend fun setBalance(accountId: Int, balance: Long) {
        val acc = dao.getAccount(accountId) ?: return
        dao.updateAccount(acc.copy(balance = balance))
    }

    /** حذف حساب همراه با تمام تراکنش‌هایش؛ اثر انتقال‌ها روی حساب مقابل هم برگردانده می‌شود. */
    suspend fun deleteAccount(account: AccountEntity) {
        db.withTransaction {
            val related = dao.getRecordsOfAccount(account.id)
            related.filter { it.type == "TRANSFER" }.forEach { applyEffect(it, -1) }
            dao.deleteRecordsOfAccount(account.id)
            dao.deleteAccount(account)
        }
    }

    // ───── دسته‌بندی‌ها (درختی) ─────

    suspend fun saveCategory(category: CategoryEntity, oldName: String?) {
        db.withTransaction {
            if (category.id == 0) {
                dao.insertCategory(category)
            } else {
                dao.updateCategory(category)
                if (oldName != null && oldName != category.name) {
                    dao.renameCategoryInRecords(oldName, category.name)
                    dao.renameCategoryInTemplates(oldName, category.name)
                }
            }
            Unit
        }
    }

    /** حذف دسته‌بندی همراه با تمام زیرشاخه‌هایش؛ تراکنش‌های قبلی با همان نام می‌مانند (با آیکون پیش‌فرض). */
    suspend fun deleteCategoryTree(toDelete: List<CategoryEntity>) {
        db.withTransaction {
            toDelete.forEach { dao.deleteCategory(it) }
        }
    }

    // ───── قالب‌ها ─────

    suspend fun saveTemplate(t: TemplateEntity) { dao.insertTemplate(t) }
    suspend fun deleteTemplate(t: TemplateEntity) { dao.deleteTemplate(t) }

    // ───── یادداشت‌ها، کارت‌ها، نرخ ارز ─────

    suspend fun saveNote(n: NoteEntity) { dao.upsertNote(n) }
    suspend fun deleteNote(n: NoteEntity) { dao.deleteNote(n) }
    suspend fun saveCard(c: LoyaltyCardEntity) { dao.upsertCard(c) }
    suspend fun deleteCard(c: LoyaltyCardEntity) { dao.deleteCard(c) }
    suspend fun saveExpected(e: ExpectedExpenseEntity) { dao.upsertExpected(e) }
    suspend fun deleteExpected(e: ExpectedExpenseEntity) { dao.deleteExpected(e) }
    suspend fun saveSaving(s: SavingEntity) { dao.upsertSaving(s) }
    suspend fun deleteSaving(s: SavingEntity) { dao.deleteSaving(s) }
    suspend fun saveRate(code: String, rate: Double) { dao.upsertRate(CurrencyRateEntity(code, rate)) }
    suspend fun deleteRate(code: String) { dao.deleteRate(code) }
    suspend fun clearRates() { dao.deleteAllRates() }
}
