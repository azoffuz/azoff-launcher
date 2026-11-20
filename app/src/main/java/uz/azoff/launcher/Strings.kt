package uz.azoff.launcher

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.staticCompositionLocalOf

val LocalStrings = staticCompositionLocalOf { Strings("uz") }

/** Uch tildagi matnlar: o'zbek (standart), rus, ingliz */
class Strings(val lang: String) {
    private fun t(uz: String, ru: String, en: String) = when (lang) {
        "ru" -> ru
        "en" -> en
        else -> uz
    }

    // Umumiy
    val ok = t("OK", "ОК", "OK")
    val cancel = t("Bekor qilish", "Отмена", "Cancel")
    val close = t("Yopish", "Закрыть", "Close")
    val save = t("Saqlash", "Сохранить", "Save")
    val done = t("Tayyor", "Готово", "Done")
    val edit = t("Tahrirlash", "Изменить", "Edit")
    val add = t("Qo'shish", "Добавить", "Add")
    val create = t("Yaratish", "Создать", "Create")
    val back = t("Orqaga", "Назад", "Back")
    val error = t("Xatolik yuz berdi", "Произошла ошибка", "Something went wrong")
    val added = t("Qo'shildi", "Добавлено", "Added")
    val cannotOpen = t("Ilovani ochib bo'lmadi", "Не удалось открыть приложение", "Couldn't open the app")

    // Bosh ekran
    val widgets = t("Vidjetlar", "Виджеты", "Widgets")
    val noWidgets = t(
        "Hozircha vidjet yo'q. \"Qo'shish\" tugmasini bosing.",
        "Виджетов пока нет. Нажмите «Добавить».",
        "No widgets yet. Tap \"Add\".",
    )
    val chooseWidget = t("Vidjet tanlang", "Выберите виджет", "Choose a widget")
    val addWidget = t("Vidjet qo'shish", "Добавить виджет", "Add widget")
    val addApp = t("Ilova qo'shish", "Добавить приложение", "Add app")
    val addPage = t("Sahifa qo'shish", "Добавить страницу", "Add page")
    val removePage = t("Sahifani o'chirish", "Удалить страницу", "Remove page")
    val editHome = t("Bosh ekranni tahrirlash", "Изменить главный экран", "Edit home screen")
    val emptyPageHintOld = t(
        "Bo'sh sahifa. Yuqoridagi \"Ilova qo'shish\" tugmasidan foydalaning.",
        "Пустая страница. Используйте «Добавить приложение» сверху.",
        "Empty page. Use \"Add app\" above.",
    )
    val emptyPageHint get() = dragHint
    val dockHint = t("Dock bo'sh. Ilovaga uzoq bosib \"Dock'ga qo'shish\"ni tanlang.", "Док пуст. Удерживайте приложение и выберите «В док».", "Dock is empty. Long-press an app and choose \"Add to dock\".")
    val wallpaper = t("Fon rasmi", "Обои", "Wallpaper")
    val launcherSettings = t("Launcher sozlamalari", "Настройки лаунчера", "Launcher settings")

    // Ilova menyusi
    val addToHome = t("Bosh ekranga qo'shish", "На главный экран", "Add to home screen")
    val removeFromHome = t("Bosh ekrandan olib tashlash", "Убрать с главного экрана", "Remove from home screen")
    val moveToNextPage = t("Keyingi sahifaga o'tkazish", "На следующую страницу", "Move to next page")
    val addToDock = t("Dock'ga qo'shish", "В док", "Add to dock")
    val removeFromDock = t("Dock'dan olib tashlash", "Убрать из дока", "Remove from dock")
    val dockFull = t("Dock to'la (5 ta ilova)", "Док заполнен (5 приложений)", "Dock is full (5 apps)")
    val addToFolder = t("Papkaga qo'shish", "В папку", "Add to folder")
    val removeFromFolder = t("Papkadan chiqarish", "Убрать из папки", "Remove from folder")
    val hide = t("Yashirish", "Скрыть", "Hide")
    val unhide = t("Ko'rsatish (yashirishni bekor qilish)", "Показать", "Unhide")
    val lockApp = t("Qulflash", "Заблокировать", "Lock app")
    val unlockApp = t("Qulfni olib tashlash", "Снять блокировку", "Unlock app")
    val appInfo = t("Ilova haqida", "О приложении", "App info")
    val uninstall = t("O'chirib tashlash", "Удалить", "Uninstall")

    // Papkalar
    val folder = t("Papka", "Папка", "Folder")
    val folderName = t("Papka nomi", "Название папки", "Folder name")
    val newFolder = t("Yangi papka nomi", "Новая папка", "New folder name")
    val chooseFolder = t("Papkani tanlang", "Выберите папку", "Choose a folder")
    val ungroupFolder = t("Papkani tarqatish", "Расформировать", "Ungroup")

    // Ilovalar menyusi va qidiruv
    val searchHint = t("Ilova, kontakt, sozlama qidirish…", "Поиск приложений, контактов…", "Search apps, contacts, settings…")
    val all = t("Hammasi", "Все", "All")
    val frequent = t("Ko'p ishlatiladigan", "Часто используемые", "Frequently used")
    val apps = t("Ilovalar", "Приложения", "Apps")
    val contacts = t("Kontaktlar", "Контакты", "Contacts")
    val allowContacts = t("Kontaktlarni qidirishga ruxsat berish", "Разрешить поиск контактов", "Allow contact search")
    val settingsSection = t("Sozlamalar", "Настройки", "Settings")
    val web = t("Internet", "Интернет", "Web")
    val nothingFound = t("Ilova topilmadi", "Приложения не найдены", "No apps found")
    fun searchWeb(q: String) = t("Google'da qidirish: \"$q\"", "Искать в Google: «$q»", "Search Google for \"$q\"")
    fun searchPlay(q: String) = t("Play Market'da qidirish: \"$q\"", "Искать в Play Маркете: «$q»", "Search Play Store for \"$q\"")
    val chooseApp = t("Ilovani tanlang", "Выберите приложение", "Choose an app")

    fun category(c: Int): String = when (c) {
        ApplicationInfo.CATEGORY_GAME -> t("O'yinlar", "Игры", "Games")
        ApplicationInfo.CATEGORY_AUDIO -> t("Musiqa", "Музыка", "Audio")
        ApplicationInfo.CATEGORY_VIDEO -> t("Video", "Видео", "Video")
        ApplicationInfo.CATEGORY_IMAGE -> t("Rasmlar", "Фото", "Photos")
        ApplicationInfo.CATEGORY_SOCIAL -> t("Ijtimoiy", "Общение", "Social")
        ApplicationInfo.CATEGORY_NEWS -> t("Yangiliklar", "Новости", "News")
        ApplicationInfo.CATEGORY_MAPS -> t("Xaritalar", "Карты", "Maps")
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> t("Ish", "Работа", "Productivity")
        8 -> t("Maxsus imkoniyatlar", "Спец. возможности", "Accessibility")
        else -> t("Boshqa", "Другое", "Other")
    }

    // Yashirin / qulf / PIN
    val hiddenApps = t("Yashirin ilovalar", "Скрытые приложения", "Hidden apps")
    val noHiddenApps = t("Yashirin ilova yo'q", "Нет скрытых приложений", "No hidden apps")
    val lockedApps = t("Qulflangan ilovalar", "Заблокированные приложения", "Locked apps")
    val authTitle = t("Tasdiqlang", "Подтвердите", "Confirm it's you")
    val enterPin = t("PIN kodni kiriting", "Введите PIN-код", "Enter PIN")
    val pin = t("PIN kod", "PIN-код", "PIN")
    val wrongPin = t("PIN noto'g'ri", "Неверный PIN", "Wrong PIN")
    val useBiometric = t("Barmoq izi / ekran qulfi", "Отпечаток / блокировка экрана", "Fingerprint / screen lock")
    val setPin = t("PIN kod o'rnatish", "Установить PIN-код", "Set PIN")
    val changePin = t("PIN kodni o'zgartirish", "Изменить PIN-код", "Change PIN")
    val removePin = t("PIN kodni o'chirish", "Удалить PIN-код", "Remove PIN")
    val newPin = t("Yangi PIN", "Новый PIN", "New PIN")
    val repeatPin = t("PIN'ni takrorlang", "Повторите PIN", "Repeat PIN")
    val pinRules = t("Kamida 4 ta raqam", "Минимум 4 цифры", "At least 4 digits")
    val pinSaved = t("PIN saqlandi", "PIN сохранён", "PIN saved")
    val setPinFirst = t(
        "Avval sozlamalarda PIN kod o'rnating",
        "Сначала установите PIN-код в настройках",
        "Set a PIN in settings first",
    )

    // Bolalar rejimi
    val kidsMode = t("Bolalar rejimi", "Детский режим", "Kids mode")
    val kidsApps = t("Bolalar uchun ilovalar", "Приложения для детей", "Apps for kids")
    val kidsDesc = t(
        "Faqat tanlangan ilovalar ko'rinadi. Chiqish uchun PIN kerak.",
        "Видны только выбранные приложения. Для выхода нужен PIN.",
        "Only chosen apps are visible. Exiting requires the PIN.",
    )
    val chooseKidsApps = t("Avval bolalar uchun ilovalarni tanlang", "Сначала выберите приложения для детей", "Choose apps for kids first")
    val exitKidsMode = t("Bolalar rejimidan chiqish", "Выйти из детского режима", "Exit kids mode")

    // Gesture'lar
    val gestures = t("Gesture'lar", "Жесты", "Gestures")
    val swipeUp = t("Yuqoriga surish", "Свайп вверх", "Swipe up")
    val swipeDown = t("Pastga surish", "Свайп вниз", "Swipe down")
    val doubleTap = t("Ikki marta bosish", "Двойное касание", "Double tap")
    val chooseAction = t("Amalni tanlang", "Выберите действие", "Choose an action")
    val needAccessibility = t(
        "Buning uchun \"Azoff Launcher\" maxsus imkoniyat xizmatini yoqing",
        "Для этого включите службу спец. возможностей «Azoff Launcher»",
        "Enable the \"Azoff Launcher\" accessibility service for this",
    )
    val accessibilityService = t("Ekranni qulflash xizmatini yoqish", "Включить службу блокировки экрана", "Enable screen lock service")
    val accessibilityDesc = t(
        "Ikki marta bosib ekranni o'chirish va so'nggi ilovalar uchun kerak",
        "Нужно для блокировки двойным касанием и недавних приложений",
        "Needed for double-tap to lock and recents",
    )

    fun actionName(code: String, apps: List<AppInfo>): String = when {
        code == "none" -> t("Hech narsa", "Ничего", "Nothing")
        code == "drawer" -> t("Ilovalar ro'yxati", "Список приложений", "App drawer")
        code == "search" -> t("Qidiruv", "Поиск", "Search")
        code == "notifications" -> t("Bildirishnomalar paneli", "Панель уведомлений", "Notifications")
        code == "quick_settings" -> t("Tezkor sozlamalar", "Быстрые настройки", "Quick settings")
        code == "lock" -> t("Ekranni o'chirish", "Выключить экран", "Lock screen")
        code == "recents" -> t("So'nggi ilovalar", "Недавние приложения", "Recent apps")
        code == "settings" -> t("Launcher sozlamalari", "Настройки лаунчера", "Launcher settings")
        code == "wallpaper" -> t("Fon rasmini o'zgartirish", "Сменить обои", "Change wallpaper")
        code == "turbo" -> t("Turbo rejimni yoqish/oʻchirish", "Вкл/выкл турбо-режим", "Toggle Turbo mode")
        code == "app" -> t("Ilovani ochish…", "Открыть приложение…", "Open an app…")
        code.startsWith("app:") -> {
            val k = code.removePrefix("app:")
            val label = apps.firstOrNull { it.key == k }?.label ?: k.substringBefore('/')
            t("Ochish: $label", "Открыть: $label", "Open: $label")
        }
        else -> code
    }

    // Turbo va menyusiz rejim
    val turboSection = t("Batareya va tezlik", "Батарея и скорость", "Battery & speed")
    val turboMode = t("Turbo rejim", "Турбо-режим", "Turbo mode")
    val turboDesc = t(
        "Animatsiyalar, ob-havo, vidjetlar va bildirishnoma nuqtalari oʻchadi, qora mavzu yoqiladi — maksimal tezlik va batareya tejash",
        "Отключает анимации, погоду, виджеты и значки, включает чёрную тему — максимум скорости и экономии",
        "Turns off animations, weather, widgets and badges, uses a pure black theme — maximum speed and battery life",
    )
    val turboOn = t("Turbo rejim", "Турбо-режим", "Turbo mode")
    val turboEnable = t("Turbo rejimni yoqish", "Включить турбо-режим", "Turn on Turbo mode")
    val turboDisable = t("Turbo rejimni oʻchirish", "Выключить турбо-режим", "Turn off Turbo mode")
    val turboOnToast = t("⚡ Turbo rejim yoqildi", "⚡ Турбо-режим включён", "⚡ Turbo mode on")
    val turboOffToast = t("Turbo rejim oʻchirildi", "Турбо-режим выключен", "Turbo mode off")
    val autoTurbo = t("Avtomatik Turbo", "Авто-турбо", "Auto Turbo")
    fun autoTurboDesc(level: Int) = t(
        "Batareya $level% dan pastga tushganda yoqiladi, quvvatlanganda oʻchadi",
        "Включается при заряде ниже $level%, выключается при зарядке",
        "Turns on below $level% battery, off when charging",
    )
    val autoTurboLevel = t("Batareya chegarasi", "Порог заряда", "Battery threshold")
    val followPowerSave = t("Quvvat tejash rejimiga ergashish", "Следовать режиму энергосбережения", "Follow battery saver")
    val followPowerSaveDesc = t(
        "Telefonning quvvat tejash rejimi yoqilsa, Turbo ham yoqiladi",
        "Турбо включается вместе с системным энергосбережением",
        "Turbo turns on together with system battery saver",
    )
    val drawerless = t("Menyusiz rejim", "Без меню приложений", "No app drawer")
    val drawerlessDesc = t(
        "Barcha ilovalar bosh ekranda turadi (Xiaomi uslubi). Yangi ilovalar avtomatik qoʻshiladi.",
        "Все приложения на главном экране (как в MIUI). Новые добавляются автоматически.",
        "All apps live on the home screen (MIUI style). New apps are added automatically.",
    )
    val drawerlessRemoveHint = t(
        "Menyusiz rejimda ilovani yashirish yoki oʻchirish uchun uni uzoq bosing",
        "В этом режиме удерживайте приложение, чтобы скрыть или удалить его",
        "In this mode, long-press an app to hide or uninstall it",
    )
    val homeRows = t("Bosh ekran qatorlari", "Строки главного экрана", "Home screen rows")
    val pageFull = t("Sahifa toʻla", "Страница заполнена", "Page is full")
    val dragHint = t(
        "Ikonni uzoq bosib suring: chetga — boshqa sahifa, pastga — dock, boshqa ikon ustiga — papka",
        "Удерживайте и тяните: к краю — другая страница, вниз — док, на другую иконку — папка",
        "Long-press and drag: to an edge — other page, down — dock, onto another icon — folder",
    )

    // Sozlamalar ekrani
    val settingsTitle = t("Sozlamalar", "Настройки", "Settings")
    val notDefault = t(
        "Azoff Launcher hali standart bosh ekran emas. Barcha imkoniyatlar (masalan, tezkor amallar) ishlashi uchun uni standart qiling.",
        "Azoff Launcher ещё не главный экран по умолчанию. Сделайте его основным, чтобы работали все функции.",
        "Azoff Launcher isn't your default home app yet. Make it default so every feature works.",
    )
    val makeDefault = t("Standart launcher qilish", "Сделать лаунчером по умолчанию", "Set as default launcher")
    val appearance = t("Ko'rinish", "Оформление", "Appearance")
    val language = t("Til", "Язык", "Language")
    val theme = t("Mavzu", "Тема", "Theme")
    val themeNames = listOf(
        t("Tizim bo'yicha", "Как в системе", "System"),
        t("Yorug'", "Светлая", "Light"),
        t("Qorong'i", "Тёмная", "Dark"),
    )
    val materialYou = t("Material You ranglari", "Цвета Material You", "Material You colors")
    val materialYouDesc = t("Ranglar fon rasmiga moslashadi", "Цвета подстраиваются под обои", "Colors follow your wallpaper")
    val accentColor = t("Asosiy rang", "Акцентный цвет", "Accent color")
    val iconSize = t("Ikon o'lchami", "Размер иконок", "Icon size")
    val showLabels = t("Ilova nomlarini ko'rsatish", "Показывать подписи", "Show app labels")
    val iconPack = t("Icon paketi", "Пакет иконок", "Icon pack")
    val defaultIcons = t("Standart ikonlar", "Стандартные иконки", "Default icons")
    val noIconPacks = t(
        "Icon paketlar topilmadi. Play Market'dan istalgan Nova/ADW mos icon paketini o'rnating.",
        "Пакеты иконок не найдены. Установите любой пакет, совместимый с Nova/ADW.",
        "No icon packs found. Install any Nova/ADW compatible icon pack.",
    )
    val animations = t("Animatsiyalar", "Анимации", "Animations")
    val animationsDesc = t("O'chirilsa, ilovalar tezroq ochiladi", "Если выключить, приложения открываются быстрее", "Turn off for snappier app launches")
    val homeScreen = t("Bosh ekran", "Главный экран", "Home screen")
    val showClock = t("Soat va sana", "Часы и дата", "Clock and date")
    val showWeather = t("Ob-havo", "Погода", "Weather")
    val weatherDesc = t("Open-Meteo orqali, har 30 daqiqada yangilanadi", "Через Open-Meteo, обновление каждые 30 мин", "Via Open-Meteo, refreshed every 30 min")
    val city = t("Shahar", "Город", "City")
    val homeColumns = t("Bosh ekran ustunlari", "Столбцы главного экрана", "Home screen columns")
    val badges = t("Bildirishnoma nuqtalari", "Значки уведомлений", "Notification badges")
    val badgesDesc = t("Ikonlarda o'qilmagan bildirishnomalar soni", "Число непрочитанных уведомлений на иконках", "Unread notification count on icons")
    val badgesNeedAccess = t("Bildirishnomalarga ruxsat kerak", "Нужен доступ к уведомлениям", "Needs notification access")
    val grantAccess = t("Bildirishnomalarga ruxsat berish", "Предоставить доступ к уведомлениям", "Grant notification access")
    val restrictedHint = t(
        "Eslatma: APK Play Market'dan o'rnatilmagani uchun Android maxsus imkoniyat va bildirishnoma ruxsatlarini bloklashi mumkin. Bunda: Sozlamalar → Ilovalar → Azoff Launcher → ⋮ → \"Cheklangan sozlamalarga ruxsat berish\".",
        "Примечание: для APK не из Play Маркета Android может блокировать доступ к спец. возможностям и уведомлениям. Тогда: Настройки → Приложения → Azoff Launcher → ⋮ → «Разрешить ограниченные настройки».",
        "Note: Android may block accessibility and notification access for apps not from the Play Store. If so: Settings → Apps → Azoff Launcher → ⋮ → \"Allow restricted settings\".",
    )
    val drawer = t("Ilovalar menyusi", "Меню приложений", "App drawer")
    val drawerColumns = t("Menyu ustunlari", "Столбцы меню", "Drawer columns")
    val transparency = t("Fon shaffofligi", "Непрозрачность фона", "Background opacity")
    val frequentOnTop = t("Ko'p ishlatiladiganlar tepada", "Частые приложения сверху", "Frequent apps on top")
    val security = t("Xavfsizlik", "Безопасность", "Security")
    fun countApps(n: Int) = t("$n ta ilova", "Приложений: $n", "$n apps")
    val backup = t("Zaxira nusxa", "Резервная копия", "Backup")
    val exportSettings = t("Sozlamalarni saqlash (eksport)", "Экспорт настроек", "Export settings")
    val exportDesc = t("Barcha sozlamalar, sahifalar va papkalar JSON faylga", "Все настройки, страницы и папки в JSON-файл", "All settings, pages and folders to a JSON file")
    val importSettings = t("Sozlamalarni tiklash (import)", "Импорт настроек", "Import settings")
    val importDesc = t("Avval saqlangan JSON fayldan", "Из ранее сохранённого JSON-файла", "From a previously saved JSON file")
    val backupDone = t("Zaxira nusxa saqlandi", "Резервная копия сохранена", "Backup saved")
    val restoreDone = t("Sozlamalar tiklandi", "Настройки восстановлены", "Settings restored")
    val about = t("Ilova haqida", "О приложении", "About")

    // Sana
    private val weekdays = listOf(
        t("Yakshanba", "Воскресенье", "Sunday"),
        t("Dushanba", "Понедельник", "Monday"),
        t("Seshanba", "Вторник", "Tuesday"),
        t("Chorshanba", "Среда", "Wednesday"),
        t("Payshanba", "Четверг", "Thursday"),
        t("Juma", "Пятница", "Friday"),
        t("Shanba", "Суббота", "Saturday"),
    )
    private val months = when (lang) {
        "ru" -> listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
        "en" -> listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        else -> listOf("yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr")
    }

    /** dayOfWeek: Calendar.DAY_OF_WEEK (1 = yakshanba), month: 0..11 */
    fun formatDate(dayOfWeek: Int, day: Int, month: Int): String {
        val wd = weekdays[(dayOfWeek - 1).coerceIn(0, 6)]
        val m = months[month.coerceIn(0, 11)]
        return when (lang) {
            "en" -> "$wd, $m $day"
            else -> "$wd, $day-$m".let { if (lang == "ru") "$wd, $day $m" else it }
        }
    }
}
