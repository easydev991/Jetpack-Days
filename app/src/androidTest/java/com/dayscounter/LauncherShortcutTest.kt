package com.dayscounter

import android.content.Intent
import android.content.pm.ShortcutManager
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.database.dao.ItemDao
import com.dayscounter.data.database.entity.ItemEntity
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.navigation.ShortcutLaunchResolver
import com.dayscounter.reminder.ReminderIntentContract
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

/**
 * Инструментальные тесты статического лаунчер-шортката «Добавить запись».
 *
 * Покрывают контракт шортката:
 * - cold start по intent шортката открывает экран создания, «назад» возвращает на главный экран;
 * - повторная доставка того же intent не дублирует экран создания (guard по back stack);
 * - recreate и повторный обычный launch не открывают экран создания заново;
 * - манифестный шорткат объявлен с подписью [R.string.add_item] и иконкой `@drawable/ic_shortcut_add`,
 *   его action совпадает с [ShortcutLaunchResolver.ACTION_CREATE_ITEM] и резолвится только в MainActivity.
 *
 * Activity всегда стартует с intent шортката (одно правило на класс), остальные сценарии
 * доставляются через `onNewIntent` — канонический путь для `singleTop`.
 */
@RunWith(AndroidJUnit4::class)
class LauncherShortcutTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val shortcutIntent: Intent =
        Intent(context, MainActivity::class.java).apply {
            action = ShortcutLaunchResolver.ACTION_CREATE_ITEM
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    private val mainIntent: Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    @get:Rule
    val composeTestRule =
        AndroidComposeTestRule(
            activityRule = ActivityScenarioRule<MainActivity>(shortcutIntent),
            activityProvider = ::activityFromRule
        )

    private val dao: ItemDao
        get() = DaysDatabase.getDatabase(context.applicationContext).itemDao()

    private val newItemTitle: String
        get() = context.getString(R.string.new_item)

    private val addItemLabel: String
        get() = context.getString(R.string.add_item)

    private val cancelLabel: String
        get() = context.getString(R.string.cancel)

    private val closeLabel: String
        get() = context.getString(R.string.close)

    @Before
    fun setUp() {
        runBlocking { dao.deleteAllItems() }
    }

    @After
    fun tearDown() {
        runBlocking { dao.deleteAllItems() }
    }

    @Test
    fun coldStart_shortcutIntent_opensCreateScreenAndBackReturnsToEvents() {
        // Given: Activity стартовала по intent шортката
        awaitCreateScreen()

        // Then: открыт экран создания, FAB главного экрана не показан
        composeTestRule.onNodeWithText(newItemTitle).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(addItemLabel).assertDoesNotExist()

        // When: пользователь уходит «назад»
        pressBack()

        // Then: вернулись на главный экран с FAB
        composeTestRule.onNodeWithContentDescription(addItemLabel).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(newItemTitle).assertCountEquals(0)
    }

    @Test
    fun repeatedShortcutIntent_whenCreateFormOpen_doesNotDuplicateScreen() {
        // Given: форма создания открыта, в поле заголовка введён текст
        awaitCreateScreen()
        titleField().performTextInput(TYPED_TITLE)
        composeTestRule.waitForIdle()

        // When: лаунчер повторно доставляет intent шортката
        deliverShortcutIntent()
        composeTestRule.waitForIdle()

        // Then: экран создания по-прежнему ровно один, введённый текст на месте
        composeTestRule.onAllNodesWithText(newItemTitle).assertCountEquals(1)
        composeTestRule.onNodeWithText(TYPED_TITLE).assertIsDisplayed()
    }

    @Test
    fun repeatedShortcutIntent_whenCreateFormInMiddleOfStack_noNavigation() {
        // Given: запись в БД и экран деталей, открытый поверх формы создания
        val itemId = insertSeedItem()
        deliverReminderOpenIntent(itemId)
        awaitItemDetail()

        // And: форма создания осталась в back stack, но уже не скомпована
        composeTestRule.waitUntil(timeoutMillis = NAVIGATION_TIMEOUT_MS) {
            composeTestRule.onAllNodesWithText(newItemTitle).fetchSemanticsNodes().isEmpty()
        }

        // When: лаунчер повторно доставляет intent шортката
        deliverShortcutIntent()
        composeTestRule.waitForIdle()

        // Then: навигации нет — экран деталей на месте, второй экран создания не появился
        composeTestRule.onNodeWithContentDescription(closeLabel).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(newItemTitle).assertCountEquals(0)
    }

    @Test
    fun recreate_afterFormClosed_doesNotReopenCreateScreen() {
        // Given: форма создания открыта шорткатом и закрыта «назад»
        awaitCreateScreen()
        pressBack()
        awaitEventsScreen()

        // When: система пересоздаёт Activity
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        // Then: экран создания не открылся заново — виден главный экран
        awaitEventsScreen()
        composeTestRule.onAllNodesWithText(newItemTitle).assertCountEquals(0)
        composeTestRule.onNodeWithContentDescription(addItemLabel).assertIsDisplayed()
    }

    @Test
    fun plainRelaunch_whenAppRunning_returnsToCurrentScreen() {
        // Given: приложение открыто по шорткату, форма закрыта — мы на главном экране
        awaitCreateScreen()
        pressBack()
        awaitEventsScreen()

        // When: приложение снова открывают обычным launch-intent (singleTop → onNewIntent)
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onNewIntent(mainIntent)
        }
        composeTestRule.waitForIdle()

        // Then: экран создания не открылся, мы остались на главном экране
        awaitEventsScreen()
        composeTestRule.onAllNodesWithText(newItemTitle).assertCountEquals(0)
        composeTestRule.onNodeWithContentDescription(addItemLabel).assertIsDisplayed()

        // После setIntent(mainIntent) в onNewIntent intent Activity перестаёт совпадать
        // со стартовым — ActivityScenario игнорирует lifecycle-события и close() зависает
        // (caveat «setIntent + recreation ломает cleanup»). Возвращаем соответствие явно.
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.setIntent(shortcutIntent)
        }
    }

    @Test
    fun staticShortcuts_containsAddItemWithLabel() {
        // Given: статические шорткаты приложения
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        // «Статические» шорткаты в платформенном API называются manifest shortcuts
        val staticShortcuts = shortcutManager.getManifestShortcuts()

        // Then: ровно один шорткат add_item с подписью R.string.add_item
        val addItemShortcuts = staticShortcuts.filter { it.id == ADD_ITEM_SHORTCUT_ID }
        assertEquals("Ожидался ровно один статический шорткат add_item", 1, addItemShortcuts.size)
        assertEquals(
            "Подпись шортката должна совпадать с R.string.add_item",
            addItemLabel,
            addItemShortcuts.first().shortLabel
        )

        // And: иконка сверяется по цепочке манифест → shortcuts.xml → android:icon
        // (ShortcutInfo.icon — скрытый API; AAPT компилирует ссылку в числовой id ресурса)
        assertEquals(
            "Иконка шортката должна ссылаться на @drawable/ic_shortcut_add",
            R.drawable.ic_shortcut_add,
            parseShortcutsXml().iconResId
        )
    }

    @Suppress("DEPRECATION")
    @Test
    fun shortcutActionConsistent_betweenShortcutsXmlAndManifest() {
        // Given: action, объявленный в res/xml/shortcuts.xml
        val xmlAction = parseShortcutsXml().intentAction

        // Then: action совпадает с контрактом ShortcutLaunchResolver
        assertEquals(
            "Action в shortcuts.xml должен совпадать с ShortcutLaunchResolver.ACTION_CREATE_ITEM",
            ShortcutLaunchResolver.ACTION_CREATE_ITEM,
            xmlAction
        )

        // And: на этот action отвечает ровно одна activity — MainActivity
        val intent =
            Intent(ShortcutLaunchResolver.ACTION_CREATE_ITEM)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setPackage(context.packageName)
        val resolved = context.packageManager.queryIntentActivities(intent, 0)
        assertEquals("На action шортката должен отвечать только MainActivity", 1, resolved.size)
        assertEquals(
            "Activity, обрабатывающая action шортката, должна быть MainActivity",
            MainActivity::class.java.name,
            resolved.first().activityInfo.name
        )
    }

    /**
     * Доставляет intent шортката в уже запущенную Activity (`singleTop` → `onNewIntent`).
     */
    private fun deliverShortcutIntent() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onNewIntent(shortcutIntent)
        }
    }

    /**
     * Открывает экран деталей записи поверх текущего экрана — тем же путём, что и тап по уведомлению.
     */
    private fun deliverReminderOpenIntent(itemId: Long) {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                action = ReminderIntentContract.ACTION_OPEN_FROM_REMINDER
                putExtra(ReminderIntentContract.EXTRA_ITEM_ID, itemId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onNewIntent(intent)
        }
    }

    /**
     * «Назад» — кнопка возврата в top bar экрана создания.
     *
     * Espresso в compile classpath модуля `app` недоступен (espresso-core приходит туда
     * только runtime-транзитивно через compose ui-test), поэтому назад эмулируется
     * собственной кнопкой приложения — она вызывает тот же `navController.popBackStack()`.
     */
    private fun pressBack() {
        composeTestRule.onNodeWithContentDescription(cancelLabel).performClick()
        composeTestRule.waitForIdle()
    }

    private fun awaitCreateScreen() {
        composeTestRule.waitUntil(timeoutMillis = NAVIGATION_TIMEOUT_MS) {
            composeTestRule.onAllNodesWithText(newItemTitle).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitEventsScreen() {
        composeTestRule.waitUntil(timeoutMillis = NAVIGATION_TIMEOUT_MS) {
            composeTestRule.onAllNodesWithContentDescription(addItemLabel).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitItemDetail() {
        composeTestRule.waitUntil(timeoutMillis = NAVIGATION_TIMEOUT_MS) {
            composeTestRule.onAllNodesWithContentDescription(closeLabel).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Поле заголовка формы — первое текстовое поле экрана создания
     * (порядок полей: заголовок → описание → дата).
     */
    private fun titleField(): SemanticsNodeInteraction = composeTestRule.onAllNodes(hasSetTextAction()).onFirst()

    private fun insertSeedItem(): Long =
        runBlocking {
            dao.insertItem(
                ItemEntity(
                    title = SEED_ITEM_TITLE,
                    details = "",
                    timestamp = System.currentTimeMillis(),
                    colorTag = null,
                    displayOption = DisplayOption.DAY.name
                )
            )
        }

    /**
     * Разбор `res/xml/shortcuts.xml`: иконка шортката и action его intent.
     */
    private fun parseShortcutsXml(): ShortcutXml {
        val parser = context.resources.getXml(R.xml.shortcuts)
        var iconResId = 0
        var intentAction: String? = null
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        // @drawable/... компилируется в числовую ссылку — читаем как resource id
                        SHORTCUT_TAG ->
                            iconResId = parser.getAttributeResourceValue(ANDROID_NS, "icon", 0)
                        INTENT_TAG -> intentAction = parser.getAttributeValue(ANDROID_NS, "action")
                    }
                }
                eventType = parser.next()
            }
        } finally {
            parser.close()
        }
        return ShortcutXml(iconResId = iconResId, intentAction = intentAction)
    }

    private data class ShortcutXml(
        val iconResId: Int,
        val intentAction: String?
    )

    private companion object {
        const val ADD_ITEM_SHORTCUT_ID = "add_item"
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        const val SHORTCUT_TAG = "shortcut"
        const val INTENT_TAG = "intent"
        const val NAVIGATION_TIMEOUT_MS = 5_000L
        const val TYPED_TITLE = "TypedShortcutTitle"
        const val SEED_ITEM_TITLE = "SeedItem"
    }
}

/**
 * Activity, запущенная правилом: compose-правило умеет стартовать Activity с произвольным
 * intent, поэтому провайдер достаёт её из сценария.
 */
private fun activityFromRule(rule: ActivityScenarioRule<MainActivity>): MainActivity {
    var activity: MainActivity? = null
    rule.scenario.onActivity { activity = it }
    return checkNotNull(activity) { "Activity не была запущена правилом" }
}
