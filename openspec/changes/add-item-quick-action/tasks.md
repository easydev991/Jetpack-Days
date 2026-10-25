# Tasks

## 1. Чистая логика резолвера шортката (TDD, JVM)

- [x] 1.1 Падающий тест `ShortcutLaunchResolverTest` (`app/src/test/.../navigation/`): наш action → `isCreateItemAction` возвращает true; чужой action и null → false. Проверка: `./gradlew testGithubDebugUnitTest --tests "com.dayscounter.navigation.ShortcutLaunchResolverTest"` — красный (класса ещё нет).
- [x] 1.2 Реализация `navigation/ShortcutLaunchResolver.kt`: константа `ACTION_CREATE_ITEM = "com.dayscounter.action.CREATE_ITEM"` + чистая функция `isCreateItemAction(action: String?): Boolean` (без `!!`, KDoc на русском). Проверка: тот же точечный прогон — зелёный.

## 2. Объявление шортката: ресурсы и манифест (проверка сборкой)

- [x] 2.1 Создать `app/src/main/res/xml/shortcuts.xml`: статический шорткат `add_item`, `shortcutShortLabel="@string/add_item"`, `android:icon="@drawable/ic_shortcut_add"`, intent с action из 1.2, `targetPackage="com.dayscounter"`, `targetClass="com.dayscounter.MainActivity"`; создать вектор `res/drawable/ic_shortcut_add.xml` (круг с плюсом). Проверка: `make build` — merge ресурсов проходит.
- [x] 2.2 `AndroidManifest.xml`: у MainActivity intent-filter (action шортката + `android.intent.category.DEFAULT`) и `android:launchMode="singleTop"`; meta-data `android.app.shortcuts` → `@xml/shortcuts` внутри каждого из шести activity-alias (`.MainActivityAliasIcon1`, `.MainActivityIcon2`…`.MainActivityIcon6`). Проверка: `make build` — манифест собирается; связка подтверждается тестами 4.4–4.5.

## 3. Доставка запроса и навигация

- [x] 3.1 `MainActivity`: одноразовый сигнал `pendingCreateItem` (по образцу `pendingOpenDetailItemId`); разбор intent в `onCreate` только при `savedInstanceState == null` и в `onNewIntent` (с `setIntent`) через `isCreateItemAction`; неизвестные action игнорируются; существующая reminder-логика не меняется. Проверка: `make build`; поведение подтверждается тестами 4.1–4.3.
- [x] 3.2 `RootScreen`: `LaunchedEffect` выполняет запрос после построения графа — инлайн-guard по всему back stack (`runCatching { navController.getBackStackEntry(Screen.CreateItem.route) }.isSuccess`; эквивалент проверки «route есть в любом месте стека» — см. D6: `currentBackStack` помечен `@RestrictTo(LIBRARY_GROUP)`), если экрана нет в стеке — логирование `AnalyticsEvent.UserAction(UserActionType.CREATE)` и `navController.navigate(Screen.CreateItem.route)`, сброс сигнала после обработки — по образцу `onPendingOpenHandled`. Проверка: `make build`; guard покрыт тестом 4.2, переход — тестом 4.1.

## 4. AndroidTest `LauncherShortcutTest` (`app/src/androidTest/`)

- [x] 4.1 Холодный старт: `ActivityScenario` с intent шортката → открыт экран создания записи; «назад» → главный экран (back stack events → create_item). Проверка: `make android-test ANDROID_TEST_FILTER=com.dayscounter.LauncherShortcutTest` — зелёный (эмулятор подключён).
- [x] 4.2 Повторная доставка intent при открытой форме: второй экран создания не открывается, введённый текст остаётся; сценарий «форма в середине стека» — навигации нет. Проверка: тот же точечный прогон — зелёный.
- [x] 4.3 Пересоздание Activity (поворот/пересоздание) после закрытия формы: экран создания самовольно не открывается; тап-повторный запуск без шортката возвращает на текущий экран (singleTop). Проверка: тот же точечный прогон — зелёный.
- [x] 4.4 `ShortcutManager.getManifestShortcuts()` (манифестные шорткаты; в тексте исходной задачи API ошибочно назван `getStaticShortcuts()` — такого метода в платформе нет) содержит шорткат с id `add_item` и подписью из строки `add_item`; иконка сверяется по цепочке манифест → shortcuts.xml → `android:icon` (публичного `ShortcutInfo.icon` не существует — скрытый API). Проверка: тот же точечный прогон — зелёный.
- [x] 4.5 Согласованность action: intent из shortcuts.xml и intent-filter манифеста (`queryIntentActivities`) равны `ACTION_CREATE_ITEM` из Kotlin-константы. Проверка: тот же точечный прогон — зелёный.

## 5. Ручная проверка в лаунчере (эмулятор)

- [x] 5.1 Долгое нажатие на иконку: пункт «Добавить запись» с иконкой «плюс» (не серая заглушка); подпись читаема — обрезка, если есть, не мешает распознаванию пункта; холодный запуск по пункту открывает форму создания; «назад» — главный экран. Проверка: наблюдение на эмуляторе (`make install` + лаунчер), заметка в итоговом описании задачи.
- [x] 5.2 Смена иконки через «Ещё» → «Тема и иконка» на каждую из шести: после смены шорткат остаётся у включённой иконки (учесть возможную задержку кэша лаунчера). Проверка: наблюдение на эмуляторе.
- [x] 5.3 Запущенное приложение: шорткат с экрана «Ещё» открывает форму поверх; при открытой форме — не дублируется; при открытом редактировании записи — открывает чистую форму создания поверх; тап по иконке возвращает на текущий экран. Проверка: наблюдение на эмуляторе.

### Наблюдения ручной проверки (Pixel Launcher, эмулятор Android 16)

- 5.1: пункт «Добавить запись» с иконкой «плюс» (брендовый жёлтый круг, не серая заглушка); подпись читаема, без обрезки; холодный запуск открывает форму создания; «назад» — главный экран.
- 5.2: все шесть иконок: шорткат следует за включённым алиасом (`dumpsys shortcut`: AliasIcon1 / Icon2…Icon6; визуально подтверждено на иконках 1, 2 и 6); задержки кэша лаунчера не встретились.
- 5.3: с глубокого экрана («Тема и иконка») шорткат открывает чистую форму поверх; при открытом редактировании записи — чистая форма создания поверх, «назад» возвращает к редактированию; тап по иконке возвращает на текущий экран без перезапуска.
- Платформенное наблюдение: при выборе шортката в лаунчере платформа перебейзливает корень задачи и пересоздаёт Activity (новый ActivityRecord в той же задаче, `onNewIntent` из лаунчера не вызывается) — лаунчерный путь приходит как холодный старт; второй экземпляр формы не появляется (guard и singleTop держат один Activity), но несохранённый ввод формы теряется. Соответствует Risks в design.md («часть лаунчеров пересоздаёт корень»); прямые доставки intent без перебейзливания покрыты androidTest 4.2.

## 6. Финальная проверка

- [x] 6.1 Полный прогон: `make format && make lint && make test` — зелёные; detekt `maxIssues: 5` не повышался; новый код без замечаний ktlint/detekt. Проверка: команды проходят, отчёт прочитан (один прогон → один отчёт; перечитать — `python3 scripts/test_report.py`).
- [x] 6.2 Прогон androidTest затронутого экрана: `make android-test ANDROID_TEST_FILTER=com.dayscounter.LauncherShortcutTest` (точечный вместо полного `make android-test` — решение владельца при реализации: задачу не гонять все UI-тесты, затронут только LauncherShortcutTest). Проверка: отчёт `python3 scripts/android_test_report.py GithubDebug` — 7/7 без упавших (два прогона: до и после замены guard на `getBackStackEntry`).
- [x] 6.3 Валидация документации: `openspec validate add-item-quick-action --strict`. Проверка: команда проходит без ошибок.

## Workflow follow-up

- Архивировать change после выполнения требований ревью проекта (`openspec archive`).
- Проверить заархивированный результат.
