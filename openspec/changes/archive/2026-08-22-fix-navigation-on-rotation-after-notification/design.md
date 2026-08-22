## Context

`MainActivity.onCreate` безусловно вызывал `handleReminderIntent(intent, reminderManager)`. При recreation Activity (rotation, смена темы, восстановление процесса) Android подсовывает сохранённый `Intent` пуша из `ActivityRecord`, в котором лежит `EXTRA_ITEM_ID`. Это приводит к двум связанным проблемам:

1. **Дублирование навигации.** `pendingOpenDetailItemId` перезаписывается прежним значением, и `LaunchedEffect(pendingOpenDetailItemId)` в `RootScreen.kt:76-80` повторно пушит `ItemDetail` в стек навигации поверх текущего экрана (например, `ThemeIconScreen` на вкладке **More**). Пользователь после поворота оказывается на экране деталей уже открытого события.
2. **Повторные side-effects.** `handleReminderIntent` дёргает `reminderManager.consumeReminder(itemId)` (помечает напоминание доставленным в БД) и `NotificationManagerCompat.cancel(...)` (снимает уведомление). При recreation эти операции выполняются повторно — пограничный случай, который сейчас маскируется первой проблемой.

Архитектура deep-link через `pendingOpenDetailItemId` + `onPendingOpenHandled` в `RootScreen` — рабочая, баг был только в условии срабатывания `handleReminderIntent`. `onNewIntent` (пуш при живом процессе) уже обрабатывается отдельным методом и не требует изменений.

## Goals / Non-Goals

**Goals:**
- Обрабатывать reminder-intent ровно один раз на доставку пуша (cold-start + `onNewIntent`).
- Пропускать обработку при recreation (rotation, theme change, возврат из фона после смерти процесса).
- Сохранить текущее поведение: cold-start с push-интентом открывает `DetailScreen`, пуш при живом процессе — `DetailScreen` (через `onNewIntent`).
- Покрыть гейт unit-тестом без Android-зависимостей (pure-JVM), чтобы тест был детерминированным и не зависел от эмулятора.

**Non-Goals:**
- Менять архитектуру deep-link (`pendingOpenDetailItemId` + `onPendingOpenHandled`).
- Выносить `handleReminderIntent` в отдельный сервис / `UseCase`.
- Поддерживать глубокие ссылки с восстановлением стека навигации (deep-state restoration) — это отдельная задача.
- Добавлять Robolectric в проект ради unit-теста Activity-логики.

## Decisions

### Decision 1: Гейт в `companion object` через проверку `savedInstanceState == null`

Выбран гейт `MainActivity.shouldHandleReminderIntent(savedInstanceState: Bundle?): Boolean = savedInstanceState == null` в `companion object`, помеченный `@VisibleForTesting internal`. Вызов из `onCreate` обёрнут в `if (shouldHandleReminderIntent(savedInstanceState))`.

**Почему `savedInstanceState == null`:**
- Android передаёт `savedInstanceState != null` ровно тогда, когда Activity пересоздаётся системой при rotation / theme change / configuration change / возврате из фона после смерти процесса (savedInstance есть всегда, даже если recreation по rotation).
- Cold-start (новый процесс через launcher) → `savedInstanceState == null`.
- Process-restart (Android убил процесс, пользователь вернулся по savedInstance из `ActivityRecord`) — здесь контекст сложнее: Android пересоздаёт Activity и передаёт `savedInstanceState` со старым `Bundle`, поэтому гейт вернёт `false`, и reminder-intent не обработается повторно.

**Альтернативы, отвергнутые:**
- **`intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY` / `FLAG_FROM_BACKGROUND`** — флаги ненадёжны для различения cold-start и recreation, зависят от пути запуска.
- **Хранить «уже обработан» в `SharedPreferences` / статике** — добавляет состояние вне `savedInstanceState`, риск рассинхронизации, не idiomatic.
- **`pendingOpenDetailItemId.value != null`** как gate — курица-и-яйцо: gate должен сработать ДО установки значения.
- **`onNewIntent` для всех путей** — не работает: при cold-start `onNewIntent` не вызывается, только `onCreate(intent)`.

### Decision 2: Pure-JVM unit-тест гейта (без Robolectric, без Compose)

Гейт — pure-функция от `savedInstanceState`, поэтому тестируется как обычная JVM-функция в `app/src/test/` (JUnit 5).

**Почему pure-JVM:**
- Детерминированно: нет race conditions, нет cleanup-проблем, нет зависаний.
- Не требует эмулятора → быстрая итерация в IDE / CI.
- Не конфликтует с Jupiter-стеком проекта.
- Проверяет ровно то, что фикс делает — условие срабатывания `handleReminderIntent`.

**Альтернативы, отвергнутые:**
- **C.5 — UI-flow через `AndroidComposeTestRule + setIntent(pushIntent) + rotation`.** `ActivityScenario.close()` в `@After` зависает на таймауте `Activity never becomes DESTROYED` (45–90 сек). Воспроизводится стабильно: **без `setIntent`** cleanup проходит за <1 сек; **с `setIntent`** — таймаут и stack-trace `at androidx.test.core.app.ActivityScenario.waitForActivityToBecomeAnyOf`. Это известная проблема совместимости `Activity.setIntent()` и `ActivityScenario` в Compose Testing v2.
- **C.5' — `ActivityScenarioRule + setIntent(pushIntent) + recreate`.** `scenario.recreate()` бросает `IllegalStateException: Requested a re-creation of Activity but didn't happen` после `setIntent`.
- **Cold-start с push-интентом через `ActivityScenario.launch(pushIntent)`.** Race-condition в `LaunchedEffect(pendingOpenDetailItemId)`: срабатывает при первом composition до того, как `NavHost` инициализирует граф `NavController`. В production timing работает, в Compose Testing — `IllegalArgumentException: Cannot navigate to item_detail/1001. Navigation graph has not been set for NavController`.
- **Robolectric.** Scope нового dep ~10MB, friction с `RobolectricExtension` для JUnit 5, поддержка Compose. Слишком тяжело для проверки тривиального условия `savedInstanceState == null`.

### Decision 3: AndroidTest как антирегрессия, а не воспроизведение бага

`MainActivityDeepLinkRotationUiTest` — единственный тест `given_regular_launch_when_activity_recreated_then_open_detail_item_id_remains_null`. Воспроизводит recreate без push и проверяет, что `@get:VisibleForTesting openDetailItemId` остаётся `null` через реальный `ActivityScenario.recreate()`. Это не воспроизводит исходный баг напрямую (для воспроизведения нужен `setIntent` + recreate, что ломает instrumentation), но подтверждает, что getter корректно отдаёт `pendingOpenDetailItemId` после recreation.

**Почему не воспроизводим баг в androidTest:** см. Decision 2 — `setIntent` ломает `ActivityScenario.close()` / `recreate()`. JVM unit-тест гейта + androidTest антирегрессия дают достаточное покрытие для тривиального фикса.

### Decision 4: `ANDROID_TEST_FILTER` в Makefile

Добавлена переменная `ANDROID_TEST_FILTER` для запуска подмножества androidTest:

```bash
make android-test ANDROID_TEST_FILTER=MainActivityDeepLinkRotationUiTest
```

Цель: быстрая итерация при отладке тестов на эмуляторе без прогона всех 104 androidTest.

## Risks / Trade-offs

- **Process-restart edge case.** При process-restart Android передаёт `savedInstanceState != null` и `intent` со старым пушем. Гейт вернёт `false`, и `handleReminderIntent` не вызовется → пользователь не попадёт на `DetailScreen` через пуш после возврата в убитое системой приложение. **Mitigation:** это исходное поведение бага — пользователь всё равно не попадал на `DetailScreen` через пуш после process-restart (там другая сложность с восстановлением стека). Cold-start после force-stop работает корректно (`savedInstanceState == null`). Если в будущем потребуется — это отдельная задача deep-state restoration.
- **Гейт не различает recreate и process-restart.** Оба дают `savedInstanceState != null`. Для фикса это корректно (оба случая не должны обрабатывать reminder-intent повторно). Если в будущем понадобится различать — нужен дополнительный сигнал (например, `Activity.isChangingConfigurations`).
- **`openDetailItemId` как `@get:VisibleForTesting`.** Поле проброшено наружу для интеграционного теста. Минимальное «производственное» загрязнение — getter только читает `MutableState.value`, не меняет поведение в release.
- **Pure-JVM gate покрывает только логику гейта, не полный flow.** Багфикс — это и есть гейт. Полный flow (rotation + push) покрыт ручной приёмкой (Этап 6 плана) и частично — androidTest антирегрессией.
