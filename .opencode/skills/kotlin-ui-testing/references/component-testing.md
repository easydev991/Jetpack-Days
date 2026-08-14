# Component testing — изолированные UI-тесты Compose

Компонентные тесты проверяют отдельный Compose-компонент без реальной
Activity. В JetpackDays так тестируются `DaysCountText`, `ColorSelector`,
`ColorTagFilterDialog`, `SaveButton`, секции формы CreateEdit.

Примеры: `app/src/androidTest/java/com/dayscounter/ui/ds/DaysCountTextTest.kt`,
`ui/screens/createedit/ColorSelectorUiTest.kt`,
`ui/screens/events/ColorTagFilterDialogTest.kt`.

## Скелет компонентного теста

```kotlin
import androidx.compose.ui.test.junit4.v2.createComposeRule

class SomeComponentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun subject_whenCondition_thenResult() {
        // Given
        val testText = "5 дней"

        // When
        composeTestRule.setContent {
            JetpackDaysTheme {
                DaysCountText(formattedText = testText)
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(testText)
            .assertExists()
            .assertTextEquals(testText)
    }
}
```

Ключевое:
- `createComposeRule()` — v2 API (`androidx.compose.ui.test.junit4.v2`),
  без Activity, без `@RunWith`
- `setContent { JetpackDaysTheme { ... } }` — компонент в теме приложения
  (DaysCountTextTest обходится без темы — если компонент не зависит от
  неё, обёртка необязательна)
- Состояние компонента — через `mutableStateOf` локально в тесте:

```kotlin
var selectedColor by mutableStateOf<Color?>(customColor)

composeTestRule.setContent {
    JetpackDaysTheme {
        ColorSelector(
            selectedColor = selectedColor,
            onColorSelected = { selectedColor = it }
        )
    }
}
```

## Изоляция зависимостей — TestViewModel

Для тестов экрана CreateEdit используется factory-функция из
`app/src/androidTest/java/com/dayscounter/ui/screens/createedit/TestViewModel.kt`:

```kotlin
fun createTestViewModel(): CreateEditScreenViewModel =
    CreateEditScreenViewModel(
        repository = createTestItemRepository(),
        resourceProvider = createTestResourceProvider(),
        savedStateHandle = SavedStateHandle(),
        analyticsService = AnalyticsService(listOf(NoopAnalyticsProvider()))
    )
```

Фейковый репозиторий — анонимный `object : ItemRepository` с пустыми
реализациями (все Flow возвращают `flowOf()`, suspend-методы — no-op):

```kotlin
private fun createTestItemRepository(): ItemRepository =
    object : ItemRepository {
        override fun getAllItems(): Flow<List<Item>> = flowOf()
        override fun getAllItems(sortOrder: SortOrder): Flow<List<Item>> = flowOf()
        override suspend fun getItemById(id: Long): Item? = null
        override fun getItemFlow(id: Long): Flow<Item?> = flowOf()
        override fun searchItems(query: String): Flow<List<Item>> = flowOf()
        override suspend fun insertItem(item: Item): Long = 0L
        override suspend fun updateItem(item: Item) {}
        override suspend fun deleteItem(item: Item) {}
        override suspend fun deleteAllItems() {}
        override suspend fun getItemsCount(): Int = 0
    }
```

Фейковый ResourceProvider — пустые строки, без `!!`:

```kotlin
internal fun createTestResourceProvider(): ResourceProvider =
    object : ResourceProvider {
        override fun getString(resId: Int, vararg formatArgs: Any): String = ""
        override fun getQuantityString(resId: Int, quantity: Int, vararg formatArgs: Any): String = ""
        override fun getYearsString(quantity: Int): String = ""
        override fun getMonthsString(quantity: Int): String = ""
    }
```

Правила для фейков:
- Никакого `!!` — все методы возвращают значения по умолчанию, не null
- `flowOf()` для Flow-методов, `0L` / no-op для suspend
- Factory-функции лежат рядом с тестами, которые их используют
