# План автоматизации создания скриншотов

## Обзор

Документ описывает процесс настройки автоматизации создания локализованных скриншотов для публикации приложения "Счётчик дней" (Android) в магазины приложений (RuStore и Google Play Store).

**Примечание:** Подпись и публикация сборок в RuStore уже настроены через приватный репозиторий `android-secrets`. Команда `make release` создает подписанный AAB-файл для загрузки в магазины.

## Цели

- Настроить автоматическую генерацию локализованных скриншотов
- Подготовить скриншоты для RuStore и Google Play Store
- Подготовить метаданные для магазинов приложений
- Загрузка скриншотов в магазины выполняется вручную

---

## Часть 1: Автоматизация создания скриншотов с fastlane

### Обзор подхода

Для автоматизации создания скриншотов в Android-версии используется **fastlane screengrab** - готовый инструмент, аналогичный fastlane snapshot в iOS-версии.

**Ключевые преимущества fastlane screengrab:**

- Автоматическое переключение локалей (не нужно вручную менять язык устройства)
- Работает с Espresso (встроенный инструмент Android для UI тестов)
- Автоматическая загрузка скриншотов в Google Play Store через `fastlane supply`
- Правильный захват status bar, теней, диалогов (UI Automator)
- Конфигурация в одном файле (Screengrabfile)
- Единый подход с iOS-проектом (используется fastlane)

**Документация:** [fastlane Android Screenshots](https://docs.fastlane.tools/getting-started/android/screenshots/)

### Цели автоматизации

- Быстрая генерация локализованных скриншотов для RuStore и Google Play Store
- Автоматическое переключение локалей (русский, английский)
- Создание скриншотов на разных эмуляторах (телефон, планшет)
- Автоматическая загрузка в Google Play Store (опционально)

---

## Часть 1.1: Установка и настройка fastlane

### Необходимые действия

#### Шаг 1.1.1: Установить fastlane и screengrab ✅

**Реализовано:**

- fastlane 2.228.0 установлен через Gemfile (соответствует iOS-проекту)
- screengrab 2.1.1 установлен через Gradle (см. Шаг 1.1.2)

#### Шаг 1.1.2: Добавить зависимость screengrab в Gradle ✅

**Реализовано:**

- Зависимость `screengrab` 2.1.1 добавлена в `app/build.gradle.kts` через version catalog

- Версия указана в `gradle/libs.versions.toml`

#### Шаг 1.1.3: Настроить разрешения в AndroidManifest ✅

**Реализовано:**

- Создан файл `app/src/debug/AndroidManifest.xml`

- Добавлены разрешения для screengrab: DISABLE_KEYGUARD, WAKE_LOCK, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, CHANGE_CONFIGURATION

#### Шаг 1.1.4: Инициализировать fastlane в проекте ✅

**Реализовано:**

- fastlane инициализирован в проекте

- Созданы файлы: `fastlane/Appfile`, `fastlane/Fastfile`, `fastlane/README.md`

- В Gemfile добавлена версия fastlane `~> 2.228.0`

- Makefile содержит команду `make fastlane` для запуска меню fastlane

- `Screengrabfile` будет создан в шаге 1.3.1

### Критерии готовности

✅ Все шаги Части 1.1 и 1.2 выполнены.

---

## Часть 1.2: Настройка rbenv и обновление Makefile ✅

### Обзор

Для согласования с iOS-проектом настроено:

- rbenv для управления версиями Ruby
- Gemfile для Ruby-зависимостей fastlane
- .ruby-version для фиксирования версии Ruby (создаётся автоматически)
- Обёртки в Makefile для вызова fastlane через `bundle exec`
- Команда `make setup` для автоматической настройки всего окружения

### Реализовано

#### Шаг 1.2.1: Обновить Makefile ✅

**Добавлены переменные:**

```makefile
RUBY_VERSION=3.2.2
SHELL := /bin/bash
.ONESHELL:
BUNDLE_EXEC := RBENV_VERSION=$(RUBY_VERSION) bundle exec
```

**Обновлена команда `make setup`:**

- Автоматическая установка rbenv, Ruby 3.2.2, Bundler, fastlane, markdownlint

- Создание `.ruby-version`

- Выполнение `bundle install`

- Разбита на переиспользуемые части по аналогии с iOS-проектом

**Добавлены команды:**

- `make setup_fastlane` — инициализация fastlane

- `make update_fastlane` — обновление fastlane

- `make fastlane` — запуск меню fastlane

- `make screenshots` — генерация скриншотов (через `$(BUNDLE_EXEC)`)

#### Шаг 1.2.2: Обновить .gitignore ✅

Добавлены правила для Ruby и fastlane артефактов:

- `.rbenv-version`, `.ruby-gemset`, `*.gem`

- `vendor/bundle/`, `.bundle/`

- `fastlane/report.xml`, `fastlane/Preview.html`, `fastlane/test_output/`

**Примечание:** `.ruby-version` и скриншоты коммитятся.

### Критерии готовности

- ✅ Makefile обновлён с переменными `RUBY_VERSION` и `BUNDLE_EXEC`
- ✅ Команда `make setup` автоматически настраивает rbenv, Ruby, Bundler, fastlane и markdownlint
- ✅ Команда `make setup_fastlane` инициализирует fastlane
- ✅ Команда `make update_fastlane` обновляет fastlane
- ✅ Команда `make fastlane` запускает меню fastlane
- ✅ Файл `.gitignore` обновлён
- ✅ fastlane версии 2.228.0 установлен (соответует iOS-проекту)
- ✅ Bundler версии 2.6.5 установлен (соответует Gemfile.lock)
- ✅ Отсутствуют предупреждения multipart-post при запуске fastlane

### Аналогия с iOS-проектом

| iOS (SwiftUI-Days) | Android (JetpackDays) |
|-------------------|----------------------|
| `.ruby-version` с версией 3.2.2 | `.ruby-version` с версией 3.2.2 |
| `BUNDLE_EXEC := RBENV_VERSION=$(RUBY_VERSION) bundle exec` | `BUNDLE_EXEC := RBENV_VERSION=$(RUBY_VERSION) bundle exec` |
| `make setup` настраивает всё | `make setup` настраивает всё |
| `make setup_fastlane` инициализирует fastlane | `make setup_fastlane` инициализирует fastlane |
| `make update_fastlane` обновляет fastlane | `make update_fastlane` обновляет fastlane |
| `make screenshots` использует `$(BUNDLE_EXEC)` | `make screenshots` использует `$(BUNDLE_EXEC)` |

---

## ✅ Часть 1.3: Создание Screengrabfile (выполнено)

### Обзор

Screengrabfile - это конфигурационный файл для screengrab, который определяет:

- Пакет приложения
- Пакет тестов
- Локали для скриншотов
- Пути к сохраненным скриншотам

### Необходимые действия

#### Шаг 1.3.1: Создать Screengrabfile

**Файл:** `fastlane/Screengrabfile`

```ruby
# Имя пакета приложения
app_package_name 'com.dayscounter'

# Имя пакета тестов
tests_package_name 'com.dayscounter.test'

# Локали для скриншотов
locales ['ru-RU', 'en-US']

# Очистить предыдущие скриншоты перед генерацией
clear_previous_screenshots true

# Пакет с тестами для скриншотов (будет создан в части 1.4)
# UI-тесты для screengrab должны находиться в этом пакете
use_tests_in_packages ['com.dayscounter.screenshots']

# Путь к сгенерированным скриншотам
screengrab_path '../fastlane/metadata/android'
```

### Критерии готовности

- ✅ Screengrabfile создан
- ✅ Пакет приложения указан правильно (com.dayscounter)
- ✅ Пакет тестов указан правильно (com.dayscounter.test)
- ✅ Локали настроены (ru-RU, en-US)
- ✅ Путь к скриншотам настроен (fastlane/metadata/android)
- ✅ Пакет для тестов скриншотов указан (com.dayscounter.screenshots)

### Примечание

Параметр `use_tests_in_packages` указывает на пакет `com.dayscounter.screenshots`, который будет создан в **части 1.4** при написании UI тестов для screengrab. Это соответствует рекомендациям fastlane и обеспечивает правильную организацию тестов скриншотов.

---

## ✅ Часть 1.4: Написание UI тестов с Espresso и screengrab (выполнено)

### Обзор

UI тесты с Espresso автоматически выполняются screengrab для создания скриншотов на разных локалях.

**Основные компоненты:**

- `LocaleTestRule` - автоматическое переключение локалей
- `Screengrab.screenshot()` - захват скриншота
- Espresso UI тесты - навигация по приложению

**Сценарий теста (аналогично iOS-проекту, 5 скриншотов):**

1. **1-demoList** — демо-список записей на главном экране
2. **2-chooseDate** — выбор даты при создании новой записи
3. **3-chooseDisplayOption** — выбор displayOption (в Android это радио-кнопки, а не меню как в iOS)
4. **4-beforeSave** — перед сохранением новой записи
5. **5-sortByDate** — нажатая кнопка сортировки на главном экране после сохранения новой записи

### Необходимые действия

#### Шаг 1.4.1: Создать тестовый класс для скриншотов

**Файл:** `app/src/androidTest/java/com/dayscounter/ScreenshotsTest.kt`

```kotlin
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule
import com.dayscounter.MainActivity

@RunWith(AndroidJUnit4::class)
class ScreenshotsTest {

    @get:Rule
    val localeTestRule = LocaleTestRule()

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @Before
    fun setup() {
        // Загрузить демо-данные перед тестом
        // Это может быть отдельный метод в тестовом классе
    }

    @Test
    fun testScreenshots() {
        // Скриншот 1: Демо-список на главном экране
        Screengrab.screenshot("1-demoList")

        // Нажать кнопку добавления
        // Espresso.onView(withId(R.id.fab)).perform(click())

        // Ввести название
        // Espresso.onView(withId(R.id.title_input)).perform(typeText("Название события"))

        // Выбрать дату
        // Espresso.onView(withId(R.id.date_button)).perform(click())

        // Скриншот 2: Выбор даты при создании новой записи
        Screengrab.screenshot("2-chooseDate")

        // Закрыть диалог выбора даты
        // Espresso.onView(withId(R.id.confirm_button)).perform(click())

        // Выбрать displayOption (радио-кнопки)
        // Espresso.onView(withId(R.id.display_option_radio_group))
        //     .check(matches(isDisplayed()))
        //     .perform(click())

        // Скриншот 3: Выбор displayOption (радио-кнопки)
        Screengrab.screenshot("3-chooseDisplayOption")

        // Выбрать конкретный displayOption
        // Espresso.onView(withId(R.id.display_option_day)).perform(click())

        // Скриншот 4: Перед сохранением новой записи
        Screengrab.screenshot("4-beforeSave")

        // Сохранить
        // Espresso.onView(withId(R.id.save_button)).perform(click())

        // Нажать кнопку сортировки
        // Espresso.onView(withId(R.id.sort_nav_button)).perform(click())

        // Скриншот 5: Нажатая  кнопка сортировки нажата на главном экране после сохранения
        Screengrab.screenshot("5-sortByDate")
    }
}
```

#### Шаг 1.4.2: Оптимизировать стратегию захвата

Для улучшения качества скриншотов (тени, тени Material UI, диалоги) использовать UI Automator:

```kotlin
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

@RunWith(AndroidJUnit4::class)
class ScreenshotsTest {

    @get:Rule
    val localeTestRule = LocaleTestRule()

    @Before
    fun setup() {
        // Использовать UI Automator для лучшего качества
        Screengrab.setDefaultScreenshotStrategy(
            UiAutomatorScreenshotStrategy()
        )
    }
}
```

**Примечание:** UI Automator требует API >= 18.

### Критерии готовности

- ✅ Тестовый класс ScreenshotsTest создан
- ✅ LocaleTestRule добавлен для автоматического переключения локалей
- ✅ Espresso тесты реализованы (5 скриншотов)
- ✅ Скриншот 1 (1-demoList): демо-список на главном экране
- ✅ Скриншот 2 (2-chooseDate): выбор даты при создании новой записи
- ✅ Скриншот 3 (3-chooseDisplayOption): выбор displayOption (радио-кнопки)
- ✅ Скриншот 4 (4-beforeSave): перед сохранением новой записи
- ✅ Скриншот 5 (5-sortByDate): нажатая кнопка сортировки на главном экране после сохранения
- ✅ Screengrab.screenshot() вызывается на каждом шаге
- ⏸️ UI Automator стратегия включена (опционально - не включена, использует стандартную стратегию)

### Реализация

Создан тестовый класс `ScreenshotsTest.kt` в пакете `com.dayscounter.screenshots`:

**Файл:** `app/src/androidTest/java/com/dayscounter/screenshots/ScreenshotsTest.kt`

**Особенности реализации:**

1. **Пакет тестов:** `com.dayscounter.screenshots` (соответствует Screengrabfile)
2. **Правила теста:**
   - `createAndroidComposeRule<MainActivity>()` - для доступа к Compose UI
   - `LocaleTestRule()` - автоматическое переключение локалей (ru-RU, en-US)
3. **Демо-данные:** Загружаются 3 тестовые записи перед тестом для демонстрации разных вариантов отображения
4. **5 скриншотов:** Реализованы все сценарии из плана
5. **Очистка данных:** База данных очищается до и после теста для идентичности результатов

**Команда для запуска:**

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dayscounter.screenshots.ScreenshotsTest
```

---

## ✅ Часть 1.5: Создание lanes в Fastfile (выполнено)

### Обзор

Fastfile содержит lanes (команды) для автоматизации процессов. Создадим lanes для генерации и загрузки скриншотов.

### Необходимые действия

#### Шаг 1.5.1: Создать lane для генерации скриншотов

**Файл:** `fastlane/Fastfile`

```ruby
# Создание скриншотов для всех локалей
lane :screenshots do
  capture_android_screenshots(
    locales: ['ru-RU', 'en-US'],
    clear_previous_screenshots: true
  )
end

# Создание скриншотов только для русского
lane :screenshots_ru do
  capture_android_screenshots(
    locales: ['ru-RU'],
    clear_previous_screenshots: true
  )
end

# Создание скриншотов только для английского
lane :screenshots_en do
  capture_android_screenshots(
    locales: ['en-US'],
    clear_previous_screenshots: true
  )
end
```

#### Шаг 1.5.2: Добавить команды в Makefile

**Файл:** `Makefile`

```makefile
## screenshots: Генерировать скриншоты для всех локалей через fastlane
screenshots:
    @echo "$(YELLOW)Генерирую скриншоты через fastlane...$(RESET)"
    fastlane screenshots
    @echo "$(GREEN)Скриншоты готовы: fastlane/metadata/android$(RESET)"

## screenshots-ru: Генерировать скриншоты только на русском
screenshots-ru:
    @echo "$(YELLOW)Генерирую скриншоты (русский)...$(RESET)"
    fastlane screenshots_ru
    @echo "$(GREEN)Скриншоты готовы: fastlane/metadata/android$(RESET)"

## screenshots-en: Генерировать скриншоты только на английском
screenshots-en:
    @echo "$(YELLOW)Генерирую скриншоты (английский)...$(RESET)"
    fastlane screenshots_en
    @echo "$(GREEN)Скриншоты готовы: fastlane/metadata/android$(RESET)"
```

### Критерии готовности

- ✅ Lanes в Fastfile созданы (screenshots, screenshots_ru, screenshots_en)
- ✅ Команды в Makefile добавлены
- ✅ Локали настроены правильно (ru-RU, en-US)
- ✅ Генерирует 5 скриншотов по сценарию iOS-проекта

### Реализация

**Fastfile добавлены lanes:**

```ruby
# Создание скриншотов для всех локалей
lane :screenshots do
  capture_android_screenshots(
    locales: ['ru-RU', 'en-US'],
    clear_previous_screenshots: true
  )
end

# Создание скриншотов только для русского
lane :screenshots_ru do
  capture_android_screenshots(
    locales: ['ru-RU'],
    clear_previous_screenshots: true
  )
end

# Создание скриншотов только для английского
lane :screenshots_en do
  capture_android_screenshots(
    locales: ['en-US'],
    clear_previous_screenshots: true
  )
end
```

**Makefile добавлены команды:**

- `make screenshots` - генерировать скриншоты для всех локалей (ru-RU, en-US)
- `make screenshots-ru` - генерировать скриншоты только для русского
- `make screenshots-en` - генерировать скриншоты только для английского

Все команды автоматически проверяют наличие fastlane и загружают скриншоты в `fastlane/metadata/android`.

---

## Часть 1.6: Интеграция с RuStore

### Обзор

После генерации скриншотов через fastlane их можно использовать для RuStore.

### Необходимые действия

#### Шаг 1.6.1: Подготовить скриншоты для RuStore

**Где находятся скриншоты:** `fastlane/metadata/android/`

**Структура:**

```
fastlane/metadata/android/
  ru-RU/
    phoneScreenshots/
      1-demoList.png
      2-chooseDate.png
      3-chooseDisplayOption.png
      4-beforeSave.png
      5-sortByDate.png
  en-US/
    phoneScreenshots/
      1-demoList.png
      2-chooseDate.png
      3-chooseDisplayOption.png
      4-beforeSave.png
      5-sortByDate.png
```

**Для RuStore нужно:**

1. Скопировать скриншоты из `fastlane/metadata/android/ru-RU/phoneScreenshots/` в папку для загрузки в RuStore
2. Скопировать скриншоты из `fastlane/metadata/android/en-US/phoneScreenshots/` в папку для RuStore (английская версия)

#### Шаг 1.6.2: Создать команду для RuStore

**Файл:** `Makefile`

```makefile
## prepare-rustore-screenshots: Подготовить скриншоты для RuStore
prepare-rustore-screenshots:
    @echo "$(YELLOW)Подготавливаю скриншоты для RuStore...$(RESET)"
    @mkdir -p build/rustore-screenshots/ru
    @mkdir -p build/rustore-screenshots/en
    @cp fastlane/metadata/android/ru-RU/phoneScreenshots/*.png build/rustore-screenshots/ru/
    @cp fastlane/metadata/android/en-US/phoneScreenshots/*.png build/rustore-screenshots/en/
    @echo "$(GREEN)Скриншоты готовы: build/rustore-screenshots/$(RESET)"
    @echo "$(YELLOW)Загрузите скриншоты в RuStore Консоль$(RESET)"
```

### Критерии готовности

- ⏳ Скриншоты сгенерированы через fastlane
- ⏳ Скриншоты подготовлены для RuStore
- ⏳ Команда prepare-rustore-screenshots работает

---

## Резюме автоматизации скриншотов с fastlane

### Преимущества использования fastlane screengrab

1. **Единый подход с iOS** - используется тот же инструмент (fastlane)
2. **Автоматическое переключение локалей** - не нужно вручную менять язык
3. **Лучшее качество** - UI Automator правильно захватывает тени и Material UI
4. **Автоматическая загрузка в Google Play** - через `fastlane supply`
5. **Меньше кода** - конфигурация в одном файле, не нужны shell-скрипты

### Порядок реализации

1. ✅ Установить и настроить fastlane (Часть 1.1)
2. ✅ Настроить rbenv и обновить Makefile (Часть 1.2)
3. Создать Screengrabfile (Часть 1.3)
4. Написать UI тесты с Espresso (Часть 1.4)
5. Создать lanes в Fastfile (Часть 1.5)
6. Интеграция с RuStore (Часть 1.6)

### Использование

```bash
# Все локали
make screenshots
# Только русский
make screenshots-ru
# Только английский
make screenshots-en
# Подготовка для RuStore
make prepare-rustore-screenshots
```

---

## Часть 2: Подготовка метаданных для магазинов

### Шаг 2.1: Подготовить метаданные для RuStore

Создать файлы описания для RuStore:

- Название (локализованное)
- Краткое описание (до 80 символов)
- Полное описание
- Ключевые слова
- Категория
- Возрастной рейтинг

**Скриншоты для RuStore:**

Используйте команду `make prepare-rustore-screenshots` для подготовки скриншотов из `fastlane/metadata/android/ru-RU/phoneScreenshots/` и `en-US/phoneScreenshots/`.

### Шаг 2.2: Подготовить метаданные для Google Play Store

#### Структура папок метаданных

```
fastlane/metadata/android/
  en-US/
      title.txt
      short_description.txt
      full_description.txt
      phoneScreenshots/
      featureGraphic.png
      icon.png
  ru-RU/
      title.txt
      short_description.txt
      full_description.txt
      phoneScreenshots/
      featureGraphic.png
      icon.png
```

**Содержимое файлов:**

`en-US/title.txt`: `Days Counter`
`ru-RU/title.txt`: `Счётчик дней`

`en-US/short_description.txt`: `Track important dates and events with Days Counter.`
`ru-RU/short_description.txt`: `Отслеживайте важные даты и события с приложением «Счётчик дней».`

`en-US/full_description.txt`:

```
Track important dates and events with Days Counter.

Features:
• Add unlimited events
• Track days since or until events
• Color-coded tags
• Dark mode support
• Localized in Russian and English
• Offline mode - no internet required

Perfect for tracking:
• Anniversary dates
• Important deadlines
• Personal milestones
• And much more!

Days Counter helps you stay organized and never forget important dates.
```

`ru-RU/full_description.txt`:

```
Отслеживайте важные даты и события с приложением «Счётчик дней».

Возможности:
• Добавляйте неограниченное количество событий
• Отслеживайте дни с или до события
• Цветные метки для категоризации
• Темная тема
• Локализация на русском и английском языках
• Офлайн-режим - не требуется интернет

Идеально подходит для отслеживания:
• Годовщин
• Важных сроков
• Личных достижений
• И многого другого!

«Счётчик дней» поможет вам быть организованным и никогда не забывать важные даты.
```

#### Автоматическая загрузка через fastlane supply

```bash
# Загрузить только скриншоты
make upload-screenshots

# Или загрузить скриншоты и метаданные (кроме APK/AAB)
fastlane supply
```

**Примечание:** Для работы `fastlane supply` нужно настроить сервисный аккаунт Google Play (см. документацию fastlane).

### Критерии готовности

- ⏳ Метаданные подготовлены для RuStore
- ⏳ Скриншоты сгенерированы через fastlane (`make screenshots`)
- ⏳ Скриншоты подготовлены для RuStore (`make prepare-rustore-screenshots`)
- ⏳ Структура папок метаданных для Google Play Store создана в `fastlane/metadata/android/`
- ⏳ Файлы описания и заголовков заполнены для всех локалей
- ⏳ Иконки и feature graphic добавлены (опционально)

---

## Порядок выполнения

### Приоритет 1: Автоматизация скриншотов с fastlane (важно)

1. Установить fastlane и screengrab (Часть 1.1) ✅
2. Настроить rbenv и обновить Makefile (Часть 1.2) ✅
3. Создать Screengrabfile (Часть 1.3)
4. Написать UI тесты с Espresso (Часть 1.4)
5. Создать lanes в Fastfile (Часть 1.5)
6. Интеграция с RuStore (Часть 1.6)
6. Сгенерировать скриншоты для магазинов приложений (`make screenshots`)

### Приоритет 2: Подготовка метаданных (важно)

7. Подготовить метаданные для RuStore и Google Play Store (Часть 2)

### Приоритет 3: Публикация (обязательно)

8. Создать AAB-файл командой `make release`
9. Загрузить AAB и метаданные в RuStore
10. Загрузить AAB и метаданные в Google Play Store

---

## Критерии завершения

### Обязательные критерии

**Выполнено (Части 1.1 и 1.2):**

- ✅ fastlane 2.228.0, screengrab 2.1.1, Ruby 3.2.2, Bundler 2.6.5 установлены

- ✅ Makefile обновлён с rbenv интеграцией и командами setup/setup_fastlane/update_fastlane/fastlane/screenshots

- ✅ .gitignore обновлён для Ruby и fastlane артефактов

**Осталось выполнить:**

- ⏳ Screengrabfile создан с конфигурацией

- ⏳ UI тесты с Espresso написаны

- ⏳ Lanes в Fastfile созданы

- ⏳ Скриншоты сгенерированы для обоих магазинов

- ⏳ Метаданные подготовлены для обоих магазинов

### Результат

После завершения настройки можно:

- Публиковать приложение в RuStore и Google Play Store
- Создавать подписанный AAB-файл командой `make release` (файл `dayscounter{VERSION_CODE}.aab`)
- Генерировать локализованные скриншоты командой `make screenshots`
- Подготавливать скриншоты для RuStore командой `make prepare-rustore-screenshots`
- Загружать скриншоты в магазины вручную (RuStore Консоль, Google Play Console)

---

## Документация

После завершения настройки обновить документацию:

1. **README.md**: Добавить инструкции по публикации
2. **CONTRIBUTING.md**: Обновить process для релизов
3. **Makefile**: Добавить help для новых команд
4. **Этот документ**: Отметить завершенные шаги
