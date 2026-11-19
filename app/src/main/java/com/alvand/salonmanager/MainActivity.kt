package com.alvand.salonmanager

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAB_HOME = "Home"
private const val TAB_SERVICES = "Services"
private const val TAB_EXPENSES = "Expenses"
private const val TAB_CUSTOMERS = "Customers"
private const val TAB_ABOUT = "About"

/*───────────────────────── localization ─────────────────────────*/

private class Strings(
    val english: Boolean,
    val tagline: String,
    val toggleTheme: String,
    val changeLanguage: String,
    val languageButton: String,
    val tabHome: String,
    val tabServices: String,
    val tabExpenses: String,
    val tabCustomers: String,
    val tabAbout: String,
    val add: String,
    val revenueReport: String,
    val today: String,
    val thisWeek: String,
    val thisMonth: String,
    val thisYear: String,
    val todayExpenses: String,
    val registerService: String,
    val registerExpense: String,
    val recentActivity: String,
    val emptyHome: String,
    val netIncome: String,
    val todayWithDate: String,
    val itemSingular: String,
    val itemPlural: String,
    val totalSuffix: String,
    val emptyItems: String,
    val serviceList: String,
    val serviceListSubtitle: String,
    val newService: String,
    val emptyServices: String,
    val relatedRecords: String,
    val edit: String,
    val delete: String,
    val amountToman: String,
    val amountInvalid: String,
    val jalaliDate: String,
    val dateInvalid: String,
    val linkService: String,
    val registerCustomer: String,
    val customerName: String,
    val expenseTitle: String,
    val serviceName: String,
    val phoneOptional: String,
    val noteOptional: String,
    val customerNameOptional: String,
    val save: String,
    val cancel: String,
    val editCustomer: String,
    val editExpense: String,
    val editService: String,
    val addNewService: String,
    val priceToman: String,
    val descriptionOptional: String,
    val labelService: String,
    val labelServiceRecord: String,
    val labelExpense: String,
    val labelCustomer: String,
    val labelItem: String,
    val deleteCatalogMessage: String,
    val deleteMessage: String,
    val deleteTitle: String,
    val backupSaved: String,
    val backupFailed: String,
    val saveFailed: String,
    val salonLogo: String,
    val salonManagement: String,
    val appName: String,
    val aboutTitle: String,
    val aboutText: String,
    val dataBackupTitle: String,
    val dataBackupText: String,
    val exportCsv: String,
    val developerTitle: String,
    val developerText: String,
    val versionLabel: String
) {
    fun fmt(template: String, vararg args: Any): String =
        String.format(Locale.US, template, *args)
}

private val PersianStrings = Strings(
    english = false,
    tagline = "مدیریت مالی سالن",
    toggleTheme = "تغییر حالت روشن/تاریک",
    changeLanguage = "تغییر زبان",
    languageButton = "English",
    tabHome = "خانه",
    tabServices = "خدمات",
    tabExpenses = "هزینه‌ها",
    tabCustomers = "مشتریان",
    tabAbout = "درباره",
    add = "افزودن",
    revenueReport = "گزارش درآمد",
    today = "امروز",
    thisWeek = "این هفته",
    thisMonth = "این ماه",
    thisYear = "امسال",
    todayExpenses = "هزینه‌های امروز",
    registerService = "ثبت خدمت",
    registerExpense = "ثبت هزینه",
    recentActivity = "فعالیت‌های اخیر",
    emptyHome = "هنوز اطلاعاتی ثبت نشده است؛ اولین خدمت را اضافه کنید.",
    netIncome = "درآمد خالص ثبت‌شده",
    todayWithDate = "امروز: %s",
    itemSingular = "%s مورد",
    itemPlural = "%s مورد",
    totalSuffix = "%s · مجموع %s",
    emptyItems = "موردی یافت نشد. برای افزودن، روی + بزنید.",
    serviceList = "فهرست خدمات",
    serviceListSubtitle = "نام و قیمت خدمات سالن",
    newService = "خدمت جدید",
    emptyServices = "هنوز خدمتی تعریف نشده است. برای افزودن، «خدمت جدید» را بزنید.",
    relatedRecords = "رکوردهای مرتبط: %s · درآمد: %s",
    edit = "ویرایش",
    delete = "حذف",
    amountToman = "مبلغ (تومان)",
    amountInvalid = "مبلغ باید بزرگ‌تر از صفر باشد",
    jalaliDate = "تاریخ شمسی",
    dateInvalid = "تاریخ کامل و درست وارد کنید",
    linkService = "اتصال به خدمت (اختیاری)",
    registerCustomer = "ثبت مشتری",
    customerName = "نام مشتری",
    expenseTitle = "عنوان هزینه",
    serviceName = "نام خدمت",
    phoneOptional = "تلفن (اختیاری)",
    noteOptional = "یادداشت (اختیاری)",
    customerNameOptional = "نام مشتری (اختیاری)",
    save = "ذخیره",
    cancel = "انصراف",
    editCustomer = "ویرایش مشتری",
    editExpense = "ویرایش هزینه",
    editService = "ویرایش خدمت",
    addNewService = "افزودن خدمت جدید",
    priceToman = "قیمت (تومان)",
    descriptionOptional = "توضیحات (اختیاری)",
    labelService = "خدمت",
    labelServiceRecord = "رکورد خدمت",
    labelExpense = "هزینه",
    labelCustomer = "مشتری",
    labelItem = "مورد",
    deleteCatalogMessage = "با حذف «%s» ارتباط رکوردهای مالی با این خدمت قطع می‌شود؛ خود رکوردها حذف نمی‌شوند.",
    deleteMessage = "آیا از حذف «%s» مطمئن هستید؟ این عمل قابل بازگشت نیست.",
    deleteTitle = "حذف %s؟",
    backupSaved = "فایل پشتیبان ذخیره شد",
    backupFailed = "ذخیره فایل ناموفق بود",
    saveFailed = "ذخیره اطلاعات ناموفق بود",
    salonLogo = "لوگوی سالن",
    salonManagement = "مدیریت سالن",
    appName = "مدیر سالن آلوَند",
    aboutTitle = "درباره",
    aboutText = "این برنامه برای ثبت خدمات، درآمد، هزینه‌ها و مشتریان یک سالن طراحی شده است. اطلاعات به‌صورت محلی ذخیره می‌شود و به اینترنت نیاز ندارد.",
    dataBackupTitle = "اطلاعات و پشتیبان‌گیری",
    dataBackupText = "می‌توانید از این صفحه فایل CSV داده‌های خود را خارج کنید. پیش از حذف برنامه یا تعویض گوشی، از این فایل پشتیبان بگیرید.",
    exportCsv = "خروجی پشتیبان CSV",
    developerTitle = "توسعه‌دهنده",
    developerText = "این برنامه توسط توسعه‌دهنده پروژه آلوَند و با کمک ابزارهای هوش مصنوعی ساخته شده است.",
    versionLabel = "نسخه %s (کد %s)"
)

private val EnglishStrings = Strings(
    english = true,
    tagline = "Salon Financial Management",
    toggleTheme = "Toggle theme",
    changeLanguage = "Change language",
    languageButton = "فارسی",
    tabHome = "Home",
    tabServices = "Services",
    tabExpenses = "Expenses",
    tabCustomers = "Customers",
    tabAbout = "About",
    add = "Add",
    revenueReport = "Revenue Report",
    today = "Today",
    thisWeek = "This Week",
    thisMonth = "This Month",
    thisYear = "This Year",
    todayExpenses = "Today's Expenses",
    registerService = "Register Service",
    registerExpense = "Register Expense",
    recentActivity = "Recent Activity",
    emptyHome = "No information registered yet; add the first service.",
    netIncome = "Net Income Registered",
    todayWithDate = "Today: %s",
    itemSingular = "%s item",
    itemPlural = "%s items",
    totalSuffix = "%s · Total %s",
    emptyItems = "No items found. Tap + to add a new item.",
    serviceList = "Service List",
    serviceListSubtitle = "Service names and prices for the salon",
    newService = "New Service",
    emptyServices = "No services defined yet. Tap 'New Service' to add one.",
    relatedRecords = "Related records: %s · Income: %s",
    edit = "Edit",
    delete = "Delete",
    amountToman = "Amount (Toman)",
    amountInvalid = "Amount must be greater than zero",
    jalaliDate = "Jalali Date",
    dateInvalid = "Enter a complete and correct date",
    linkService = "Link to service (optional)",
    registerCustomer = "Register Customer",
    customerName = "Customer Name",
    expenseTitle = "Expense Title",
    serviceName = "Service Name",
    phoneOptional = "Phone (optional)",
    noteOptional = "Note (optional)",
    customerNameOptional = "Customer Name (optional)",
    save = "Save",
    cancel = "Cancel",
    editCustomer = "Edit Customer",
    editExpense = "Edit Expense",
    editService = "Edit Service",
    addNewService = "Add New Service",
    priceToman = "Price (Toman)",
    descriptionOptional = "Description (optional)",
    labelService = "Service",
    labelServiceRecord = "Service Record",
    labelExpense = "Expense",
    labelCustomer = "Customer",
    labelItem = "Item",
    deleteCatalogMessage = "Deleting \"%s\" will unlink financial records from this service; the records themselves will not be deleted.",
    deleteMessage = "Are you sure you want to delete \"%s\"? This cannot be undone.",
    deleteTitle = "Delete %s?",
    backupSaved = "Backup file saved",
    backupFailed = "Failed to save file",
    saveFailed = "Save failed",
    salonLogo = "Salon Logo",
    salonManagement = "Salon Management",
    appName = "Alvand Salon Manager",
    aboutTitle = "About",
    aboutText = "This app is designed to register services, income, expenses, and customers for a salon. Information is stored locally and does not require internet.",
    dataBackupTitle = "Data and Backup",
    dataBackupText = "You can export a CSV file of your data from this page. Before deleting the app or switching phones, back up this file.",
    exportCsv = "Export CSV Backup",
    developerTitle = "Developer",
    developerText = "This app was developed by the Alvand project developer with the help of AI tools.",
    versionLabel = "Version %s (Code %s)"
)

private val LocalStrings = staticCompositionLocalOf { PersianStrings }

private const val PREFS_NAME = "alvand_salon_prefs"
private const val KEY_ENGLISH = "english_ui"

private fun loadEnglish(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_ENGLISH, false)

private fun saveEnglish(context: Context, english: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_ENGLISH, english).apply()
}

private val BrandGold = Color(0xFFE6AD45)
private val OnBrandGold = Color(0xFF2B1D00)
private val Mint = Color(0xFF55B987)
private val LightPrimary = Color(0xFF7A5200)

class MainActivity : ComponentActivity() {
    private lateinit var store: SalonStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = SalonStore(applicationContext)
        setContent { SalonApp(store) }
    }

    override fun onDestroy() {
        if (::store.isInitialized) store.close()
        super.onDestroy()
    }
}

/*───────────────────────── data models ─────────────────────────*/

data class Record(
    val id: Long,
    val kind: String,
    val title: String,
    val amount: Long,
    val date: String,
    val customer: String = "",
    val phone: String = "",
    val serviceId: Long? = null
)

data class SalonService(
    val id: Long,
    val name: String,
    val price: Long,
    val description: String
)

private data class DeleteTarget(
    val id: Long,
    val kind: String,
    val title: String
)

/*───────────────────────── SQLite store ─────────────────────────*/

class SalonStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "alvand_salon.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS records(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "kind TEXT NOT NULL," +
                "title TEXT NOT NULL," +
                "amount INTEGER NOT NULL," +
                "date TEXT NOT NULL," +
                "customer TEXT NOT NULL DEFAULT ''," +
                "phone TEXT NOT NULL DEFAULT ''," +
                "service_id INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS services(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "price INTEGER NOT NULL," +
                "description TEXT NOT NULL DEFAULT '')"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS services(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "price INTEGER NOT NULL," +
                    "description TEXT NOT NULL DEFAULT '')"
            )
        }
        if (oldVersion < 3) {
            if (!hasColumn(db, "records", "service_id")) {
                db.execSQL("ALTER TABLE records ADD COLUMN service_id INTEGER")
            }
            if (!hasColumn(db, "records", "customer")) {
                db.execSQL("ALTER TABLE records ADD COLUMN customer TEXT NOT NULL DEFAULT ''")
            }
            if (!hasColumn(db, "records", "phone")) {
                db.execSQL("ALTER TABLE records ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
            }
        }
    }

    private fun hasColumn(db: SQLiteDatabase, table: String, column: String): Boolean {
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex < 0) return false
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return true
            }
        }
        return false
    }

    fun services(): List<SalonService> {
        val result = mutableListOf<SalonService>()
        readableDatabase.rawQuery(
            "SELECT id,name,price,description FROM services ORDER BY name COLLATE NOCASE",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    SalonService(
                        id = cursor.getLong(0),
                        name = cursor.getString(1) ?: "",
                        price = cursor.getLong(2),
                        description = cursor.getString(3) ?: ""
                    )
                )
            }
        }
        return result
    }

    fun saveService(id: Long?, name: String, price: Long, description: String) {
        val values = ContentValues().apply {
            put("name", name)
            put("price", price)
            put("description", description)
        }
        if (id == null) {
            writableDatabase.insertOrThrow("services", null, values)
        } else {
            writableDatabase.update("services", values, "id=?", arrayOf(id.toString()))
        }
    }

    fun deleteService(id: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply { putNull("service_id") }
            db.update("records", values, "service_id=?", arrayOf(id.toString()))
            db.delete("services", "id=?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun add(
        kind: String,
        title: String,
        amount: Long,
        date: String,
        customer: String = "",
        phone: String = "",
        serviceId: Long? = null
    ) {
        val values = ContentValues().apply {
            put("kind", kind)
            put("title", title)
            put("amount", amount)
            put("date", date)
            put("customer", customer)
            put("phone", phone)
            if (serviceId == null) putNull("service_id") else put("service_id", serviceId)
        }
        writableDatabase.insertOrThrow("records", null, values)
    }

    fun updateRecord(
        id: Long,
        kind: String,
        title: String,
        amount: Long,
        date: String,
        customer: String,
        phone: String,
        serviceId: Long?
    ) {
        val values = ContentValues().apply {
            put("kind", kind)
            put("title", title)
            put("amount", amount)
            put("date", date)
            put("customer", customer)
            put("phone", phone)
            if (serviceId == null) putNull("service_id") else put("service_id", serviceId)
        }
        writableDatabase.update("records", values, "id=?", arrayOf(id.toString()))
    }

    fun all(): List<Record> {
        val result = mutableListOf<Record>()
        readableDatabase.rawQuery(
            "SELECT id,kind,title,amount,date,customer,phone,service_id " +
                "FROM records ORDER BY date DESC, id DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    Record(
                        id = cursor.getLong(0),
                        kind = cursor.getString(1) ?: "service",
                        title = cursor.getString(2) ?: "",
                        amount = cursor.getLong(3),
                        date = normalizeDate(cursor.getString(4) ?: ""),
                        customer = cursor.getString(5) ?: "",
                        phone = cursor.getString(6) ?: "",
                        serviceId = if (cursor.isNull(7)) null else cursor.getLong(7)
                    )
                )
            }
        }
        return result
    }

    fun delete(id: Long) {
        writableDatabase.delete("records", "id=?", arrayOf(id.toString()))
    }

    fun exportCsv(): String {
        val out = StringBuilder()
        out.append('\uFEFF')
        out.append("kind,title,amount,date,customer,phone,service_id\r\n")
        all().forEach { record ->
            out.append(csv(record.kind)).append(',')
                .append(csv(record.title)).append(',')
                .append(record.amount).append(',')
                .append(csv(record.date)).append(',')
                .append(csv(record.customer)).append(',')
                .append(csv(record.phone)).append(',')
                .append(record.serviceId?.toString() ?: "")
                .append("\r\n")
        }
        out.append("\r\nservice_name,price,description\r\n")
        services().forEach { service ->
            out.append(csv(service.name)).append(',')
                .append(service.price).append(',')
                .append(csv(service.description)).append("\r\n")
        }
        return out.toString()
    }

    private fun csv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}

/*───────────────────────── date, number and text helpers ─────────────────────────*/

private fun String.toAsciiDigits(): String {
    val source = this
    return buildString(source.length) {
        for (character in source) {
            append(
                when {
                    character in '\u06F0'..'\u06F9' -> ('0'.code + (character.code - '\u06F0'.code)).toChar()
                    character in '\u0660'..'\u0669' -> ('0'.code + (character.code - '\u0660'.code)).toChar()
                    else -> character
                }
            )
        }
    }
}

private fun persianDigits(value: String): String = buildString(value.length) {
    for (character in value) {
        append(
            if (character in '0'..'9') {
                ('\u06F0'.code + (character.code - '0'.code)).toChar()
            } else {
                character
            }
        )
    }
}

private fun normalizeDate(raw: String): String {
    val ascii = raw.toAsciiDigits().trim().replace('/', '-').replace('.', '-')
    val match = Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$").matchEntire(ascii) ?: return ascii
    val year = match.groupValues[1].toIntOrNull() ?: return ascii
    val month = match.groupValues[2].toIntOrNull() ?: return ascii
    val day = match.groupValues[3].toIntOrNull() ?: return ascii
    return "%04d-%02d-%02d".format(Locale.US, year, month, day)
}

private fun numberText(value: Long, english: Boolean): String =
    if (english) value.toString() else persianDigits(value.toString())

private fun money(value: Long, english: Boolean = false): String {
    val negative = value < 0
    val digits = value.toString().removePrefix("-")
    val prefix = if (negative) "-" else ""
    return if (english) {
        prefix + digits.reversed().chunked(3).joinToString(",").reversed() + " Toman"
    } else {
        val grouped = digits.reversed().chunked(3).joinToString("\u066C").reversed()
        prefix + persianDigits(grouped) + " \u062A\u0648\u0645\u0627\u0646"
    }
}

private fun showJalali(date: String, english: Boolean = false): String {
    val normalized = normalizeDate(date)
    val match = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").matchEntire(normalized) ?: return normalized
    val (year, month, day) = match.destructured
    return if (english) "$year-$month-$day"
    else "${persianDigits(year)}-${persianDigits(month)}-${persianDigits(day)}"
}

private fun jalaliToday(): String {
    val date = LocalDate.now()
    return gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)
}

private fun gregorianToJalali(year: Int, month: Int, day: Int): String {
    val date = LocalDate.of(year, month, day)
    val gy = date.year - 1600
    var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
    gDayNo += date.dayOfYear - 1
    var jDayNo = gDayNo - 79
    val jNp = jDayNo / 12053
    jDayNo %= 12053
    var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
    jDayNo %= 1461
    if (jDayNo >= 366) {
        jy += (jDayNo - 1) / 365
        jDayNo = (jDayNo - 1) % 365
    }
    val jm = if (jDayNo < 186) 1 + jDayNo / 31 else 7 + (jDayNo - 186) / 30
    val jd = if (jDayNo < 186) 1 + jDayNo % 31 else 1 + (jDayNo - 186) % 30
    return "%04d-%02d-%02d".format(Locale.US, jy, jm, jd)
}

private fun isValidJalaliDate(raw: String): Boolean {
    val normalized = normalizeDate(raw)
    val match = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").matchEntire(normalized) ?: return false
    val year = match.groupValues[1].toIntOrNull() ?: return false
    val month = match.groupValues[2].toIntOrNull() ?: return false
    val day = match.groupValues[3].toIntOrNull() ?: return false
    if (year !in 1200..1600 || month !in 1..12 || day < 1) return false
    val maxDay = when {
        month <= 6 -> 31
        month <= 11 -> 30
        isJalaliLeap(year) -> 30
        else -> 29
    }
    return day <= maxDay
}

private fun isJalaliLeap(year: Int): Boolean {
    return when (year % 33) {
        1, 5, 9, 13, 17, 22, 26, 30 -> true
        else -> false
    }
}

private fun jalaliMonth(date: String): String = normalizeDate(date).take(7)

private fun jalaliYear(date: String): String = normalizeDate(date).take(4)

private fun periodStart(period: String): String {
    val today = jalaliToday()
    return when (period) {
        "week" -> {
            val date = LocalDate.now()
            val daysSinceSaturday = when (date.dayOfWeek) {
                DayOfWeek.SATURDAY -> 0
                DayOfWeek.SUNDAY -> 1
                DayOfWeek.MONDAY -> 2
                DayOfWeek.TUESDAY -> 3
                DayOfWeek.WEDNESDAY -> 4
                DayOfWeek.THURSDAY -> 5
                DayOfWeek.FRIDAY -> 6
            }
            val start = date.minusDays(daysSinceSaturday.toLong())
            gregorianToJalali(start.year, start.monthValue, start.dayOfMonth)
        }
        "month" -> today.take(7) + "-01"
        "year" -> today.take(4) + "-01-01"
        else -> today
    }
}

private fun formatDateInput(raw: String): String {
    val digits = raw.toAsciiDigits().filter { it in '0'..'9' }.take(8)
    return buildString {
        append(digits.take(4))
        if (digits.length > 4) {
            append('-')
            append(digits.substring(4, minOf(6, digits.length)))
        }
        if (digits.length > 6) {
            append('-')
            append(digits.substring(6))
        }
    }
}

private fun parseAmount(raw: String): Long? {
    val digits = raw.toAsciiDigits().filter { it in '0'..'9' }
    if (digits.isEmpty()) return null
    return digits.toLongOrNull()
}

/*───────────────────────── app shell ─────────────────────────*/

@Composable
fun SalonApp(store: SalonStore) {
    val context = LocalContext.current
    var dark by rememberSaveable { mutableStateOf(true) }
    var english by rememberSaveable { mutableStateOf(loadEnglish(context)) }
    val strings = if (english) EnglishStrings else PersianStrings
    var tab by rememberSaveable { mutableStateOf(TAB_HOME) }
    var refresh by remember { mutableIntStateOf(0) }
    var records by remember { mutableStateOf(store.all()) }
    var services by remember { mutableStateOf(store.services()) }
    var showAdd by remember { mutableStateOf(false) }
    var addKind by remember { mutableStateOf("service") }
    var showServiceEditor by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<SalonService?>(null) }
    var editingRecord by remember { mutableStateOf<Record?>(null) }
    var deleteTarget by remember { mutableStateOf<DeleteTarget?>(null) }

    val scope = rememberCoroutineScope()

    fun mutate(block: () -> Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
            } catch (_: Exception) {
                Toast.makeText(context, strings.saveFailed, Toast.LENGTH_LONG).show()
            } finally {
                refresh++
            }
        }
    }

    LaunchedEffect(refresh) {
        if (refresh > 0) {
            records = withContext(Dispatchers.IO) { store.all() }
            services = withContext(Dispatchers.IO) { store.services() }
        }
    }

    val scheme = if (dark) {
        darkColorScheme(
            primary = BrandGold,
            onPrimary = OnBrandGold,
            primaryContainer = Color(0xFF5A4210),
            onPrimaryContainer = Color(0xFFFFE8B8),
            secondary = Color(0xFF7FD8AA),
            onSecondary = Color(0xFF002114),
            secondaryContainer = Color(0xFF1F4D38),
            onSecondaryContainer = Color(0xFFA8F0CC),
            background = Color(0xFF080B0E),
            onBackground = Color(0xFFF4F0E8),
            surface = Color(0xFF14191E),
            onSurface = Color(0xFFF4F0E8),
            surfaceVariant = Color(0xFF2A3440),
            onSurfaceVariant = Color(0xFFB4B4B4),
            surfaceContainer = Color(0xFF171D24),
            surfaceContainerHigh = Color(0xFF1E2630),
            surfaceContainerHighest = Color(0xFF2A3440),
            outline = Color(0xFF8A939C)
        )
    } else {
        lightColorScheme(
            primary = LightPrimary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFE8B8),
            onPrimaryContainer = Color(0xFF2B1D00),
            secondary = Color(0xFF2E7D5B),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFFE8B8),
            onSecondaryContainer = Color(0xFF2B1D00),
            background = Color(0xFFF5F6FC),
            onBackground = Color(0xFF1A1A1A),
            surface = Color.White,
            onSurface = Color(0xFF1A1A1A),
            surfaceVariant = Color(0xFFE3E3E8),
            onSurfaceVariant = Color(0xFF555555),
            surfaceContainer = Color(0xFFF0F1F5),
            surfaceContainerHigh = Color(0xFFECECEC),
            surfaceContainerHighest = Color(0xFFD9D9D9),
            outline = Color(0xFF74777F)
        )
    }

    val activity = context as? Activity
    SideEffect {
        if (activity != null) {
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (english) LayoutDirection.Ltr else LayoutDirection.Rtl,
            LocalStrings provides strings
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        listOf(
                            Triple(TAB_HOME, strings.tabHome, Icons.Default.Dashboard),
                            Triple(TAB_SERVICES, strings.tabServices, Icons.Default.ContentCut),
                            Triple(TAB_EXPENSES, strings.tabExpenses, Icons.Default.ReceiptLong),
                            Triple(TAB_CUSTOMERS, strings.tabCustomers, Icons.Default.People),
                            Triple(TAB_ABOUT, strings.tabAbout, Icons.Default.Info)
                        ).forEach { (tabName, label, icon) ->
                            NavigationBarItem(
                                selected = tab == tabName,
                                onClick = { tab = tabName },
                                icon = { Icon(icon, contentDescription = null) },
                                label = { Text(label) }
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (tab == TAB_EXPENSES || tab == TAB_CUSTOMERS) {
                        FloatingActionButton(
                            onClick = {
                                addKind = if (tab == TAB_EXPENSES) "expense" else "customer"
                                showAdd = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Add, contentDescription = strings.add)
                        }
                    }
                }
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 18.dp)
                ) {
                    Header(
                        dark = dark,
                        onToggleTheme = { dark = !dark },
                        onToggleLanguage = {
                            english = !english
                            saveEnglish(context, english)
                        }
                    )
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (tab) {
                            TAB_HOME -> HomeScreen(
                                records = records,
                                onAddService = {
                                    addKind = "service"
                                    showAdd = true
                                },
                                onAddExpense = {
                                    addKind = "expense"
                                    showAdd = true
                                },
                                onDelete = { record ->
                                    deleteTarget = DeleteTarget(record.id, record.kind, record.title)
                                },
                                onEdit = { editingRecord = it }
                            )
                            TAB_SERVICES -> ServiceCatalog(
                                services = services,
                                records = records,
                                onAdd = {
                                    editingService = null
                                    showServiceEditor = true
                                },
                                onEdit = {
                                    editingService = it
                                    showServiceEditor = true
                                },
                                onDelete = {
                                    deleteTarget = DeleteTarget(it.id, "catalog", it.name)
                                }
                            )
                            TAB_EXPENSES -> RecordsList(
                                title = strings.tabExpenses,
                                records = records.filter { it.kind == "expense" },
                                showTotal = true,
                                onDelete = { record ->
                                    deleteTarget = DeleteTarget(record.id, record.kind, record.title)
                                },
                                onEdit = { editingRecord = it }
                            )
                            TAB_CUSTOMERS -> RecordsList(
                                title = strings.tabCustomers,
                                records = records.filter { it.kind == "customer" },
                                showTotal = false,
                                onDelete = { record ->
                                    deleteTarget = DeleteTarget(record.id, record.kind, record.title)
                                },
                                onEdit = { editingRecord = it }
                            )
                            else -> AboutScreen(store)
                        }
                    }
                }
            }

            if (showAdd) {
                AddDialog(
                    kind = addKind,
                    services = services,
                    onDismiss = { showAdd = false },
                    onSave = { title, amount, date, customer, phone, serviceId ->
                        mutate {
                            store.add(addKind, title, amount, date, customer, phone, serviceId)
                        }
                        showAdd = false
                    }
                )
            }

            if (showServiceEditor) {
                ServiceEditorDialog(
                    service = editingService,
                    onDismiss = {
                        showServiceEditor = false
                        editingService = null
                    },
                    onSave = { id, name, price, description ->
                        mutate { store.saveService(id, name, price, description) }
                        showServiceEditor = false
                        editingService = null
                    }
                )
            }

            val recordBeingEdited = editingRecord
            if (recordBeingEdited != null) {
                RecordEditorDialog(
                    record = recordBeingEdited,
                    services = services,
                    onDismiss = { editingRecord = null },
                    onSave = { title, amount, date, customer, phone, serviceId ->
                        mutate {
                            store.updateRecord(
                                recordBeingEdited.id,
                                recordBeingEdited.kind,
                                title,
                                amount,
                                date,
                                customer,
                                phone,
                                serviceId
                            )
                        }
                        editingRecord = null
                    }
                )
            }

            val target = deleteTarget
            if (target != null) {
                ConfirmDeleteDialog(
                    target = target,
                    onDismiss = { deleteTarget = null },
                    onConfirm = {
                        deleteTarget = null
                        mutate {
                            if (target.kind == "catalog") {
                                store.deleteService(target.id)
                            } else {
                                store.delete(target.id)
                            }
                        }
                    }
                )
            }
        }
    }
}

/*───────────────────────── shared UI ─────────────────────────*/

@Composable
private fun Header(dark: Boolean, onToggleTheme: () -> Unit, onToggleLanguage: () -> Unit) {
    val strings = LocalStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "ALVAND",
                fontSize = 27.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                strings.tagline,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onToggleLanguage)
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Translate,
                    contentDescription = strings.changeLanguage,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    strings.languageButton,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        IconButton(onClick = onToggleTheme) {
            Icon(
                if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = strings.toggleTheme
            )
        }
    }
}

@Composable
private fun HomeScreen(
    records: List<Record>,
    onAddService: () -> Unit,
    onAddExpense: () -> Unit,
    onDelete: (Record) -> Unit,
    onEdit: (Record) -> Unit
) {
    val strings = LocalStrings.current
    val today = jalaliToday()
    val income = records.filter { it.kind == "service" }
    val expenses = records.filter { it.kind == "expense" }
    val daily = income.filter { it.date == today }.sumOf { it.amount }
    val dailyCost = expenses.filter { it.date == today }.sumOf { it.amount }
    val weekStart = periodStart("week")
    val week = income.filter { it.date >= weekStart && it.date <= today }.sumOf { it.amount }
    val month = income
        .filter { jalaliMonth(it.date) == jalaliMonth(today) && it.date <= today }
        .sumOf { it.amount }
    val year = income
        .filter { jalaliYear(it.date) == jalaliYear(today) && it.date <= today }
        .sumOf { it.amount }
    val net = income.sumOf { it.amount } - expenses.sumOf { it.amount }

    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { HeroCard(net = net, today = today) }
        item { Text(strings.revenueReport, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(strings.today, daily, Modifier.weight(1f), BrandGold)
                StatCard(strings.thisWeek, week, Modifier.weight(1f), Mint)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(strings.thisMonth, month, Modifier.weight(1f), Color(0xFFE59B38))
                StatCard(strings.thisYear, year, Modifier.weight(1f), Color(0xFFDB6D9A))
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.todayExpenses,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            money(dailyCost, strings.english),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAddService,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.registerService)
                }
                OutlinedButton(
                    onClick = onAddExpense,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.registerExpense)
                }
            }
        }
        item { Text(strings.recentActivity, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        if (records.isEmpty()) {
            item {
                EmptyState(strings.emptyHome)
            }
        } else {
            items(records.take(5), key = { it.id }) { record ->
                RecordCard(
                    record = record,
                    onEdit = { onEdit(record) },
                    onDelete = { onDelete(record) }
                )
            }
        }
    }
}

@Composable
private fun HeroCard(net: Long, today: String) {
    val strings = LocalStrings.current
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = BrandGold,
            contentColor = OnBrandGold
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp)) {
            Text(
                strings.netIncome,
                color = OnBrandGold.copy(alpha = 0.78f),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                money(net, strings.english),
                color = OnBrandGold,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = OnBrandGold.copy(alpha = 0.12f),
                    contentColor = OnBrandGold,
                    shape = CircleShape
                ) {
                    Text(
                        "  " + strings.fmt(strings.todayWithDate, showJalali(today, strings.english)) + "  ",
                        modifier = Modifier.padding(7.dp),
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = OnBrandGold,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, amount: Long, modifier: Modifier, accent: Color) {
    val strings = LocalStrings.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(accent, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                money(amount, strings.english),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecordsList(
    title: String,
    records: List<Record>,
    showTotal: Boolean,
    onDelete: (Record) -> Unit,
    onEdit: (Record) -> Unit
) {
    val strings = LocalStrings.current
    Column(Modifier.fillMaxSize()) {
        val countText = strings.fmt(
            if (records.size == 1) strings.itemSingular else strings.itemPlural,
            numberText(records.size.toLong(), strings.english)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(
                if (showTotal) {
                    strings.fmt(
                        strings.totalSuffix,
                        countText,
                        money(records.sumOf { it.amount }, strings.english)
                    )
                } else {
                    countText
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (records.isEmpty()) {
            EmptyState(strings.emptyItems)
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    RecordCard(
                        record = record,
                        onEdit = { onEdit(record) },
                        onDelete = { onDelete(record) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordCard(record: Record, onEdit: () -> Unit, onDelete: () -> Unit) {
    val strings = LocalStrings.current
    val isExpense = record.kind == "expense"
    val isCustomer = record.kind == "customer"
    val icon: androidx.compose.ui.graphics.vector.ImageVector
    val tint: Color
    val background: Color
    when {
        isExpense -> {
            icon = Icons.Default.ReceiptLong
            tint = MaterialTheme.colorScheme.error
            background = Color(0xFFFFE7D8)
        }
        isCustomer -> {
            icon = Icons.Default.Person
            tint = Color(0xFF8E24AA)
            background = Color(0xFFF3E5F5)
        }
        else -> {
            icon = Icons.Default.ContentCut
            tint = MaterialTheme.colorScheme.primary
            background = Color(0xFFE6E8FF)
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(43.dp)
                    .background(background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    record.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = when {
                    record.customer.isNotBlank() && record.phone.isNotBlank() ->
                        "${record.customer} · ${record.phone}"
                    record.customer.isNotBlank() -> record.customer
                    record.phone.isNotBlank() -> record.phone
                    else -> ""
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    showJalali(record.date, strings.english),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isCustomer) {
                Text(
                    money(record.amount, strings.english),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isExpense) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = strings.edit,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = strings.delete,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

/*───────────────────────── service catalog ─────────────────────────*/

@Composable
private fun ServiceCatalog(
    services: List<SalonService>,
    records: List<Record>,
    onAdd: () -> Unit,
    onEdit: (SalonService) -> Unit,
    onDelete: (SalonService) -> Unit
) {
    val strings = LocalStrings.current
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(strings.serviceList, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(
                    strings.serviceListSubtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onAdd, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(strings.newService)
            }
        }
        if (services.isEmpty()) {
            EmptyState(strings.emptyServices)
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(services, key = { it.id }) { service ->
                    val related = records.filter { it.serviceId == service.id }
                    val relatedIncome = related
                        .filter { it.kind == "service" }
                        .sumOf { it.amount }
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFE6E8FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ContentCut,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    service.name,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (service.description.isNotBlank()) {
                                    Text(
                                        service.description,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    money(service.price, strings.english),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (related.isNotEmpty()) {
                                    Text(
                                        strings.fmt(
                                            strings.relatedRecords,
                                            numberText(related.size.toLong(), strings.english),
                                            money(relatedIncome, strings.english)
                                        ),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onEdit(service) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = strings.edit,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onDelete(service) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = strings.delete,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/*───────────────────────── form fields ─────────────────────────*/

@Composable
private fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null
) {
    val strings = LocalStrings.current
    val parsed = parseAmount(value)
    val invalid = value.isNotBlank() && (parsed == null || parsed <= 0)
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            onValueChange(
                input.filter { character ->
                    character.isDigit() || character in '\u06F0'..'\u06F9' || character in '\u0660'..'\u0669'
                }.take(15)
            )
        },
        label = { Text(label ?: strings.amountToman) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        isError = invalid,
        supportingText = if (invalid) {
            { Text(strings.amountInvalid) }
        } else {
            null
        }
    )
}

@Composable
private fun DateField(value: String, onValueChange: (String) -> Unit) {
    val strings = LocalStrings.current
    val invalid = value.isNotBlank() && !isValidJalaliDate(value)
    Column {
        OutlinedTextField(
            value = if (strings.english) value else persianDigits(value),
            onValueChange = { input -> onValueChange(formatDateInput(input)) },
            label = { Text(strings.jalaliDate) },
            placeholder = {
                Text(if (strings.english) "1405-08-22" else persianDigits("1405-08-22"))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = invalid,
            supportingText = if (invalid) {
                { Text(strings.dateInvalid) }
            } else {
                null
            }
        )
        TextButton(onClick = { onValueChange(jalaliToday()) }) {
            Text(strings.today)
        }
    }
}

@Composable
private fun ServiceChips(
    services: List<SalonService>,
    selected: Long?,
    onSelect: (SalonService) -> Unit
) {
    if (services.isEmpty()) return
    val strings = LocalStrings.current
    Text(
        strings.linkService,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        services.forEach { service ->
            FilterChip(
                selected = selected == service.id,
                onClick = { onSelect(service) },
                label = { Text(service.name) }
            )
        }
    }
}

/*───────────────────────── add dialog ─────────────────────────*/

@Composable
private fun AddDialog(
    kind: String,
    services: List<SalonService>,
    onDismiss: () -> Unit,
    onSave: (String, Long, String, String, String, Long?) -> Unit
) {
    val strings = LocalStrings.current
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(jalaliToday()) }
    var customer by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf<Long?>(null) }
    val customerOnly = kind == "customer"
    val expense = kind == "expense"
    val parsedAmount = parseAmount(amount)
    val validAmount = !customerOnly && parsedAmount != null && parsedAmount > 0
    val canSave = title.isNotBlank() && isValidJalaliDate(date) && (customerOnly || validAmount)
    val heading = when {
        customerOnly -> strings.registerCustomer
        expense -> strings.registerExpense
        else -> strings.registerService
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(heading, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = {
                        Text(
                            if (customerOnly) {
                                strings.customerName
                            } else if (expense) {
                                strings.expenseTitle
                            } else {
                                strings.serviceName
                            }
                        )
                    },
                    singleLine = true
                )
                if (!customerOnly) {
                    AmountField(value = amount, onValueChange = { amount = it })
                }
                DateField(value = date, onValueChange = { date = it })
                if (customerOnly || !expense) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { input ->
                            phone = input.filter {
                                it.isDigit() || it == '+' || it == '-' || it == ' '
                            }.take(20)
                        },
                        label = { Text(strings.phoneOptional) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customer,
                        onValueChange = { customer = it },
                        label = {
                            Text(
                                if (customerOnly) strings.noteOptional
                                else strings.customerNameOptional
                            )
                        },
                        singleLine = !customerOnly,
                        minLines = if (customerOnly) 2 else 1
                    )
                }
                if (!customerOnly && !expense) {
                    ServiceChips(
                        services = services,
                        selected = selectedService,
                        onSelect = { service ->
                            if (selectedService == service.id) {
                                selectedService = null
                            } else {
                                selectedService = service.id
                                if (title.isBlank()) title = service.name
                                if (amount.isBlank()) amount = service.price.toString()
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title.trim(),
                        if (customerOnly) 0L else parsedAmount!!,
                        normalizeDate(date),
                        customer.trim(),
                        phone.toAsciiDigits().trim(),
                        if (customerOnly || expense) null else selectedService
                    )
                },
                enabled = canSave
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

/*───────────────────────── record editor ─────────────────────────*/

@Composable
private fun RecordEditorDialog(
    record: Record,
    services: List<SalonService>,
    onDismiss: () -> Unit,
    onSave: (String, Long, String, String, String, Long?) -> Unit
) {
    val strings = LocalStrings.current
    var title by remember(record.id) { mutableStateOf(record.title) }
    var amount by remember(record.id) { mutableStateOf(record.amount.toString()) }
    var date by remember(record.id) { mutableStateOf(normalizeDate(record.date)) }
    var customer by remember(record.id) { mutableStateOf(record.customer) }
    var phone by remember(record.id) { mutableStateOf(record.phone) }
    var selectedService by remember(record.id) { mutableStateOf(record.serviceId) }
    val customerOnly = record.kind == "customer"
    val expense = record.kind == "expense"
    val parsedAmount = parseAmount(amount)
    val validAmount = !customerOnly && parsedAmount != null && parsedAmount > 0
    val canSave = title.isNotBlank() && isValidJalaliDate(date) && (customerOnly || validAmount)
    val heading = when {
        customerOnly -> strings.editCustomer
        expense -> strings.editExpense
        else -> strings.editService
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(heading, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = {
                        Text(
                            if (customerOnly) {
                                strings.customerName
                            } else if (expense) {
                                strings.expenseTitle
                            } else {
                                strings.serviceName
                            }
                        )
                    },
                    singleLine = true
                )
                if (!customerOnly) {
                    AmountField(value = amount, onValueChange = { amount = it })
                }
                DateField(value = date, onValueChange = { date = it })
                if (customerOnly || !expense) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { input ->
                            phone = input.filter {
                                it.isDigit() || it == '+' || it == '-' || it == ' '
                            }.take(20)
                        },
                        label = { Text(strings.phoneOptional) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customer,
                        onValueChange = { customer = it },
                        label = {
                            Text(
                                if (customerOnly) strings.noteOptional
                                else strings.customerNameOptional
                            )
                        },
                        singleLine = !customerOnly,
                        minLines = if (customerOnly) 2 else 1
                    )
                }
                if (!customerOnly && !expense) {
                    ServiceChips(
                        services = services,
                        selected = selectedService,
                        onSelect = { service ->
                            selectedService =
                                if (selectedService == service.id) null else service.id
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title.trim(),
                        if (customerOnly) 0L else parsedAmount!!,
                        normalizeDate(date),
                        customer.trim(),
                        phone.toAsciiDigits().trim(),
                        if (customerOnly || expense) null else selectedService
                    )
                },
                enabled = canSave
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

/*───────────────────────── service editor ─────────────────────────*/

@Composable
private fun ServiceEditorDialog(
    service: SalonService?,
    onDismiss: () -> Unit,
    onSave: (Long?, String, Long, String) -> Unit
) {
    val strings = LocalStrings.current
    var name by remember(service?.id) { mutableStateOf(service?.name ?: "") }
    var price by remember(service?.id) { mutableStateOf(service?.price?.toString() ?: "") }
    var description by remember(service?.id) {
        mutableStateOf(service?.description ?: "")
    }
    val parsedPrice = parseAmount(price)
    val canSave = name.isNotBlank() && parsedPrice != null && parsedPrice > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (service == null) strings.addNewService else strings.editService,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.serviceName) },
                    singleLine = true
                )
                AmountField(
                    value = price,
                    onValueChange = { price = it },
                    label = strings.priceToman
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.descriptionOptional) },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(service?.id, name.trim(), parsedPrice!!, description.trim())
                },
                enabled = canSave
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

/*───────────────────────── delete confirmation ─────────────────────────*/

@Composable
private fun ConfirmDeleteDialog(
    target: DeleteTarget,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val strings = LocalStrings.current
    val label = when (target.kind) {
        "catalog" -> strings.labelService
        "service" -> strings.labelServiceRecord
        "expense" -> strings.labelExpense
        "customer" -> strings.labelCustomer
        else -> strings.labelItem
    }
    val message = if (target.kind == "catalog") {
        strings.fmt(strings.deleteCatalogMessage, target.title)
    } else {
        strings.fmt(strings.deleteMessage, target.title)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.fmt(strings.deleteTitle, label), fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(strings.delete)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

/*───────────────────────── about screen ─────────────────────────*/

@Composable
private fun AboutScreen(store: SalonStore) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(store.exportCsv().toByteArray(Charsets.UTF_8))
                            true
                        } ?: false
                    } catch (_: Exception) {
                        false
                    }
                }
                Toast.makeText(
                    context,
                    if (ok) strings.backupSaved else strings.backupFailed,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.alvand_salon_logo),
            contentDescription = strings.salonLogo,
            modifier = Modifier.size(104.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(strings.salonManagement, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(strings.appName, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        AboutCard(title = strings.aboutTitle) {
            Text(strings.aboutText)
        }
        Spacer(Modifier.height(12.dp))
        AboutCard(title = strings.dataBackupTitle) {
            Text(strings.dataBackupText)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    exportLauncher.launch("AlvandSalonManager-backup-${jalaliToday()}.csv")
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.exportCsv)
            }
        }
        Spacer(Modifier.height(12.dp))
        AboutCard(title = strings.developerTitle) {
            Text(strings.developerText)
            Spacer(Modifier.height(10.dp))
            Text(
                "https://github.com/Alvandcode/",
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Alvandcode/"))
                        )
                    } catch (_: Exception) {
                        // No browser installed.
                    }
                }
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            strings.fmt(
                strings.versionLabel,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE.toString()
            ),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AboutCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
