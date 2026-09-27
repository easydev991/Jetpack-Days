# Архитектура

## MVVM + Clean Architecture

- **Model** — Data layer (Room, repositories); **View** — Compose UI;
  **ViewModel** — состояние UI
- Слои: Presentation (UI, ViewModel) → Domain (Use Cases, entities) →
  Data (Repositories, только локальные источники)
- **Нет сетевых источников данных**

## Dependency Injection

Ручной DI через factory-методы: `FormatterModule` (форматирование, use
cases) и `AppModule` (репозиторий, DataStore). Hilt не используется —
простой граф, быстрая компиляция, простые тесты. Пересмотреть при росте
(>10 ViewModel, сложные графы) — план в `docs/Hilt_Setup_Plan.md`.

## Слои

- **Data**: `data/database/` (Room: entities, DAO, DB, конвертеры, мапперы),
  `data/repository/` (реализации), `data/preferences/` (AppSettingsDataStore),
  `data/provider/` (DaysFormatter, ResourceProvider)
- **Domain**: `domain/usecase/` (один use case — одна ответственность),
  `domain/model/`, `domain/repository/` (интерфейсы), `domain/exception/`
- **Presentation**: `ui/screens/`, `ui/viewmodel/`, `ui/state/` (sealed
  Loading/Success/Error, data class для простых состояний), `ui/ds/`,
  `ui/theme/`, `navigation/Screen.kt`
- **Reminder**: `reminder/` — планировщик напоминаний и уведомления
- **Прочее**: `analytics/`, `crash/` (только release), `di/`, `util/`
