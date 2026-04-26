<div align="center">

# 🌐 Lango

### Мобильное приложение для изучения английских слов с помощью ИИ

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.x-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-2025.03.00-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Android](https://img.shields.io/badge/Android-7.0%2B_(API_24%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Firebase](https://img.shields.io/badge/Firebase-Auth_%26_Firestore-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)
[![Лицензия: MIT](https://img.shields.io/badge/Лицензия-MIT-blue.svg)](LICENSE)

[📖 Установка и запуск](docs/SETUP.md) · [🏗️ Архитектура](#архитектура) · [✨ Возможности](#возможности) · [📱 Экраны](#экраны)

</div>

---

## О проекте

**Lango** — полнофункциональное Android-приложение для изучения английских слов на основе карточек. В основе лежит проверенный **алгоритм интервального повторения SM-2**, усиленный **генерацией колод через ИИ**. Вместо ручного создания карточек достаточно ввести тему — и приложение само создаст до 50 карточек с переводом, транскрипцией и примерами предложений за считанные секунды.

> Разработано в **META University, Алматы (2026)** в рамках курса «Разработка мобильных приложений».  
> Авторы: **Беляев Валентин** · **Корнев Никита** · Группа УС2-ВТиПО-25-2Р

---

## Возможности

| Функция | Описание |
|---------|---------|
| 🧠 **Интервальное повторение (SM-2)** | Приложение само планирует повторение каждого слова — сложные показываются чаще, выученные реже |
| 🤖 **Генерация колод через ИИ** | Введи любую тему → Groq API (LLaMA 3.3 70B) создаёт до 50 карточек с переводом, транскрипцией и тремя примерами (A1 / B1 / B2) |
| 🌐 **Публичный каталог** | Публикуй свои колоды или импортируй чужие через Cloud Firestore |
| 🔊 **Озвучка слов** | Встроенный Android TTS произносит каждое слово вслух |
| 🖼️ **Автоподбор изображений** | Pexels API автоматически подбирает картинку к каждому слову |
| 🔐 **Авторизация** | Firebase Auth: вход по email/паролю и гостевой режим |
| 🏆 **Система достижений** | 6 ачивок, привязанных к прогрессу обучения |
| 🔔 **Ежедневные напоминания** | WorkManager отправляет уведомление каждый день, чтобы не терять серию |
| 📴 **Офлайн-режим** | Room (SQLite) хранит все данные локально; синхронизация с облаком при наличии сети |

---

## Архитектура

Lango построен по чистой **4-слойной архитектуре**:

```
┌─────────────────────────────────────────────────┐
│         UI Layer — Jetpack Compose              │
│  AuthScreen · DecksScreen · DeckDetailScreen    │
│  TrainingScreen · CatalogScreen · ProfileScreen  │
└────────────────────┬────────────────────────────┘
                     │  наблюдает StateFlow
┌────────────────────▼────────────────────────────┐
│       ViewModel Layer  (5 ViewModel-классов)    │
│  AuthVM · DecksVM · TrainingVM · CatalogVM …   │
└────────────────────┬────────────────────────────┘
                     │  suspend-функции / Flow
┌────────────────────▼────────────────────────────┐
│     Repository Layer  (LangoRepository)         │
│       Единая точка доступа ко всем данным       │
└──────┬──────────────────────────┬───────────────┘
       │ локально                 │ удалённо
┌──────▼──────┐          ┌────────▼──────────────┐
│  Room (БД)  │          │ Firebase Auth          │
│  SQLite v5  │          │ Cloud Firestore        │
│  DAO-классы │          │ Groq API (LLaMA 3.3)  │
│  Entity     │          │ Pexels API             │
└─────────────┘          └───────────────────────┘
```

Полная диаграмма: [`docs/lango_architecture.svg`](docs/lango_architecture.svg)

---

## Технический стек

| Слой | Технология |
|------|-----------|
| Язык | Kotlin 2.1.x |
| UI | Jetpack Compose + Material 3 |
| Управление состоянием | ViewModel + StateFlow |
| Навигация | Navigation Compose |
| Локальная БД | Room 2.7.1 (SQLite, схема v5) |
| Облако | Firebase Auth + Cloud Firestore |
| ИИ | Groq API — `llama-3.3-70b-versatile` |
| Изображения | Pexels API + Coil |
| Фоновые задачи | WorkManager |
| HTTP | OkHttp 4.12 |
| Настройки | DataStore |
| Минимальный SDK | 24 (Android 7.0) |
| Target / Compile SDK | 35 (Android 15) |

---

## Экраны

### 1 · Auth — Вход и регистрация
Авторизация по email/паролю или в гостевом режиме. Работает на Firebase Auth.

### 2 · Decks — Список колод
Градиентные карточки с количеством слов, словами на повторение и выученными словами. Кнопка **+** — создать колоду вручную или сгенерировать через ИИ.

### 3 · DeckDetail — Слова в колоде
Каждое слово отображается со статусом: `NEW` · `LEARNING` · `KNOWN`. Тап — редактировать, долгий тап — удалить.

### 4 · Training — Тренировка (SM-2)
Переворачиваемые карточки. После просмотра оцени себя: **Снова / Сложно / Хорошо / Легко**. Алгоритм SM-2 сам пересчитает дату следующего повторения.

### 5 · Catalog — Публичные колоды
Поиск по всем опубликованным колодам в реальном времени. Один тап — импортировать в свою библиотеку.

### 6 · Profile — Профиль и достижения
Общая статистика, серия дней, 6 ачивок. Кнопка выхода из аккаунта.

---

## Структура проекта

```
app/src/main/java/com/lango/app/
├── data/
│   ├── local/
│   │   ├── dao/            # DeckDao, WordDao
│   │   ├── entity/         # DeckEntity, WordEntity + маппинг
│   │   └── LangoDatabase.kt
│   ├── remote/
│   │   ├── GroqApiService.kt      # Генерация карточек через ИИ
│   │   ├── ImageApiService.kt     # Поиск изображений Pexels
│   │   └── FirestoreService.kt    # Синхронизация с облаком и каталог
│   └── repository/
│       └── LangoRepository.kt     # Единая точка доступа к данным
├── domain/
│   └── model/
│       └── Models.kt              # Deck, Word, WordLevel, Rating
├── notifications/
│   ├── DailyReminderWorker.kt
│   └── NotificationHelper.kt
├── ui/
│   ├── components/
│   │   └── Components.kt          # Общие Compose-компоненты
│   ├── screens/                   # 6 экранов приложения
│   └── theme/                     # Цвета, типографика, тема
├── viewmodel/
│   └── ViewModels.kt              # 5 ViewModel-классов
├── EditWordActivity.kt
├── LangoApplication.kt
└── MainActivity.kt

docs/
├── lango_architecture.svg         # Диаграмма архитектуры
├── lango_architecture.png         # Диаграмма архитектуры (растр)
├── LANGO-Presentation.pptx        # Презентация проекта
└── SETUP.md                       # Инструкция по запуску

media/
└── demo.mp4                       # Видеодемонстрация приложения
```

---

## Быстрый старт

Подробная инструкция с настройкой Firebase и API-ключей — в файле **[docs/SETUP.md](docs/SETUP.md)**.

```bash
git clone https://github.com/<ваш-логин>/Lango.git
cd Lango
# Положите google-services.json в папку app/
# Добавьте API-ключи в GroqApiService.kt и ImageApiService.kt
./gradlew assembleDebug
```

---

## Лицензия

Проект распространяется под лицензией **MIT** — подробнее в файле [LICENSE](LICENSE).

---

<div align="center">
Сделано с ❤️ в Алматы · META University · 2026
</div>
