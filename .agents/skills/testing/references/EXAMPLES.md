# Примеры тестирования

> Версии зависимостей и их актуальные настройки — в `gradle/libs.versions.toml` и `app/build.gradle.kts`. Здесь приведены только паттерны.
>
> **Соглашения в проекте:**
> - Unit-тесты в `app/src/test/` — JUnit 5 (`@BeforeEach`, `@AfterEach`)
> - Интеграционные и UI тесты в `app/src/androidTest/` — JUnit 4 (`@Before`, `@After`, `@RunWith(AndroidJUnit4::class)`)
> - DI ручной, без Hilt
> - Без `!!` — только `?.`, `?:`, `let`, `checkNotNull`
> - Для ViewModel с `viewModelScope` — `StandardTestDispatcher` + `Dispatchers.setMain` / `resetMain`

## Unit-тесты ViewModels с MockK

### Базовый пример (JUnit 5, без Turbine)

```kotlin
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

    private lateinit var viewModel: MainScreenViewModel
    private val mockRepository: ItemRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { mockRepository.getAllItems() } returns flowOf(listOf(testItem))
        viewModel = MainScreenViewModel(mockRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadItems_whenRepositoryReturnsData_thenEmitsSuccessState() = runTest {
        // Given
        val expectedItems = listOf(testItem)
        every { mockRepository.getAllItems() } returns flowOf(expectedItems)

        // When
        viewModel.loadItems()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(MainScreenState.Success(expectedItems), state)
    }

    @Test
    fun deleteItem_whenCalled_thenRepositoryDeleteInvoked() = runTest {
        // When
        viewModel.deleteItem(testItem)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify { mockRepository.deleteItem(testItem) }
    }

    private companion object {
        val testItem = Item(
            id = 1L,
            title = "Тест",
            details = "Описание",
            timestamp = System.currentTimeMillis(),
            colorTag = 0xFFFF00,
            displayOption = DisplayOption.DAY
        )
    }
}
```

### Unit-тест с Turbine (для StateFlow с несколькими эмиссиями)

```kotlin
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailScreenViewModelTest {

    private lateinit var viewModel: DetailScreenViewModel
    private val mockRepository: ItemRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DetailScreenViewModel(
            repository = mockRepository,
            logger = NoOpLogger(),
            savedStateHandle = SavedStateHandle()
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadItem_whenItemExists_thenEmitsLoadingThenSuccess() = runTest {
        // Given
        val expected = testItem()
        coEvery { mockRepository.getItemById(1L) } returns expected

        // When
        viewModel.loadItem(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState is DetailScreenState.Loading)

            val successState = awaitItem()
            assertTrue(successState is DetailScreenState.Success)
            assertEquals(expected, (successState as DetailScreenState.Success).item)
        }
    }
}
```

## Интеграционные тесты DAO и Repository (JUnit 4, androidTest)

### Интеграционный тест DAO

```kotlin
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.database.dao.ItemDao
import com.dayscounter.data.database.entity.ItemEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemDaoTest {

    private lateinit var database: DaysDatabase
    private lateinit var dao: ItemDao

    @Before
    fun createDb() {
        database = Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                DaysDatabase::class.java
            )
            .allowMainThreadQueries()
            .build()
        dao = database.itemDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertItem_whenInsert_thenCanRetrieve() = runBlocking {
        // Given
        val item = testEntity(id = 1L)

        // When
        dao.insert(item)
        val retrieved = dao.getById(item.id)

        // Then
        assertNotNull(retrieved)
        assertEquals(item.title, retrieved?.title)
    }

    @Test
    fun getAllItems_whenMultipleItems_thenReturnsAll() = runBlocking {
        // Given
        listOf(
            testEntity(id = 1L, title = "Первый"),
            testEntity(id = 2L, title = "Второй"),
            testEntity(id = 3L, title = "Третий")
        ).forEach { dao.insert(it) }

        // When
        val retrievedItems = dao.getAll()

        // Then
        assertEquals(3, retrievedItems.size)
        assertEquals("Первый", retrievedItems[0].title)
    }

    @Test
    fun deleteItem_whenDeleted_thenCannotRetrieve() = runBlocking {
        // Given
        val item = testEntity(id = 1L)
        dao.insert(item)

        // When
        dao.delete(item)
        val retrieved = dao.getById(item.id)

        // Then
        assertNull(retrieved)
    }

    private fun testEntity(
        id: Long = 1L,
        title: String = "Тест"
    ) = ItemEntity(
        id = id,
        title = title,
        details = "Описание",
        timestamp = System.currentTimeMillis(),
        colorTag = 0xFFFF00,
        displayOption = "day"
    )
}
```

### Интеграционный тест Repository

```kotlin
@RunWith(AndroidJUnit4::class)
class ItemRepositoryIntegrationTest {

    private lateinit var database: DaysDatabase
    private lateinit var repository: ItemRepositoryImpl

    @Before
    fun setUp() {
        database = Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                DaysDatabase::class.java
            )
            .allowMainThreadQueries()
            .build()
        repository = ItemRepositoryImpl(database.itemDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun fullCycle_createReadUpdateDelete_thenWorksCorrectly() = runBlocking {
        // Given - Create
        val item = Item(
            title = "Тестовое событие",
            details = "Описание",
            timestamp = 1234567890000L,
            colorTag = 0xFFFF0000.toInt(),
            displayOption = DisplayOption.MONTH_DAY
        )

        // When - Insert
        val insertedId = repository.insertItem(item)

        // Then - Read
        val retrieved = repository.getItemById(insertedId)
        assertNotNull(retrieved)
        assertEquals("Тестовое событие", retrieved?.title)
    }
}
```

## Интеграционные тесты ViewModels (только для существующих)

⚠️ **Важно:** Создание новых интеграционных тестов ViewModels запрещено. Раздел только для обслуживания уже существующих тестов. В JetpackDays DI ручной — Hilt не используется.

```kotlin
@RunWith(AndroidJUnit4::class)
class DetailScreenViewModelIntegrationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: DaysDatabase
    private lateinit var repository: ItemRepository
    private lateinit var viewModel: DetailScreenViewModel
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        // Ручное создание зависимостей (без Hilt)
        database = Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                DaysDatabase::class.java
            )
            .allowMainThreadQueries()
            .build()
        repository = ItemRepositoryImpl(database.itemDao())

        savedStateHandle = SavedStateHandle(mapOf("itemId" to 1L))
        viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun whenItemExistsInDatabase_thenLoadsSuccessfully() = runTest {
        // Given
        val testItem = Item(
            id = 1L,
            title = "Тестовый элемент",
            details = "Описание",
            timestamp = System.currentTimeMillis(),
            colorTag = 0xFFFF00,
            displayOption = DisplayOption.DAY
        )
        repository.insertItem(testItem)

        // When - пересоздаём ViewModel, чтобы он подхватил элемент
        viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)

        // Then - Turbine для проверки эмиссий StateFlow
        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState is DetailScreenState.Loading)

            val successState = awaitItem()
            assertTrue(successState is DetailScreenState.Success)
            assertEquals(testItem.title, (successState as DetailScreenState.Success).item.title)
        }
    }
}
```

## UI-тесты Compose компонентов (androidTest)

### Простой UI-тест (без бизнес-логики)

```kotlin
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaysCountTextTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun daysCountText_whenToday_thenShowsToday() {
        // Given
        val item = Item(
            id = 1L,
            title = "Тест",
            details = "Описание",
            timestamp = System.currentTimeMillis(),
            colorTag = 0xFFFF00,
            displayOption = DisplayOption.DAY
        )

        // When
        composeTestRule.setContent {
            DaysCountText(item)
        }

        // Then
        composeTestRule.onNodeWithText("Сегодня").assertIsDisplayed()
    }
}
```

### UI-тест с взаимодействием и моком навигации

```kotlin
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenAddButtonClicked_thenNavigatesToCreateScreen() {
        // Given
        val mockNavController = mockk<NavController>(relaxed = true)

        composeTestRule.setContent {
            MainScreen(
                navController = mockNavController,
                viewModel = MainScreenViewModel(mockk(relaxed = true))
            )
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Добавить")
            .performClick()

        // Then
        verify { mockNavController.navigate("create_edit") }
    }
}
```

## Тестирование Flow с исключениями

> Turbine нельзя использовать для Flow, ошибки в которых обрабатываются через `catch` — оператор поглотит исключение раньше, чем `awaitItem()` его увидит. В таких случаях применяют `first()` или `collect()`.

### Тестирование IOException (обрабатывается в catch)

```kotlin
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class ExportBackupUseCaseTest {

    private val mockRepository: ItemRepository = mockk()
    private val useCase = ExportBackupUseCase(mockRepository)

    @Test
    fun invoke_whenRepositoryThrowsIOException_thenReturnsFailure() = runTest {
        // Given
        coEvery { mockRepository.getAllItems() } throws IOException("Нет доступа к базе данных")

        // When
        val result = useCase(Uri.parse("content://test"))

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BackupException)
    }
}
```

### Тестирование других исключений (пробрасываются дальше)

```kotlin
class ImportBackupUseCaseTest {

    private val mockRepository: ItemRepository = mockk()
    private val useCase = ImportBackupUseCase(mockRepository)

    @Test
    fun invoke_whenJsonInvalid_thenThrowsSerializationException() = runTest {
        // Given
        val invalidJson = "{ invalid json }"
        val uri = mockk<Uri>()

        // When & Then
        assertThrows<SerializationException> {
            useCase(uri, invalidJson)
        }
    }
}
```

### Мокирование логгера

```kotlin
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.sql.SQLException

class ItemRepositoryTest {

    private val mockDao: ItemDao = mockk()
    private val mockLogger: Logger = mockk()
    private val repository = ItemRepositoryImpl(mockDao, mockLogger)

    @Test
    fun insertItem_whenError_thenLogsError() = runTest {
        // Given
        coEvery { mockDao.insert(any()) } throws SQLException("Ошибка базы данных")

        // When
        runCatching { repository.insertItem(testItem()) }

        // Then
        verify {
            mockLogger.e(
                "ItemRepository",
                match { it.contains("Ошибка при вставке элемента") }
            )
        }
    }
}
```

## Тестирование Use Cases

### Простой тест Use Case

```kotlin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CalculateDaysDifferenceUseCaseTest {

    private val useCase = CalculateDaysDifferenceUseCase()

    @Test
    fun invoke_whenSameDay_thenReturnsZero() {
        // Given
        val date = LocalDate.now()

        // When
        val result = useCase(date, date)

        // Then
        assertEquals(0L, result.days)
    }

    @Test
    fun invoke_whenYesterday_thenReturnsOne() {
        // Given
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // When
        val result = useCase(yesterday, today)

        // Then
        assertEquals(1L, result.days)
    }
}
```

### Тест форматирования

```kotlin
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormatDaysTextUseCaseTest {

    private val mockResourceProvider: ResourceProvider = mockk()
    private val useCase = FormatDaysTextUseCase(mockResourceProvider)

    @Test
    fun invoke_whenZeroDays_thenReturnsToday() {
        // Given
        val daysDifference = DaysDifference(0L)
        every { mockResourceProvider.getString(R.string.today) } returns "Сегодня"

        // When
        val result = useCase(daysDifference)

        // Then
        assertEquals("Сегодня", result)
    }

    @Test
    fun invoke_whenMultipleDaysAgo_thenReturnsFormattedText() {
        // Given
        val daysDifference = DaysDifference(5L)
        every {
            mockResourceProvider.getQuantityString(R.plurals.days_ago, 5, 5)
        } returns "5 дней назад"

        // When
        val result = useCase(daysDifference)

        // Then
        assertEquals("5 дней назад", result)
    }
}
```

## Параметризированные тесты

```kotlin
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate

class DaysDifferenceTest {

    @ParameterizedTest
    @CsvSource(
        "0, 0",
        "1, 1",
        "2, 2",
        "10, 10",
        "365, 365"
    )
    fun calculateDaysDifference_withVariousOffsets_returnsCorrectDays(
        daysOffset: Long,
        expectedDays: Long
    ) {
        // Given
        val fromDate = LocalDate.now().minusDays(daysOffset)
        val toDate = LocalDate.now()

        // When
        val result = DaysCalculator.calculateDaysDifference(fromDate, toDate)

        // Then
        assertEquals(expectedDays, result.days)
    }
}
```

## Тестирование резервного копирования

### Тест экспорта

```kotlin
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExportBackupUseCaseTest {

    private val mockRepository: ItemRepository = mockk()
    private val useCase = ExportBackupUseCase(mockRepository)

    @Test
    fun invoke_whenItemsExist_thenReturnsBackupJson() = runTest {
        // Given
        val items = listOf(
            Item(
                id = 1L,
                title = "Первый",
                details = "Описание",
                timestamp = 1234567890000,
                colorTag = 0xFFFF00,
                displayOption = DisplayOption.DAY
            )
        )
        coEvery { mockRepository.getAllItems() } returns flowOf(items)

        // When
        val result = useCase(Uri.parse("content://test"))

        // Then
        assertTrue(result.isSuccess)
        val jsonString = result.getOrNull()
        assertTrue(jsonString?.contains("Первый") == true)
        assertTrue(jsonString?.contains("1234567890000") == true)
    }
}
```

### Тест импорта

```kotlin
class ImportBackupUseCaseTest {

    private val mockRepository: ItemRepository = mockk()
    private val useCase = ImportBackupUseCase(mockRepository)

    @Test
    fun invoke_whenValidJson_thenImportsItems() = runTest {
        // Given
        val jsonString = """
            [
                {
                    "title": "Импортированный",
                    "details": "Описание",
                    "timestamp": 1234567890000,
                    "colorTag": "#FFFF00",
                    "displayOption": "day"
                }
            ]
        """.trimIndent()
        val uri = mockk<Uri>()
        coEvery { mockRepository.insertItem(any()) } returns 1L

        // When
        val result = useCase(uri, jsonString)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockRepository.insertItem(any()) }
    }

    @Test
    fun invoke_whenDuplicateItem_thenSkipsDuplicate() = runTest {
        // Given
        val existingItem = Item(
            id = 1L,
            title = "Существующий",
            details = "Описание",
            timestamp = 1234567890000,
            colorTag = 0xFFFF00,
            displayOption = DisplayOption.DAY
        )
        val jsonString = """
            [
                {
                    "title": "Существующий",
                    "details": "Описание",
                    "timestamp": 1234567890000,
                    "colorTag": "#FFFF00",
                    "displayOption": "day"
                }
            ]
        """.trimIndent()
        val uri = mockk<Uri>()
        coEvery { mockRepository.getAllItems() } returns flowOf(listOf(existingItem))

        // When
        val result = useCase(uri, jsonString)

        // Then
        assertTrue(result.isSuccess)
        // Дубликат не должен быть добавлен
        coVerify(exactly = 0) { mockRepository.insertItem(any()) }
    }
}
```

## Чек-лист проверки тестов

Перед коммитом тестов:

- [ ] Все тесты проходят
- [ ] Имена тестов описательные (format: `functionName_whenCondition_thenExpectedResult`)
- [ ] Использован AAA паттерн (Arrange-Act-Assert или Given-When-Then)
- [ ] Один тест — одна проверка
- [ ] Тесты независимы друг от друга
- [ ] Тесты быстрые
- [ ] Моки настроены корректно
- [ ] Для Flow используются Turbine или `first()`
- [ ] Для исключений в Flow используется `first()` или `collect()` вместо Turbine
- [ ] Новые интеграционные тесты ViewModels не создаются (запрещено)
- [ ] Существующие интеграционные тесты ViewModels используют `runTest`, `MainDispatcherRule` и `Turbine`
- [ ] Unit-тесты используют JUnit 5 (`@BeforeEach`, `org.junit.jupiter.api.Assertions`)
- [ ] Android-тесты используют JUnit 4 (`@Before`, `org.junit.Assert`)
- [ ] Нет оператора `!!` — только `?.`, `?:`, `let`, `checkNotNull`
