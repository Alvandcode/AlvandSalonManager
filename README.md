# Alvand Salon Manager | مدیریت آرایشگاه الوند

[![Build and Release APK](https://github.com/Alvandcode/AlvandSalonManager/actions/workflows/android.yml/badge.svg)](https://github.com/Alvandcode/AlvandSalonManager/actions/workflows/android.yml)

اپلیکیشن اندرویدی فارسی برای مدیریت خدمات، درآمد، هزینه‌ها و مشتریان آرایشگاه. اطلاعات به‌صورت محلی در SQLite ذخیره می‌شود و برنامه به اینترنت یا هیچ مجوز حساسی نیاز ندارد.

## قابلیت‌ها

- فهرست خدمات: افزودن، ویرایش و حذف خدمت همراه با قیمت و توضیحات
- ثبت درآمد خدمات و هزینه‌ها با تاریخ شمسی
- ثبت و ویرایش اطلاعات مشتریان و یادداشت‌ها
- گزارش درآمد روزانه، هفتگی، ماهانه و سالانه (هفته از شنبه)
- پشتیبانی از ارقام فارسی و عربی در ورودی مبلغ و تاریخ
- اعتبارسنجی تاریخ شمسی و نمایش تاریخ با ارقام فارسی
- تأییدیه قبل از حذف و امکان ویرایش رکوردها
- اتصال رکوردهای درآمدی به خدمات کاتالوگ
- تم تیره و روشن با کنتراست اصلاح‌شده و رابط کاملاً راست‌چین
- خروجی پشتیبان CSV از داده‌ها
- آیکون اختصاصی برنامه
- ساخت و انتشار خودکار APK امضاشده در GitHub Actions

## دریافت و نصب

آخرین نسخه را از صفحه [Releases](https://github.com/Alvandcode/AlvandSalonManager/releases) دانلود کنید. نام فایل خروجی به این شکل است:

```text
AlvandSalonManager-v<version>-Android.apk
```

مثال:

```text
AlvandSalonManager-v1.2.0.42-Android.apk
```

> **توجه:** نسخه‌های قدیمی با کلید Debug منتشر شده‌اند. اگر نسخه قبلی روی گوشی نصب است، پیش از نصب نسخه جدید آن را حذف کنید؛ پس از آن، نسخه‌های بعدی با کلید پایدار قابل به‌روزرسانی هستند.

## نسخه‌گذاری

- نسخه پایه در `app/build.gradle.kts` قرار دارد (`versionName` و `versionCode`).
- هنگام اجرای Workflow، شماره اجرای GitHub Actions به نسخه پایه اضافه می‌شود تا هر انتشار نسخه و تگ یکتا داشته باشد.
- نام APK، نام Release، تگ و `versionName` داخل برنامه همگی از همان نسخه ساخته می‌شوند.
- `versionCode` نیز به‌صورت خودکار افزایش می‌یابد تا نصب به‌روزرسانی روی نسخه قبلی ممکن باشد.

## انتشار خودکار

Workflow در `.github/workflows/android.yml` قرار دارد و با هر Push به شاخه `main` یا اجرای دستی فعال می‌شود:

1. پروژه با JDK 17 و Android SDK 35 ساخته می‌شود.
2. APK نسخه Release با R8 بهینه و با کلید موجود در GitHub Secrets امضا می‌شود.
3. فایل با نام `AlvandSalonManager-v<version>-Android.apk` بارگذاری می‌شود.
4. GitHub Release و تگ مربوط به همان Commit ایجاد می‌شود.

کلیدهای امضا به‌صورت GitHub Secrets نگهداری می‌شوند و در مخزن قرار نمی‌گیرند:

- `RELEASE_SIGNING_KEYSTORE_B64`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

## ساخت محلی

پیش‌نیازها:

- JDK 17
- Android SDK 35
- Gradle 8.9 (یا استفاده از Wrapper موجود در مخزن)

```bash
./gradlew assembleDebug
```

خروجی در `app/build/outputs/apk/debug/` قرار می‌گیرد. برای ساخت نسخه Release به فایل `release-keystore.jks` و متغیرهای محیطی امضا نیاز است.

## داده‌ها و پشتیبان‌گیری

داده‌ها در پایگاه داده محلی `alvand_salon.db` ذخیره می‌شوند. برنامه مجوز اینترنت ندارد و اطلاعات را جایی ارسال نمی‌کند.

برای تهیه نسخه پشتیبان:

1. به تب «درباره» بروید.
2. دکمه «خروجی CSV از داده‌ها» را بزنید.
3. محل ذخیره فایل را انتخاب کنید.

فایل CSV شامل رکوردهای مالی/مشتریان و فهرست خدمات است. پیش از حذف برنامه یا انتقال به گوشی جدید، از این فایل پشتیبان بگیرید.

## ساختار پروژه

```text
app/
  src/main/java/com/alvand/salonmanager/MainActivity.kt   # رابط Compose و لایه داده SQLite
  src/main/res/                                          # آیکون، تم و منابع
  build.gradle.kts                                       # تنظیمات ساخت، نسخه و امضا
.github/workflows/android.yml                            # ساخت و انتشار خودکار
```

## حریم خصوصی

اطلاعات مشتریان و مالی فقط روی دستگاه ذخیره می‌شود. جزئیات در [PRIVACY.md](PRIVACY.md) آمده است.

## مجوز

این پروژه تحت مجوز MIT منتشر شده است. فایل [LICENSE](LICENSE) را ببینید.

## سازنده

GitHub: [Alvandcode](https://github.com/Alvandcode/)
