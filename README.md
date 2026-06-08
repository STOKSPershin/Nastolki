# 🎮 TB Games — Мобильное игровое лобби

> Android-приложение — мультиплеерная платформа для настольных и казуальных игр с системой лобби, комнат и real-time взаимодействием между игроками.

---

## 📋 Оглавление

- [Обзор проекта](#-обзор-проекта)
- [Скриншоты / Флоу](#-скриншоты--флоу)
- [Архитектура](#-архитектура)
- [Технологический стек](#-технологический-стек)
- [Структура проекта](#-структура-проекта)
- [Настройка и запуск](#-настройка-и-запуск)
- [Функционал](#-функционал)
- [База данных (Supabase)](#-база-данных-supabase)
- [Навигация](#-навигация)
- [Дизайн-система](#-дизайн-система)
- [Разработка](#-разработка)
- [Планы](#-планы)

---

## 🎯 Обзор проекта

**TB Games** — это Android-приложение, которое служит платформой (лобби) для подключения игроков к различным играм. Пользователь устанавливает приложение, выбирает никнейм и аватарку, после чего попадает в лобби, где видит других онлайн-игроков и может создать/присоединиться к игровой комнате.

### Основные концепции

| Концепция | Описание |
|---|---|
| **Лобби** | Центральная «площадка», где видны все онлайн-игроки и доступные комнаты |
| **Игровая комната** | Предигровое пространство — хост создаёт комнату, игроки присоединяются, ставят статус «Готов» |
| **Профиль** | Никнейм + аватарка (из пресетов-животных или своё фото) + статистика |
| **Игры** | Подключаемые мини-игры, запускающиеся из комнаты когда все готовы |

---

## 📱 Скриншоты / Флоу

```
┌─────────────┐     ┌──────────────┐     ┌───────────────┐     ┌─────────────┐
│   Splash    │────▸│   Никнейм    │────▸│    Аватар     │────▸│    Лобби    │
│   Screen    │     │   Screen     │     │   Selection   │     │   Screen    │
└─────────────┘     └──────────────┘     └───────────────┘     └──────┬──────┘
                                                                      │
                                                        ┌─────────────┼─────────────┐
                                                        ▼             ▼             ▼
                                                   ┌─────────┐  ┌──────────┐  ┌──────────┐
                                                   │ Игроки  │  │ Комнаты  │  │ Профиль  │
                                                   │  (таб)  │  │  (таб)   │  │  (таб)   │
                                                   └─────────┘  └────┬─────┘  └──────────┘
                                                                     ▼
                                                               ┌──────────┐
                                                               │ Комната  │
                                                               │  (игра)  │
                                                               └──────────┘
```

---

## 🏗 Архитектура

Проект построен на **Clean Architecture + MVVM** с чётким разделением слоёв:

```
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                    │
│  Composable Screens  ←→  ViewModels  ←→  UI State      │
├────────────────────────────────────────────────────────┤
│                     Domain Layer                        │
│              Models (data classes)                      │
├────────────────────────────────────────────────────────┤
│                      Data Layer                         │
│  Repositories  ←→  Supabase Client  ←→  DataStore      │
└────────────────────────────────────────────────────────┘
```

### Принципы

- **Однонаправленный поток данных (UDF)**: Screen → ViewModel (events) → Repository → ViewModel (state) → Screen
- **Состояние через `StateFlow`**: Каждый ViewModel хранит `UiState` в `MutableStateFlow`
- **DI через Hilt**: Все зависимости инжектируются автоматически
- **Навигация**: Jetpack Navigation Compose с типизированными маршрутами

---

## 🛠 Технологический стек

| Категория | Технология | Версия |
|---|---|---|
| **Язык** | Kotlin | 2.1.0 |
| **UI** | Jetpack Compose (Material 3) | BOM 2025.01.00 |
| **Навигация** | Navigation Compose | 2.8.5 |
| **DI** | Hilt (Dagger) | 2.56 |
| **Бэкенд** | Supabase | BOM 3.6.0 |
| **Аутентификация** | Supabase Auth (анонимная) | — |
| **БД** | Supabase Postgrest (PostgreSQL) | — |
| **Realtime** | Supabase Realtime (Presence) | — |
| **Хранилище** | Supabase Storage | — |
| **Сеть** | Ktor Client (OkHttp) | 3.0.3 |
| **Сериализация** | Kotlinx Serialization | 1.7.3 |
| **Изображения** | Coil 3 Compose | 3.1.0 |
| **Кроппер фото** | CanHub Image Cropper | 4.6.0 |
| **Локальное хранилище** | DataStore Preferences | 1.1.2 |
| **Сборка** | Gradle (Kotlin DSL) + Version Catalog | 8.11.1 |
| **Мин. Android** | API 26 (Android 8.0) | — |
| **Целевой Android** | API 35 (Android 15) | — |

---

## 📂 Структура проекта

```
TB Games/
├── app/
│   ├── build.gradle.kts                    # Конфигурация модуля app
│   └── src/main/
│       ├── AndroidManifest.xml             # Разрешения и компоненты
│       ├── res/
│       │   ├── values/
│       │   │   ├── strings.xml             # Строки (русский)
│       │   │   └── themes.xml              # XML-тема для splash
│       │   └── values-night/
│       │       └── themes.xml              # Тёмная XML-тема
│       └── java/com/tbgames/app/
│           ├── MainActivity.kt             # Точка входа, Compose host
│           ├── TBGamesApplication.kt       # Hilt Application
│           │
│           ├── core/                       # 🟦 Общий модуль
│           │   ├── common/
│           │   │   ├── AppResult.kt        # Sealed Result<T> для ошибок
│           │   │   └── Constants.kt        # Константы приложения
│           │   ├── data/
│           │   │   └── PreferencesManager.kt   # DataStore настройки
│           │   ├── domain/model/
│           │   │   ├── PlayerProfile.kt    # Модель игрока
│           │   │   ├── GameRoom.kt         # Модель комнаты
│           │   │   ├── RoomPlayer.kt       # Игрок в комнате
│           │   │   └── OnlinePlayer.kt     # Presence-данные
│           │   ├── network/
│           │   │   └── SupabaseModule.kt   # Hilt DI для Supabase
│           │   └── ui/
│           │       ├── components/
│           │       │   ├── AvatarCircle.kt     # Аватар (пресет/фото)
│           │       │   ├── PlayerCard.kt       # Карточка игрока
│           │       │   ├── RoomCard.kt         # Карточка комнаты
│           │       │   └── LoadingButton.kt    # Кнопка с лоадером
│           │       └── theme/
│           │           ├── Color.kt        # Цветовая палитра
│           │           ├── Typography.kt   # Типографика
│           │           └── Theme.kt        # Material 3 тема
│           │
│           ├── feature/                    # 🟩 Фичи
│           │   ├── onboarding/
│           │   │   ├── data/
│           │   │   │   ├── AuthRepository.kt       # Анонимная аутентификация
│           │   │   │   └── ProfileRepository.kt    # CRUD профиля
│           │   │   └── presentation/
│           │   │       ├── SplashScreen.kt          # Сплэш с анимацией
│           │   │       ├── NicknameScreen.kt        # Ввод никнейма
│           │   │       ├── AvatarSelectionScreen.kt # Выбор аватара
│           │   │       └── OnboardingViewModel.kt   # VM онбординга
│           │   │
│           │   ├── lobby/
│           │   │   ├── data/
│           │   │   │   ├── PresenceRepository.kt   # Realtime Presence
│           │   │   │   └── RoomRepository.kt       # CRUD комнат
│           │   │   └── presentation/
│           │   │       ├── LobbyScreen.kt           # Главный экран
│           │   │       └── LobbyViewModel.kt        # VM лобби
│           │   │
│           │   └── profile/
│           │       ├── data/
│           │       │   └── AvatarStorageRepository.kt  # Загрузка фото
│           │       └── presentation/
│           │           ├── ProfileSettingsScreen.kt  # Экран настроек
│           │           └── ProfileViewModel.kt       # VM профиля
│           │
│           └── navigation/
│               └── TBGamesNavGraph.kt      # Граф навигации
│
├── gradle/
│   ├── libs.versions.toml                  # Version Catalog (все зависимости)
│   └── wrapper/
│       └── gradle-wrapper.properties       # Gradle 8.11.1
│
├── build.gradle.kts                        # Корневой build файл
├── settings.gradle.kts                     # Настройки проекта
├── gradle.properties                       # Gradle конфигурация
├── local.properties                        # SDK path + Supabase ключи
└── README.md                               # ← вы здесь
```

---

## 🚀 Настройка и запуск

### Предварительные требования

- **Android Studio** Quail 1 или новее
- **JDK 17+**
- **Android SDK** (API 35)
- Аккаунт **Supabase** (бесплатный)

### 1. Клонирование

```bash
git clone https://github.com/<your-username>/tb-games.git
cd tb-games
```

### 2. Настройка Supabase

1. Создайте проект на [supabase.com](https://supabase.com)
2. Скопируйте **Project URL** и **anon key** из Settings → API
3. Откройте `local.properties` и заполните:

```properties
sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
SUPABASE_URL=https://<your-project>.supabase.co
SUPABASE_ANON_KEY=eyJ...ваш_ключ
```

> ⚠️ **`local.properties` не коммитится в Git** — он в `.gitignore`

### 3. Создание таблиц в Supabase

Выполните SQL в SQL Editor Supabase:

```sql
-- Таблица профилей
CREATE TABLE profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    nickname TEXT UNIQUE NOT NULL,
    avatar_type TEXT DEFAULT 'preset',
    avatar_preset_id INTEGER DEFAULT 1,
    avatar_url TEXT,
    status_text TEXT,
    is_online BOOLEAN DEFAULT false,
    current_room_id UUID,
    total_wins INTEGER DEFAULT 0,
    total_losses INTEGER DEFAULT 0,
    total_games INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Таблица игровых комнат
CREATE TABLE game_rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    game_type TEXT DEFAULT 'default',
    host_id UUID REFERENCES profiles(id),
    status TEXT DEFAULT 'waiting',
    max_players INTEGER DEFAULT 4,
    current_players INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Игроки в комнате
CREATE TABLE room_players (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID REFERENCES game_rooms(id) ON DELETE CASCADE,
    player_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    is_ready BOOLEAN DEFAULT false,
    joined_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(room_id, player_id)
);

-- RLS политики
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE game_rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE room_players ENABLE ROW LEVEL SECURITY;

-- Профили: чтение всем, запись только себе
CREATE POLICY "Profiles: read all" ON profiles FOR SELECT USING (true);
CREATE POLICY "Profiles: insert own" ON profiles FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "Profiles: update own" ON profiles FOR UPDATE USING (auth.uid() = id);

-- Комнаты: чтение всем, создание авторизованным
CREATE POLICY "Rooms: read all" ON game_rooms FOR SELECT USING (true);
CREATE POLICY "Rooms: insert auth" ON game_rooms FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);
CREATE POLICY "Rooms: update auth" ON game_rooms FOR UPDATE USING (auth.uid() IS NOT NULL);

-- Игроки в комнатах
CREATE POLICY "RoomPlayers: read all" ON room_players FOR SELECT USING (true);
CREATE POLICY "RoomPlayers: insert own" ON room_players FOR INSERT WITH CHECK (auth.uid() = player_id);
CREATE POLICY "RoomPlayers: update own" ON room_players FOR UPDATE USING (auth.uid() = player_id);
CREATE POLICY "RoomPlayers: delete own" ON room_players FOR DELETE USING (auth.uid() = player_id);

-- Storage bucket для аватарок
INSERT INTO storage.buckets (id, name, public) VALUES ('avatars', 'avatars', true);
```

### 4. Включение анонимной авторизации

В Supabase Dashboard:
1. **Authentication** → **Providers**
2. Включите **Anonymous Sign-Ins** (Allow anonymous sign-ins)

### 5. Сборка и запуск

```bash
# Синхронизация зависимостей
./gradlew --refresh-dependencies

# Сборка debug APK
./gradlew assembleDebug

# Или просто нажмите ▶️ Run в Android Studio
```

---

## ✨ Функционал

### 🔐 Онбординг (первый запуск)

| Шаг | Описание |
|---|---|
| **Splash Screen** | Анимированный экран загрузки с лого. Проверяет, есть ли сохранённая сессия |
| **Никнейм** | Ввод имени (3-16 символов: буквы RU/EN, цифры, `_`). Проверка уникальности |
| **Аватар** | Сетка из 20 пресетных аватаров-животных (🐶🐱🐻🦊🐼 и т.д.) или загрузка фото |
| **Авторизация** | Автоматическая анонимная регистрация через Supabase Auth |

### 🏠 Лобби

Три вкладки через Bottom Navigation:

| Вкладка | Описание |
|---|---|
| **👥 Игроки** | Список онлайн-игроков через Supabase Realtime Presence. Показывает аватар, ник и статус (в лобби / в комнате / в игре) |
| **🚪 Комнаты** | Список активных игровых комнат с количеством игроков и кнопкой «Войти». FAB для создания новой комнаты |
| **👤 Профиль** | Переход в настройки профиля |

### ⚙️ Настройки профиля

- Смена никнейма (inline-редактирование)
- Смена аватара (выбор из пресетов)
- Статистика: побед / поражений / всего игр
- Переключатели: звук, вибрация
- Выбор темы: светлая / тёмная / системная

### 🎲 Игровая комната (в разработке)

- Список игроков в комнате
- Статус «Готов» / «Не готов» для каждого
- Хост может запустить игру когда все готовы

---

## 🗄 База данных (Supabase)

### Схема таблиц

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   profiles   │     │  game_rooms  │     │ room_players │
├──────────────┤     ├──────────────┤     ├──────────────┤
│ id (PK, FK)  │◄────│ host_id (FK) │     │ id (PK)      │
│ nickname     │     │ id (PK)      │◄────│ room_id (FK) │
│ avatar_type  │     │ name         │     │ player_id(FK)│──►│profiles│
│ avatar_*     │     │ game_type    │     │ is_ready     │
│ is_online    │     │ status       │     │ joined_at    │
│ total_*      │     │ max_players  │     └──────────────┘
│ created_at   │     │ current_pl.  │
└──────────────┘     │ created_at   │
                     └──────────────┘
```

### Realtime Presence

Для отслеживания онлайн-игроков используется **Supabase Realtime Presence**:

```kotlin
// Игрок присоединяется к каналу "lobby"
channel.subscribe()
channel.track(mapOf(
    "user_id" to playerId,
    "nickname" to nickname,
    "avatar_type" to avatarType,
    "status" to "in_lobby"
))
```

Presence автоматически убирает игрока из списка при отключении.

### Row Level Security (RLS)

Все таблицы защищены RLS-политиками:
- **Чтение**: доступно всем авторизованным
- **Запись**: только свои данные (`auth.uid() = id`)
- **Удаление**: только свои записи в `room_players`

---

## 🧭 Навигация

```kotlin
// Маршруты (Routes)
object Routes {
    const val SPLASH   = "splash"    // Стартовый экран
    const val NICKNAME = "nickname"  // Ввод никнейма
    const val AVATAR   = "avatar"    // Выбор аватара
    const val LOBBY    = "lobby"     // Главный экран
    const val PROFILE  = "profile"   // Настройки
}
```

| Откуда | Куда | Условие |
|---|---|---|
| Splash | Lobby | Есть сохранённая сессия и профиль |
| Splash | Nickname | Нет сессии |
| Nickname | Avatar | Никнейм валиден |
| Avatar | Lobby | Профиль успешно создан |
| Lobby | Profile | Нажатие на ⚙️ или вкладку «Профиль» |
| Profile | Lobby | Кнопка «Назад» |

---

## 🎨 Дизайн-система

### Цветовая палитра

| Роль | Light | Dark | Применение |
|---|---|---|---|
| **Primary** | `#4B5EAA` (индиго) | `#B9C3FF` | Кнопки, акценты |
| **Secondary** | `#3C6373` (тил) | `#A4CDDF` | Второстепенные элементы |
| **Tertiary** | `#7B5733` (амбер) | `#EBBF94` | Дополнительные акценты |
| **Surface** | `#FBF8FF` | `#121318` | Фон |
| **Error** | `#BA1A1A` | `#FFB4AB` | Ошибки |

### Статусные цвета

| Статус | Цвет | Hex |
|---|---|---|
| 🟢 Онлайн | Зелёный | `#4CAF50` |
| ⚪ Офлайн | Серый | `#9E9E9E` |
| 🟠 В игре | Оранжевый | `#FF9800` |
| 🔵 В комнате | Синий | `#2196F3` |

### Типографика

Используется системный шрифт с настроенными весами:

| Стиль | Размер | Вес | Применение |
|---|---|---|---|
| `displayLarge` | 36sp | Bold | Лого на splash |
| `headlineLarge` | 28sp | Bold | Заголовки экранов |
| `titleMedium` | 16sp | Medium | Никнеймы, названия |
| `bodyLarge` | 16sp | Normal | Основной текст |
| `bodySmall` | 12sp | Normal | Статусы, подсказки |
| `labelLarge` | 14sp | Medium | Кнопки |

### Компоненты

| Компонент | Описание |
|---|---|
| `AvatarCircle` | Круглый аватар (пресет с эмодзи животного ИЛИ фото через Coil). Опциональная точка онлайн-статуса |
| `PlayerCard` | Карточка игрока в списке: аватар + ник + статус |
| `RoomCard` | Карточка комнаты: название + счётчик игроков + кнопка «Войти» |
| `LoadingButton` | Кнопка с встроенным `CircularProgressIndicator` |

---

## 👨‍💻 Разработка

### Добавление новой игры

1. Создайте пакет `feature/<game_name>/`
2. Добавьте `game_type` в `Constants.kt`
3. Реализуйте экран игры и ViewModel
4. Добавьте маршрут в `TBGamesNavGraph.kt`
5. Из `GameRoom` с соответствующим `gameType` запускайте экран

### Добавление новых аватаров

1. Добавьте изображения в `res/drawable/` (формат: `avatar_animal_XX.png`)
2. Обновите `PRESET_AVATARS_COUNT` в `Constants.kt`
3. Обновите массивы `colors` и `animals` в `AvatarCircle.kt`

### Переменные окружения

Чувствительные данные хранятся в `local.properties` и попадают в `BuildConfig`:

```kotlin
// Доступно в коде через:
BuildConfig.SUPABASE_URL
BuildConfig.SUPABASE_ANON_KEY
```

### Полезные команды

```bash
# Сборка debug APK
./gradlew assembleDebug

# Сборка release APK
./gradlew assembleRelease

# Запуск линтера
./gradlew lint

# Очистка сборки
./gradlew clean
```

---

## 📅 Планы

### Ближайшие (v0.2)
- [ ] Экран игровой комнаты (Ready/Not Ready)
- [ ] Первая игра (механика TBD)
- [ ] Push-уведомления при приглашении в комнату
- [ ] Загрузка кастомного фото аватара (Camera + Crop)

### Среднесрочные (v0.3)
- [ ] Чат в комнате
- [ ] Система приглашений (ссылки)
- [ ] Таблица лидеров
- [ ] Звуковые эффекты

### Долгосрочные (v1.0)
- [ ] Несколько игр на выбор
- [ ] Рейтинговая система
- [ ] Достижения и награды
- [ ] Локализация (EN)

---

## 📄 Лицензия

Этот проект является приватным и не предназначен для публичного распространения.

---

<p align="center">
  <b>TB Games</b> · Сделано с ❤️ · 2025-2026
</p>
