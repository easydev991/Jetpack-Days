#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Скрипт для обновления таблицы скриншотов в README.md
Находит актуальные файлы скриншотов по шаблону {номер}-{описание}_*.png
и заменяет HTML-комментарии на реальные теги <img>
"""

import os
import glob
from pathlib import Path

# Список скриншотов для замены (формат: "номер-описание")
SCREENSHOTS = [
    "1-demoList",
    "2-chooseDate",
    "3-chooseDisplayOption",
    "4-beforeSave",
    "5-sortByDate",
]

LOCALE = "ru-RU"
SCREENSHOTS_DIR = f"fastlane/metadata/android/{LOCALE}/images/phoneScreenshots"
README_FILE = "README.md"


def find_latest_screenshot(pattern):
    """Найти актуальный файл скриншота (с последней временной меткой)"""
    search_pattern = os.path.join(SCREENSHOTS_DIR, f"{pattern}_*.png")
    files = glob.glob(search_pattern)

    if not files:
        return None

    # Сортируем по временной метке (последняя часть названия файла)
    files.sort(key=lambda x: x.split('_')[-1], reverse=True)
    return files[0]


def update_readme():
    """Обновление README.md"""
    if not os.path.isdir(SCREENSHOTS_DIR):
        print(f"❌ Ошибка: папка со скриншотами не найдена: {SCREENSHOTS_DIR}")
        print("💡 Сначала выполните: make screenshots")
        return False

    print("📸 Обновляю таблицу скриншотов в README.md...")

    # Читаем содержимое README.md
    try:
        with open(README_FILE, 'r', encoding='utf-8') as f:
            content = f.read()
    except FileNotFoundError:
        print(f"❌ Ошибка: файл {README_FILE} не найден")
        return False

    updated = False
    for screenshot in SCREENSHOTS:
        screenshot_path = find_latest_screenshot(screenshot)

        if not screenshot_path:
            print(f"⚠️  Предупреждение: файл скриншота не найден для шаблона {screenshot}_*.png")
            continue

        # Создаем тег с атрибутом alt для markdownlint
        placeholder = f"<!-- SCREENSHOT: {LOCALE}, {screenshot} -->"
        img_tag = f'<img src="./{screenshot_path}" alt="">'

        # Проверяем, есть ли placeholder в файле
        if placeholder in content:
            content = content.replace(placeholder, img_tag)
            updated = True
            print(f"✅ Обновлен: {screenshot}")
        else:
            print(f"⚠️  Предупреждение: placeholder {placeholder} не найден в {README_FILE}")

    if updated:
        # Записываем обновленное содержимое
        with open(README_FILE, 'w', encoding='utf-8') as f:
            f.write(content)
        print("\n🎉 Таблица скриншотов обновлена успешно!")
        print("\n💡 Проверьте README.md перед коммитом изменений")
        return True
    else:
        print("\n⚠️  Никаких обновлений не было сделано")
        return False


if __name__ == "__main__":
    success = update_readme()
    exit(0 if success else 1)
