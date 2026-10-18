package com.dayscounter.domain

import org.junit.jupiter.api.Test
import java.io.File

/**
 * Guard-тест чистоты доменного слоя (см. squash-коммит фазы 0 де-андроидизации
 * в git log ветки `refactor/layers-clean`).
 *
 * Сканирует текст исходников пакета `domain` (все .kt файлы рекурсивно) на запрещённые зависимости:
 * Android SDK (`android.*`/`androidx.*`), `BuildConfig`, `R`, а также слои
 * `data/`, `crash/`, `analytics/`, `di/` и Android-реализацию логгера
 * (`com.dayscounter.util.AndroidLogger`) — домен не должен знать о них и
 * не должен сам выбирать платформенную реализацию.
 *
 * Ищет и `import X`, и голое FQN (`android.util.Log.e(...)` без import),
 * поэтому сканируется текст файла, а не только import-строки.
 * Комментарии, KDoc и содержимое строковых литералов вырезаются
 * (там упоминания платформенных типов легальны или ложноположительны).
 */
class DomainPurityTest {
    @Test
    fun domain_sources_have_no_forbidden_dependencies() {
        // Given: исходники домена
        val domainDir = resolveDomainDir()

        // When: сканируем текст всех .kt файлов домена
        val violations =
            domainDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .flatMap { file -> findViolations(file) }
                .sortedBy { "${it.file}:${it.line}" }
                .toList()

        // Then: запрещённых зависимостей нет
        assert(violations.isEmpty()) {
            "Домен протекает платформенными зависимостями: ${violations.size} нарушений\n" +
                violations.joinToString("\n") { v ->
                    "${v.file}:${v.line}: ${v.prefix} | ${v.text.trim()}"
                }
        }
    }

    /**
     * Ищет путь к `domain/` подъёмом от рабочего каталога теста:
     * gradle может запускать тесты из `app/` или из корня репозитория.
     */
    private fun resolveDomainDir(): File {
        val relativePath = "src/main/java/com/dayscounter/domain"
        var dir: File? = File(System.getProperty("user.dir"))
        while (dir != null) {
            val candidate = dir.resolve(relativePath)
            if (candidate.isDirectory) return candidate
            dir = dir.parentFile
        }
        error("Не найден каталог $relativePath от ${System.getProperty("user.dir")}")
    }

    private fun findViolations(file: File): List<Violation> {
        val relativeFile = domainRelativePath(file)
        val codeLines = stripComments(file.readText()).lines()
        return codeLines.flatMapIndexed { index, line ->
            forbiddenPrefixes.flatMap { prefix ->
                matchPositions(line, prefix).map { position ->
                    Violation(
                        file = relativeFile,
                        line = index + 1,
                        prefix = prefix,
                        text = line
                    )
                }
            }
        }
    }

    private fun domainRelativePath(file: File): String {
        val domainRoot = resolveDomainDir().invariantSeparatorsPath
        return file.invariantSeparatorsPath.removePrefix("$domainRoot/")
    }

    /**
     * Позиции вхождения [prefix], не являющиеся частью более длинного имени:
     * `kotlinx.android.` не должен ловиться как `android.`, поэтому вхождение
     * пропускается, если перед ним буква/цифра/`.`/`_`.
     */
    private fun matchPositions(
        line: String,
        prefix: String
    ): List<Int> {
        val positions = mutableListOf<Int>()
        var index = line.indexOf(prefix)
        while (index >= 0) {
            val prev = if (index == 0) ' ' else line[index - 1]
            val insideLongerName = prev.isLetterOrDigit() || prev == '_' || prev == '.'
            if (!insideLongerName) positions.add(index)
            index = line.indexOf(prefix, index + 1)
        }
        return positions
    }

    /**
     * Заменяет комментарии и содержимое строковых литералов пробелами,
     * сохраняя длину и переводы строк (номера строк остаются точными).
     */
    private fun stripComments(source: String): String = CommentStripper().strip(source)

    /**
     * Посимвольный конечный автомат: line-комментарий, блок-комментарий,
     * строковый литерал с escape-последовательностями.
     *
     * shortcut: тройные кавычки (""") со встроенными " маскируются неточно —
     * фрагмент между внутренними кавычками остаётся видимым, и запрещённый
     * префикс там даст ложное срабатывание; чинить (отдельное состояние
     * до закрывающей """), если в domain/ появятся raw strings.
     */
    private class CommentStripper {
        private var inLineComment = false
        private var inBlockComment = false
        private var inString = false

        fun strip(source: String): String {
            val out = StringBuilder(source.length)
            var i = 0
            while (i < source.length) {
                i += consume(source, i, out)
            }
            return out.toString()
        }

        /** @return количество обработанных символов (1 или 2) */
        private fun consume(
            source: String,
            i: Int,
            out: StringBuilder
        ): Int {
            val c = source[i]
            val next = source.getOrElse(i + 1) { ' ' }
            return when {
                inLineComment -> consumeLineComment(c, out)
                inBlockComment -> consumeBlockComment(c, next, out)
                inString -> consumeString(i, c, out)
                c == '/' && next == '/' -> startComment(out) { inLineComment = true }
                c == '/' && next == '*' -> startComment(out) { inBlockComment = true }
                else -> consumeCode(c, out)
            }
        }

        private fun consumeLineComment(
            c: Char,
            out: StringBuilder
        ): Int {
            if (c == '\n') {
                inLineComment = false
                out.append(c)
            } else {
                out.append(' ')
            }
            return 1
        }

        private fun consumeBlockComment(
            c: Char,
            next: Char,
            out: StringBuilder
        ): Int =
            if (c == '*' && next == '/') {
                inBlockComment = false
                out.append("  ")
                2
            } else {
                out.append(if (c == '\n') '\n' else ' ')
                1
            }

        private fun consumeString(
            i: Int,
            c: Char,
            out: StringBuilder
        ): Int {
            if (c == '"') {
                inString = false
                out.append(c)
                return 1
            }
            // Содержимое литерала гасим пробелами: русский текст внутри строк
            // не должен ловиться как FQN-нарушение. Escape-последовательность
            // (включая \") тоже содержимое — гасим оба символа.
            val consumed = if (c == '\\') 2 else 1
            repeat(consumed) { out.append(' ') }
            return consumed
        }

        private fun startComment(
            out: StringBuilder,
            enterState: () -> Unit
        ): Int {
            enterState()
            out.append("  ")
            return 2
        }

        private fun consumeCode(
            c: Char,
            out: StringBuilder
        ): Int {
            out.append(c)
            if (c == '"') inString = true
            return 1
        }
    }

    private data class Violation(
        val file: String,
        val line: Int,
        val prefix: String,
        val text: String
    )

    private companion object {
        /**
         * Запрещённые префиксы в тексте доменных исходников — и как `import X`,
         * и как голое FQN. `kotlin*`/`kotlinx*`/`java.*`/`com.dayscounter.domain.*`/
         * `com.dayscounter.util.*` разрешены (см. план, Этап 0), КРОМЕ
         * `AndroidLogger`: домен принимает `Logger` явно и не выбирает
         * Android-реализацию сам (её создаёт di-слой через `AppModule.createLogger`).
         * Слои `reminder/`, `ui/`, `navigation/` тоже запрещены — домен не знает
         * ни о платформенных фичах, ни о представлении.
         */
        val forbiddenPrefixes =
            listOf(
                "android.",
                "androidx.",
                "com.dayscounter.BuildConfig",
                "com.dayscounter.R",
                "com.dayscounter.data.",
                "com.dayscounter.crash.",
                "com.dayscounter.analytics.",
                "com.dayscounter.di.",
                "com.dayscounter.reminder.",
                "com.dayscounter.ui.",
                "com.dayscounter.navigation.",
                "com.dayscounter.util.AndroidLogger"
            )
    }
}
