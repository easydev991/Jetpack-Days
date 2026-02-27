# План: Поддержка кастомных цветов в ColorSelector

## Проблема

При импорте резервной копии из iOS-приложения с кастомным цветом (не из предустановленных 6 цветов), в CreateEditScreen цвет визуально не выделяется в селекторе — ни у одного из кругов нет обводки.

## Решение

Добавить отображение кастомного цвета в начале списка ColorSelector, если выбранный цвет не совпадает ни с одним из предустановленных.

---

## Выполненные этапы ✅

### Этап 1-2: Unit-тесты и логика

- [x] `ColorSelectorUtils.kt` — функция `isCustomColor()` для определения кастомного цвета
- [x] `ColorSelectorUtilsTest.kt` — 3 unit-теста
- [x] Объект `PresetColors` с 6 константами цветов
- [x] Обновлён `ColorSelector` — отображение кастомного чипа с обводкой, accessibility

### Этап 3-4: UI-тесты и Preview

- [x] `ColorSelectorUiTest.kt` — 4 UI-теста (7 чипов при custom, 6 при preset)
- [x] `CreateEditScreenCustomColorTest.kt` — 4 UI-теста для CreateEditFormContent
- [x] Preview с кастомным оранжевым цветом

### Этап 5-6: Локализация и верификация

- [x] `R.string.color` добавлен в `values/strings.xml`, `values-ru/`, `values-en/`
- [x] `values-en/strings.xml` синхронизирован с остальными локализациями
- [x] `make test` — 336/336 прошли
- [x] `make lint` — успешно

---

## Статус: ✅ ЗАВЕРШЕНО
