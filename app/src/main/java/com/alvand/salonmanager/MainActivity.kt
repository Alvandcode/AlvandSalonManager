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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private fun money(value: Long): String {
    val negative = value < 0
    val digits = value.toString().removePrefix("-")
    val grouped = digits.reversed().chunked(3).joinToString("\u066B").reversed()
    val prefix = if (negative) "-" else ""
    return prefix + persianDigits(grouped) + " \u062A\u0648\u0645\u0627\u0646"
}

private fun showJalali(date: String): String {
    val normalized = normalizeDate(date)
    val match = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").matchEntire(normalized) ?: return normalized
    return "${persianDigits(match.groupValues[1])}-" +
        "${persianDigits(match.groupValues[2])}-" +
        persianDigits(match.groupValues[3])
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
    var dark by rememberSaveable { mutableStateOf(true) }
    var tab by rememberSaveable { mutableStateOf(TAB_HOME) }
    var refresh by remember { mutableIntStateOf(0) }
    var records by remember { mutableStateOf(store.all()) }
    var services by remember { mutableStateOf(store.services()) }
    var showAdd by remember { mutableStateOf(false) }
    var addKind by remember { mutableStateOf("service") }
    var showServiceIcons.Default.Editor by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<SalonService?>(null) }
    var editingRecord by remember { mutableStateOf<Record?>(null) }
    var deleteTarget by remember { mutableStateOf<DeleteTarget?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun mutate(block: () -> Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
            } catch (_: Exception) {
                Toast.makeText(context, "Save failed", Toast.LENGTH_LONG).show()
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
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        listOf(
                            Triple(TAB_HOME, "Home", Icons.Default.Dashboard),
                            Triple(TAB_SERVICES, "Services", Icons.Default.ContentCut),
                            Triple(TAB_EXPENSES, "Expenses", Icons.Default.ReceiptLong),
                            Triple(TAB_CUSTOMERS, "Customers", Icons.Default.Person),
                            Triple(TAB_ABOUT, "About", Icons.Default.Info)
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
                            Icon(Icons.Default.Add, contentDescription = "Add")
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
                    Header(dark = dark, onToggleTheme = { dark = !dark })
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
                                onIcons.Default.Edit = { editingRecord = it }
                            )
                            TAB_SERVICES -> ServiceCatalog(
                                services = services,
                                records = records,
                                onAdd = {
                                    editingService = null
                                    showServiceIcons.Default.Editor = true
                                },
                                onIcons.Default.Edit = {
                                    editingService = it
                                    showServiceIcons.Default.Editor = true
                                },
                                onDelete = {
                                    deleteTarget = DeleteTarget(it.id, "catalog", it.name)
                                }
                            )
                            TAB_EXPENSES -> RecordsList(
                                title = "Expenses",
                                records = records.filter { it.kind == "expense" },
                                showTotal = true,
                                onDelete = { record ->
                                    deleteTarget = DeleteTarget(record.id, record.kind, record.title)
                                },
                                onIcons.Default.Edit = { editingRecord = it }
                            )
                            TAB_CUSTOMERS -> RecordsList(
                                title = "Customers",
                                records = records.filter { it.kind == "customer" },
                                showTotal = false,
                                onDelete = { record ->
                                    deleteTarget = DeleteTarget(record.id, record.kind, record.title)
                                },
                                onIcons.Default.Edit = { editingRecord = it }
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

            if (showServiceIcons.Default.Editor) {
                ServiceIcons.Default.EditorDialog(
                    service = editingService,
                    onDismiss = {
                        showServiceIcons.Default.Editor = false
                        editingService = null
                    },
                    onSave = { id, name, price, description ->
                        mutate { store.saveService(id, name, price, description) }
                        showServiceIcons.Default.Editor = false
                        editingService = null
                    }
                )
            }

            val recordBeingIcons.Default.Edited = editingRecord
            if (recordBeingIcons.Default.Edited != null) {
                RecordIcons.Default.EditorDialog(
                    record = recordBeingIcons.Default.Edited,
                    services = services,
                    onDismiss = { editingRecord = null },
                    onSave = { title, amount, date, customer, phone, serviceId ->
                        mutate {
                            store.updateRecord(
                                recordBeingIcons.Default.Edited.id,
                                recordBeingIcons.Default.Edited.kind,
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
private fun Header(dark: Boolean, onToggleTheme: () -> Unit) {
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
                "Salon Financial Management",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onToggleTheme) {
            Icon(
                if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle theme"
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
    onIcons.Default.Edit: (Record) -> Unit
) {
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
        item { Text("Revenue Report", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Today", daily, Modifier.weight(1f), BrandGold)
                StatCard("This Week", week, Modifier.weight(1f), Mint)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("This Month", month, Modifier.weight(1f), Color(0xFFE59B38))
                StatCard("This Year", year, Modifier.weight(1f), Color(0xFFDB6D9A))
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
                            "Today's Expenses",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(money(dailyCost), fontSize = 19.sp, fontWeight = FontWeight.Bold)
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
                    Text("Register Service")
                }
                OutlinedButton(
                    onClick = onAddExpense,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Register Expense")
                }
            }
        }
        item { Text("Recent Activity", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        if (records.isEmpty()) {
            item {
                EmptyState("No information registered yet; add the first service.")
            }
        } else {
            items(records.take(5), key = { it.id }) { record ->
                RecordCard(
                    record = record,
                    onIcons.Default.Edit = { onIcons.Default.Edit(record) },
                    onDelete = { onDelete(record) }
                )
            }
        }
    }
}

@Composable
private fun HeroCard(net: Long, today: String) {
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
                "Net Income Registered",
                color = OnBrandGold.copy(alpha = 0.78f),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                money(net),
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
                        "  Today: ${showJalali(today)}  ",
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
                money(amount),
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
    onIcons.Default.Edit: (Record) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(
                if (showTotal) {
                    "${records.size} items · Total ${money(records.sumOf { it.amount })}"
                } else {
                    "${records.size} items"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (records.isEmpty()) {
            EmptyState("No items found. Tap + to add a new item.")
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    RecordCard(
                        record = record,
                        onIcons.Default.Edit = { onIcons.Default.Edit(record) },
                        onDelete = { onDelete(record) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordCard(record: Record, onIcons.Default.Edit: () -> Unit, onDelete: () -> Unit) {
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
                    showJalali(record.date),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isCustomer) {
                Text(
                    money(record.amount),
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
            IconButton(onClick = onIcons.Default.Edit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Icons.Default.Edit",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
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
    onIcons.Default.Edit: (SalonService) -> Unit,
    onDelete: (SalonService) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Service List", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Service names and prices for the salon",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onAdd, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("New Service")
            }
        }
        if (services.isEmpty()) {
            EmptyState("No services defined yet. Tap 'New Service' to add one.")
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
                                    money(service.price),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (related.isNotEmpty()) {
                                    Text(
                                        "Related records: ${related.size} · Income: ${money(relatedIncome)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onIcons.Default.Edit(service) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Icons.Default.Edit",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onDelete(service) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
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
    label: String = "Amount (Toman)"
) {
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
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        isError = invalid,
        supportingText = if (invalid) {
            { Text("Amount must be greater than zero") }
        } else {
            null
        }
    )
}

@Composable
private fun DateField(value: String, onValueChange: (String) -> Unit) {
    val invalid = value.isNotBlank() && !isValidJalaliDate(value)
    Column {
        OutlinedTextField(
            value = persianDigits(value),
            onValueChange = { input -> onValueChange(formatDateInput(input)) },
            label = { Text("Jalali Date") },
            placeholder = { Text("1405-08-22") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = invalid,
            supportingText = if (invalid) {
                { Text("Enter a complete and correct date") }
            } else {
                null
            }
        )
        TextButton(onClick = { onValueChange(jalaliToday()) }) {
            Text("Today")
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
    Text(
        "Link to service (optional)",
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
        customerOnly -> "Register Customer"
        expense -> "Register Expense"
        else -> "Register Service"
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
                                "Customer Name"
                            } else if (expense) {
                                "Expense Title"
                            } else {
                                "Service Name"
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
                        label = { Text("Phone (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customer,
                        onValueChange = { customer = it },
                        label = {
                            Text(if (customerOnly) "Note (optional)" else "Customer Name (optional)")
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/*───────────────────────── record editor ─────────────────────────*/

@Composable
private fun RecordIcons.Default.EditorDialog(
    record: Record,
    services: List<SalonService>,
    onDismiss: () -> Unit,
    onSave: (String, Long, String, String, String, Long?) -> Unit
) {
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
        customerOnly -> "Icons.Default.Edit Customer"
        expense -> "Icons.Default.Edit Expense"
        else -> "Icons.Default.Edit Service"
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
                                "Customer Name"
                            } else if (expense) {
                                "Expense Title"
                            } else {
                                "Service Name"
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
                        label = { Text("Phone (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customer,
                        onValueChange = { customer = it },
                        label = {
                            Text(if (customerOnly) "Note (optional)" else "Customer Name (optional)")
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/*───────────────────────── service editor ─────────────────────────*/

@Composable
private fun ServiceIcons.Default.EditorDialog(
    service: SalonService?,
    onDismiss: () -> Unit,
    onSave: (Long?, String, Long, String) -> Unit
) {
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
                if (service == null) "Add New Service" else "Icons.Default.Edit Service",
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
                    label = { Text("Service Name") },
                    singleLine = true
                )
                AmountField(
                    value = price,
                    onValueChange = { price = it },
                    label = "Price (Toman)"
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
    val label = when (target.kind) {
        "catalog" -> "Service"
        "service" -> "Service Record"
        "expense" -> "Expense"
        "customer" -> "Customer"
        else -> "Item"
    }
    val message = if (target.kind == "catalog") {
        "Deleting \"${target.title}\" will unlink financial records from this service; the records themselves will not be deleted."
    } else {
        "Are you sure you want to delete \"${target.title}\"? This cannot be undone."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete $label?", fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/*───────────────────────── about screen ─────────────────────────*/

@Composable
private fun AboutScreen(store: SalonStore) {
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
                    if (ok) "Backup file saved" else "Failed to save file",
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
        Image(
            painter = painterResource(id = R.drawable.alvand_salon_logo),
            contentDescription = "Salon Logo",
            modifier = Modifier.size(104.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text("Salon Management", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Alvand Salon Manager", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        AboutCard(title = "About") {
            Text(
                "This app is designed to register services, income, expenses, and customers for a salon. " +
                    "Icons.Default.Information is stored locally and does not require internet."
            )
        }
        Spacer(Modifier.height(12.dp))
        AboutCard(title = "Data and Backup") {
            Text(
                "You can export a CSV file of your data from this page. " +
                    "Before deleting the app or switching phones, back up this file."
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    exportLauncher.launch("AlvandSalonManager-backup-${jalaliToday()}.csv")
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export CSV Backup")
            }
        }
        Spacer(Modifier.height(12.dp))
        AboutCard(title = "Developer") {
            Text(
                "This app was developed by the Alvand project developer with the help of AI tools."
            )
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
            "Version ${BuildConfig.VERSION_NAME} (Code ${BuildConfig.VERSION_CODE})",
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
