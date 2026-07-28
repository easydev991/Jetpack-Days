# Timestamp Precision

## Purpose

Enable fine-grained sorting of same-date events by storing timestamps with millisecond precision including time-of-day, rather than truncating to start of day. This allows events created on the same calendar date to be ordered by their actual creation time, while keeping the `id` tie-breaker only for edge-case collisions.

## Requirements

### Requirement: Timestamp storage with millisecond precision

The system SHALL store `timestamp` in `ItemEntity` as epoch milliseconds with full `LocalDateTime` precision (date + time-of-day), without normalizing to start of day.

#### Scenario: New event captures current time-of-day

- **WHEN** user opens the create-event screen, selects a date, and taps Save
- **THEN** the saved `timestamp` SHALL be `selectedDate.atTime(LocalTime.now()).atZone(systemDefault()).toInstant().toEpochMilli()`
- **AND** two events created on the same calendar date SHALL have distinct `timestamp` values (assuming non-zero time gap between user actions)

#### Scenario: Editing an event preserves original time-of-day

- **WHEN** user opens the edit screen for an existing event, changes the date, and taps Save
- **THEN** the saved `timestamp` SHALL retain the original time-of-day component and use the new date component
- **AND** the `timestamp` SHALL equal `newDate.atTime(originalTimeOfDay).atZone(systemDefault()).toInstant().toEpochMilli()`

#### Scenario: StateSavers stores date without losing precision

- **WHEN** the create/edit screen state is saved to `SavedStateHandle` (e.g., on configuration change)
- **THEN** the `Saver.save` output SHALL be an epoch millisecond `Long` value that includes sub-day precision (not truncated to `atStartOfDay(...)`)
- **AND** the `Saver.restore` SHALL reconstruct the original `LocalDate` by discarding the time-of-day component via `toLocalDate()`
- **NOTE:** the actual `Item.timestamp` saved via `toItem()` uses `LocalTime.now()` at the moment of save, not at the moment of state serialization. The two timestamps may differ by the duration of a configuration change.

### Requirement: Sort by timestamp with id as defensive tie-breaker

The system SHALL sort items primarily by `timestamp` (ascending or descending, matching the user's selected sort order), and SHALL use `id` only as a defensive secondary key for edge-case collisions.

#### Scenario: Same date, different time-of-day, descending sort

- **WHEN** two events have the same calendar date but different time-of-day (event A at 09:00, event B at 18:00)
- **AND** the user selects "newest first" (DESC) sort
- **THEN** the list SHALL show event B (18:00) before event A (09:00)

#### Scenario: Same date, different time-of-day, ascending sort

- **WHEN** two events have the same calendar date but different time-of-day (event A at 09:00, event B at 18:00)
- **AND** the user selects "oldest first" (ASC) sort
- **THEN** the list SHALL show event A (09:00) before event B (18:00)

#### Scenario: Same exact timestamp, id breaks tie deterministically

- **WHEN** two events have identical `timestamp` values (rare collision or legacy data with `atStartOfDay` normalization)
- **AND** the user selects "newest first" (DESC) sort
- **THEN** the event with the higher `id` SHALL appear first
- **AND** the ordering SHALL be deterministic and stable across queries

#### Scenario: Search results ordered by timestamp descending

- **WHEN** the user enters a search query
- **THEN** the matching events SHALL be returned in `ORDER BY timestamp DESC, id DESC` order regardless of the user's currently selected sort order
- **AND** same-timestamp results SHALL be ordered by `id DESC` (newer creation first)

### Requirement: Sort order preserved across backup and restore

The system SHALL preserve the relative order of same-date events across the full backup → factory reset → restore cycle, because the `timestamp` value is stored in the backup and re-inserted with the same value.

#### Scenario: Backup then restore with delete-all-data preserves order

- **WHEN** the user has two events with the same date but different time-of-day (A at 09:00, B at 18:00)
- **AND** the user exports a backup
- **AND** the user activates "Delete all data" (clears the items table)
- **AND** the user imports the backup
- **THEN** the restored events SHALL have the same `timestamp` values as before
- **AND** sorting by timestamp SHALL yield the same relative order (B before A in DESC, A before B in ASC)

#### Scenario: Legacy data with atStartOfDay timestamp after restore

- **WHEN** legacy events (created before this change) have `timestamp = atStartOfDay(...)` and identical `timestamp` values
- **AND** the user exports a backup
- **AND** the user activates "Delete all data"
- **AND** the user imports the backup
- **THEN** the restored events MAY have a different relative order than the original (because `id` is reused by SQLite)
- **AND** this is documented as a known limitation for legacy data

### Requirement: Compatibility with iOS backup format

The system SHALL remain compatible with iOS-generated backup files. iOS already stores `timestamp` with millisecond precision, and the Android import path SHALL accept and use the full millisecond value from the backup without truncation.

#### Scenario: Importing an iOS-generated backup

- **WHEN** the user imports a backup file produced by the iOS app
- **THEN** the imported events SHALL retain the iOS `timestamp` value with full precision
- **AND** sorting same-date iOS events SHALL work correctly (because their `timestamp` values differ by time-of-day)

### Requirement: Days-since calculation unaffected by time-of-day

The system SHALL compute "days since event" using date-precision arithmetic, regardless of the `timestamp` storage precision. The presence of time-of-day in `timestamp` SHALL NOT change the displayed day count.

#### Scenario: Two events on the same date show the same day count

- **WHEN** two events have `timestamp` values that fall on the same calendar date (even with different time-of-day)
- **THEN** the "days since event" indicator SHALL show the same integer day count for both events

### Requirement: Change detection compares date, not time-of-day

The system SHALL detect whether the user has changed the event by comparing the **date component** of the selected `LocalDate` against the **date component** of the original `Item.timestamp`. The time-of-day component SHALL NOT participate in change detection.

#### Scenario: Opening edit screen does not falsely report changes

- **WHEN** the user opens the edit screen for an existing event with `timestamp = 2026-01-15 14:30:00.000`
- **AND** the user does not modify any field
- **THEN** the "has changes" indicator SHALL be `false`
- **AND** the back button SHALL NOT prompt "discard changes?"

#### Scenario: Changing the date triggers change detection

- **WHEN** the user opens the edit screen for an existing event with `timestamp = 2026-01-15 14:30:00.000`
- **AND** the user changes the date to 2026-01-16 via the date picker
- **THEN** the "has changes" indicator SHALL be `true`
- **AND** the back button SHALL prompt "discard changes?"

#### Scenario: Changing only the time-of-day is not possible

- **WHEN** the user opens the edit screen for an existing event
- **THEN** there SHALL be no UI element to modify the time-of-day
- **AND** the time-of-day SHALL remain at the value stored in `originalItem.timestamp`
