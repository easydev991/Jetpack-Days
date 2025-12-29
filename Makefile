# Makefile для проекта JetpackDays

# Сборка проекта
build:
	./gradlew assembleDebug

# Очистка кэша проекта
clean:
	./gradlew clean

# Запуск всех unit-тестов
test:
	./gradlew test --console=plain && echo "Тесты выполнены. Результаты:" && find app/build/test-results -name "*.xml" -exec grep -h "testsuite" {} \; | head -10

# Запуск линтера (ktlint и detekt)
lint:
	./gradlew ktlintCheck detekt

# Форматирование кода
format:
	./gradlew ktlintFormat

# Сборка и запуск всех проверок
check: build
	./gradlew test --console=plain && echo "Тесты выполнены. Результаты:" && find app/build/test-results -name "*.xml" -exec grep -h "testsuite" {} \; | head -10
	./gradlew ktlintCheck detekt

# Установка приложения
install:
	./gradlew installDebug

# Запуск всех задач (сборка, тесты, линтер)
all: check install

.PHONY: build clean test lint format check install all