# План публикации приложения в магазины приложений

## Обзор

Документ описывает процесс настройки и автоматизации публикации приложения "Счётчик дней" (Android) в магазины приложений.

## Порядок публикации

1. **RuStore** - российский магазин приложений (первичная публикация)
2. **Google Play Store** - международный магазин приложений

## Цели

1. Настроить локализованное название приложения для отображения на устройствах пользователей
2. Настроить создание сборок для публикации в магазинах приложений
3. Подготовить скриншоты и метаданные для магазинов приложений

## Форматы сборок

- **AAB (Android App Bundle)** - формат для RuStore и Google Play Store (актуальный)
  - RuStore поддерживает загрузку AAB-файлов (до 5 Гб)
  - Google Play Store требует AAB-формат
  - RuStore формирует APK из AAB и подписывает их самостоятельно
- **APK** - опциональный формат для альтернативных магазинов (не нужен для RuStore и Google Play)

## Требования RuStore для AAB

- Размер файла AAB - не более 5 Гб
- Размер каждого APK-файла, сформированного из AAB - не более 5 Гб
- Java версии 11 или выше для работы с ключами подписи
- Ключ подписи приложения: RSA размером не менее 2048 бит

## Процесс подписи AAB для RuStore

RuStore использует двухуровневую систему подписи:

1. **Ключ подписи приложения (App Signing Key)** - используется RuStore для подписи APK-файлов
2. **Ключ загрузки (Upload Key)** - используется разработчиком для подписи AAB-файла

Процесс:

- Разработчик подписывает файл AAB ключом загрузки и загружает сборку в RuStore
- RuStore подтверждает личность разработчика с помощью сертификата ключа загрузки
- RuStore формирует набор APK-файлов из AAB-файла и подписывает каждый APK ключом подписи приложения
- Пользователи загружают из RuStore подписанные APK-файлы

---

## Часть 1: Настройка названия приложения ✅

Выполнено. Созданы локализованные строковые ресурсы `app_display_name` (ru, en), обновлен `AndroidManifest.xml`.

---

## Часть 2: Подготовка к публикации в магазинах приложений ✅

### Шаг 2.1: Настройка подписания AAB ✅

Выполнено через приватный репозиторий `android-secrets`. Используется упрощенный подход с одним ключом подписи `upload` (используется для подписи AAB и RuStore для APK). Keystore, сертификаты (ZIP и PEM) загружены в RuStore Консоль, статус подписи - "Активна".

### Критерии готовности

- ✅ Приватный репозиторий `android-secrets` создан на GitHub
- ✅ Структура папок для JetpackDays, README.md, секреты (keystore, сертификаты, secrets.properties) подготовлены
- ✅ `build.gradle.kts` настроен для чтения секретов из `.secrets/secrets.properties`
- ✅ Папка `.secrets/` добавлена в `.gitignore`
- ✅ Команда `make release` работает (создает подписанную AAB-сборку `dayscounter{VERSION_CODE}.aab`)
- ✅ Подпись загружена в RuStore Консоль

### Шаг 2.2: Добавить команды в Makefile для создания сборок ✅

Команда `make release` добавлена в Makefile. Создает файл `dayscounter{VERSION_CODE}.aab` (формируется на основе VERSION_CODE из `gradle.properties`), автоматически загружает секреты из `../android-secrets/jetpackdays/`.

### Критерии готовности

- ✅ Команда release добавлена в Makefile
- ✅ Сборка release AAB работает корректно
- ✅ Файл сборки создается в ожидаемом месте (`dayscounter{VERSION_CODE}.aab`)
- ✅ Размер AAB-файла не превышает 5 Гб
- ✅ Автоматическое копирование секретов из репозитория `android-secrets` работает

### Правила .gitignore

Следующие правила добавлены в `.gitignore`:

```gitignore
# Папка с секретами (все секреты хранятся здесь)
.secrets/

# Ключи подписи и сертификаты (на случай, если попадут в другие места)
*.keystore
*.jks
*.p12
*.pem
*.pfx
*.p7b
*.p7c
*.der
*.crt
*.cer
*.crl
*.sst
*.csr
*.key

# ZIP-архивы с подписями (для RuStore)
*pepk*.zip
*pepk_out.zip

# Локальные конфигурации
local.properties
release.properties
secrets.properties

# Отладочные keystore
debug.keystore
debug.jks
```

**ВАЖНО:** Основная защита - папка `.secrets/` в корне проекта.

---

## Часть 2.3: Проверка размера APK-файлов (рекомендуется)

### Необходимые действия

Для проверки, что APK-файлы, сформированные из AAB, соответствуют требованию RuStore (не более 5 Гб), можно использовать официальное утилиту Google BundleTool:

**1. Скачать BundleTool**

Скачайте bundletool с официальной страницы Google.

**2. Генерация APK-файлов из AAB**

```bash
# В директории с AAB файлом (используйте актуальное имя файла, например dayscounter10.aab)
java -jar bundletool-all-1.18.1.jar build-apks --bundle=dayscounter10.aab --output-format=DIRECTORY --output=temp_apks/ --mode=universal
```

Примечание: Используйте актуальное имя файла сборки (например, `dayscounter10.aab` для сборки № 10)

**3. Проверка размера APK-файлов**

После выполнения в указанной папке будут созданы APK-файлы. Убедитесь, что размер любого из них не превышает 5 Гб.

Это особенно важно для крупных игр и приложений, использующих большой объём ресурсов.

### Критерии готовности

- ⏳ BundleTool скачан
- ⏳ APK-файлы успешно сгенерированы из AAB
- ⏳ Размер APK-файлов не превышает 5 Гб

---

## Часть 3: Автоматизация создания скриншотов с fastlane

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

## Часть 3.1: Установка и настройка fastlane

### Необходимые действия

#### Шаг 3.1.1: Установить fastlane и screengrab

**Команда для установки:**

```bash
# Через RubyGems
sudo gem install fastlane -NV
sudo gem install screengrab

# Или через Homebrew
brew install fastlane
```

#### Шаг 3.1.2: Добавить зависимость screengrab в Gradle

**Файл:** `app/build.gradle.kts`

Добавить зависимость для instrumentation тестов:

```kotlin
dependencies {
    // ... другие зависимости
    androidTestImplementation("tools.fastlane:screengrab:2.1.0")
}
```

**Последняя версия:** см. [Maven Central](https://mvnrepository.com/artifact/tools.fastlane/screengrab)

#### Шаг 3.1.3: Настроить разрешения в AndroidManifest

**Файл:** `app/src/debug/AndroidManifest.xml`

Добавить необходимые разрешения для screengrab:

```xml
<!-- Разрешает разблокировку устройства и активацию экрана для UI тестов -->
<uses-permission android:name="android.permission.DISABLE_KEYGUARD"/>
<uses-permission android:name="android.permission.WAKE_LOCK"/>

<!-- Разрешает сохранение и загрузку скриншотов -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

<!-- Разрешает смену локали -->
<uses-permission android:name="android.permission.CHANGE_CONFIGURATION" />
```

#### Шаг 3.1.4: Инициализировать fastlane в проекте

**Команда:**

```bash
cd JetpackDays
fastlane init
```

Это создаст структуру:

```
JetpackDays/
├── fastlane/
│   ├── Appfile
│   ├── Fastfile
│   ├── Screengrabfile
│   └── metadata/
│       └── android/
```

### Критерии готовности

- ⏳ fastlane и screengrab установлены
- ⏳ Зависимость `screengrab` добавлена в build.gradle.kts
- ⏳ Разрешения добавлены в AndroidManifest.xml
- ⏳ fastlane инициализирован в проекте

---

## Часть 3.2: Создание Screengrabfile

### Обзор

Screengrabfile - это конфигурационный файл для screengrab, который определяет:

- Пакет приложения
- Пакет тестов
- Локали для скриншотов
- Пути к сохраненным скриншотам

### Необходимые действия

#### Шаг 3.2.1: Создать Screengrabfile

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

# Использовать UI Automator для захвата (лучшее качество)
use_tests_in_packages ['com.dayscounter']

# Путь к сгенерированным скриншотам
screengrab_path '../fastlane/metadata/android'
```

### Критерии готовности

- ⏳ Screengrabfile создан
- ⏳ Пакет приложения указан правильно
- ⏳ Локали настроены (ru-RU, en-US)
- ⏳ Путь к скриншотам настроен

---

## Часть 3.3: Написание UI тестов с Espresso и screengrab

### Обзор

UI тесты с Espresso автоматически выполняются screengrab для создания скриншотов на разных локалях.

**Основные компоненты:**

- `LocaleTestRule` - автоматическое переключение локалей
- `Screengrab.screenshot()` - захват скриншота
- Espresso UI тесты - навигация по приложению

### Необходимые действия

#### Шаг 3.3.1: Создать тестовый класс для скриншотов

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
        // Скриншот 1: Главный экран с демо-списком
        Screengrab.screenshot("1_main_screen")

        // Нажать кнопку добавления
        // Espresso onView(withId(R.id.fab)).perform(click())

        // Скриншот 2: Экран создания события
        Screengrab.screenshot("2_create_item_screen")

        // Ввести название
        // Espresso onView(withId(R.id.title_input)).perform(typeText("New Event"))

        // Выбрать дату
        // Espresso onView(withId(R.id.date_button)).perform(click())

        // Скриншот 3: Выбор даты
        Screengrab.screenshot("3_date_picker")

        // Сохранить
        // Espresso onView(withId(R.id.save_button)).perform(click())

        // Скриншот 4: Главный экран после сохранения
        Screengrab.screenshot("4_after_save")
    }
}
```

#### Шаг 3.3.2: Оптимизировать стратегию захвата

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

- ⏳ Тестовый класс ScreenshotsTest создан
- ⏳ LocaleTestRule добавлен для автоматического переключения локалей
- ⏳ Espresso тесты реализованы
- ⏳ Screengrab.screenshot() вызывается на каждом шаге
- ⏳ UI Automator стратегия включена (опционально)

---

## Часть 3.4: Создание lanes в Fastfile

### Обзор

Fastfile содержит lanes (команды) для автоматизации процессов. Создадим lanes для генерации и загрузки скриншотов.

### Необходимые действия

#### Шаг 3.4.1: Создать lane для генерации скриншотов

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

# Загрузка скриншотов в Google Play Store
lane :upload_screenshots do
  upload_to_play_store(
    skip_upload_metadata: true,
    skip_upload_images: true,
    skip_upload_screenshots: false,
    skip_upload_apk: true,
    skip_upload_aab: true
  )
end

# Полный процесс: генерация и загрузка
lane :screenshots_and_upload do
  capture_android_screenshots(
    locales: ['ru-RU', 'en-US'],
    clear_previous_screenshots: true
  )
  upload_to_play_store(
    skip_upload_metadata: true,
    skip_upload_images: true,
    skip_upload_screenshots: false,
    skip_upload_apk: true,
    skip_upload_aab: true
  )
end
```

#### Шаг 3.4.2: Добавить команды в Makefile

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

## upload-screenshots: Загрузить скриншоты в Google Play Store
upload-screenshots:
    @echo "$(YELLOW)Загружаю скриншоты в Google Play Store...$(RESET)"
    fastlane upload_screenshots
    @echo "$(GREEN)Скриншоты загружены$(RESET)"

## screenshots-full: Генерировать и загрузить скриншоты
screenshots-full:
    @echo "$(YELLOW)Генерирую и загружаю скриншоты...$(RESET)"
    fastlane screenshots_and_upload
    @echo "$(GREEN)Скриншоты готовы и загружены$(RESET)"
```

### Критерии готовности

- ⏳ Lanes в Fastfile созданы
- ⏳ Команды в Makefile добавлены
- ⏳ Локали настроены правильно (ru-RU, en-US)

---

## Часть 3.5: Интеграция с RuStore

### Обзор

После генерации скриншотов через fastlane их можно использовать для RuStore.

### Необходимые действия

#### Шаг 3.5.1: Подготовить скриншоты для RuStore

**Где находятся скриншоты:** `fastlane/metadata/android/`

**Структура:**

```
fastlane/metadata/android/
  ru-RU/
    phoneScreenshots/
      1_main_screen.png
      2_create_item_screen.png
      ...
  en-US/
    phoneScreenshots/
      1_main_screen.png
      2_create_item_screen.png
      ...
```

**Для RuStore нужно:**

1. Скопировать скриншоты из `fastlane/metadata/android/ru-RU/phoneScreenshots/` в папку для загрузки в RuStore
2. Скопировать скриншоты из `fastlane/metadata/android/en-US/phoneScreenshots/` в папку для RuStore (английская версия)

#### Шаг 3.5.2: Создать команду для RuStore

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

1. Установить и настроить fastlane (Часть 3.1)
2. Создать Screengrabfile (Часть 3.2)
3. Написать UI тесты с Espresso (Часть 3.3)
4. Создать lanes в Fastfile (Часть 3.4)
5. Интеграция с RuStore (Часть 3.5)

### Использование

```bash
# Все локали
make screenshots
# Только русский
make screenshots-ru
# Только английский
make screenshots-en
# Загрузка в Google Play Store
make upload-screenshots
# Генерация и загрузка
make screenshots-full
# Подготовка для RuStore
make prepare-rustore-screenshots
```

---

## Часть 4: Подготовка метаданных для магазинов

### Шаг 4.1: Подготовить метаданные для RuStore

Создать файлы описания для RuStore:

- Название (локализованное)
- Краткое описание (до 80 символов)
- Полное описание
- Ключевые слова
- Категория
- Возрастной рейтинг

**Скриншоты для RuStore:**

Используйте команду `make prepare-rustore-screenshots` для подготовки скриншотов из `fastlane/metadata/android/ru-RU/phoneScreenshots/` и `en-US/phoneScreenshots/`.

### Шаг 4.2: Подготовить метаданные для Google Play Store

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

### Приоритет 1: Название приложения (обязательно) ✅

Созданы локализованные названия для русского и английского языков.

### Приоритет 2: Подготовка к публикации в магазинах (обязательно) ✅

**ВЫПОЛНЕНО:** Секреты настроены через приватный репозиторий `android-secrets`.

- ✅ Создан репозиторий `android-secrets` на GitHub
- ✅ Подготовлена структура секретов для JetpackDays в репозитории
- ✅ Создан keystore и ключи подписи в репозитории (ключ `upload`)
- ✅ Получены ZIP-архив с подписью и сертификат PEM для RuStore
- ✅ Настроен build.gradle.kts для чтения секретов из `.secrets/secrets.properties`
- ✅ Папка `.secrets/` добавлена в `.gitignore` проекта JetpackDays
- ✅ Команда `make release` работает (создает подписанную AAB-сборку `dayscounter{VERSION_CODE}.aab`)
- ✅ Подпись загружена в RuStore Консоль (обязательно перед загрузкой AAB)

### Приоритет 3: Автоматизация скриншотов с fastlane (важно)

11. Установить fastlane и screengrab (Часть 3.1)
12. Создать Screengrabfile (Часть 3.2)
13. Написать UI тесты с Espresso (Часть 3.3)
14. Создать lanes в Fastfile (Часть 3.4)
15. Интеграция с RuStore (Часть 3.5)
16. Сгенерировать скриншоты для магазинов приложений (`make screenshots`)

### Приоритет 4: Публикация (обязательно)

16. Создать AAB-файл командой `make release` ✅
17. Загрузить подпись в RuStore Консоль ✅
18. Загрузить AAB и метаданные в RuStore
19. Загрузить AAB и метаданные в Google Play Store

---

## Критерии завершения

### Обязательные критерии

- ✅ **Часть 1: Настройка названия приложения** - выполнено
- ✅ **Часть 2: Подготовка к публикации** - выполнено
  - ✅ Секреты настроены через приватный репозиторий `android-secrets`
  - ✅ Команда `make release` работает (создает файл `dayscounter{VERSION_CODE}.aab`)
  - ✅ Подпись загружена в RuStore Консоль

- ⏳ fastlane и screengrab установлены
- ⏳ Зависимость `screengrab` добавлена в build.gradle.kts
- ⏳ Разрешения добавлены в AndroidManifest.xml
- ⏳ fastlane инициализирован в проекте
- ⏳ Screengrabfile создан с конфигурацией
- ⏳ UI тесты с Espresso написаны
- ⏳ LocaleTestRule добавлен для автоматического переключения локалей
- ⏳ Lanes в Fastfile созданы
- ⏳ Команды в Makefile добавлены
- ⏳ Команда `make screenshots` работает
- ⏳ Скриншоты сгенерированы для обоих магазинов
- ⏳ Скриншоты подготовлены для RuStore
- ⏳ Скриншоты можно загрузить в Google Play Store через `make upload-screenshots`
- ⏳ Метаданные подготовлены для обоих магазинов

### Результат

После завершения настройки можно:

- Публиковать приложение в RuStore и Google Play Store
- Создавать подписанный AAB-файл командой `make release` (файл `dayscounter{VERSION_CODE}.aab`)
- Генерировать локализованные скриншоты командой `make screenshots`
- Загружать скриншоты в Google Play Store командой `make upload-screenshots` или `make screenshots-full`
- Подготавливать скриншоты для RuStore командой `make prepare-rustore-screenshots`

---

## Документация

После завершения настройки обновить документацию:

1. **README.md**: Добавить инструкции по публикации
2. **CONTRIBUTING.md**: Обновить process для релизов
3. **Makefile**: Добавить help для новых команд
4. **Этот документ**: Отметить завершенные шаги
