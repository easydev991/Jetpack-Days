# Makefile для проекта JetpackDays

# Цвета и шрифт для вывода в консоль
YELLOW=\033[1;33m
GREEN=\033[1;32m
RED=\033[1;31m
BOLD=\033[1m
RESET=\033[0m

## help: Показать это справочное сообщение
help:
	@echo "Доступные команды Makefile:"
	@echo ""
	@sed -n 's/^##//p' ${MAKEFILE_LIST} | \
	awk -F ':' '{printf "  $(BOLD)%s$(RESET):%s\n", $$1, $$2}' BOLD="$(BOLD)" RESET="$(RESET)" | column -t -s ':'
	@echo ""

# Сборка проекта
## build: Сборка APK для отладки
build:
	./gradlew assembleDebug

## clean: Очистка кеша проекта
clean:
	./gradlew clean

# Тестирование
## test: Запуск unit-тестов (JVM, без устройства)
test:
	./gradlew test --console=plain && echo "Тесты выполнены. Результаты:" && find app/build/test-results -name "*.xml" -exec grep -h "testsuite" {} \; | head -10

## android-test: Запуск интеграционных тестов на Android устройстве
android-test:
	./gradlew connectedDebugAndroidTest --console=plain
	@echo ""
	@echo "Интеграционные тесты выполнены"
	@if [ -f app/build/reports/androidTests/connected/debug/index.html ]; then \
		echo "HTML отчет: app/build/reports/androidTests/connected/debug/index.html"; \
	fi

## test-all: Запуск всех тестов (unit + интеграционные)
test-all:
	@echo ""
	@echo "Все тесты выполнены"
	@echo "Unit: app/build/test-results/"
	@echo "Интеграционные: app/build/reports/androidTests/connected/debug/index.html"

# Анализ кода
## lint: Запуск ktlint, detekt и markdownlint (проверка)
lint:
	./gradlew ktlintCheck
	./gradlew app:detekt
	@if command -v markdownlint >/dev/null 2>&1; then \
		markdownlint "**/*.md" ".cursor/rules/*.mdc"; \
	else \
		echo "$(YELLOW)markdownlint-cli не установлен. Для установки: npm install -g markdownlint-cli$(RESET)"; \
	fi

## format: Форматирование кода (ktlint + detekt с исправлениями) и Markdown-файлов
format:
	./gradlew ktlintFormat
	./gradlew app:detekt -Pdetekt.autoCorrect=true
	@if command -v markdownlint >/dev/null 2>&1; then \
		markdownlint --fix "**/*.md" ".cursor/rules/*.mdc"; \
	else \
		echo "$(YELLOW)markdownlint-cli не установлен. Для установки: npm install -g markdownlint-cli$(RESET)"; \
	fi

# Сборка и запуск всех проверок
## check: Полная проверка (сборка + тесты + линтер)
check: build
	./gradlew test --console=plain && echo "Тесты выполнены. Результаты:" && find app/build/test-results -name "*.xml" -exec grep -h "testsuite" {} \; | head -10
	./gradlew ktlintCheck detekt

# Установка приложения
## install: Установка APK на устройство
install:
	./gradlew installDebug

# Настройка окружения
## setup: Установка и настройка инструментов для локальной разработки (markdownlint-cli)
setup:
	@echo "$(YELLOW)Установка инструментов для локальной разработки...$(RESET)"
	@if ! command -v npm >/dev/null 2>&1; then \
		echo "$(RED)Ошибка: Node.js/npm не установлен. Установите Node.js с https://nodejs.org/$(RESET)"; \
		exit 1; \
	fi
	@if ! command -v markdownlint >/dev/null 2>&1; then \
		echo "Установка markdownlint-cli..."; \
		npm install -g markdownlint-cli; \
	fi
	@echo "$(GREEN)Установка завершена!$(RESET)"
	@echo ""
	@echo "$(BOLD)Установленные инструменты:$(RESET)"
	@echo "  - Gradle: $(shell ./gradlew --version 2>/dev/null | head -1 || echo 'не определен')"
	@echo "  - npm: $(shell npm --version 2>/dev/null || echo 'не определен')"
	@echo "  - markdownlint: $(shell markdownlint --version 2>/dev/null | head -1 || echo 'не установлен')"
	@echo ""
	@echo "$(YELLOW)Далее выполните для первой сборки:$(RESET)"
	@echo "  make build"

# Дополнительно
## android-test-report: Открыть HTML отчет интеграционных тестов в браузере
android-test-report:
	@if [ -f app/build/reports/androidTests/connected/debug/index.html ]; then \
		open app/build/reports/androidTests/connected/debug/index.html; \
	else \
		echo "Отчет не найден. Сначала запустите: make android-test"; \
	fi

# Запуск всех задач
## all: Полная проверка (сборка + тесты + линтер) и установка APK на устройство
all: check install

.PHONY: build clean test lint format check install all android-test test-all android-test-report setup help
