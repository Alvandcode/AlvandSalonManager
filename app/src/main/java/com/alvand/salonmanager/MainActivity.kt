package com.alvand.salonmanager

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.floor

private val PersianBlue = Color(0xFFE6AD45)
private val Mint = Color(0xFF55B987)

class MainActivity : ComponentActivity() {
    private lateinit var store: SalonStore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SalonStore(this)
        setContent { SalonApp(store) }
    }
}

data class Record(val id: Long, val kind: String, val title: String, val amount: Long,
                  val date: String, val customer: String = "", val phone: String = "")
data class SalonService(val id: Long, val name: String, val price: Long, val description: String)

class SalonStore(ctx: android.content.Context) : SQLiteOpenHelper(ctx, "alvand_salon.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE records(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, title TEXT NOT NULL, amount INTEGER NOT NULL, date TEXT NOT NULL, customer TEXT DEFAULT '', phone TEXT DEFAULT '')")
        db.execSQL("CREATE TABLE services(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, price INTEGER NOT NULL, description TEXT NOT NULL DEFAULT '')")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("CREATE TABLE IF NOT EXISTS services(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, price INTEGER NOT NULL, description TEXT NOT NULL DEFAULT '')")
    }
    fun services(): List<SalonService> {
        val out = mutableListOf<SalonService>()
        readableDatabase.rawQuery("SELECT id,name,price,description FROM services ORDER BY name COLLATE NOCASE", null).use { c ->
            while (c.moveToNext()) out.add(SalonService(c.getLong(0), c.getString(1), c.getLong(2), c.getString(3)))
        }
        return out
    }
    fun saveService(id: Long?, name: String, price: Long, description: String) {
        val values = ContentValues().apply { put("name", name); put("price", price); put("description", description) }
        if (id == null) writableDatabase.insert("services", null, values)
        else writableDatabase.update("services", values, "id=?", arrayOf(id.toString()))
    }
    fun deleteService(id: Long) { writableDatabase.delete("services", "id=?", arrayOf(id.toString())) }
    fun add(kind: String, title: String, amount: Long, date: String, customer: String = "", phone: String = "") {
        val v = ContentValues().apply {
            put("kind", kind); put("title", title); put("amount", amount); put("date", date)
            put("customer", customer); put("phone", phone)
        }
        writableDatabase.insert("records", null, v)
    }
    fun all(): List<Record> {
        val out = mutableListOf<Record>()
        readableDatabase.rawQuery("SELECT id,kind,title,amount,date,customer,phone FROM records ORDER BY id DESC", null).use { c ->
            while (c.moveToNext()) out.add(Record(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getString(4), c.getString(5), c.getString(6)))
        }
        return out
    }
    fun delete(id: Long) { writableDatabase.delete("records", "id=?", arrayOf(id.toString())) }
}

private fun money(n: Long): String = DecimalFormat("#,###").format(n) + " تومان"
private fun persianDigits(s: String): String = s.map { if (it in '0'..'9') ('۰'.code + (it.code - '0'.code)).toChar() else it }.joinToString("")

/** Gregorian -> Jalali conversion; stores dates as sortable YYYY-MM-DD Jalali strings. */
private fun jalaliToday(): String {
    val d = LocalDate.now()
    val gDays = intArrayOf(0,31, if (d.isLeapYear) 29 else 28,31,30,31,30,31,31,30,31,30,31)
    var gy = d.year - 1600
    var gm = d.monthValue - 1
    var gd = d.dayOfMonth - 1
    var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
    for (i in 0 until gm) gDayNo += gDays[i + 1]
    gDayNo += gd
    var jDayNo = gDayNo - 79
    val jNp = jDayNo / 12053
    jDayNo %= 12053
    var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
    jDayNo %= 1461
    if (jDayNo >= 366) {
        jy += (jDayNo - 1) / 365
        jDayNo = (jDayNo - 1) % 365
    }
    val jm: Int
    val jd: Int
    if (jDayNo < 186) {
        jm = 1 + (jDayNo / 31); jd = 1 + (jDayNo % 31)
    } else {
        jm = 7 + ((jDayNo - 186) / 30); jd = 1 + ((jDayNo - 186) % 30)
    }
    return "%04d-%02d-%02d".format(jy, jm, jd)
}

private fun jalaliMonth(date: String) = date.take(7)
private fun jalaliYear(date: String) = date.take(4)
private fun periodStart(period: String): String {
    val today = jalaliToday()
    return when (period) {
        "امروز" -> today
        "ماه" -> today.take(7) + "-01"
        "سال" -> today.take(4) + "-01-01"
        else -> {
            // Current Jalali week approximation based on Gregorian weekday.
            val back = LocalDate.now().dayOfWeek.value - 1
            val d = LocalDate.now().minusDays(back.toLong())
            val converted = gregorianToJalali(d.year, d.monthValue, d.dayOfMonth)
            converted
        }
    }
}
private fun gregorianToJalali(year: Int, month: Int, day: Int): String {
    val d = LocalDate.of(year, month, day)
    val gDays = intArrayOf(0,31,if(d.isLeapYear)29 else 28,31,30,31,30,31,31,30,31,30,31)
    var gy=d.year-1600; val gm=d.monthValue-1; val gd=d.dayOfMonth-1
    var g=365*gy+(gy+3)/4-(gy+99)/100+(gy+399)/400
    for(i in 0 until gm) g+=gDays[i+1]
    g+=gd
    var j=g-79; val np=j/12053; j%=12053
    var jy=979+33*np+4*(j/1461); j%=1461
    if(j>=366){jy+=(j-1)/365;j=(j-1)%365}
    val jm:Int; val jd:Int
    if(j<186){jm=1+j/31;jd=1+j%31}else{jm=7+(j-186)/30;jd=1+(j-186)%30}
    return "%04d-%02d-%02d".format(jy,jm,jd)
}

@Composable
fun SalonApp(store: SalonStore) {
    var dark by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("خانه") }
    var refresh by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var addKind by remember { mutableStateOf("service") }
    var showServiceEditor by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<SalonService?>(null) }
    val records = remember(refresh) { store.all() }
    val scheme = if (dark) darkColorScheme(primary=Color(0xFFFFC45B), secondary=Mint, background=Color(0xFF080B0E), surface=Color(0xFF14191E), onSurface=Color(0xFFF4F0E8), onSurfaceVariant=Color(0xFFB4B4B4))
                 else lightColorScheme(primary=PersianBlue, secondary=Mint, background=Color(0xFF101216), surface=Color(0xFF191C21), onBackground=Color(0xFFF4F0E8), onSurface=Color(0xFFF4F0E8), onSurfaceVariant=Color(0xFFB4B4B4))
    MaterialTheme(colorScheme=scheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor=MaterialTheme.colorScheme.surface) {
                    listOf("خانه" to Icons.Default.Dashboard, "خدمات" to Icons.Default.ContentCut,
                        "هزینه‌ها" to Icons.Default.ReceiptLong, "مشتریان" to Icons.Default.People,
                        "درباره" to Icons.Default.Info).forEach { (label, icon) ->
                        NavigationBarItem(selected=tab==label, onClick={tab=label}, icon={Icon(icon, null)}, label={Text(label)})
                    }
                }
            },
            floatingActionButton = {
                if (tab != "خانه" && tab != "درباره" && tab != "خدمات") FloatingActionButton(onClick={
                    addKind = if(tab=="هزینه‌ها") "expense" else "customer"
                    showAdd=true
                }, containerColor=PersianBlue, contentColor=Color.White) { Icon(Icons.Default.Add, "افزودن") }
            }
        ) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=18.dp)) {
                Row(Modifier.fillMaxWidth().padding(top=16.dp, bottom=12.dp), verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("اَلْوَنْد", fontSize=27.sp, fontWeight=FontWeight.Black, color=MaterialTheme.colorScheme.primary)
                        Text("مدیریت مالی آرایشگاه", fontSize=13.sp, color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick={dark=!dark}) { Icon(if(dark) Icons.Default.LightMode else Icons.Default.DarkMode, "تغییر تم") }
                }
                when(tab) {
                    "خانه" -> Dashboard(records, onAdd={ addKind="service"; showAdd=true }, onExpenses={addKind="expense";showAdd=true})
                    "خدمات" -> ServiceCatalog(store, refresh, {refresh++},
                        onEdit={ editingService=it; showServiceEditor=true },
                        onAdd={ editingService=null; showServiceEditor=true })
                    "هزینه‌ها" -> RecordsList(records.filter{it.kind=="expense"}, "هزینه‌های ثبت‌شده", store, {refresh++})
                    "مشتریان" -> RecordsList(records.filter{it.kind=="customer"}, "مشتریان", store, {refresh++})
                    else -> AboutScreen()
                }
            }
        }
        if(showAdd) AddDialog(kind=addKind, onDismiss={showAdd=false}, onSave={title,amount,date,customer,phone->
            store.add(addKind,title,amount,date,customer,phone); refresh++; showAdd=false
        })
        if(showServiceEditor) ServiceEditorDialog(service=editingService, onDismiss={showServiceEditor=false},
            onSave={id,name,price,description -> store.saveService(id,name,price,description); refresh++; showServiceEditor=false })
    }
}

@Composable
fun Dashboard(records: List<Record>, onAdd:()->Unit, onExpenses:()->Unit) {
    val today = jalaliToday()
    val services = records.filter{it.kind=="service"}
    val expenses = records.filter{it.kind=="expense"}
    val daily = services.filter{it.date==today}.sumOf{it.amount}
    val dailyCost = expenses.filter{it.date==today}.sumOf{it.amount}
    val week = services.filter{it.date >= periodStart("هفته")}.sumOf{it.amount}
    val month = services.filter{jalaliMonth(it.date)==jalaliMonth(today)}.sumOf{it.amount}
    val year = services.filter{jalaliYear(it.date)==jalaliYear(today)}.sumOf{it.amount}
    val allNet = services.sumOf{it.amount} - expenses.sumOf{it.amount}
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp), contentPadding=PaddingValues(bottom=20.dp)) {
        item {
            Card(shape=RoundedCornerShape(26.dp), colors=CardDefaults.cardColors(containerColor=PersianBlue), modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Text("درآمد خالص ثبت‌شده", color=Color.White.copy(alpha=.8f), fontSize=14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(money(allNet), color=Color.White, fontSize=27.sp, fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Surface(color=Color.White.copy(alpha=.18f), shape=CircleShape) { Text("  امروز: ${persianDigits(today)}  ", color=Color.White, modifier=Modifier.padding(7.dp), fontSize=12.sp) }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.TrendingUp, null, tint=Color.White, modifier=Modifier.size(34.dp))
                    }
                }
            }
        }
        item { Text("گزارش درآمد", fontSize=18.sp, fontWeight=FontWeight.Bold) }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                StatCard("امروز", daily, Modifier.weight(1f), PersianBlue)
                StatCard("این هفته", week, Modifier.weight(1f), Mint)
            }
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                StatCard("این ماه", month, Modifier.weight(1f), Color(0xFFE59B38))
                StatCard("امسال", year, Modifier.weight(1f), Color(0xFFDB6D9A))
            }
        }
        item {
            Card(shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("هزینه‌های امروز", color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(money(dailyCost), fontSize=19.sp, fontWeight=FontWeight.Bold)
                    }
                    Icon(Icons.Default.Payments, null, tint=Color(0xFFE59B38), modifier=Modifier.size(30.dp))
                }
            }
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Button(onClick=onAdd, modifier=Modifier.weight(1f), shape=RoundedCornerShape(15.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("ثبت خدمت") }
                OutlinedButton(onClick=onExpenses, modifier=Modifier.weight(1f), shape=RoundedCornerShape(15.dp)) { Icon(Icons.Default.ReceiptLong, null); Spacer(Modifier.width(6.dp)); Text("ثبت هزینه") }
            }
        }
        item { Text("آخرین فعالیت‌ها", fontSize=18.sp, fontWeight=FontWeight.Bold) }
        if(records.isEmpty()) item { EmptyState("هنوز اطلاعاتی ثبت نشده؛ اولین خدمت را اضافه کنید.") }
        items(records.take(5)) { r -> RecordCard(r, onDelete={}) }
    }
}

@Composable
fun StatCard(label:String, amount:Long, modifier:Modifier, accent:Color) {
    Card(modifier=modifier, shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(accent, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(label, fontSize=12.sp, color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            Text(money(amount), fontSize=15.sp, fontWeight=FontWeight.Bold, maxLines=1)
        }
    }
}

@Composable
fun RecordsList(records:List<Record>, title:String, store:SalonStore, changed:()->Unit) {
    Column {
        Text(title, fontSize=20.sp, fontWeight=FontWeight.Bold, modifier=Modifier.padding(vertical=10.dp))
        if(records.isEmpty()) EmptyState("موردی وجود ندارد. برای ثبت مورد جدید، دکمه + را بزنید.")
        else LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp), contentPadding=PaddingValues(bottom=20.dp)) {
            items(records, key={it.id}) { r ->
                RecordCard(r, onDelete={store.delete(r.id); changed()})
            }
        }
    }
}

@Composable
fun RecordCard(r:Record, onDelete:()->Unit) {
    Card(shape=RoundedCornerShape(18.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface), modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(43.dp).background(if(r.kind=="expense") Color(0xFFFFE7D8) else Color(0xFFE6E8FF), CircleShape), contentAlignment=Alignment.Center) {
                Icon(if(r.kind=="expense") Icons.Default.ReceiptLong else if(r.kind=="customer") Icons.Default.Person else Icons.Default.ContentCut,
                    null, tint=if(r.kind=="expense") Color(0xFFD66B27) else PersianBlue)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.title, fontWeight=FontWeight.Bold, maxLines=1)
                if(r.customer.isNotBlank()) Text(r.customer + if(r.phone.isNotBlank()) " · ${r.phone}" else "", fontSize=12.sp, color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(persianDigits(r.date), fontSize=11.sp, color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(r.kind!="customer") Text(money(r.amount), fontWeight=FontWeight.Bold, fontSize=13.sp,
                color=if(r.kind=="expense") Color(0xFFE0793D) else MaterialTheme.colorScheme.primary)
            IconButton(onClick=onDelete) { Icon(Icons.Default.DeleteOutline, "حذف", tint=MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun EmptyState(message:String) {
    Card(shape=RoundedCornerShape(18.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface), modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(25.dp), horizontalAlignment=Alignment.CenterHorizontally) {
            Icon(Icons.Default.Inbox, null, tint=MaterialTheme.colorScheme.primary, modifier=Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(message, textAlign=TextAlign.Center, color=MaterialTheme.colorScheme.onSurfaceVariant, fontSize=13.sp)
        }
    }
}

@Composable
fun AddDialog(kind:String, onDismiss:()->Unit, onSave:(String,Long,String,String,String)->Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(jalaliToday()) }
    var customer by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val customerOnly = kind=="customer"
    val expense = kind=="expense"
    val heading = when(kind) {"expense"->"ثبت هزینه";"customer"->"ثبت مشتری";else->"ثبت خدمت"}
    AlertDialog(onDismissRequest=onDismiss, title={Text(heading, fontWeight=FontWeight.Bold)},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value=title, onValueChange={title=it}, label={Text(if(customerOnly) "نام مشتری" else if(expense) "عنوان هزینه" else "نام خدمت")}, singleLine=true)
                if(!customerOnly) {
                    OutlinedTextField(value=amount, onValueChange={amount=it.filter{c->c.isDigit()}}, label={Text("مبلغ (تومان)")},
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), singleLine=true)
                    OutlinedTextField(value=date, onValueChange={date=it.filter{c->c.isDigit()||c=='-'}.take(10)}, label={Text("تاریخ شمسی (YYYY-MM-DD)")}, singleLine=true)
                }
                if(!expense) {
                    OutlinedTextField(value=customer, onValueChange={customer=it}, label={Text(if(customerOnly) "یادداشت (اختیاری)" else "نام مشتری (اختیاری)")}, singleLine=true)
                    OutlinedTextField(value=phone, onValueChange={phone=it.filter{c->c.isDigit()||c=='+'}.take(15)}, label={Text("شماره تماس")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone), singleLine=true)
                }
            }
        },
        confirmButton={ Button(onClick={
            if(title.isNotBlank() && (customerOnly || amount.toLongOrNull()!=null && date.length==10))
                onSave(title, amount.toLongOrNull() ?: 0L, date, customer, phone)
        }, enabled=title.isNotBlank() && (customerOnly || amount.toLongOrNull()!=null && date.length==10)) { Text("ذخیره") } },
        dismissButton={ TextButton(onClick=onDismiss) { Text("انصراف") } }
    )
}


@Composable
fun ServiceCatalog(store: SalonStore, refreshKey: Int, onChanged: () -> Unit, onEdit: (SalonService) -> Unit, onAdd: () -> Unit) {
    var listRefresh by remember { mutableIntStateOf(0) }
    val services = remember(listRefresh, refreshKey) { store.services() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(vertical=10.dp), verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("فهرست خدمات", fontSize=21.sp, fontWeight=FontWeight.Bold)
                Text("نام و قیمت خدمات آرایشگاه", fontSize=12.sp, color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick=onAdd, shape=RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("خدمت جدید") }
        }
        if (services.isEmpty()) EmptyState("هنوز خدمتی تعریف نشده است. برای افزودن خدمت، دکمه «خدمت جدید» را بزنید.")
        else LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp), contentPadding=PaddingValues(bottom=20.dp)) {
            items(services, key={it.id}) { service ->
                Card(shape=RoundedCornerShape(18.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface), modifier=Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background(Color(0xFFE6E8FF), CircleShape), contentAlignment=Alignment.Center) { Icon(Icons.Default.ContentCut, null, tint=PersianBlue) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(service.name, fontWeight=FontWeight.Bold)
                            if (service.description.isNotBlank()) Text(service.description, fontSize=12.sp, color=MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(money(service.price), color=MaterialTheme.colorScheme.primary, fontWeight=FontWeight.Bold)
                        }
                        IconButton(onClick={onEdit(service)}) { Icon(Icons.Default.Edit, "ویرایش") }
                        IconButton(onClick={store.deleteService(service.id); listRefresh++; onChanged()}) { Icon(Icons.Default.DeleteOutline, "حذف", tint=Color(0xFFD95757)) }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceEditorDialog(service: SalonService?, onDismiss: () -> Unit, onSave: (Long?, String, Long, String) -> Unit) {
    var name by remember(service?.id) { mutableStateOf(service?.name ?: "") }
    var price by remember(service?.id) { mutableStateOf(service?.price?.toString() ?: "") }
    var description by remember(service?.id) { mutableStateOf(service?.description ?: "") }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text(if (service == null) "افزودن خدمت جدید" else "ویرایش خدمت", fontWeight=FontWeight.Bold)},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value=name, onValueChange={name=it}, label={Text("نام خدمت")}, singleLine=true)
                OutlinedTextField(value=price, onValueChange={price=it.filter { ch -> ch.isDigit() }}, label={Text("قیمت (تومان)")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), singleLine=true)
                OutlinedTextField(value=description, onValueChange={description=it}, label={Text("توضیحات (اختیاری)")}, minLines=2)
            }
        },
        confirmButton={Button(onClick={onSave(service?.id, name.trim(), price.toLong(), description.trim())}, enabled=name.isNotBlank() && (price.toLongOrNull() ?: 0L) > 0L) { Text("ذخیره")}},
        dismissButton={TextButton(onClick=onDismiss) { Text("انصراف")}}
    )
}

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(top=18.dp), horizontalAlignment=Alignment.CenterHorizontally) {
        Image(painter=painterResource(id=R.drawable.alvand_salon_logo), contentDescription="لوگوی آرایشگاه الوند", modifier=Modifier.size(104.dp))
        Spacer(Modifier.height(14.dp))
        Text("مدیریت آرایشگاه الوند", fontSize=22.sp, fontWeight=FontWeight.Bold)
        Text("Alvand Salon Manager", color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        Card(shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("درباره سازنده", fontWeight=FontWeight.Bold, fontSize=18.sp)
                Spacer(Modifier.height(8.dp))
                Text("این برنامه با کمک ابزارهای هوش مصنوعی و تلاش سازنده پروژه الوند توسعه داده شده است.")
                Spacer(Modifier.height(14.dp))
                Text("گیت‌هاب سازنده", fontWeight=FontWeight.SemiBold, color=MaterialTheme.colorScheme.primary)
                Text("https://github.com/Alvandcode/", color=MaterialTheme.colorScheme.primary,
                    modifier=Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Alvandcode/")))
                    })
                Spacer(Modifier.height(12.dp))
                Text("نسخه برنامه: ${BuildConfig.VERSION_NAME}", fontSize=13.sp, color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
