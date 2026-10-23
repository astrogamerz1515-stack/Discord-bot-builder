package com.example.engine

import com.example.data.model.BotFile
import com.example.data.model.BotProject
import com.example.data.repository.BotRepository

object BotFileAutoCreator {

    private val REQUIRE_REGEX = Regex("""require\s*\(\s*['"](\.[^'"]+)['"]\s*\)""", RegexOption.IGNORE_CASE)
    private val IMPORT_REGEX = Regex("""import\s+.*?from\s+['"](\.[^'"]+)['"]""", RegexOption.IGNORE_CASE)
    private val IMPORT_DIRECT_REGEX = Regex("""import\s+['"](\.[^'"]+)['"]""", RegexOption.IGNORE_CASE)
    private val FS_CALL_REGEX = Regex(
        """fs\.(?:writeFileSync|writeFile|readFileSync|readFile|appendFileSync|appendFile|createWriteStream|createReadStream|existsSync|openSync)\s*\(\s*['"]([^'"]+)['"]""",
        RegexOption.IGNORE_CASE
    )
    private val PY_OPEN_REGEX = Regex("""(?:with\s+)?open\s*\(\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
    private val PY_PATH_REGEX = Regex("""Path\s*\(\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
    private val LUA_IO_OPEN_REGEX = Regex("""io\.open\s*\(\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)

    // Detect common file references like 'data.json', 'commands/kick.js', 'storage.txt'
    private val FILE_PATH_LITERAL_REGEX = Regex(
        """['"`]((?:[a-zA-Z0-9_\-\.]+/)*[a-zA-Z0-9_\-]+\.(?:json|txt|md|js|py|ts|lua|env|csv|yml|yaml|ini|sql))['"`]""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Scans project code for referenced files and automatically creates any that are missing.
     * Returns the list of created file paths.
     */
    suspend fun autoCreateReferencedFiles(
        project: BotProject,
        files: List<BotFile>,
        repository: BotRepository
    ): List<String> {
        val existingPaths = files.map { normalizePath(it.filePath) }.toSet()
        val detectedPaths = mutableSetOf<String>()

        for (file in files) {
            val content = file.content
            if (content.isBlank()) continue

            // 1. JS / TS require('./...')
            for (m in REQUIRE_REGEX.findAll(content)) {
                val raw = m.groupValues[1]
                val path = resolveExtensionIfMissing(raw, defaultExt = "js")
                detectedPaths.add(normalizePath(path))
            }

            // 2. JS / TS import ... from './...'
            for (m in IMPORT_REGEX.findAll(content)) {
                val raw = m.groupValues[1]
                val path = resolveExtensionIfMissing(raw, defaultExt = "js")
                detectedPaths.add(normalizePath(path))
            }

            // 3. JS / TS direct import './...'
            for (m in IMPORT_DIRECT_REGEX.findAll(content)) {
                val raw = m.groupValues[1]
                val path = resolveExtensionIfMissing(raw, defaultExt = "js")
                detectedPaths.add(normalizePath(path))
            }

            // 4. Node.js fs.writeFileSync / readFileSync etc.
            for (m in FS_CALL_REGEX.findAll(content)) {
                val path = m.groupValues[1]
                if (isValidProjectFilePath(path)) {
                    detectedPaths.add(normalizePath(path))
                }
            }

            // 5. Python open('path', ...)
            for (m in PY_OPEN_REGEX.findAll(content)) {
                val path = m.groupValues[1]
                if (isValidProjectFilePath(path)) {
                    detectedPaths.add(normalizePath(path))
                }
            }

            // 6. Python Path('path')
            for (m in PY_PATH_REGEX.findAll(content)) {
                val path = m.groupValues[1]
                if (isValidProjectFilePath(path)) {
                    detectedPaths.add(normalizePath(path))
                }
            }

            // 7. Lua io.open('path')
            for (m in LUA_IO_OPEN_REGEX.findAll(content)) {
                val path = m.groupValues[1]
                if (isValidProjectFilePath(path)) {
                    detectedPaths.add(normalizePath(path))
                }
            }

            // 8. General string literals with file extensions (e.g. 'settings.json')
            for (m in FILE_PATH_LITERAL_REGEX.findAll(content)) {
                val candidate = m.groupValues[1]
                if (isValidProjectFilePath(candidate)) {
                    detectedPaths.add(normalizePath(candidate))
                }
            }
        }

        val createdFiles = mutableListOf<String>()

        for (targetPath in detectedPaths) {
            if (targetPath.isBlank() || existingPaths.contains(targetPath)) continue

            val initialContent = generateStarterContentForPath(targetPath)
            try {
                repository.createFile(project.id, targetPath, initialContent)
                createdFiles.add(targetPath)
                repository.addTerminalLog(
                    project.id,
                    "📁 [AUTO FILE] Created missing file referenced in code: $targetPath",
                    "SUCCESS"
                )
            } catch (_: Exception) {}
        }

        return createdFiles
    }

    private fun normalizePath(raw: String): String {
        var clean = raw.trim().replace('\\', '/')
        while (clean.startsWith("./")) {
            clean = clean.substring(2)
        }
        while (clean.startsWith("/")) {
            clean = clean.substring(1)
        }
        return clean
    }

    private fun resolveExtensionIfMissing(path: String, defaultExt: String): String {
        val normalized = normalizePath(path)
        val lastSegment = normalized.substringAfterLast('/')
        return if (!lastSegment.contains('.')) {
            "$normalized.$defaultExt"
        } else {
            normalized
        }
    }

    private fun isValidProjectFilePath(path: String): Boolean {
        val p = path.trim()
        if (p.isEmpty()) return false
        if (p.startsWith("http://") || p.startsWith("https://") || p.startsWith("data:") || p.startsWith("//")) {
            return false
        }
        // Avoid package imports like 'discord.js' or 'fs' or 'path'
        if (p == "discord.js" || p == "dotenv" || p == "fs" || p == "path" || p == "os" || p == "sys") {
            return false
        }
        // Must contain a valid file extension
        val validExtensions = setOf(
            "json", "txt", "md", "js", "py", "ts", "lua", "env", "csv", "yml", "yaml", "ini", "sql", "rs", "go", "java", "cs"
        )
        val ext = p.substringAfterLast('.', "").lowercase()
        return validExtensions.contains(ext)
    }

    private fun generateStarterContentForPath(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "json" -> "{\n}\n"
            else -> ""
        }
    }
}
