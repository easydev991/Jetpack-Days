# Design

## Context

Аудит 66 catch-сайтов (сент. 2026, см. историю git) показал: в Crashlytics попадают только ошибки репозиториев и `CalculateDaysDifferenceUseCase`. Канал отчёта существует — `CrashlyticsHelper.logException(e, message)`, прецедент вызова в domain-слое есть (`CalculateDaysDifferenceUseCase`); тестовый же паттерн `mockkObject(CrashlyticsHelper)` для проекта новый — в тестах не применялся. Сбор активен только в release (в debug collection off), поэтому отправка не влияет на debug- и тестовые окружения.

## Goals / Non-Goals

**Goals:**

- Закрыть 6 точек отказа (8 catch-блоков) приоритета A (см. proposal) с сохранением существующего поведения каждого пути.
- Каждый сайт покрыт unit-тестом с детерминированной проверкой отправки (`mockkObject(CrashlyticsHelper)` + verify).
- Контекстные сообщения на русском (как в репозиториях: `"Ошибка ... itemId=..."`).

**Non-Goals:**

- Приоритет B (MoreScreen, AnalyticsService) — решает владелец отдельно, в change не входит.
- Отчёт об ошибках, уже видимых пользователю (`Result.failure`/UI-состояния) и об осознанном control flow с дефолтом — не дублируем существующие каналы.
- Подключение ожидаемых ошибок к статистическому каналу `AnalyticsEvent.AppError` — отдельная задача.
- Side-task унификации логгера (`android.util.Log.e` → `logger.e`) — не здесь.

## Decisions

- **Канал — существующий `CrashlyticsHelper.logException`, без новой абстракции.** Прецедент в domain-слое есть (`CalculateDaysDifferenceUseCase`); интерфейс с одной реализацией не нужен. Альтернатива (свой репортер поверх `AnalyticsProvider`) отвергнута: канал статистики vs crash-канал решают разные задачи, и смешивать нельзя — `AnalyticsService` и так вызывает провайдеры, рекурсия по `recordException` исключена.
- **Тесты через `mockkObject(CrashlyticsHelper)`, а не через реальные вызовы.** `verify` должен быть детерминированным; в JVM-тестах Firebase SDK не активен, но полагаться на это нельзя — явно мокаем. Паттерн для проекта новый: в каждом затронутом тестовом классе — `unmockkAll()` в `@AfterEach` (образец — `AlarmReminderSchedulerTest`), чтобы мок не протекал между тестами.
- **`IconManager.disableComponent` тестируется через публичный `changeIcon`** (метод приватный): четыре сценария Red/Green на `changeIcon`, включая «сбой disable после успешной enable». `logException` — в существующий общий catch `Exception` самого `disableComponent` (сегодня он глотает исключения и это сохраняется: throw-семантика не меняется, вызывающий `ThemeIconViewModel` без catch-all не страдает). Критерий репорта — по шагам: сбой enable = запрошенное действие не выполнено → отчёт; сбой disable (косметика: действие выполнено, остался дубликат старой иконки) репортится одним non-fatal из этого catch; четыре узких catch сброса (SecurityException / NameNotFoundException / IllegalArgumentException / IllegalStateException) — без отчёта, иначе шум на каждый alias.
- **Порядок работ — TDD Red/Green поэтапно** (сначала тесты, потом `logException` в catch), чтобы каждый сайт попадал в коммит вместе с проверкой.

## Risks / Trade-offs

- [Шум в консоли от новых non-fatal] → Сайты приоритета A — редкие внутренние сбои, не пользовательский ввод; объём оценивается по неделе на release, расширение на приоритет B — отдельное решение владельца.
- [`CrashlyticsHelper` глотает исключения внутри себя] → Осознанно (защита от бесконечного цикла): сбой самого канала не может уронить приложение, но и не репортится.
- [Номера строк в исходном плане устареют] → Спека и tasks привязаны к именам классов/методов и сценариям, не к строкам.

## Migration Plan

Обычный релизный цикл: этапы 1–4 с тестами, верификация (`make lint` / `make test`, android-test на эмуляторе API 36, финальная release-сборка и проверка non-fatal в Firebase Console). Откат — revert без миграций данных (изменения только в логировании).
