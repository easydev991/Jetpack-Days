## ADDED Requirements

### Requirement: Stable Sort Order for Same-Date Events

The system MUST order `Item` lists by `timestamp` (event date) first, then by `id` as a deterministic tie-breaker. The chosen direction (ascending/descending) MUST apply consistently to both keys.

This requirement covers `getAllItems()`, `getAllItemsAsc()`, `getAllItemsDesc()`, and `searchItems()` flows.

#### Scenario: Same-date events sort by id in ascending order

- **WHEN** two events share the same `timestamp` and have different `id` values
- **AND** the user selects "oldest to newest" (ascending) sort order
- **THEN** the event with the smaller `id` appears first

#### Scenario: Same-date events sort by id in descending order

- **WHEN** two events share the same `timestamp` and have different `id` values
- **AND** the user selects "newest to oldest" (descending) sort order
- **THEN** the event with the larger `id` appears first

#### Scenario: Event date dominates id

- **WHEN** the list contains events with different `timestamp` values
- **THEN** ordering is determined by `timestamp` first; `id` applies only within the same `timestamp`

#### Scenario: Search results keep the same ordering

- **WHEN** the user runs a search query that returns items sharing the same `timestamp`
- **THEN** the result list follows `timestamp → id` ordering in DESCENDING direction (the direction is fixed by `ItemDao.searchItems()` and does not depend on the user's chosen sort order)
- **AND** the user's selected sort order MUST NOT affect search result ordering
