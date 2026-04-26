# Установка и запуск

## Требования

| Инструмент | Версия |
|-----------|--------|
| Android Studio | Hedgehog 2023.1.1 и новее |
| JDK | 17 |
| Kotlin | 2.1.x |
| Android Gradle Plugin | 8.x |
| Минимальный Android SDK | 24 (Android 7.0) |
| Target / Compile SDK | 35 (Android 15) |

---

## 1. Клонировать репозиторий

```bash
git clone https://github.com/<ваш-логин>/Lango.git
cd Lango
```

## 2. Настройка Firebase

Проект использует **Firebase Authentication** и **Cloud Firestore**.

1. Создайте проект на [console.firebase.google.com](https://console.firebase.google.com)
2. Добавьте Android-приложение с именем пакета `com.lango.app`
3. Скачайте файл `google-services.json` и положите его в папку `app/`
4. В Firebase Auth включите способ входа **Email/пароль**
5. Создайте базу данных Firestore (для разработки — в режиме **тест**)

> ⚠️ Файл `app/google-services.json` в репозитории содержит заглушку.  
> Замените его своим файлом перед сборкой.

## 3. API-ключи

Проект использует два внешних API. Добавьте свои ключи в соответствующие файлы:

| API | Где получить | Файл |
|-----|-------------|------|
| [Groq API](https://console.groq.com) | groq.com → API Keys | `app/src/main/java/com/lango/app/data/remote/GroqApiService.kt` |
| [Pexels API](https://www.pexels.com/api/) | pexels.com → Ваш API Key | `app/src/main/java/com/lango/app/data/remote/ImageApiService.kt` |

Найдите в коде `YOUR_GROQ_API_KEY` и `YOUR_PEXELS_API_KEY` и замените на свои ключи.

## 4. Сборка и запуск

Откройте корневую папку проекта в **Android Studio** и нажмите **Run ▶**.  
Или соберите через командную строку:

```bash
./gradlew assembleDebug
```

Готовый APK будет находиться по пути: `app/build/outputs/apk/debug/app-debug.apk`

---

## Структура исходного кода

```
app/src/main/java/com/lango/app/
├── data/
│   ├── local/          # Room: база данных, DAO, сущности
│   ├── remote/         # Клиенты Groq, Pexels, Firestore
│   └── repository/     # LangoRepository — единая точка доступа к данным
├── domain/
│   └── model/          # Чистые Kotlin-модели (Deck, Word и др.)
├── notifications/       # WorkManager воркер + NotificationHelper
├── ui/
│   ├── components/     # Переиспользуемые Compose-компоненты
│   ├── screens/        # 6 экранов приложения
│   └── theme/          # Тема Material 3, цвета, типографика
├── viewmodel/          # 5 ViewModel-классов
├── EditWordActivity.kt
├── LangoApplication.kt
└── MainActivity.kt
```

## Частые проблемы

**Сборка падает с ошибкой «KSP not found»** → Убедитесь, что KSP подключён в `app/build.gradle.kts` и версия совпадает с версией Kotlin.

**Firestore возвращает «Permission denied»** → Переключите Firestore в тестовый режим или настройте правила безопасности для авторизованных пользователей.

**Groq возвращает 401** → API-ключ отсутствует или указан неверно. Проверьте `GroqApiService.kt`.
