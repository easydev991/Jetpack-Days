# Design

## Context

См. proposal.md — Why. Текущее состояние, существенное для подхода (все факты сверены с кодом):

- `DetailScreenViewModel.observeItem()` (`DetailScreenViewModel.kt:72-81`) собирает `getItemFlow(itemId).filterNotNull().collect { updateSuccessState(it) }`; `updateSuccessState` идемпотентен — пишет nextState только при изменении (`DetailScreenViewModel.kt:97`).
- `DetailScreenState.Error(message: String)` существует (`DetailScreenViewModel.kt:150-152`); `DetailContentByState` рендерит его через `ErrorContent(message = uiState.message)` (`DetailContent.kt:85-90`); форма `Error(message)` пинится `DetailScreenStateTest` (`ui/state/DetailScreenStateTest.kt:68,168,181`).
- Nav icon «назад» в `DetailTopAppBar` рисуется безусловно, action-кнопки — только в Success (`DetailAppBar.kt:40-56`); подтверждение удаления закрывает экран сразу: `onConfirmDelete = { viewModel.confirmDelete(); onBackClick() }` (`DetailScreen.kt:131-134`), при этом scope VM живёт до уничтожения back stack entry — null-эмиссия после удаления может прилететь до отмены корутины.
- Тесты пинят текущее поведение: Loading при отсутствии записи (unit `DetailScreenViewModelTest.kt:93`, integration `DetailScreenViewModelIntegrationTest.kt:110`) и Success после собственного удаления (`whenconfirmdelete_thendeletesitem` — `DetailScreenViewModelTest.kt:338`, `whenstaleemissionafterdelete_thenreminderisnotrestored` — `:371`).
- Прецедент того же сценария в VM: `CreateEditScreenViewModel` при отсутствии записи эмитит `Error(resourceProvider.getString(ResourceIds.EVENT_NOT_FOUND))` + `logger.w` (`CreateEditScreenViewModel.kt:163-167`).
- Строка `event_not_found` уже существует: `values/strings.xml:153` «Event not found», `values-ru/strings.xml:153` «Событие не найдено»; константа `ResourceIds.EVENT_NOT_FOUND` (`ResourceIds.kt:25`).
- Решения владельца (зафиксированы через AskUserQuestion): транзит состояния — Error только из Loading; имя change — `fix-detail-screen-infinite-loading`.

## Goals / Non-Goals

**Goals:**

- Экран с несуществующим id → `DetailScreenState.Error` с локализованным сообщением вместо вечного Loading.
- Идемпотентная обработка повторных null-эмиссий Room.
- Сохранение контракта транзита удаления: Success после собственного удаления, без Error на закрывающемся экране.
- Минимальный диф: только VM + wiring + тесты.

**Non-Goals:**

- Изменение UI-компонентов (`DetailStates.kt`, `DetailAppBar.kt`, `DetailContent.kt`) — уже поддерживают Error.
- Изменение Success-флоу, напоминаний, диалога и логики удаления.
- Перевод в Error из Success при «внешнем» удалении записи во время видимого экрана — в текущих флоу приложения недостижимо (см. D2); поведение остаётся как сегодня.
- Новые строки локализации, миграции БД, изменения бэкап-формата.
- Изменение формы sealed-класса `DetailScreenState` (в т.ч. `Error(message: String)`).

## Decisions

### D1. Убрать `filterNotNull()`, явная ветка null-эмиссии

`observeItem()` собирает поток без фильтра: `if (item == null) updateErrorState() else updateSuccessState(item)`. Переход в Error логируется однократно через `logger.w("DetailScreenViewModel", "Событие не найдено: $itemId")` — по паттерну `CreateEditScreenViewModel.kt:167`.

Альтернативы:
- One-shot проверка `getItemById` до подписки (как в CreateEdit). Отклонено: теряется реактивность Room-потока — текущий контракт экрана — живая подписка (тест `whenFlowEmitsNewItem_thenViewModelUpdatesState` — `DetailScreenViewModelIntegrationTest.kt:205`); one-shot решил бы только стартовый кейс.
- Менять форму состояния (`Error` → data object / `@StringRes`). Отклонено: ломает `DetailScreenStateTest` и сигнатуру `ErrorContent`, больший диф без выгоды.

### D2. Guard: Error устанавливается только из Loading (решение владельца)

`updateErrorState()` переводит состояние в `Error` только если `_uiState.value is Loading`. Следствия:
- Транзит после `confirmDelete`: экран уже закрывается, null-эмиссия не переводит его в Error — тесты `whenconfirmdelete_thendeletesitem` и `whenstaleemissionafterdelete_thenreminderisnotrestored` остаются пинить Success без правок.
- Идемпотентность повторных null — по построению guard'а (второй null попадает в не-Loading).
- `updateSuccessState` остаётся без guard'а (как сегодня): контракт живой подписки сохраняется. Возврат в Success при ненулевой эмиссии после Error в spec delta не фиксируется — флоу недостижим (пока открыт экран деталей, изменять данные неоткуда, см. Non-Goals).

Альтернатива — Error из любого состояния без guard'а. Отклонена (решение владельца): недетерминизм в тестах удаления, возможный однокадровый флэш ошибки при popBackStack; выгода (Error при внешнем удалении при видимом экране) в текущих флоу недостижима — запись нельзя удалить, находясь на экране деталей (главный список недостижим, экран редактирования не удаляет). Компромисс зафиксирован: если такие флоу появятся, guard пересматривается отдельным change.

### D3. Источник сообщения: `ResourceProvider` в VM по прецеденту CreateEdit

Параметр `resourceProvider: ResourceProvider` в `DetailScreenViewModel` и его factory; в `RootScreenComponents.kt:132` передаётся существующий `AppModule.resourceProvider` (`AppModule.kt:70`). Сообщение: `resourceProvider.getString(ResourceIds.EVENT_NOT_FOUND)`.

Альтернативы:
- Резолв строки в UI (`stringResource(R.string.event_not_found)` в `DetailContentByState`, паттерн `MoreScreenUiState.Error`). Отклонено: потребовал бы менять форму состояния или сигнатуру `ErrorContent`, а состояние теряло бы самодостаточный текст для тестов/превью.
- Новый строковый ресурс. Отклонено: `event_not_found` уже есть в обеих локалях с точной семантикой.

### D4. Тест-двойники `ResourceProvider` — приватные фейки по образцу CreateEdit-тестов

Unit: `private class FakeResourceProvider : ResourceProvider` в `DetailScreenViewModelTest` (возвращает детерминированную строку по resId — образец `CreateEditScreenViewModelTest.kt:1022`). Integration: объектный фейк в `DetailScreenViewModelIntegrationTest` (образец `TestViewModel.kt:60`).

Альтернатива — реальный `ResourceProviderImpl(context)` в androidTest. Отклонено: ассерты не должны зависеть от локали устройства; проверяется факт Error-состояния и детерминированный текст фейка.

## Risks / Trade-offs

- [Устаревший Success при внешнем удалении во время видимого экрана] → В текущих флоу недостижимо; зафиксировано как известное ограничение guard'а (D2), при появлении таких флоу — отдельный change.
- [`Error` используется впервые в этом экране] → Рендер уже покрыт кодом (`ErrorContent`, `DetailTopAppBar`), форма состояния пинится существующим `DetailScreenStateTest` — контракты не меняются.
- [Room ре-эммитит null многократно] → Guard делает обработку идемпотентной, лог пишется только на переход Loading→Error.
- [Detekt] → Диф минимальный; `maxIssues: 5` не повышается, новые предупреждения исправляются до завершения (правило AGENTS.md).

## Migration Plan

Не требуется: БД/схема/бэкап-формат не затрагиваются. Rollback — revert коммита (`filterNotNull` возвращается, тесты обратно).

## Open Questions

Нет. Развилки решены владельцем (транзит — Error только из Loading; имя change), источник сообщения снят прецедентом CreateEdit.
