# План настройки Hilt для Jetpack Days

## Статус

**Текущее состояние:** Hilt временно отключен из-за проблем с версиями  
**Дата создания плана:** 2025-01-XX  
**Приоритет:** СРЕДНИЙ (ручной DI работает, но Hilt упростит управление зависимостями)

---

## Анализ необходимости Hilt

### Текущая ситуация

- ✅ Ручной DI через `FormatterModule` работает
- ✅ Все зависимости создаются через factory методы
- ✅ Тесты работают без Hilt
- ⚠️ Hilt упростит управление зависимостями при росте проекта

### Преимущества Hilt

1. **Автоматическое управление зависимостями** - не нужно вручную создавать экземпляры
2. **Интеграция с ViewModel** - `@HiltViewModel` упрощает создание ViewModel
3. **Тестирование** - `@HiltAndroidTest` упрощает интеграционные тесты
4. **Масштабируемость** - легче добавлять новые зависимости

### Недостатки Hilt

1. **Дополнительная зависимость** - увеличивает размер APK
2. **Сложность настройки** - требует настройки плагинов и аннотаций
3. **Время компиляции** - KSP обработка аннотаций замедляет сборку
4. **Оверкилл для малого проекта** - для текущего размера проекта ручной DI достаточен

---

## Рекомендация

**НЕ ОБЯЗАТЕЛЬНО** использовать Hilt на данном этапе проекта.

**Причины:**

1. Проект небольшой, ручной DI через `FormatterModule` работает хорошо
2. Все зависимости простые, нет сложных графов зависимостей
3. Тесты работают без Hilt
4. Нет критической необходимости в автоматическом DI

**Когда стоит рассмотреть Hilt:**

- Когда проект вырастет до 10+ ViewModel
- Когда появятся сложные графы зависимостей
- Когда потребуется интеграционное тестирование с DI
- Когда команда разработчиков вырастет и нужна стандартизация

---

## План настройки Hilt (если все же решим использовать)

### Этап 1: Обновление зависимостей

**Файл:** `gradle/libs.versions.toml`

```toml
[versions]
hilt = "2.52"  # Уже есть в файле

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hilt" }

[plugins]
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

**Проверка совместимости:**

- Hilt 2.52 совместим с Kotlin 2.0.21 ✅
- Hilt 2.52 совместим с KSP 2.0.21-1.0.25 ✅
- Hilt 2.52 совместим с AGP 8.13.2 ✅

---

### Этап 2: Настройка плагинов

**Файл:** `app/build.gradle.kts`

```kotlin
plugins {
    // ... существующие плагины
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)  // Уже есть
}

dependencies {
    // ... существующие зависимости
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // Для тестирования
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
```

**Файл:** `build.gradle.kts` (root)

```kotlin
plugins {
    // ... существующие плагины
    alias(libs.plugins.hilt.android) apply false
}
```

---

### Этап 3: Настройка Application

**Файл:** `app/src/main/java/com/dayscounter/DaysCounterApplication.kt`

```kotlin
package com.dayscounter

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DaysCounterApplication : Application() {
    // ... существующий код
}
```

**Файл:** `app/src/main/AndroidManifest.xml`

```xml
<application
    android:name=".DaysCounterApplication"
    ...>
</application>
```

---

### Этап 4: Создание DI модулей

**Файл:** `app/src/main/java/com/dayscounter/di/FormatterModule.kt`

```kotlin
package com.dayscounter.di

import android.content.Context
import com.dayscounter.data.formatter.DaysFormatter
import com.dayscounter.data.formatter.DaysFormatterImpl
import com.dayscounter.data.formatter.ResourceProvider
import com.dayscounter.data.formatter.ResourceProviderImpl
import com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCase
import com.dayscounter.domain.usecase.FormatDaysTextUseCase
import com.dayscounter.domain.usecase.GetFormattedDaysForItemUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FormatterModule {
    
    @Provides
    @Singleton
    fun provideResourceProvider(
        @ApplicationContext context: Context
    ): ResourceProvider = ResourceProviderImpl(context)
    
    @Provides
    @Singleton
    fun provideDaysFormatter(): DaysFormatter = DaysFormatterImpl()
    
    @Provides
    @Singleton
    fun provideCalculateDaysDifferenceUseCase(): CalculateDaysDifferenceUseCase =
        CalculateDaysDifferenceUseCase()
    
    @Provides
    @Singleton
    fun provideFormatDaysTextUseCase(
        daysFormatter: DaysFormatter
    ): FormatDaysTextUseCase = FormatDaysTextUseCase(daysFormatter)
    
    @Provides
    @Singleton
    fun provideGetFormattedDaysForItemUseCase(
        calculateDaysDifferenceUseCase: CalculateDaysDifferenceUseCase,
        formatDaysTextUseCase: FormatDaysTextUseCase
    ): GetFormattedDaysForItemUseCase = GetFormattedDaysForItemUseCase(
        calculateDaysDifferenceUseCase,
        formatDaysTextUseCase
    )
}
```

---

### Этап 5: Обновление ViewModel

**Файл:** `app/src/main/java/com/dayscounter/viewmodel/DaysCalculatorViewModel.kt`

```kotlin
package com.dayscounter.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
// ... остальные импорты

class DaysCalculatorViewModel @ViewModelInject constructor(
    private val calculateDaysDifferenceUseCase: CalculateDaysDifferenceUseCase,
    private val formatDaysTextUseCase: FormatDaysTextUseCase,
    private val resourceProvider: ResourceProvider,
    private val defaultDisplayOption: DisplayOption = DisplayOption.DAY,
) : ViewModel() {
    // ... существующий код
}
```

**Примечание:** `@ViewModelInject` устарел в новых версиях Hilt. Используйте `@HiltViewModel`:

```kotlin
@HiltViewModel
class DaysCalculatorViewModel @Inject constructor(
    // ... параметры
) : ViewModel() {
    // ...
}
```

---

### Этап 6: Обновление MainActivity

**Файл:** `app/src/main/java/com/dayscounter/MainActivity.kt`

```kotlin
package com.dayscounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // ... существующий код
}
```

---

### Этап 7: Обновление тестов

**Файл:** `app/src/test/java/com/dayscounter/viewmodel/DaysCalculatorViewModelTest.kt`

```kotlin
// Тесты остаются без изменений, т.к. ViewModel можно тестировать без Hilt
// или использовать @HiltAndroidTest для интеграционных тестов
```

**Для интеграционных тестов:**

```kotlin
@HiltAndroidTest
class DaysCalculatorViewModelIntegrationTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    // ... тесты
}
```

---

## Проблемы и решения

### Проблема 1: Конфликты версий

**Симптомы:** Ошибки компиляции, конфликты зависимостей  
**Решение:**

- Проверить совместимость версий Hilt с Kotlin, KSP, AGP
- Обновить все зависимости до совместимых версий
- Использовать `./gradlew :app:dependencies` для проверки

### Проблема 2: KSP не генерирует код

**Симптомы:** Ошибки "Cannot find symbol" для сгенерированных классов  
**Решение:**

- Очистить проект: `./gradlew clean`
- Пересобрать: `./gradlew build`
- Проверить настройки KSP в `build.gradle.kts`

### Проблема 3: Циклические зависимости

**Симптомы:** Ошибки компиляции о циклических зависимостях  
**Решение:**

- Проверить граф зависимостей
- Использовать `@Singleton` для общих зависимостей
- Разделить модули на более мелкие

---

## Чеклист внедрения Hilt

- [ ] Обновлены зависимости в `libs.versions.toml`
- [ ] Добавлены плагины Hilt в `build.gradle.kts`
- [ ] Application класс помечен `@HiltAndroidApp`
- [ ] AndroidManifest обновлен с правильным Application классом
- [ ] Создан DI модуль `FormatterModule` с `@Module` и `@Provides`
- [ ] ViewModel обновлены с `@HiltViewModel` и `@Inject`
- [ ] MainActivity помечена `@AndroidEntryPoint`
- [ ] Тесты обновлены (если нужно)
- [ ] Проект собирается без ошибок
- [ ] Все тесты проходят
- [ ] Приложение запускается и работает

---

## Альтернативы Hilt

### 1. Ручной DI (текущий подход)

- ✅ Простота
- ✅ Нет дополнительных зависимостей
- ✅ Быстрая компиляция
- ❌ Ручное управление зависимостями

### 2. Koin

- ✅ Проще, чем Hilt
- ✅ Легче настраивается
- ✅ Меньше зависимостей
- ❌ Медленнее Hilt в runtime

### 3. Kodein

- ✅ Легковесный
- ✅ Простой синтаксис
- ❌ Менее популярный

---

## Вывод

**Рекомендация:** Продолжить использовать ручной DI через `FormatterModule` до тех пор, пока проект не вырастет или не появятся сложные графы зависимостей.

**Если все же решим использовать Hilt:**

1. Следовать плану выше
2. Тестировать на отдельной ветке
3. Убедиться, что все тесты проходят
4. Обновить документацию

---

## План полного отказа от Hilt

### Статус

**Решение:** Hilt не используется в проекте и будет удален из зависимостей  
**Дата принятия решения:** 2024-12-30  
**Причина:** Ручной DI через `FormatterModule` полностью удовлетворяет потребности проекта

---

### Почему Hilt не нужен

1. **Проект небольшой** - одна ViewModel, простой граф зависимостей
2. **Ручной DI работает отлично** - `FormatterModule` с factory методами решает все задачи
3. **Нет сложных зависимостей** - нет необходимости в автоматическом DI
4. **Тесты работают без Hilt** - создание экземпляров вручную не создает проблем
5. **Быстрее компиляция** - нет KSP обработки аннотаций Hilt
6. **Меньше зависимостей** - уменьшение размера APK

---

### Шаги для полного удаления Hilt

#### Шаг 1: Удаление зависимостей из `libs.versions.toml`

**Файл:** `gradle/libs.versions.toml`

**Удалить:**

```toml
hilt = "2.52"  # Удалить из секции [versions]

# Удалить из секции [libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hilt" }

# Удалить из секции [plugins]
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

---

#### Шаг 2: Очистка `app/build.gradle.kts`

**Файл:** `app/build.gradle.kts`

**Удалить:**

```kotlin
// Удалить из секции plugins
// ВРЕМЕННО ОТКЛЮЧЕН Hilt из-за проблем с версиями
// alias(libs.plugins.hilt.android)

// Удалить из секции dependencies
// ВРЕМЕННО ОТКЛЮЧЕН Hilt из-за проблем с версиями
// Hilt
// implementation(libs.hilt.android)
// ksp(libs.hilt.compiler)
```

---

#### Шаг 3: Обновление комментариев в `FormatterModule.kt`

**Файл:** `app/src/main/java/com/dayscounter/di/FormatterModule.kt`

**Заменить:**

```kotlin
/**
 * DI модуль для форматирования количества дней.
 *
 * ВРЕМЕННО ОТКЛЮЧЕН из-за проблем с Hilt.
 * Используется для создания зависимостей вручную в проекте.
 *
 * Для создания экземпляров используйте factory методы ниже.
 */
```

**На:**

```kotlin
/**
 * DI модуль для форматирования количества дней.
 *
 * Использует ручной подход к внедрению зависимостей через factory методы.
 * Hilt не используется в проекте, так как ручной DI полностью удовлетворяет потребности.
 *
 * Для создания экземпляров используйте factory методы ниже.
 */
```

---

#### Шаг 4: Обновление комментариев в `DaysCounterApplication.kt`

**Файл:** `app/src/main/java/com/dayscounter/DaysCounterApplication.kt`

**Заменить:**

```kotlin
/**
 * Класс Application для приложения Days Counter.
 *
 * ВРЕМЕННО ОТКЛЮЧЕН Hilt из-за проблем с версиями.
 * DI будет реализован вручную или через другой контейнер.
 */
```

**На:**

```kotlin
/**
 * Класс Application для приложения Days Counter.
 *
 * Проект использует ручной подход к внедрению зависимостей через factory методы.
 * Hilt не используется, так как ручной DI полностью удовлетворяет потребности проекта.
 */
```

---

#### Шаг 5: Обновление комментариев в `MainActivity.kt`

**Файл:** `app/src/main/java/com/dayscounter/MainActivity.kt`

**Заменить:**

```kotlin
/**
 * Главная Activity приложения.
 *
 * ВРЕМЕННО ОТКЛЮЧЕН Hilt из-за проблем с версиями.
 * DI будет реализован вручную или через другой контейнер.
 */
```

**На:**

```kotlin
/**
 * Главная Activity приложения.
 *
 * Проект использует ручной подход к внедрению зависимостей через factory методы.
 * Hilt не используется, так как ручной DI полностью удовлетворяет потребности проекта.
 */
```

---

#### Шаг 6: Обновление комментариев в тестах

**Файл:** `app/src/test/java/com/dayscounter/viewmodel/DaysCalculatorViewModelTest.kt`

**Заменить:**

```kotlin
/**
 * Unit-тесты для [DaysCalculatorViewModel].
 *
 * Проверяют корректность вычисления разницы дат и форматирования текста.
 *
 * ВНИМАНИЕ: Тесты создают реальные экземпляры Use Cases вместо моков,
 * т.к. Hilt временно отключен.
 */
```

**На:**

```kotlin
/**
 * Unit-тесты для [DaysCalculatorViewModel].
 *
 * Проверяют корректность вычисления разницы дат и форматирования текста.
 *
 * Тесты создают реальные экземпляры Use Cases вместо моков,
 * так как ручной DI не требует сложной настройки для unit-тестирования.
 */
```

---

#### Шаг 7: Обновление AndroidManifest (если нужно)

**Файл:** `app/src/main/AndroidManifest.xml`

Проверить, что в теге `<application>` нет атрибута `android:name=".DaysCounterApplication"` если класс был помечен `@HiltAndroidApp`.

Если класс существует и не используется, можно удалить, или оставить для будущего использования (например, для Application callbacks).

---

#### Шаг 8: Валидация

После выполнения всех шагов необходимо проверить:

1. **Сборка проекта:**

   ```bash
   ./gradlew clean build
   ```

2. **Запуск тестов:**

   ```bash
   ./gradlew test
   ```

3. **Проверка отсутствия Hilt в зависимостях:**

   ```bash
   ./gradlew :app:dependencies | grep -i hilt
   ```

   *Должен вернуть пустой результат*

4. **Поиск остатков Hilt в коде:**

   ```bash
   grep -r "hilt\|Hilt\|@Hilt\|@Inject\|@AndroidEntryPoint\|@HiltAndroidApp" app/src/
   ```

   *Должен вернуть только комментарии или этот документ*

---

#### Шаг 9: Обновление документации

Обновить этот файл, чтобы раздел "План настройки Hilt" стал исторической справкой, а основной упор был на ручной DI.

Обновить `.cursor/rules/architecture.mdc` с указанием, что проект использует ручной DI.

---

### Чеклист удаления Hilt

- [ ] Удалены зависимости Hilt из `libs.versions.toml`
- [ ] Удалены комментарии про Hilt из `app/build.gradle.kts`
- [ ] Обновлены комментарии в `FormatterModule.kt`
- [ ] Обновлены комментарии в `DaysCounterApplication.kt`
- [ ] Обновлены комментарии в `MainActivity.kt`
- [ ] Обновлены комментарии в тестах
- [ ] Проект собирается без ошибок: `./gradlew clean build`
- [ ] Тесты проходят: `./gradlew test`
- [ ] Нет Hilt в зависимостях: `./gradlew :app:dependencies | grep -i hilt`
- [ ] Нет аннотаций Hilt в коде: `grep -r "hilt\|Hilt\|@Hilt\|@Inject" app/src/`

---

## Ссылки

- [Hilt Documentation](https://developer.android.com/training/dependency-injection/hilt-android)
- [Hilt Migration Guide](https://developer.android.com/training/dependency-injection/hilt-migration)
- [Hilt Version Compatibility](https://github.com/google/dagger/releases)
