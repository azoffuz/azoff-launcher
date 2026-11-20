# Azoff Launcher

Redmi Note 13 (HyperOS / Android 13–14) uchun yozilgan Android launcher. Kotlin va Jetpack Compose'da yaratilgan, minSdk 26.

## Imkoniyatlar

**Bosh ekran:** soat, sana va ob-havo · dock (5 ta ilova) · Android vidjetlari (chap sahifada) · bir nechta sahifa · tahrirlash rejimida ilovalarni sudrab joylashtirish

**Ilovalar menyusi:** alifbo bo'yicha ro'yxat va harf bo'yicha tez o'tish · ilova, kontakt, sozlama, Google va Play Market bo'yicha qidiruv · papkalar va avtomatik kategoriyalar · yashirin ilovalar (PIN yoki barmoq izi bilan) · ko'p ishlatiladigan ilovalar tepada

**Gesture'lar:** yuqoriga, pastga surish va ikki marta bosish. Har biriga istalgan amalni biriktirish mumkin: menyu, qidiruv, bildirishnomalar, tezkor sozlamalar, ekranni o'chirish, so'nggi ilovalar yoki istalgan ilovani ochish.

**Dizayn:** tizim, yorug' va qorong'i tema · Material You yoki 8 xil asosiy rang · icon paketlar (Nova/ADW mos) · ikon o'lchami va ustunlar soni · menyu shaffofligi · animatsiyalarni yoqish/o'chirish

**Foydali:** uzoq bosilganda ilova shortcut'lari · bildirishnoma nuqtalari (badge) · ilova haqida va o'chirib tashlash · sozlamalarni JSON faylga eksport va import · o'zbek, rus va ingliz tillari

**Maxfiylik:** ilovalarni qulflash (PIN yoki barmoq izi) · bolalar rejimi (faqat tanlangan ilovalar ko'rinadi, chiqish uchun PIN kerak)

## APK qanday build qilinadi

Hamma narsa GitHub Actions orqali avtomatik bajariladi:

| Nima qilasiz | Natija |
|---|---|
| `main` branch'ga push | APK build bo'ladi va **Actions → run → Artifacts** bo'limiga chiqadi |
| **Actions → Build APK → Run workflow** ni bosib, versiya yozasiz (masalan `1.0.0`) | APK build bo'ladi va **Releases** bo'limida `v1.0.0` release paydo bo'ladi |
| `v1.0.1` kabi tag push qilasiz | Release avtomatik yaratiladi |

## Telefonga o'rnatish

1. Repo'dagi **Releases** bo'limidan `AzoffLauncher-x.y.z.apk` faylini yuklab oling va o'rnating ("Noma'lum manbalar"ga ruxsat bering).
2. Launcher'ni oching va **Sozlamalar → Standart launcher qilish** ni bosing. Yoki telefoningizda: Sozlamalar → Ilovalar → Standart ilovalar → Bosh ekran → Azoff Launcher.
3. Bildirishnoma nuqtalari va "ikki marta bosib ekranni o'chirish" uchun launcher sozlamalaridagi ruxsat tugmalarini bosing.

> **HyperOS / Android 13+ eslatmasi:** Play Market'dan o'rnatilmagan ilovalar uchun maxsus imkoniyat va bildirishnoma ruxsatlari bloklanishi mumkin. Bunday holatda: Sozlamalar → Ilovalar → Azoff Launcher → ⋮ (yuqori o'ng burchak) → **"Cheklangan sozlamalarga ruxsat berish"**, keyin ruxsatni qayta yoqing.
>
> MIUI/HyperOS boshqa launcher'larda ba'zan gesture navigatsiyani cheklaydi. Agar to'liq ekran gesture'lari ishlamasa, navigatsiyani vaqtincha tugmalar rejimiga o'tkazing.

## Imzo (signing) haqida

Yangilanishlar bir-biriga mos bo'lishi uchun APK doim bir xil kalit bilan imzolanadi. Repo ichida `keystore/azoff.jks` (parol `azoff2026`) bor, workflow standart holatda shundan foydalanadi.

Agar repo **public** bo'lsa, xavfsizlik uchun o'z kalitingizni GitHub Secrets'ga qo'yish tavsiya etiladi: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Bu secret'lar berilsa, workflow avtomatik ravishda ulardan foydalanadi.

## Kompyuterda build qilish

```bash
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

JDK 17 va Android SDK (API 35) kerak.
