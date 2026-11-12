package com.personal.wallet

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// ───────────────────────────── Entities ─────────────────────────────

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String = "Cash",
    val balance: Long,
    val colorHex: Long,
    val sortOrder: Int = 0,
    @ColumnInfo(defaultValue = "'IRR'") val currency: String = "IRR",
    @ColumnInfo(defaultValue = "'coins'") val iconKey: String = "coins"
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    @ColumnInfo(defaultValue = "4279060385") val colorHex: Long = 0xFF0D47A1,
    @ColumnInfo(defaultValue = "'category'") val iconKey: String = "category",
    val parentId: Int? = null
)

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Long,
    val type: String, // EXPENSE, INCOME, TRANSFER
    val accountId: Int,
    val targetAccountId: Int? = null,
    val categoryName: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val payee: String = "",
    @ColumnInfo(defaultValue = "''") val labels: String = "",
    @ColumnInfo(defaultValue = "'Cash'") val paymentType: String = "Cash",
    @ColumnInfo(defaultValue = "'Cleared'") val status: String = "Cleared",
    @ColumnInfo(defaultValue = "'None'") val warranty: String = "None",
    @ColumnInfo(defaultValue = "''") val place: String = "",
    /** مبلغ واریزی به حساب مقصد (در ارز مقصد)؛ ۰ یعنی برابر با «amount» */
    @ColumnInfo(defaultValue = "0") val targetAmount: Long = 0L
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String,
    val amount: Long,
    val accountId: Int,
    val targetAccountId: Int? = null,
    val categoryName: String,
    val note: String = ""
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    /** برای چک‌لیست: هر خط «۰|متن» یا «۱|متن» (۱ = انجام‌شده) */
    val body: String,
    val colorHex: Long,
    val pinned: Boolean,
    val isList: Boolean,
    val updatedAt: Long,
    // ── قالب‌بندی یادداشت (نسخه ۵) ──
    @ColumnInfo(defaultValue = "16") val fontSize: Int = 16,
    @ColumnInfo(defaultValue = "'default'") val fontFamily: String = "default",
    @ColumnInfo(defaultValue = "0") val bold: Boolean = false,
    @ColumnInfo(defaultValue = "0") val italic: Boolean = false,
    @ColumnInfo(defaultValue = "0") val underline: Boolean = false,
    @ColumnInfo(defaultValue = "0") val strike: Boolean = false,
    @ColumnInfo(defaultValue = "'start'") val align: String = "start",
    @ColumnInfo(defaultValue = "0") val textColor: Long = 0L
)

/** هزینه‌ی مورد انتظار: عنوان، مبلغ، تاریخ سررسید و وضعیت پرداخت */
@Entity(tableName = "expected_expenses")
data class ExpectedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Long,
    val currency: String,
    val dueDate: Long,
    val note: String,
    val paid: Boolean
)

/** یادداشت پس‌انداز: عنوان، مبلغ (اختیاری)، «نگهداری در» به‌صورت متن آزاد و توضیحات */
@Entity(tableName = "savings")
data class SavingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val holder: String,
    val amount: Long,
    val currency: String,
    val note: String,
    val updatedAt: Long
)

@Entity(tableName = "loyalty_cards")
data class LoyaltyCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val number: String,
    val note: String,
    val colorHex: Long
)

@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey val code: String,
    /** ۱ واحد این ارز = «rate» واحد ارز پایه */
    val rate: Double
)

// ───────────────────────────── DAO ─────────────────────────────

@Dao
interface WalletDao {
    // accounts
    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccount(id: Int): AccountEntity?

    @Query("UPDATE accounts SET sortOrder = :order WHERE id = :id")
    suspend fun setAccountOrder(id: Int, order: Int)

    @Query("SELECT MAX(sortOrder) FROM accounts")
    suspend fun maxAccountSort(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    // categories
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("UPDATE records SET categoryName = :newName WHERE categoryName = :oldName")
    suspend fun renameCategoryInRecords(oldName: String, newName: String)

    @Query("UPDATE templates SET categoryName = :newName WHERE categoryName = :oldName")
    suspend fun renameCategoryInTemplates(oldName: String, newName: String)

    // records
    @Query("SELECT * FROM records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE accountId = :accountId OR targetAccountId = :accountId")
    suspend fun getRecordsOfAccount(accountId: Int): List<RecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RecordEntity): Long

    @Update
    suspend fun updateRecord(record: RecordEntity)

    @Delete
    suspend fun deleteRecord(record: RecordEntity)

    @Query("DELETE FROM records WHERE accountId = :accountId OR targetAccountId = :accountId")
    suspend fun deleteRecordsOfAccount(accountId: Int)

    // templates
    @Query("SELECT * FROM templates ORDER BY id DESC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity): Long

    @Delete
    suspend fun deleteTemplate(template: TemplateEntity)

    // notes
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity): Long

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    // loyalty cards
    @Query("SELECT * FROM loyalty_cards ORDER BY id DESC")
    fun getAllCards(): Flow<List<LoyaltyCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCard(card: LoyaltyCardEntity): Long

    @Delete
    suspend fun deleteCard(card: LoyaltyCardEntity)

    // expected expenses
    @Query("SELECT * FROM expected_expenses ORDER BY paid ASC, dueDate ASC")
    fun getAllExpected(): Flow<List<ExpectedExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpected(e: ExpectedExpenseEntity): Long

    @Delete
    suspend fun deleteExpected(e: ExpectedExpenseEntity)

    // savings
    @Query("SELECT * FROM savings ORDER BY updatedAt DESC")
    fun getAllSavings(): Flow<List<SavingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSaving(s: SavingEntity): Long

    @Delete
    suspend fun deleteSaving(s: SavingEntity)

    // currency rates
    @Query("SELECT * FROM currency_rates")
    fun getAllRates(): Flow<List<CurrencyRateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRate(rate: CurrencyRateEntity)

    @Query("DELETE FROM currency_rates WHERE code = :code")
    suspend fun deleteRate(code: String)

    @Query("DELETE FROM currency_rates")
    suspend fun deleteAllRates()
}

// ───────────────────────────── Database ─────────────────────────────

val CategoryPalette = listOf(
    0xFF0D47A1, 0xFF29B6F6, 0xFFFFA726, 0xFF78909C, 0xFFAD1457, 0xFF5C6BC0, 0xFF00BFA5, 0xFFFF6F00,
    0xFFFFB300, 0xFF6A1B9A, 0xFFD32F2F, 0xFF388E3C, 0xFF00838F, 0xFF8D6E63, 0xFF7CB342, 0xFFE91E63,
    0xFF3949AB, 0xFF546E7A
)

val AccountPalette = listOf(
    0xFFD32F2F, 0xFFFFA000, 0xFFFBC02D, 0xFF388E3C, 0xFF00897B, 0xFF0C469E,
    0xFF1976D2, 0xFF5E35B1, 0xFF8E24AA, 0xFFC2185B, 0xFF6D4C41, 0xFF546E7A
)

/**
 * مهاجرت ۲ → ۳: بدون پاک شدن هیچ داده‌ای.
 * ستون‌های جدید به دسته‌بندی‌ها و تراکنش‌ها اضافه می‌شود و جدول قالب‌ها ساخته می‌شود.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN colorHex INTEGER NOT NULL DEFAULT 4279060385")
        db.execSQL("ALTER TABLE categories ADD COLUMN iconKey TEXT NOT NULL DEFAULT 'category'")
        db.execSQL("ALTER TABLE categories ADD COLUMN parentId INTEGER")

        db.execSQL("ALTER TABLE records ADD COLUMN payee TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE records ADD COLUMN labels TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE records ADD COLUMN paymentType TEXT NOT NULL DEFAULT 'Cash'")
        db.execSQL("ALTER TABLE records ADD COLUMN status TEXT NOT NULL DEFAULT 'Cleared'")
        db.execSQL("ALTER TABLE records ADD COLUMN warranty TEXT NOT NULL DEFAULT 'None'")
        db.execSQL("ALTER TABLE records ADD COLUMN place TEXT NOT NULL DEFAULT ''")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `templates` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `type` TEXT NOT NULL, `amount` INTEGER NOT NULL, " +
                "`accountId` INTEGER NOT NULL, `targetAccountId` INTEGER, " +
                "`categoryName` TEXT NOT NULL, `note` TEXT NOT NULL)"
        )

        // رنگ‌های متنوع برای دسته‌بندی‌های موجود
        val cases = CategoryPalette.take(8).mapIndexed { i, c -> "WHEN $i THEN $c" }.joinToString(" ")
        db.execSQL("UPDATE categories SET colorHex = CASE (id % 8) $cases ELSE 4279060385 END")

        // حدس آیکون بر اساس نام دسته‌بندی‌های قبلی
        db.execSQL("UPDATE categories SET iconKey = 'medical' WHERE name LIKE '%دندان%'")
        db.execSQL("UPDATE categories SET iconKey = 'book' WHERE name LIKE '%کتاب%'")
        db.execSQL("UPDATE categories SET iconKey = 'edit' WHERE name LIKE '%التحریر%'")
        db.execSQL("UPDATE categories SET iconKey = 'subway' WHERE name LIKE '%مترو%'")
        db.execSQL("UPDATE categories SET iconKey = 'wifi' WHERE name LIKE '%اینترنت%'")
        db.execSQL("UPDATE categories SET iconKey = 'payments' WHERE name LIKE '%حقوق%'")
        db.execSQL("UPDATE categories SET iconKey = 'home' WHERE name LIKE '%خونه%'")
        db.execSQL("UPDATE categories SET iconKey = 'atm' WHERE name LIKE '%نقد%'")
        db.execSQL("UPDATE categories SET iconKey = 'language' WHERE name LIKE '%زبان%'")
    }
}

/** مهاجرت ۳ → ۴: ارز و آیکون حساب‌ها، مبلغ مقصد انتقال، یادداشت‌ها، کارت‌های وفاداری و نرخ ارز */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN currency TEXT NOT NULL DEFAULT 'IRR'")
        db.execSQL("ALTER TABLE accounts ADD COLUMN iconKey TEXT NOT NULL DEFAULT 'coins'")
        db.execSQL("UPDATE accounts SET iconKey = 'bank' WHERE type = 'Bank account'")
        db.execSQL("UPDATE accounts SET iconKey = 'card' WHERE type = 'Credit card'")
        db.execSQL("UPDATE accounts SET iconKey = 'savings' WHERE type = 'Savings'")
        db.execSQL("UPDATE accounts SET iconKey = 'trending' WHERE type = 'Investment'")
        db.execSQL("ALTER TABLE records ADD COLUMN targetAmount INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, `body` TEXT NOT NULL, `colorHex` INTEGER NOT NULL, " +
                "`pinned` INTEGER NOT NULL, `isList` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `loyalty_cards` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `number` TEXT NOT NULL, `note` TEXT NOT NULL, `colorHex` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `currency_rates` (`code` TEXT NOT NULL, `rate` REAL NOT NULL, PRIMARY KEY(`code`))"
        )
    }
}

/** مهاجرت ۴ → ۵: قالب‌بندی یادداشت‌ها، هزینه‌های مورد انتظار و پس‌انداز. هیچ داده‌ای پاک نمی‌شود. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN fontSize INTEGER NOT NULL DEFAULT 16")
        db.execSQL("ALTER TABLE notes ADD COLUMN fontFamily TEXT NOT NULL DEFAULT 'default'")
        db.execSQL("ALTER TABLE notes ADD COLUMN bold INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE notes ADD COLUMN italic INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE notes ADD COLUMN underline INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE notes ADD COLUMN strike INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE notes ADD COLUMN align TEXT NOT NULL DEFAULT 'start'")
        db.execSQL("ALTER TABLE notes ADD COLUMN textColor INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `expected_expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, `amount` INTEGER NOT NULL, `currency` TEXT NOT NULL, " +
                "`dueDate` INTEGER NOT NULL, `note` TEXT NOT NULL, `paid` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `savings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, `holder` TEXT NOT NULL, `amount` INTEGER NOT NULL, " +
                "`currency` TEXT NOT NULL, `note` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL)"
        )
    }
}

@Database(
    entities = [
        AccountEntity::class, CategoryEntity::class, RecordEntity::class, TemplateEntity::class,
        NoteEntity::class, LoyaltyCardEntity::class, CurrencyRateEntity::class,
        ExpectedExpenseEntity::class, SavingEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wallet_secure_v2.db"
                )
                    // عمداً fallbackToDestructiveMigration حذف شده تا هیچ‌وقت داده‌های شما پاک نشود.
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(SeedCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    /** داده‌های اولیه همزمان با ساخت دیتابیس درج می‌شوند (وابسته به Activity یا coroutine نیست). */
    private class SeedCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            InitialPersonalData.defaultAccounts.forEach { a ->
                db.execSQL(
                    "INSERT INTO accounts (id, name, type, balance, colorHex, sortOrder) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf<Any?>(a.id, a.name, a.type, a.balance, a.colorHex, a.sortOrder)
                )
            }
            InitialPersonalData.defaultCategories.forEach { c ->
                db.execSQL(
                    "INSERT INTO categories (name, colorHex, iconKey) VALUES (?, ?, ?)",
                    arrayOf<Any?>(c.name, c.colorHex, c.iconKey)
                )
            }
        }
    }
}

object InitialPersonalData {
    val defaultAccounts = listOf(
        AccountEntity(1, "ملت", "Cash", 6378813L, 0xFFD32F2F, 1),
        AccountEntity(2, "ملی", "Cash", 438479410L, 0xFFFFA000, 2),
        AccountEntity(3, "تجارت", "Cash", 62093773L, 0xFF0C469E, 3)
    )

    val defaultCategories = listOf(
        CategoryEntity(name = "بهداشت دندان", colorHex = 0xFF0D47A1, iconKey = "medical"),
        CategoryEntity(name = "دندانپزشکی", colorHex = 0xFF29B6F6, iconKey = "medical"),
        CategoryEntity(name = "لوازم التحریر", colorHex = 0xFFFFA726, iconKey = "edit"),
        CategoryEntity(name = "کتاب شخصی", colorHex = 0xFF78909C, iconKey = "book"),
        CategoryEntity(name = "کتاب درسی ملیکا", colorHex = 0xFFAD1457, iconKey = "school"),
        CategoryEntity(name = "کتاب زبان", colorHex = 0xFF5C6BC0, iconKey = "language"),
        CategoryEntity(name = "خرج خونه", colorHex = 0xFF00BFA5, iconKey = "home"),
        CategoryEntity(name = "حقوق", colorHex = 0xFF388E3C, iconKey = "payments"),
        CategoryEntity(name = "کارت مترو", colorHex = 0xFF6A1B9A, iconKey = "subway"),
        CategoryEntity(name = "پول نقد", colorHex = 0xFFFF6F00, iconKey = "atm"),
        CategoryEntity(name = "بسته اینترنت", colorHex = 0xFF00838F, iconKey = "wifi"),
        CategoryEntity(name = "عمومی", colorHex = 0xFF78909C, iconKey = "category")
    )
}
