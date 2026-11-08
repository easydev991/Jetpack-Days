# Proposal

## Why

Экран деталей при открытии с несуществующим `itemId` навсегда остаётся в `DetailScreenState.Loading`: `DetailScreenViewModel.observeItem()` (`DetailScreenViewModel.kt:72-81`) собирает `repository.getItemFlow(itemId).filterNotNull()`, и `filterNotNull()` отбрасывает null-эмиссию «записи нет» — Flow не отдаёт ни одного значения, спиннер крутится бесконечно, выход только стрелкой назад. Воспроизводится при восстановлении back stack после смерти процесса (запись удалена) и при битом deep link/шорткате с устаревшим id. UI уже умеет показывать ошибку (`ErrorContent` в `DetailStates.kt`, sealed-вариант `DetailScreenState.Error(message)`), но VM никогда не эмитит его для этого случая. Баг закреплён тестами как «ожидаемое поведение»: unit `whenitemnotfound_thenremainsinloadingstate` и integration `whenItemDoesNotExistInDatabase_thenRemainsInLoadingState`.

## What Changes

- `DetailScreenViewModel.observeItem()`: убрать `filterNotNull()`; null-эмиссия обрабатывается явно — перевод в `DetailScreenState.Error` с локализованным сообщением.
- Сообщение об ошибке — существующая строка `R.string.event_not_found` (уже в `values/strings.xml` «Event not found» и `values-ru/strings.xml` «Событие не найдено»), резолвится в VM через `ResourceProvider` — по паттерну `CreateEditScreenViewModel` (`resourceProvider.getString(ResourceIds.EVENT_NOT_FOUND)`).
- Переход в `Error` ограничен состоянием `Loading` (решение владельца): null-эмиссия после собственного удаления (confirmDelete → экран сразу закрывается через popBackStack) не меняет состояние на закрывающемся экране; повторные null-эмиссии Room не дёргают состояние (идемпотентность по построению guard'а).
- Wiring: `DetailScreenViewModel` получает `ResourceProvider` (factory + точка вызова в `RootScreenComponents.kt`).
- Тесты, пинящие баг, переворачиваются на новое ожидание (TDD red → green): unit `whenitemnotfound_thenremainsinloadingstate` → `whenitemnotfound_thenshowserrorstate`; integration `whenItemDoesNotExistInDatabase_thenRemainsInLoadingState` → ожидание Error. Тесты транзита удаления (`whenconfirmdelete_thendeletesitem`, `whenstaleemissionafterdelete_thenreminderisnotrestored`) остаются пинить Success — guard сохраняет их контракт.
- UI не меняется: `ErrorContent` уже рендерит сообщение, стрелка назад в `DetailTopAppBar` рисуется безусловно во всех состояниях (проверено по коду), action-кнопки скрыты вне Success.

## Capabilities

### New Capabilities

- `detail-screen`: поведение экрана деталей при загрузке записи по id — успешная загрузка потока `ItemRepository.getItemFlow`, реакция на отсутствие записи (Error с сообщением вместо вечного Loading), стабильность состояния при повторных null-эмиссиях и после собственного удаления записи.

### Modified Capabilities

Нет. Спека `detail-screen-text-copy` покрывает только копирование текста в буфер обмена; поведение загрузки/отсутствия записи нигде не специфицировано.

## Impact

- Код: `app/src/main/java/com/dayscounter/ui/viewmodel/DetailScreenViewModel.kt` (observeItem, factory, новый параметр `resourceProvider`); `app/src/main/java/com/dayscounter/ui/screens/common/RootScreenComponents.kt` (передача `AppModule.resourceProvider` в factory).
- Тесты: `app/src/test/java/com/dayscounter/ui/viewmodel/DetailScreenViewModelTest.kt` (unit, FakeResourceProvider по образцу CreateEdit-тестов); `app/src/androidTest/java/com/dayscounter/ui/viewmodel/DetailScreenViewModelIntegrationTest.kt`.
- Локализация: новых строк нет — `event_not_found` уже есть в обеих локалях.
- UI/Compose: без изменений (`DetailStates.kt`, `DetailAppBar.kt`, `DetailContent.kt` не трогаются).
- БД/схема/бэкап-формат: без изменений (миграций нет, `BackupItem` не затрагивается).
- Активный change `add-rustore-in-app-review` не затрагивается.
