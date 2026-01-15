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

**Выполнено:** Локализованные названия приложения настроены для русского и английского языков.

### Что было сделано

- Созданы строковые ресурсы `app_display_name` в `values/`, `values-ru/`, `values-en/`
- `AndroidManifest.xml` настроен для использования локализованного названия
- Приложение корректно отображает название при смене языка устройства

### Критерии готовности

- ✅ Строковые ресурсы `app_display_name` созданы для всех локалей
- ✅ AndroidManifest.xml обновлен для использования локализованного названия
- ✅ Приложение отображает правильное название при смене языка устройства

---

## Часть 2: Подготовка к публикации в магазинах приложений

### Шаг 2.1: Настройка подписания AAB ✅

**Выполнено:** Секреты настроены через приватный репозиторий `android-secrets`.

#### Обзор процесса

**ВАЖНО:** В проекте используется упрощенный подход с одним ключом подписи.

- **Ключ загрузки (upload)** - используется для подписи AAB-файла
- **RuStore использует этот же ключ** для подписи APK-файлов

Этот подход упрощает процесс и соответствует настройке в приватном репозитории `android-secrets`.

#### Что было сделано

**Секреты настроены через приватный репозиторий `android-secrets`**

Подробная инструкция см. в README.md репозитория `android-secrets`.

**Краткая информация о структуре секретов:**

```
android-secrets/
├── README.md
└── jetpackdays/
    ├── keystore/
    │   └── dayscounter-release.keystore
    ├── certificates/
    │   ├── pepk_out.zip
    │   └── uploadcert.pem
    ├── tools/
    │   └── pepk.jar
    └── secrets.properties
        ├── KEYSTORE_FILE=keystore/dayscounter-release.keystore
        ├── KEYSTORE_PASSWORD=***
        ├── KEY_ALIAS=upload
        └── KEY_PASSWORD=***
```

**Команда для настройки секретов (выполнено):**

```bash
cd /Users/Oleg991/Documents/GitHub/android-secrets
make init
```

Команда автоматически создала:

- Keystore с ключом `upload` (один пароль для keystore и ключа)
- ZIP-архив для RuStore (< 100 KB)
- PEM сертификат для RuStore (< 100 KB)

**Примечание:** В репозитории `android-secrets` файл `secrets.properties` содержит реальные пароли. Репозиторий должен быть приватным.

#### Интеграция с build.gradle.kts ✅

**Выполнено:** `app/build.gradle.kts` настроен для чтения секретов из `.secrets/secrets.properties`.

`build.gradle.kts` настроен для чтения секретов из локальной папки `.secrets/secrets.properties`:

- Команда `make release` автоматически копирует секреты из `../android-secrets/jetpackdays/` в `.secrets/`
- Путь к keystore обновляется для корректного разрешения относительно корня проекта
- Используется ключ загрузки `upload` для подписи AAB-файла

```kotlin
import java.util.Properties

val secretsProperties = Properties()
val secretsPropertiesFile = rootProject.file(".secrets/secrets.properties")
if (secretsPropertiesFile.exists()) {
    secretsPropertiesFile.inputStream().use { secretsProperties.load(it) }
}

android {
    signingConfigs {
        create("release") {
            val keystoreFile = secretsProperties["KEYSTORE_FILE"] as? String ?: ".secrets/keystore/dayscounter-release.keystore"
            val keystorePassword = secretsProperties["KEYSTORE_PASSWORD"] as? String ?: ""
            val keyAlias = secretsProperties["KEY_ALIAS"] as? String ?: "upload"
            val keyPassword = secretsProperties["KEY_PASSWORD"] as? String ?: ""

            storeFile = rootProject.file(keystoreFile)
            storePassword = keystorePassword
            this.keyAlias = keyAlias
            this.keyPassword = keyPassword
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

#### Загрузка подписи в RuStore ✅

**Выполнено:** Подпись загружена в RuStore Консоль.

- ZIP-архив `pepk_out.zip` загружен (< 100 KB)
- PEM-сертификат `uploadcert.pem` загружен (< 100 KB)
- Статус подписи в RuStore Консоль - "Активна"

### Критерии готовности

- ✅ Приватный репозиторий `android-secrets` создан на GitHub
- ✅ Структура папок для JetpackDays создана в репозитории
- ✅ README.md с инструкцией по работе с репозиторием секретов добавлен
- ✅ Секреты подготовлены в репозитории (keystore, сертификаты, secrets.properties с реальными паролями)
- ✅ build.gradle.kts настроен для чтения секретов из `.secrets/secrets.properties`
- ✅ Папка `.secrets/` добавлена в `.gitignore` проекта JetpackDays
- ✅ Команда `make release` работает (создает подписанную AAB-сборку `dayscounter{VERSION_CODE}.aab`)
- ✅ Подпись загружена в RuStore Консоль (для RuStore)

### Шаг 2.2: Добавить команды в Makefile для создания сборок ✅

**Выполнено:** Команда `make release` добавлена в Makefile.

```makefile
## release: Создать подписанную AAB-сборку для публикации (аналог testflight в iOS). Файл: dayscounter{VERSION_CODE}.aab
release:
    @echo "$(YELLOW)Проверка секретов для подписи...$(RESET)"
    @if [ ! -d ".secrets" ]; then \
        echo "$(YELLOW)Загрузка секретов из репозитория android-secrets...$(RESET)"; \
        if [ -d "../android-secrets/jetpackdays" ]; then \
            mkdir -p ".secrets"; \
            cp -r ../android-secrets/jetpackdays/* .secrets/; \
            # Обновляем путь к keystore в secrets.properties
            sed -i '' 's|KEYSTORE_FILE=keystore/dayscounter-release.keystore|KEYSTORE_FILE=.secrets/keystore/dayscounter-release.keystore|g' .secrets/secrets.properties; \
            echo "$(GREEN)Секреты загружены успешно$(RESET)"; \
        else \
            echo "$(RED)Ошибка: репозиторий android-secrets не найден в ../android-secrets/jetpackdays$(RESET)"; \
            echo "$(YELLOW)Проверьте, что репозиторий android-secrets склонирован в нужное место$(RESET)"; \
            exit 1; \
        fi \
    fi
    @echo "$(YELLOW)Создаю релиз-сборку (AAB)...$(RESET)"
    @./gradlew bundleRelease uploadCrashlyticsMappingFileRelease
    @VERSION_CODE=$$(grep "^VERSION_CODE=" gradle.properties | cut -d'=' -f2); \
    OUTPUT_FILE="dayscounter$$VERSION_CODE.aab"; \
    cp app/build/outputs/bundle/release/app-release.aab "$$OUTPUT_FILE"; \
    echo "$(GREEN)AAB создан и mapping files загружены в Firebase: $$OUTPUT_FILE$(RESET)"
```

### Примечания

**Формат названия AAB-файла:**

Название файла сборки включает номер сборки (VERSION_CODE) для лучшей идентификации:

- Формат: `dayscounter{VERSION_CODE}.aab`
- Примеры: `dayscounter1.aab`, `dayscounter2.aab`, `dayscounter10.aab`

Это позволяет легко различать разные сборки и отслеживать их версии.

**Логика VERSION_CODE:**

- `VERSION_CODE` увеличивается на 1 при каждом вызове `make release`
- `VERSION_CODE` никогда не сбрасывается при повышении основной версии приложения
- Это стандартная практика в Android - монотонно возрастающий номер сборки
- `VERSION_CODE` уникален для каждой сборки и служит идентификатором в магазинах приложений

- Команда `make release` создает файл `dayscounter{VERSION_CODE}.aab` для загрузки в RuStore и Google Play Store
- Команда автоматически загружает секреты из `../android-secrets/jetpackdays/` если папка `.secrets/` не существует
- RuStore поддерживает AAB-файлы (до 5 Гб), необходимо загрузить подпись для публикации через RuStore Консоль
- Файл создается в корне проекта (после копирования из `app/build/outputs/bundle/release/`)
- Для локального тестирования можно использовать команду `make install-release`

### Критерии готовности

- ✅ Команда release добавлена в Makefile
- ✅ Сборка release AAB работает корректно
- ✅ Файл сборки создается в ожидаемом месте (`dayscounter{VERSION_CODE}.aab`)
- ✅ Размер AAB-файла не превышает 5 Гб
- ✅ Автоматическое копирование секретов из репозитория `android-secrets` работает

### Правила .gitignore

Убедитесь, что следующие правила добавлены в `.gitignore`:

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

**ВАЖНО:** Основная защита - папка `.secrets/` в корне проекта. Остальные правила - для защиты от случайных файлов.

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
6. **Поддержка сообщества** - готовый инструмент с документацией
7. **Легкое поддержание** - все настройки в Fastfile и Screengrabfile

### Порядок реализации

1. **Приоритет 1:** Установить и настроить fastlane (Часть 3.1)
2. **Приоритет 2:** Создать Screengrabfile (Часть 3.2)
3. **Приоритет 3:** Написать UI тесты с Espresso (Часть 3.3)
4. **Приоритет 4:** Создать lanes в Fastfile (Часть 3.4)
5. **Приоритет 5:** Интеграция с RuStore (Часть 3.5)

### Использование

**Генерация скриншотов:**

```bash
# Все локали
make screenshots

# Только русский
make screenshots-ru

# Только английский
make screenshots-en
```

**Загрузка в Google Play Store:**

```bash
# Только загрузка
make upload-screenshots

# Генерация и загрузка
make screenshots-full
```

**Подготовка для RuStore:**

```bash
# Подготовить скриншоты для загрузки в RuStore
make prepare-rustore-screenshots
```

### Потенциальные проблемы

1. **Настройка эмулятора** - эмулятор должен быть запущен и доступен через adb
2. **Разрешения** - AndroidManifest.xml должен содержать все необходимые разрешения
3. **UI тесты** - тесты должны быть стабильными и надежными
4. **Размеры экранов** - для разных размеров нужны разные эмуляторы

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

Скриншоты генерируются через fastlane screengrab и находятся в `fastlane/metadata/android/ru-RU/phoneScreenshots/` и `fastlane/metadata/android/en-US/phoneScreenshots/`.

Используйте команду `make prepare-rustore-screenshots` для подготовки скриншотов для RuStore:

```bash
make prepare-rustore-screenshots
# Скриншоты будут в build/rustore-screenshots/ru/ и build/rustore-screenshots/en/
```

### Шаг 4.2: Подготовить метаданные для Google Play Store

#### Необходимые действия

**1. Создать файлы метаданных**

Структура папок метаданных:

```
fastlane/metadata/android/
  en-US/
      title.txt
    short_description.txt
    full_description.txt
    phoneScreenshots/
      1_main_screen.png
      2_create_item_screen.png
      ...
    featureGraphic.png
    icon.png
  ru-RU/
      title.txt
    short_description.txt
    full_description.txt
    phoneScreenshots/
      1_main_screen.png
      2_create_item_screen.png
      ...
    featureGraphic.png
    icon.png
```

**Примечание:** Google Play Store поддерживает автоматическую загрузку метаданных и скриншотов из этой структуры через `fastlane supply`.

**2. Содержимое файлов метаданных**

`fastlane/metadata/android/en-US/title.txt`:

```
Days Counter
```

`fastlane/metadata/android/ru-RU/title.txt`:

```
Счётчик дней
```

`fastlane/metadata/android/en-US/short_description.txt`:

```
Track important dates and events with Days Counter.
```

`fastlane/metadata/android/ru-RU/short_description.txt`:

```
Отслеживайте важные даты и события с приложением «Счётчик дней».
```

`fastlane/metadata/android/en-US/full_description.txt`:

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

`fastlane/metadata/android/ru-RU/full_description.txt`:

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

После подготовки метаданных и скриншотов можно автоматически загрузить их в Google Play Store:

```bash
# Загрузить только скриншоты
make upload-screenshots

# Или загрузить скриншоты и метаданные (кроме APK/AAB)
fastlane supply
```

**Что делает fastlane supply:**

- Загружает скриншоты из `fastlane/metadata/android/`
- Загружает метаданные (название, описание, иконка)
- Обновляет существующие метаданные в Google Play Console

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

1. Создать локализованные названия (Шаг 1.1-1.2) ✅

### Приоритет 2: Подготовка к публикации в магазинах (обязательно) ✅

**ВЫПОЛНЕНО:** Секреты настроены через приватный репозиторий `android-secrets`

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
  - ✅ **Часть 2.1: Настройка подписания AAB** - выполнено
  - ✅ **Часть 2.2: Добавить команды в Makefile** - выполнено
  - ✅ Секреты настроены через приватный репозиторий `android-secrets`
  - ✅ Команда `make release` работает (создает файл `dayscounter{VERSION_CODE}.aab` с номером сборки)
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
- Использовать Makefile для создания релиз-сборок
- Создавать AAB-файл для обоих магазинов одной командой `make release`
- Использовать созданный файл сборки `dayscounter{VERSION_CODE}.aab` напрямую для публикации
- Генерировать локализованные скриншоты командой `make screenshots` (через fastlane screengrab)
- Генерировать скриншоты для конкретного языка командой `make screenshots-ru` или `make screenshots-en`
- Загружать скриншоты в Google Play Store командой `make upload-screenshots` или `make screenshots-full`
- Подготавливать скриншоты для RuStore командой `make prepare-rustore-screenshots`
- Обновлять скриншоты при изменениях в UI
- Улучшать скриншоты и описания
- Проверять размер APK-файлов с помощью BundleTool (при необходимости)

---

## Документация

После завершения настройки обновить документацию:

1. **README.md**: Добавить инструкции по публикации
2. **CONTRIBUTING.md**: Обновить process для релизов
3. **Makefile**: Добавить help для новых команд
4. **Этот документ**: Отметить завершенные шаги
