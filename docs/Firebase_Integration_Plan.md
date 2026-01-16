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
- ✅ **Breadcrumb logs** - логи пользовательских действий перед крашем (требует Analytics)
- ❌ Настраиваемые события - НЕ используются на первой итерации

---

## План настройки Crashlytics

### Выполнено ✅

**Базовая интеграция (Этапы 1-5)**: зависимости, плагины, проект собирается без ошибок.

**Логика (Этапы 6-8.5)**: CrashlyticsHelper, build types, ProGuard, автоматическая загрузка mapping files. Crashlytics отключен в debug, включен в release.

**Breadcrumb logs (Этапы 6.5, 9)**: Firebase Analytics для отладки крашей, автоматический сбор `screen_view` через Navigation Compose для всех экранов.

**Интеграция с кодом (Этап 10)**: логирование через `CrashlyticsHelper.logException()` в Repository и Use Cases.

**Тестирование (Этап 11)**: unit-тесты для проверки безопасности логирования.

---

### Этап 12: Проверка работы Crashlytics в production

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

**Проверка breadcrumb logs:**

Во вкладке **Logs** убедитесь, что присутствуют:

- ✅ `screen_view` события - список экранов, просмотренных перед крашем
- ✅ `firebase_screen_class` параметр в каждом screen_view
- ✅ Пользовательские события (если логируются вручную)
- ✅ Параметры событий для понимания контекста действий пользователя

---

## Чеклист внедрения Crashlytics

### Выполнено ✅

- [x] Базовая интеграция: зависимости, плагины, google-services.json
- [x] Breadcrumb logs (Analytics): автоматический сбор `screen_view` через Navigation Compose
- [x] Логика и инфраструктура: CrashlyticsHelper, build types, ProGuard, mapping files
- [x] Интеграция с кодом: логирование в Repository и Use Cases
- [x] Unit-тесты: проверка безопасности логирования
- [x] Проект собирается: `./gradlew build`
- [x] Тесты проходят: `./gradlew test`

### Валидация в production ⏳

- [ ] Crashlytics настроен корректно (Android Emulator или beta-тестирование)
- [ ] Firebase Console показывает ошибки (при наличии крашей)
- [ ] В каждом краше есть стек-трейс, модель девайса, версия Android
- [ ] Breadcrumb logs показывают экраны и действия перед крашем
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

**✅ ДОПУСТИМО:** логировать типы исключений и действий, названия экранов, контекст действий без персональных данных, использовать breadcrumb logs.

**❌ НЕДОПУСТИМО:** логировать персональную информацию, пароли, токены, ключи шифрования, содержимое пользовательских данных, location данные, локацию пользователя, уникальные идентификаторы.

---

## Вывод

**Рекомендация:** Интегрировать Firebase Crashlytics для production сборок.

**Причины:**

1. Критически важно отслеживать краши в production
2. Бесплатный сервис с достаточным функционалом
3. Легкая интеграция и настройка
4. Хорошая документация и поддержка Google

**Статус внедрения:**

1. ✅ Все этапы интеграции выполнены (Этапы 1-11)
2. ⏳ Проверка в production ожидается (Этап 12)

---

## Обновление документации

### Обновить README.md

Добавить секцию "Настройка Firebase":

```markdown
## Настройка Firebase

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

### Analytics (для breadcrumb logs)

- Включен для работы breadcrumb logs в Crashlytics
- Автоматически собирает screen_view события через Navigation Compose
- Позволяет видеть последовательность действий пользователя перед крашем
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
- **Firebase Analytics** - breadcrumb logs для отладки крашей
- **JUnit 5** - unit-тесты
- **MockK** - мокирование
- **Espresso** - UI тесты
```

---

## Ссылки

- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)
- [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics)
- [Customize crash reports for Android](https://firebase.google.com/docs/crashlytics/android/customize-crash-reports#get-breadcrumb-logs)
- [Firebase BOM](https://firebase.google.com/docs/android/learn-more#bom)
- [Firebase Security Guidelines](https://firebase.google.com/support/guides/security)
