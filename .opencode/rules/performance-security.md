# Производительность и безопасность

## Производительность

- Корутины — `viewModelScope.launch` (автоотмена); `StateFlow` +
  `SharingStarted.WhileSubscribed(5000)`
- `rememberSaveable` — состояние при реконфигурации и локальные поля форм;
  `rememberLazyListState()` — позиция прокрутки
- `LazyColumn` с `key = { it.id }`; Room DAO через `Flow` для реактивных
  запросов
- `searchItems` — LIKE без индексов (текущее состояние)

## Безопасность

- `TextField` с `minLines` ограничивает ввод (detailsSection.kt:199)
- Room для безопасного локального хранения; безопасное разворачивание
  опционалов: `checkNotNull`, `?.let`, `?:`
- ImportBackupUseCase: обработка `FileNotFoundException`, `IOException`,
  `SerializationException`, `SQLException`; JSON с `ignoreUnknownKeys = true`
  для неизвестных полей; фильтрация дубликатов по
  title/details/timestamp/displayOption; `contentResolver.openInputStream()`
  закрывается через `use`
