# Design

## Context

См. proposal.md (Why). Текущее состояние кода (проверено):

- Маршрут создания: `Screen.CreateItem` — `"create_item"`, без аргументов (`navigation/Screen.kt`). FAB-путь: `RootScreenComponents.kt` → `EventsScreenContent.onCreateClick` логирует `AnalyticsEvent.UserAction(UserActionType.CREATE)` и выполняет `navController.navigate(Screen.CreateItem.route)`; destination регистрируется в `createEditScreenDestination` и рендерит `CreateEditScreen(itemId = null)`.
- `MainActivity` уже обрабатывает внешние intent: reminder-интент разбирается в `onCreate` только при `savedInstanceState == null` (`shouldHandleReminderIntent`) и в `onNewIntent` (с `setIntent`). `launchMode` не задан (standard): повторный тап по иконке при живом таске выводит его на передний план без доставки intent (`onNewIntent` не вызывается), а intent шортката при живом таске создал бы второй экземпляр Activity поверх корня задачи.
- Точка входа лаунчера — activity-alias: `.MainActivityAliasIcon1` (включён по умолчанию) + `.MainActivityIcon2`…`MainActivityIcon6` (смена иконки через экран «Тема и иконка»). У самой MainActivity intent-filter MAIN/LAUNCHER отсутствует.
- Прецедент «внешний intent → навигация»: `pendingOpenDetailItemId: Long?` (one-shot) + `LaunchedEffect` в `RootScreen`. Прецедент тестируемого парсера intent: `reminder/ReminderIntentContract.kt` + `ReminderIntentParser.kt` (pure-функция + extension).
- Аналитика: `ScreenView(CREATE_EDIT)` логируется внутри composable-роута автоматически; `UserActionType.CREATE` — только на FAB-пути.
- Векторной иконки «плюс» в `res/drawable/` нет; строка `add_item` существует (en «Add Item» / ru «Добавить запись»).
- minSdk 26 (статические шорткаты доступны с API 25); флейворы github/rustore без per-flavor source set — один манифест на оба; deep links в проекте отсутствуют; `applicationId` — `com.dayscounter`.

## Goals / Non-Goals

**Goals:**

- Статический шорткат «Добавить запись», видимый сразу после установки для любой из шести иконок приложения, с собственной иконкой «плюс».
- Открытие экрана создания записи из любого состояния приложения (холодный старт, свёрнутое приложение, любой текущий экран) — тот же destination, что и по FAB.
- Один экземпляр экрана создания записи в стеке; back stack: events → create_item; стартовый маршрут графа не меняется.
- Аналитика шорткат-пути существующим событием.

**Non-Goals:**

- Динамические и pinned-шорткаты, другие быстрые действия (бэкап, поиск и т.п.).
- Deep links (http/https), App Links.
- Изменение поведения FAB, строковых ресурсов и структуры маршрутов (`CreateItem`/`EditItem` остаются раздельными).
- Сохранение черновика формы при потере ввода платформенными механизмами.

## Decisions

### D1. Статический шорткат (res/xml/shortcuts.xml), не динамический

Шорткат объявляется в `app/src/main/res/xml/shortcuts.xml` и подключается meta-data `android.app.shortcuts` у launcher-точек входа. Виден в лаунчере сразу после установки без кода публикации.
Альтернатива (отклонена): динамический `ShortcutManager` — требует публикации в рантайме, добавляет состояние и код ради действия, известного на этапе сборки.

### D2. Собственный action + intent-filter у MainActivity

Action шортката — `com.dayscounter.action.CREATE_ITEM`; у MainActivity добавляется intent-filter с этим action и `category="android.intent.category.DEFAULT"` (статические шорткаты шлют intent с этим category; без фильтра платформа не стартует шорткат).
Альтернативы (отклонены): переиспользовать MAIN/LAUNCHER intent-filter — другой semantics запуска и его у MainActivity просто нет; различать источник по extra — extras не различаются в intent-filter, источник запуска становится непроверяемым.

### D3. Meta-data шортката на всех шести activity-alias

`<meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts"/>` размещается внутри каждого из шести launcher-алиасов (`.MainActivityAliasIcon1`…`.MainActivityIcon6`), а не на MainActivity: у неё нет MAIN/LAUNCHER, и meta-data на ней не привяжет шорткат к иконке лаунчера. Активен в каждый момент ровно один алиас — шорткат обязан следовать за включённым, иначе после смены иконки пункт исчезнет (требование спеки «Шорткат доступен при любой иконке»). Intent-filter action шортката при этом остаётся один — на MainActivity, общей цели всех алиасов (`targetActivity`).
Альтернативы (отклонены): meta-data только на включённом по умолчанию алиасе — теряется шорткат при смене иконки; meta-data на MainActivity — не работает без MAIN/LAUNCHER.

### D4. `launchMode="singleTop"` + обработка в onCreate (только первый запуск) и onNewIntent

MainActivity переключается на `launchMode="singleTop"`. Intent шортката обрабатывается в двух точках: `onCreate` — только при `savedInstanceState == null` (при пересоздании Activity платформа доставляет тот же launch-intent повторно, разбирать его нельзя — тот же принцип, что у `shouldHandleReminderIntent`) — и `onNewIntent` (прямая доставка в работающий экземпляр; существующая reminder-логика `onNewIntent` сохраняется). Неизвестные action игнорируются.
Назначение `singleTop` — доставка intent шортката в живой корневой экземпляр через `onNewIntent` вместо создания второго экземпляра Activity поверх задачи. Видимое поведение обычного запуска не меняется: тап по иконке при живом таске и при `standard` выводил задачу на передний план с сохранением состояния; отличие внутреннее — доставленный при `singleTop` MAIN-intent приходит в `onNewIntent` и игнорируется как чужой action. Требование спеки «Обычный запуск не пересоздаёт работающее приложение» остаётся как регрессионный guard.
Альтернативы (отклонены): оставить standard — каждый шорткат создаёт второй экземпляр Activity с дублирующим back stack; `singleTask`/`singleInstance` — избыточная перестройка launch-семантики всего приложения.

### D5. Одноразовый сигнал в MainActivity, навигация — в RootScreen после построения графа

`MainActivity` не знает про навигацию: она выставляет одноразовый сигнал `pendingCreateItem: Boolean` (по образцу существующего `pendingOpenDetailItemId`) и сбрасывает его через callback после обработки — как `onPendingOpenHandled`. `RootScreen` после построения графа (`LaunchedEffect`) читает сигнал и выполняет `navController.navigate(Screen.CreateItem.route)`. Back stack: форма кладётся поверх текущего стека (обычно events → create_item — идентично FAB), стартовый destination не меняется.
Альтернативы (отклонены): навигация прямо из Activity — нарушает существующее разделение слоёв; обобщить `pendingOpenDetailItemId` в единый sealed-тип «отложенная навигация» — рефакторинг существующего reminder-пути без нужды; менять startDestination по intent — ломает back stack («назад» закрывает приложение).

### D6. Guard от дублирования по всему back stack

В точке выполнения навигации проверяется весь back stack: экран создания присутствует в стеке, если `navController.getBackStackEntry(Screen.CreateItem.route)` не бросил `IllegalArgumentException` (эквивалент проверки «route есть в любом месте стека»; `currentBackStack` недоступен — помечен `@RestrictTo(LIBRARY_GROUP)`, ловится Android Lint RestrictedApi). Если экран создания присутствует (наверху или под другим экраном), навигация не выполняется — открытая форма с несохранённым вводом остаётся как есть. `EditItem` — отдельный маршрут: шорткат при открытом редактировании открывает чистую форму создания поверх (разные экраны, «назад» возвращается к редактированию).
Альтернатива (отклонена): всегда выполнять navigate — поверх формы с несохранённым вводом открывается её чистая копия, теряется контекст пользователя и раздувается back stack.

### D7. Чистый резолвер в navigation/, аналитика — существующим событием

`navigation/ShortcutLaunchResolver.kt`: константа `ACTION_CREATE_ITEM = "com.dayscounter.action.CREATE_ITEM"` и чистая функция `isCreateItemAction(action: String?): Boolean` — по прецеденту `ReminderIntentParser` (pure-функция покрывается JVM-тестом без Android). Guard от дублирования — инлайн-проверка back stack в `RootScreen` (см. D6): отдельная функция-обёртка над оператором отрицания логики решения не содержит. В точке выполнения навигации логируется `AnalyticsEvent.UserAction(UserActionType.CREATE)` — тот же event, что у FAB; `ScreenView(CREATE_EDIT)` залогирует сам composable-роут.
Альтернативы (отклонены): резолвер, возвращающий решение по аналитике, — лишний уровень для одного события; новый `UserActionType` для источника шортката — не запрашивалось.

### D8. Собственная иконка шортката; подпись — существующая строка `add_item`

Слот иконки пункта лаунчер резервирует всегда — без `android:icon` рисуется серая заглушка. Шорткат получает статичный вектор `res/drawable/ic_shortcut_add.xml` (круг с плюсом, не adaptive — это слот пункта меню). Подпись — `@string/add_item`, новых строк не заводим (подпись обязательна, без неё платформа отклоняет шорткат).
Альтернативы (отклонены): `@mipmap/ic_launcher_*` — иконка приложения, а не действия; остаться без иконки — серая заглушка; новая строка — дублирование существующей.

### D9. Тесты

- JVM (TDD): чистая функция разбора action резолвера — наш action, чужой/null action; guard от дублирования — androidTest-сценарии повторной доставки.
- androidTest: `ActivityScenario` с intent шортката — холодный старт открывает форму создания, «назад» → главный экран; повторная доставка intent при открытой форме не дублирует экран; пересоздание Activity после закрытия формы не возвращает на неё; `ShortcutManager.getStaticShortcuts()` содержит `add_item` с подписью; иконка и action сверяются по цепочке манифест → shortcuts.xml → атрибуты (публичный `ShortcutInfo.icon` для манифестных шорткатов всегда null); согласованность action по `queryIntentActivities` (shortcuts.xml ↔ intent-filter ↔ Kotlin-константа).
- Манифест и ресурсы проверяются фактом (androidTest + ручная проверка в лаунчере эмулятора), моков манифеста нет.

## Risks / Trade-offs

- [Подпись «Добавить запись» (15 символов) длиннее рекомендованных ~10 символов — в части лаунчеров возможна обрезка] → Новая короткая строка не заводится осознанно (переиспользуем `add_item`, см. D8); читаемость подписи фиксируется в задаче ручной проверки 5.1.
- [Не все лаунчеры показывают статические шорткаты; отображение до ~4 шорткатов на приложение] → Поведение вне контроля приложения; проверка в дефолтном лаунчере эмулятора (задача ручной проверки).
- [Часть лаунчеров при выборе шортката пересоздаёт корневую Activity (intent шортката отличается от базового launch-intent задачи — платформа перебейзливает корень): несохранённый ввод уже открытой формы может быть потерян] → Стандартное платформенное поведение, манифестного переключателя нет; guard покрывает прямые доставки intent (androidTest); сохранение черновика — вне scope.
- [Шесть копий meta-data в манифесте должны оставаться синхронными] → Ресурс один (`@xml/shortcuts`), различается только размещение; наличие шортката у каждой иконки проверяется androidTest и ручной сменой иконки.
- [Лаунчер кэширует шорткаты: после смены иконки пункт может появляться с задержкой] → Зафиксировать наблюдение в задаче ручной проверки; требование спеки — о доступности шортката у включённой иконки, а не о мгновенности кэша лаунчера.
- [Пересоздание Activity повторно доставляет launch-intent в `onCreate`] → закрыто механизмом D4 (разбор только при `savedInstanceState == null`); проверка — androidTest-сценарий «поворот после закрытия формы».
