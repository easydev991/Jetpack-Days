#!/usr/bin/env python3
"""Скрипт для вывода статистики интеграционных (Android) тестов"""

import os
import re
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

# Цвета для терминала
GREEN = "\033[1;32m"
RED = "\033[1;31m"
RESET = "\033[0m"

GRADLE_EXIT_CODE = int(os.environ.get("ANDROID_TEST_GRADLE_EXIT_CODE") or "0")

if GRADLE_EXIT_CODE != 0:
    print(
        f"{RED}Gradle connectedVariantAndroidTest завершился с ошибкой: {GRADLE_EXIT_CODE}{RESET}"
    )
    sys.exit(1)

# ANSI escape коды для очистки при подсчете длины
ANSI_ESCAPE = re.compile(r"\033\[[0-9;]*m")


def strip_ansi(text: str) -> str:
    """Удалить ANSI escape коды из строки"""
    return ANSI_ESCAPE.sub("", text)


# Вариант сборки: <flavor><BuildType> в lowercase (например, rustoreDebug, githubDebug).
# Передаётся из Makefile (android-test: $(FLAVOR)Debug) или аргументом CLI.
# Default — rustoreDebug (если запущен без аргумента и без Makefile).
variant = sys.argv[1] if len(sys.argv) > 1 else "rustoreDebug"

# Разбор variant на flavor и buildType для layout AGP 9: connected/<buildType>/flavors/<flavor>
if variant.endswith("Debug"):
    flavor, build_type = variant[: -len("Debug")], "debug"
else:
    flavor, build_type = variant[: -len("Release")], "release"

# Каталог с результатами тестов - несколько возможных путей для выбранного variant.
# Имя gradle-таски — connected<FlavorTitle><BuildType>AndroidTest: из `rustoreDebug` собираем `RustoreDebug`.
POSSIBLE_DIRS = [
    Path(
        f"app/build/outputs/androidTest-results/connected/{build_type}/flavors/{flavor}"
    ),
    Path(f"app/build/outputs/androidTest-results/connected/{variant}"),
    Path(f"app/build/reports/androidTests/connected/{variant}/results"),
    Path(f"app/build/reports/androidTests/connectedTest-results/{variant}"),
    Path(
        f"app/build/test-results/connected{variant[0].upper() + variant[1:]}AndroidTest"
    ),
]

# Поиск первой существующей директории
TEST_RESULTS_DIR = None
for dir_path in POSSIBLE_DIRS:
    if dir_path.exists():
        TEST_RESULTS_DIR = dir_path
        break

# Проверка существования директории
if TEST_RESULTS_DIR is None or not TEST_RESULTS_DIR.exists():
    print(f"Директория с результатами интеграционных тестов не найдена")
    print(f"Проверенные пути:")
    for dir_path in POSSIBLE_DIRS:
        print(f"  - {dir_path}")
    print(f"\nСначала выполните: make android-test")
    sys.exit(1)

# Поиск всех XML файлов с результатами тестов
test_xml_files = list(TEST_RESULTS_DIR.rglob("*.xml"))

if not test_xml_files:
    print("XML файлы с результатами тестов не найдены")
    sys.exit(1)

# Подсчет статистики
total = 0
failed = 0
skipped = 0
failed_tests = []
test_class_stats = {}

# AssumptionViolatedException = тест пропущен через Assume (flavor-гейт),
# это НЕ падение: AndroidJUnitRunner пишет его в <failure>, но сам раннер
# учитывает как skipped (см. logcat: "run finished: N tests, 0 failed").
ASSUME_MARKER = "AssumptionViolatedException"


def is_assumption_skipped(element) -> bool:
    """Тест пропущен через Assume (org.junit.AssumptionViolatedException)"""
    if element is None:
        return False
    haystack = " ".join(filter(None, [element.get("message"), element.text]))
    return ASSUME_MARKER in haystack


def is_skipped(testcase, failure, error) -> bool:
    if is_assumption_skipped(failure) or is_assumption_skipped(error):
        return True
    return testcase.find("skipped") is not None


# Обработка каждого XML файла
for xml_file in test_xml_files:
    try:
        tree = ET.parse(xml_file)
    except ET.ParseError:
        # ponytail: битый/недописанный XML (убитый посреди записи прогон)
        # пропускаем — такой прогон в любом случае упал раньше по
        # GRADLE_EXIT_CODE.
        continue
    root = tree.getroot()

    # Получение всех testcase элементов
    testcases = root.findall(".//testcase")

    # Подсчет всех тестов и группировка по классу
    for testcase in testcases:
        # Получение имени класса из атрибута classname каждого testcase
        class_name = testcase.get("classname", "Unknown")
        test_name = testcase.get("name", "Unknown")

        # Подсчет общего количества тестов
        total += 1

        # Инициализация статистики для класса, если её нет
        if class_name not in test_class_stats:
            test_class_stats[class_name] = {"total": 0, "failed": 0, "passed": 0}

        # Обновление статистики класса
        test_class_stats[class_name]["total"] += 1

        # Поиск упавших тестов
        failure = testcase.find("failure")
        error = testcase.find("error")

        if (
            failure is not None
            or error is not None
            or testcase.find("skipped") is not None
        ):
            if is_skipped(testcase, failure, error):
                skipped += 1
            else:
                failed += 1
                test_class_stats[class_name]["failed"] += 1
                failed_tests.append(f"{class_name}::{test_name}")
        else:
            test_class_stats[class_name]["passed"] += 1

passed = total - failed - skipped

# Вывод результатов
print("=" * 80)
if failed > 0:
    print(f"{RED}❌ ТЕСТЫ ПРОВАЛИЛИСЬ{RESET}")
else:
    print(f"{GREEN}✅ СБОРКА УСПЕШНА{RESET}")
print("=" * 80)
print()

print(f"Статистика по интеграционным тестам (Android-эмулятор):")
print(f"Всего тестов: {total}")
print(f"{GREEN}Успешные: {passed}{RESET}")
if skipped > 0:
    print(f"Пропущенные (Assume/flavor-гейт): {skipped}")
if failed > 0:
    print(f"{RED}Упавшие: {failed}{RESET}")
    print(f"{RED}Список упавших тестов:{RESET}")
    for test_name in failed_tests:
        print(f"  - {test_name}")
print()

# Вывод статистики по классам
if test_class_stats:
    print("=" * 80)
    print(f"Статистика по тестовым классам ({len(test_class_stats)} классов):")
    print("=" * 80)

    # Вычисление максимальных ширин столбцов для автоматического выравнивания
    max_class_name_length = max(
        len("Класс"), *map(lambda x: len(x), test_class_stats.keys())
    )
    max_class_name_length = min(max_class_name_length, 60)  # Ограничение до 60 символов

    total_width = max(len("Всего"), 8)
    passed_width = max(len("Успешно"), 10)
    failed_width = max(len("Упало"), 8)

    # Функция для форматирования значения по центру с учетом ANSI кодов
    def center_text(text: str, width: int) -> str:
        """Отформатировать текст по центру с учетом ANSI кодов"""
        clean_text = strip_ansi(text)
        clean_len = len(clean_text)

        if clean_len >= width:
            return text

        total_padding = width - clean_len
        left_padding = total_padding // 2
        right_padding = total_padding - left_padding

        return " " * left_padding + text + " " * right_padding

    # Формирование строки заголовка (все числовые столбцы по центру)
    header = f"{'Класс':<{max_class_name_length}} {center_text('Упало', failed_width)} {center_text('Успешно', passed_width)} {center_text('Всего', total_width)}"
    print(header)
    print("-" * len(header))

    # Сортировка классов по количеству упавших тестов (по убыванию)
    sorted_classes = sorted(
        test_class_stats.items(),
        key=lambda x: (x[1]["failed"], x[1]["total"]),
        reverse=True,
    )

    for class_name, stats in sorted_classes:
        class_display = class_name[:max_class_name_length]  # Обрезка длинных имен
        total_display = str(stats["total"])
        passed_display = (
            f"{GREEN}{stats['passed']}{RESET}" if stats["passed"] > 0 else "0"
        )
        failed_display = (
            f"{RED}{stats['failed']}{RESET}" if stats["failed"] > 0 else "0"
        )

        # Используем выравнивание по центру для всех числовых столбцов
        print(
            f"{class_display:<{max_class_name_length}} {center_text(failed_display, failed_width)} {center_text(passed_display, passed_width)} {center_text(total_display, total_width)}"
        )

    print("=" * 80)

# Итоговый статус
if failed > 0:
    sys.exit(1)
else:
    sys.exit(0)
