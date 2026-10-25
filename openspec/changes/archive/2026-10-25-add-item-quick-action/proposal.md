# Proposal

## Why

Создание новой записи — главное действие приложения, но сейчас до этого экрана можно добраться только запустив приложение и нажав FAB на главном экране. Долгое нажатие на иконку приложения в лаунчере — стандартный механизм Android для быстрых действий: пункт «Добавить запись» сокращает путь до одного жеста ещё до открытия приложения.

## What Changes

- Добавляется статический шорткат лаунчера `add_item` (`app/src/main/res/xml/shortcuts.xml` + meta-data `android.app.shortcuts`) с подписью из существующей строки `add_item` (en «Add Item» / ru «Добавить запись») и собственной иконкой «плюс» — новый вектор `res/drawable/ic_shortcut_add.xml`.
- Шорткат использует собственный action `com.dayscounter.action.CREATE_ITEM`; у MainActivity добавляется соответствующий intent-filter с `category.DEFAULT`.
- Meta-data `android.app.shortcuts` размещается на всех шести launcher-алиасах (`MainActivityAliasIcon1`…`MainActivityIcon6`), чтобы шорткат не пропадал при смене иконки приложения.
- MainActivity переключается на `launchMode="singleTop"` и обрабатывает intent шортката в двух точках: `onCreate` (только при первом создании, `savedInstanceState == null`) и `onNewIntent` (+ `setIntent`) — выставляется одноразовый сигнал «открыть создание записи»; существующая обработка reminder-интентов сохраняется.
- `RootScreen` после построения графа навигации выполняет запрос: `navController.navigate(Screen.CreateItem.route)` с guard'ом — если экран создания уже есть в back stack (включая случай, когда он не наверху), повторно он не открывается. Стартовый маршрут графа не меняется; back stack: events → create_item — тот же destination, что и по FAB.
- Открытие экрана по шорткату логируется существующим событием аналитики `UserActionType.CREATE` — тем же, что и у FAB-пути.

## Capabilities

### New Capabilities

- `launcher-shortcut`: статический шорткат лаунчера «Добавить запись» — объявление с иконкой и подписью, запуск экрана создания записи из любого состояния приложения (холодный старт, свёрнутое приложение, уже открытый экран), корректный back stack без дублирования экрана, аналитика.

### Modified Capabilities

- Нет.

## Impact

- `app/src/main/res/xml/shortcuts.xml` — новый файл: объявление статического шортката.
- `app/src/main/res/drawable/ic_shortcut_add.xml` — новый файл: векторная иконка шортката (круг с плюсом).
- `app/src/main/AndroidManifest.xml` — meta-data `android.app.shortcuts` на шести activity-alias, intent-filter action шортката у MainActivity, `launchMode="singleTop"`.
- `app/src/main/java/com/dayscounter/navigation/ShortcutLaunchResolver.kt` — новый файл: константа action шортката и чистая функция разбора action, покрывается JVM-тестом; guard от дублирования — инлайн-проверка back stack в `RootScreen`.
- `app/src/main/java/com/dayscounter/MainActivity.kt` — обработка intent шортката (onCreate/onNewIntent): одноразовый сигнал навигации, reminder-логика не меняется.
- `app/src/main/java/com/dayscounter/ui/screens/root/RootScreen.kt` — выполнение навигационного запроса после построения графа NavHost.
- Строковые ресурсы не меняются — переиспользуется `add_item`.
- Тесты: `app/src/test/` (JVM — чистый резолвер), `app/src/androidTest/` (холодный старт по intent, поведение singleTop, back stack, ShortcutManager).
