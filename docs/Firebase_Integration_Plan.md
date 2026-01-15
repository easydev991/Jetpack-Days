# План интеграции с Firebase Crashlytics для Jetpack Days

## Статус

**Текущее состояние:** Firebase интегрирован ✅
**Дата создания плана:** 2026-01-15
**Приоритет:** ВЫСОКИЙ (Crashlytics для отслеживания критических ошибок в production)

---

## Рекомендация

**РЕКОМЕНДУЕТСЯ** использовать Firebase Crashlytics.

**Причины:**

1. Критически важно отслеживать краши в production
2. Бесплатный сервис с достаточным функционалом
3. Легкая интеграция
4. Поможет быстро находить и исправлять ошибки

**Фокус на первой итерации:**

- ✅ **Crashlytics** - сбор крашей с полными стек-трейсами
- ✅ **Данные о девайсе** - модель, версия ОС (собираются автоматически)
- ❌ **Analytics** - НЕ используется на первой итерации
- ❌ Настраиваемые события - НЕ используются на первой итерации

---

## План настройки Crashlytics

### Этап 1-5: Базовая интеграция ✅

**Выполнено:**

1. ✅ Обновлены зависимости в `gradle/libs.versions.toml`:
   - Firebase BOM 34.7.0
   - google-services 4.4.4
   - firebase-crashlytics-gradle 3.0.3

2. ✅ Настроены плагины в `build.gradle.kts` (root) и `app/build.gradle.kts`

3. ✅ Добавлены зависимости через Firebase BOM

4. ✅ Файл `app/google-services.json` добавлен в `.gitignore`

**Для разработчиков:**

1. Скачать `google-services.json` из Firebase Console
2. Поместить в `app/google-services.json`

Проект собирается без ошибок и конфликтов зависимостей.

---

## Реализация логики (без UI)

### Этап 6: Crashlytics helper ✅

**Создан файл:** `app/src/main/java/com/dayscounter/crash/CrashlyticsHelper.kt`

**Функционал:**

- `logException(exception: Throwable, message: String? = null)` - безопасное логирование исключений
- Защита от бесконечного цикла при ошибках отправки
- Автоматический сбор метрик девайса Firebase (модель, версия Android и т.д.)

---

## Настройка для Production

### Этап 7: Конфигурация build types ✅

**Настроено в `app/build.gradle.kts`:**

- ✅ Debug: Crashlytics отключен
- ✅ Release: Crashlytics включен, обфускация включена, автоматическая загрузка mapping files

**Файл:** `app/src/main/AndroidManifest.xml`

- ✅ Добавлен meta-data для управления сбором крашей

### Этап 8: ProGuard для Crashlytics ✅

**Настроено в `app/proguard-rules.pro`:**

- ✅ Правила для сохранения стек-трейсов
- ✅ Правила для Crashlytics

### Этап 8.5: Автоматическая загрузка mapping files ✅

**Настроено:**

- ✅ `mappingFileUploadEnabled = true` в `app/build.gradle.kts`
- ✅ Makefile автоматически загружает mapping files при `make release`
- ✅ Декодирование обфусцированных стек-трейсов

---

## Интеграция с существующим кодом

### Этап 9: Логирование критических ошибок ✅

**Добавлено в Repository:**

```kotlin
// ItemRepositoryImpl.getItemById()
catch (e: Exception) {
    CrashlyticsHelper.logException(
        e,
        "Ошибка при получении элемента с id: $id"
    )
    Result.failure(e)
}
```

**Добавлено в Use Cases:**

```kotlin
// CalculateDaysDifferenceUseCase
catch (e: Exception) {
    CrashlyticsHelper.logException(
        e,
        "Ошибка при вычислении разницы дат: $startDate - $endDate"
    )
    Result.failure(e)
}
```

---

## Тестирование

### Этап 10: Тестирование CrashlyticsHelper ✅

**Создан файл:** `app/src/test/java/com/dayscounter/crash/CrashlyticsHelperTest.kt`

**Тест проверяет:**

- `logException_whenValidException_thenDoesNotCrash()` - отсутствие исключений при логировании

Примечание: Unit-тесты не могут реально отправить данные в Firebase, поэтому проверяется только отсутствие исключений.

---

### Этап 11: Проверка работы Crashlytics в production

**Варианты проверки:**

#### Вариант 1: Через Android Emulator

Если есть возможность запустить Android Emulator:

1. Собрать release APK
2. Установить на Android Emulator
3. Сымитировать критическую ошибку (например, разделить на ноль в коде)
4. Подождать 1-2 минуты
5. Проверить Firebase Console → Crashlytics

#### Вариант 2: Через beta-тестирование

1. Выпустить beta-версию через Google Play Internal Testing
2. Попросить тестирователей использовать приложение
3. Если будут краши - они автоматически попадут в Firebase Console
4. Проверить Firebase Console → Crashlytics

#### Вариант 3: Отложить проверку до реального использования

1. Выпустить релиз в Google Play
2. Crashlytics будет автоматически собирать краши от пользователей
3. Регулярно проверять Firebase Console → Crashlytics

**Проверка собираемых данных (в Firebase Console):**

Когда появится первый краш, убедитесь, что он содержит:

- ✅ Полный стек-трейс
- ✅ Модель устройства (например, "Samsung SM-S906B")
- ✅ Версия Android (например, "13")
- ✅ Разрешение экрана
- ✅ Версия приложения
- ✅ Дата и время краша

---

## Чеклист внедрения Crashlytics

### Базовая интеграция ✅

- [x] Добавлены зависимости в `libs.versions.toml` (включая firebase-crashlytics-gradle)
- [x] Добавлен плагин `google-services` в `build.gradle.kts` (root)
- [x] Добавлен плагин `google-services` в `app/build.gradle.kts`
- [x] Добавлен плагин `firebase-crashlytics` в `app/build.gradle.kts`
- [x] Добавлены зависимости Firebase (BOM, Crashlytics)
- [x] Добавлен `app/google-services.json` (игнорируется git)
- [x] Проект синхронизирован и собирается без ошибок

### Логика и инфраструктура ✅

- [x] Создан `CrashlyticsHelper` для безопасного логирования
- [x] Настроены build types (debug - отключено, release - включено)
- [x] Добавлен meta-data в `AndroidManifest.xml`
- [x] Настроены ProGuard правила для Crashlytics
- [x] Включена автоматическая загрузка mapping files (`mappingFileUploadEnabled = true`)
- [x] Обновлен Makefile для автоматической загрузки (`make release` загружает mapping files)

### Интеграция с кодом ✅

- [x] Добавлено логирование критических ошибок в Repository
- [x] Добавлено логирование критических ошибок в Use Cases
- [x] Написаны unit-тесты для `CrashlyticsHelper`

### Тестирование и валидация ⏳

- [x] Проект собирается: `./gradlew build`
- [x] Тесты проходят: `./gradlew test`
- [ ] Crashlytics настроен корректно (Android Emulator или beta-тестирование)
- [ ] Firebase Console показывает ошибки (при наличии крашей)
- [ ] В каждом краше есть стек-трейс, модель девайса, версия Android
- [ ] Размер APK увеличен приемлемо (< 200 КБ)

---

## Проблемы и решения

### Проблема 1: Конфликты зависимостей

**Симптомы:** Ошибки сборки, конфликты версий
**Решение:**

- Использовать Firebase BOM для управления версиями
- Проверить зависимости: `./gradlew :app:dependencies`
- Обновить все зависимости до совместимых версий

### Проблема 2: Crashlytics не собирает краши в release

**Симптомы:** Нет ошибок в Firebase Console
**Решение:**

- Проверить включена ли collection в `AndroidManifest.xml`
- Проверить настройки ProGuard
- Убедиться, что используется release build type
- Проверить интернет-соединение устройства

### Проблема 3: Увеличение размера APK

**Симптомы:** APK увеличился на > 500 КБ
**Решение:**

- Проверить включено ли code shrinking (`isMinifyEnabled = true`)
- Использовать APK Analyzer для анализа зависимостей
- Рассмотреть использование R8 вместо ProGuard
- Убедиться, что не используются лишние Firebase сервисы

### Проблема 4: Ошибки в debug сборках попадают в Crashlytics

**Симптомы:** Debug краши видны в Firebase Console
**Решение:**

- Убедиться, что `crashlyticsCollectionEnabled` = false в debug
- Проверить `AndroidManifest.xml` и `build.gradle.kts`
- Пересобрать проект после изменений

---

## Безопасность и конфиденциальность

### Рекомендации

✅ **ДОПУСТИМО:**

- Логировать типы исключений
- Логировать сообщения об ошибках (без чувствительных данных)
- Crashlytics автоматически собирает данные о девайсе (модель, версия Android)

❌ **НЕДОПУСТИМО:**

- Логировать персональную информацию (имена, email, телефон)
- Логировать пароли, токены, ключи шифрования
- Логировать содержимое пользовательских данных
- Логировать location данные

### Примеры безопасного логирования

**✅ Правильно:**

```kotlin
// Логируем тип ошибки
CrashlyticsHelper.logException(
    DatabaseException("Failed to load items"),
    "Ошибка загрузки элементов"
)

// Логируем контекст без персональных данных
CrashlyticsHelper.logException(
    e,
    "Ошибка при загрузке списка элементов"
)
```

**❌ Неправильно:**

```kotlin
// НЕ логируем персональные данные
CrashlyticsHelper.logException(
    e,
    "Ошибка для пользователя: ${user.name}, email: ${user.email}"
)

// НЕ логируем чувствительные данные
CrashlyticsHelper.logException(
    e,
    "Ошибка: данные пользователя = $userData"
)
```

---

## Вывод

**Рекомендация:** Интегрировать Firebase Crashlytics для production сборок.

**Причины:**

1. Критически важно отслеживать краши в production
2. Бесплатный сервис с достаточным функционалом
3. Легкая интеграция и настройка
4. Хорошая документация и поддержка Google

**Статус внедрения:**

1. ✅ Базовая интеграция выполнена (Этапы 1-5)
2. ✅ Логика и инфраструктура настроены (Этапы 6-8.5)
3. ✅ Интеграция с кодом завершена (Этап 9)
4. ✅ Unit-тесты написаны (Этап 10)
5. ⏳ Проверка в production ожидается (Этап 11)

---

## Обновление документации

### Обновить README.md

Добавить секцию "Настройка Crashlytics":

```markdown
## Настройка Crashlytics

Файл `app/google-services.json` добавлен в `.gitignore` и не хранится в репозитории.

Для локальной разработки:

1. Скачайте `google-services.json` из Firebase Console
   - Проект: `days-counter-5ee1f`
   - Android App: `com.dayscounter`
2. Поместите его в `app/google-services.json`
3. Файл автоматически игнорируется git

Для CI/CD:

Используйте переменные окружения для получения `google-services.json` из защищенного хранилища.

### Crashlytics

- Включен только в release сборках
- Отключен в debug сборках
- Логирует критические ошибки и ANR
- Автоматически собирает данные о девайсе (модель, версия Android)
- Не содержит персональных данных пользователей
```

### Обновить `.cursor/rules/tech-stack.mdc`

Добавить Firebase в стек:

```markdown
## Зависимости

- **Jetpack Compose** - UI
- **Navigation Compose** - навигация
- **ViewModel** - управление состоянием UI
- **Room** - локальная БД
- **DataStore** - простое хранение
- **Coroutines** - асинхронность
- **Firebase Crashlytics** - сбор ошибок (только release)
- **JUnit 5** - unit-тесты
- **MockK** - мокирование
- **Espresso** - UI тесты
```

---

## Ссылки

- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)
- [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics)
- [Firebase BOM](https://firebase.google.com/docs/android/learn-more#bom)
- [Firebase Security Guidelines](https://firebase.google.com/support/guides/security)
