# План автоматизации создания релиза в GitHub с APK

## Обзор

Документ описывает реализацию команды `make github_release`, которая автоматически:

- Создает подписанный APK-файл
- Создает git-тег на основе версии приложения
- Публикует релиз в GitHub с прикрепленным APK

## Текущее состояние проекта

### Имеющееся решение

- Команда `make apk` - создание подписанного APK (без повышения версии)
- Команда `make release` - создание подписанной AAB с повышением VERSION_CODE
- gradle.properties содержит VERSION_NAME и VERSION_CODE
- Fastlane установлен и используется для генерации скриншотов

### Недостатки

- Нет автоматической публикации релизов в GitHub
- Версионирование и публикация разрознены
- APK не доступен для скачивания из GitHub Releases

## Цель

Реализовать команду `make github_release`, которая:

1. Создает подписанный APK (без изменения VERSION_CODE)
2. Создает git-тег формата `v{VERSION_NAME}`
3. Публикует релиз в GitHub с APK-файлом

## Технический подход

### Выбор инструмента

**GitHub CLI (gh)** - рекомендуемый подход, так как:

- Стандартный инструмент для работы с GitHub
- Легко интегрируется с существующим Makefile
- Прост в использовании и отладке
- Активно поддерживается GitHub

### Альтернативные подходы

1. **fastlane-plugin-firebase_app_distribution** - не подходит (только для Firebase)
2. **GitHub API через curl** - менее удобно, больше boilerplate кода
3. **Плагин fastlane для GitHub** - дополнительная зависимость, избыточно для простой задачи

## План реализации

### Этап 1: Подготовка окружения

#### 1.1. Установка GitHub CLI

**Проверка наличия gh:**

```bash
gh --version
```

**Установка при необходимости:**

```bash
# macOS (через Homebrew)
brew install gh

# Или через Makefile (добавить в setup)
```

**Аутентификация:**

```bash
gh auth login
# Выбрать GitHub.com
# Выбрать HTTPS
# Выбрать Login with a web browser
```

#### 1.2. Добавление проверки gh в setup

**Обновить Makefile:**

Добавить новую цель `_check_gh` в секцию проверки инструментов:

```makefile
## _check_gh: Проверка наличия GitHub CLI
_check_gh:
 @printf "$(YELLOW)Проверка GitHub CLI...$(RESET)\n"
 @if ! command -v gh >/dev/null 2>&1; then \
  printf "Установка GitHub CLI...\n"; \
  brew install gh; \
 else \
  printf "$(GREEN)GitHub CLI уже установлен$(RESET)\n"; \
 fi
 @gh auth status >/dev/null 2>&1 || \
  (printf "$(YELLOW)Необходимо авторизоваться в GitHub. Выполните: gh auth login$(RESET)\n"; exit 1)
```

**Обновить цель setup:**

```makefile
setup:
 @$(MAKE) _check_rbenv
 @$(MAKE) _check_ruby
 @$(MAKE) _check_ruby_version_file
 @$(MAKE) _check_bundler
 @$(MAKE) _check_gemfile
 @$(MAKE) _install_gemfile_deps
 @$(MAKE) setup_fastlane
 @$(MAKE) _check_markdownlint
 @$(MAKE) _check_gh  # Добавить эту строку
```

### Этап 2: Реализация команды github_release

#### 2.1. Добавление цели в Makefile

Добавить новую цель после команды `apk`:

```makefile
## github_release: Создать релиз в GitHub с APK-файлом. Не меняет VERSION_CODE.
github_release:
 @printf "$(YELLOW)Проверка GitHub CLI...$(RESET)\n"
 @if ! command -v gh >/dev/null 2>&1; then \
  printf "$(RED)Ошибка: GitHub CLI не установлен. Выполните: brew install gh$(RESET)\n"; \
  exit 1; \
 fi
 @printf "$(YELLOW)Проверка авторизации в GitHub...$(RESET)\n"
 @if ! gh auth status >/dev/null 2>&1; then \
  printf "$(RED)Ошибка: Необходимо авторизоваться. Выполните: gh auth login$(RESET)\n"; \
  exit 1; \
 fi
 @printf "$(YELLOW)Проверка секретов для подписи...$(RESET)\n"
 @if [ ! -d ".secrets" ]; then \
  printf "$(YELLOW)Загрузка секретов из репозитория android-secrets...$(RESET)\n"; \
  if [ -d "../android-secrets/jetpackdays" ]; then \
   mkdir -p ".secrets"; \
   cp -r ../android-secrets/jetpackdays/* .secrets/; \
   sed -i.tmp 's|^KEYSTORE_FILE=.*|KEYSTORE_FILE=.secrets/keystore/dayscounter-release.keystore|' .secrets/secrets.properties && rm -f .secrets/secrets.properties.tmp; \
   printf "$(GREEN)Секреты загружены успешно$(RESET)\n"; \
  else \
   printf "$(RED)Ошибка: репозиторий android-secrets не найден в ../android-secrets/jetpackdays$(RESET)\n"; \
   printf "$(YELLOW)Проверьте, что репозиторий android-secrets склонирован в нужное место$(RESET)\n"; \
   exit 1; \
  fi \
 fi
 @printf "$(YELLOW)Создаю релизный APK...$(RESET)\n"
 @./gradlew assembleRelease
 @VERSION_CODE=$$(grep "^VERSION_CODE=" gradle.properties | cut -d'=' -f2); \
 VERSION_NAME=$$(grep "^VERSION_NAME=" gradle.properties | cut -d'=' -f2); \
 OUTPUT_FILE="dayscounter$$VERSION_CODE.apk"; \
 cp app/build/outputs/apk/release/app-release.apk "$$OUTPUT_FILE"; \
 printf "$(GREEN)APK создан: $$OUTPUT_FILE$(RESET)\n"; \
 printf "$(YELLOW)Версия: $$VERSION_NAME (build $$VERSION_CODE)$(RESET)\n"
 @printf "$(YELLOW)Проверяю существование тега v$$VERSION_NAME...$(RESET)\n"
 @if git rev-parse "v$$VERSION_NAME" >/dev/null 2>&1; then \
  VERSION_CODE=$$(grep "^VERSION_CODE=" gradle.properties | cut -d'=' -f2); \
  VERSION_NAME=$$(grep "^VERSION_NAME=" gradle.properties | cut -d'=' -f2); \
  printf "$(YELLOW)Тег v$$VERSION_NAME уже существует. Обновляю существующий релиз...$(RESET)\n"; \
  OUTPUT_FILE="dayscounter$$VERSION_CODE.apk"; \
  gh release edit "v$$VERSION_NAME" \
   --title "Days Counter v$$VERSION_NAME" \
   --notes "Релиз Days Counter версии $$VERSION_NAME (build $$VERSION_CODE).

**Изменения:**
- См. changelog или commit history для деталей.

**Файл для установки:**
- dayscounter$$VERSION_CODE.apk"; \
  gh release upload "v$$VERSION_NAME" "$$OUTPUT_FILE" --clobber --repo=$$(git config --get remote.origin.url | sed 's|.*github.com[:/]||' | sed 's|\.git$$||'); \
  printf "$(GREEN)Релиз v$$VERSION_NAME обновлен в GitHub$(RESET)\n"; \
 else \
  VERSION_CODE=$$(grep "^VERSION_CODE=" gradle.properties | cut -d'=' -f2); \
  VERSION_NAME=$$(grep "^VERSION_NAME=" gradle.properties | cut -d'=' -f2); \
  printf "$(YELLOW)Создаю новый тег v$$VERSION_NAME...$(RESET)\n"; \
  git tag -a "v$$VERSION_NAME" -m "Релиз v$$VERSION_NAME (build $$VERSION_CODE)"; \
  printf "$(YELLOW)Публикую тег...$(RESET)\n"; \
  git push origin "v$$VERSION_NAME"; \
  printf "$(YELLOW)Создаю релиз в GitHub...$(RESET)\n"; \
  OUTPUT_FILE="dayscounter$$VERSION_CODE.apk"; \
  gh release create "v$$VERSION_NAME" "$$OUTPUT_FILE" \
   --title "Days Counter v$$VERSION_NAME" \
   --notes "Релиз Days Counter версии $$VERSION_NAME (build $$VERSION_CODE).

**Изменения:**
- См. changelog или commit history для деталей.

**Файл для установки:**
- dayscounter$$VERSION_CODE.apk" \
   --repo=$$(git config --get remote.origin.url | sed 's|.*github.com[:/]||' | sed 's|\.git$$||'); \
  printf "$(GREEN)Релиз v$$VERSION_NAME создан и опубликован в GitHub$(RESET)\n"; \
 fi
```

#### 2.2. Обновление .PHONY

Добавить `github_release` в список целей:

```makefile
.PHONY: build clean test lint format check install all android-test test-all android-test-report screenshots screenshots-ru screenshots-en _build_screenshots_apk _cleanup_screenshots_apk _ensure_fastlane setup setup_fastlane update_fastlane fastlane help release apk github_release _check_rbenv _check_ruby _check_ruby_version_file _check_bundler _check_gemfile _install_gemfile_deps _check_markdownlint _check_gh
```

### Этап 3: Тестирование

#### 3.1. Тестовый сценарий

1. Установить GitHub CLI (если не установлен):

   ```bash
   brew install gh
   gh auth login
   ```

2. Убедиться, что репозиторий настроен с remote origin:

   ```bash
   git remote -v
   ```

3. Выполнить команду:

   ```bash
   make github_release
   ```

4. Проверить:
   - APK создан в корне проекта
   - Тег создан в GitHub (v1.0 или текущая VERSION_NAME)
   - Релиз опубликован в GitHub
   - APK файл прикреплен к релизу

5. Повторный запуск для проверки обновления:

   ```bash
   make github_release
   ```

   Должен обновить существующий релиз, а не создавать новый.

#### 3.2. Проверка в браузере

1. Открыть: <https://github.com/{owner}/JetpackDays/releases>
2. Проверить наличие релиза с верным тегом
3. Проверить наличие APK-файла в asset-ах
4. Скачать и установить APK на устройство

### Этап 4: Документация

#### 4.1. Обновление README.md

Добавить секцию с описанием новой команды:

```markdown
## Публикация релизов

### GitHub Release

Для создания релиза в GitHub с APK-файлом:

```bash
make github_release
```

Эта команда:

- Создает подписанный APK (без изменения VERSION_CODE)
- Создает git-тег в формате v{VERSION_NAME}
- Публикует релиз в GitHub с прикрепленным APK

**Требования:**

- Установлен GitHub CLI: `brew install gh`
- Авторизованы в GitHub: `gh auth login`
- Настроен remote origin в git

**Примечание:**

- Повторный запуск команды обновит существующий релиз
- Для изменения версии отредактируйте `gradle.properties`

```

#### 4.2. Обновление Makefile help

Добавить описание новой команды в help-секцию (это автоматически работает благодаря комментариям `##`).

### Этап 5: Дополнительные улучшения (опционально)

#### 5.1. Автоматизация changelog

Возможные улучшения:
- Чтение changelog из отдельного файла (CHANGELOG.md)
- Генерация notes на основе коммитов с момента последнего тега

#### 5.2. GitHub Actions

Возможность создания CI/CD для автоматических релизов:
- GitHub Action для сборки APK
- Автоматический релиз при создании тега
- Подпись APK с GitHub Secrets

Пример workflow (для будущего):
```yaml
name: Release APK

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build APK
        run: ./gradlew assembleRelease
      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          files: app/build/outputs/apk/release/*.apk
```

#### 5.3. Проверка зависимостей

Добавить автоматическую проверку всех зависимостей при запуске `make github_release`:

```makefile
github_release: _check_gh _check_secrets
 # ... остальная логика
```

## Резюме

### Что будет реализовано

1. ✅ Установка и проверка GitHub CLI (через setup)
2. ✅ Команда `make github_release` для публикации релизов
3. ✅ Автоматическое создание тегов v{VERSION_NAME}
4. ✅ Автоматическая публикация APK в GitHub Releases
5. ✅ Обновление существующих релизов при повторном запуске
6. ✅ Документация в README.md

### Преимущества

- **Простота**: Одна команда для всего процесса
- **Безопасность**: Использует стандартные механизмы GitHub
- **Надежность**: Автоматическая проверка всех зависимостей
- **Гибкость**: Можно запускать многократно без проблем
- **Совместимость**: Интегрируется с существующим workflow

### Ограничения

- Требует установленный GitHub CLI
- Требует авторизацию в GitHub
- Не меняет VERSION_CODE (использует текущее значение)
- Не генерирует автоматический changelog

### Будущие улучшения

- GitHub Actions для автоматических релизов
- Генерация changelog на основе коммитов
- Поддержка App Bundle (.aab) в GitHub Releases
- Автоматическое тестирование перед релизом

## Порядок выполнения

1. [ ] Установить GitHub CLI и авторизоваться
2. [ ] Добавить цель `_check_gh` в Makefile
3. [ ] Обновить цель `setup` с проверкой gh
4. [ ] Добавить цель `github_release` в Makefile
5. [ ] Обновить `.PHONY` в Makefile
6. [ ] Протестировать команду на тестовом репозитории
7. [ ] Обновить README.md с документацией
8. [ ] Создать первый релиз в основном репозитории
