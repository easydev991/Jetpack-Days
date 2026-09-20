# Alarm and Reminder — системные тесты

Тесты системных компонентов в `app/src/androidTest/java/com/dayscounter/reminder/`:
планировщик будильников и BroadcastReceiver уведомлений. Работают
с реальными Android-сервисами (`AlarmManager`, `NotificationManager`),
без Compose и без эмуляции.

Примеры:
- `reminder/AlarmReminderSchedulerInstrumentedTest.kt`
- `reminder/ReminderAlarmReceiverInstrumentedTest.kt`

## Контекст

```kotlin
private val context = InstrumentationRegistry.getInstrumentation().targetContext
```

## Планировщик: проверка PendingIntent

Полный тест — `references/EXAMPLE.md` (раздел «Alarm-тест
(PendingIntent + пермишены)»). Суть:

```kotlin
scheduler.schedule(reminder, "title")
assertNotNull(findReminderPendingIntent(itemId))   // запланировано

scheduler.cancel(itemId)
assertNull(findReminderPendingIntent(itemId))      // снято
```

Поиск PendingIntent — без ожидания времени, через `FLAG_NO_CREATE`
(вернуть существующий PendingIntent или null, не создавая новый).
Позволяет проверять наличие без ожидания срабатывания. Определение
`findReminderPendingIntent` — в `references/EXAMPLE.md`
(раздел «Alarm-тест (PendingIntent + пермишены)»).

## Receiver: реальный вызов onReceive

```kotlin
@Test
fun onReceive_whenReminderIntentIsValid_thenPostsNotificationAndCreatesChannel() {
    runWithNotificationPermission {
        val receiver = ReminderAlarmReceiver()
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderIntentContract.ACTION_FIRE_REMINDER
            putExtra(ReminderIntentContract.EXTRA_ITEM_ID, 77L)
            putExtra(ReminderIntentContract.EXTRA_ITEM_TITLE, "День рождения")
        }

        receiver.onReceive(context, intent)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(
                ReminderIntentContract.CHANNEL_ID
            )
            assertNotNull(channel)
        }
    }
}
```

## Пермишены для уведомлений (API 33+)

`POST_NOTIFICATIONS` — runtime-пермишен с Android 13 (TIRAMISU).
В тестах выдаётся через shell:

```kotlin
private fun runWithNotificationPermission(block: () -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        block()
        return
    }
    val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    uiAutomation.adoptShellPermissionIdentity(Manifest.permission.POST_NOTIFICATIONS)
    try {
        block()
    } finally {
        uiAutomation.dropShellPermissionIdentity()
    }
}
```

## Очистка после тестов

```kotlin
@After
fun tearDown() {
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    notificationManager.cancelAll()
}
```

## Правила

- **Нет `Thread.sleep`** — планировщик проверяется через `PendingIntent`
  (`FLAG_NO_CREATE`), а не ожиданием срабатывания
- **Receiver-тесты** — реальный вызов `onReceive()`, проверка побочных
  эффектов (канал уведомлений)
- **TIRAMISU handling** — отдельная ветка для Android 13+ через
  `runWithNotificationPermission`
- `@After` — `notificationManager.cancelAll()` для изоляции
- Нет `ActivityScenarioRule` — только `InstrumentationRegistry`
