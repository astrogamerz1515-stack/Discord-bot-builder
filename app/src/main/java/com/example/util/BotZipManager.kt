package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.BotFile
import com.example.data.model.BotLanguage
import com.example.data.model.BotProject
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class RawZipFile(
    val path: String,
    val content: String,
    val size: Long
)

data class ParsedBotPackage(
    val name: String,
    val description: String,
    val language: String,
    val prefix: String,
    val botToken: String,
    val clientId: String,
    val status: String,
    val activityType: String,
    val activityText: String,
    val files: List<BotFile>,
    val autoFixes: List<String> = emptyList()
)

data class ExportSummary(
    val success: Boolean,
    val message: String,
    val fileCount: Int = 0,
    val totalBytes: Long = 0
)

object BotZipManager {

    private const val MAX_EXTRACT_FILES = 500
    private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10MB per file

    private val IGNORED_PREFIXES = listOf(
        "__MACOSX/",
        ".git/",
        "node_modules/",
        ".idea/",
        ".vscode/",
        ".gradle/",
        "build/",
        "dist/",
        "target/",
        ".next/",
        ".cache/"
    )

    private val IGNORED_FILENAMES = setOf(
        ".ds_store",
        "thumbs.db",
        "desktop.ini"
    )

    private val BINARY_EXTENSIONS = setOf(
        "png", "jpg", "jpeg", "gif", "ico", "webp", "bmp", "tiff",
        "mp3", "wav", "ogg", "flac", "m4a", "aac",
        "zip", "tar", "gz", "7z", "rar", "bz2", "xz",
        "pdf", "exe", "bin", "so", "dll", "dylib",
        "db", "sqlite", "sqlite3", "pyc", "pyo", "pyd", "class", "jar", "wasm", "node"
    )

    /**
     * Versatile parser that handles either a ZIP archive of bot files,
     * OR a single source code file (.js, .ts, .py, .json, .env, .txt, etc.).
     */
    fun parseImportBytes(bytes: ByteArray, fallbackName: String = "Imported Bot"): ParsedBotPackage {
        // Attempt ZIP decompression first
        val rawPkg = try {
            val bais = java.io.ByteArrayInputStream(bytes)
            val pkg = parseZipStream(bais, fallbackName)
            if (pkg.files.isNotEmpty()) pkg else null
        } catch (_: Exception) {
            null
        } ?: parseSingleFile(bytes, fallbackName)

        // Automatically detect and fix common mistakes, missing intents, missing client.login, and auto-add missing files
        val tempProj = BotProject(
            name = rawPkg.name,
            description = rawPkg.description,
            language = rawPkg.language,
            prefix = rawPkg.prefix,
            botToken = rawPkg.botToken,
            clientId = rawPkg.clientId
        )
        val report = BotCodeAutoFixer.autoFixProject(tempProj, rawPkg.files)

        return rawPkg.copy(
            files = report.updatedFiles,
            autoFixes = report.fixesApplied
        )
    }

    private fun parseSingleFile(bytes: ByteArray, fileName: String): ParsedBotPackage {
        val cleanContent = String(bytes, StandardCharsets.UTF_8)
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        val ext = fileName.substringAfterLast('.', "").lowercase()
        val detectedLanguage = when (ext) {
            "py" -> BotLanguage.PYTHON
            "ts" -> BotLanguage.TYPESCRIPT
            "rs" -> BotLanguage.RUST
            "go" -> BotLanguage.GO
            "java" -> BotLanguage.JAVA
            "cs" -> BotLanguage.CSHARP
            else -> BotLanguage.JAVASCRIPT
        }

        val baseName = fileName.substringBeforeLast('.').ifBlank { "Imported Bot" }
            .replace("-", " ").replace("_", " ").capitalizeWords()

        val rawFile = RawZipFile(fileName, cleanContent, bytes.size.toLong())
        val (token, prefix, clientId) = extractConfigSecrets(listOf(rawFile))

        val botFile = BotFile(
            projectId = 0,
            filePath = fileName,
            content = cleanContent,
            isEntrypoint = true,
            updatedAt = System.currentTimeMillis()
        )

        return ParsedBotPackage(
            name = baseName,
            description = "Imported ${detectedLanguage.displayName} Discord bot file",
            language = detectedLanguage.name,
            prefix = prefix.ifBlank { "!" },
            botToken = token,
            clientId = clientId,
            status = "Online",
            activityType = "PLAYING",
            activityText = "${prefix.ifBlank { "!" }} help | ${detectedLanguage.displayName}",
            files = listOf(botFile)
        )
    }

    /**
     * Reads a ZIP input stream and parses all bot files and project metadata.
     * Handles single-folder GitHub archives by automatically flattening root folder prefixes.
     */
    fun parseZipStream(inputStream: InputStream, fallbackName: String = "Imported Bot"): ParsedBotPackage {
        val rawFiles = mutableListOf<RawZipFile>()
        val zipIn = ZipInputStream(BufferedInputStream(inputStream))

        try {
            var entry: ZipEntry? = zipIn.nextEntry
            var count = 0

            while (entry != null && count < MAX_EXTRACT_FILES) {
                val rawName = entry.name.replace("\\", "/")

                // Check directory and path traversal
                if (!entry.isDirectory && !rawName.contains("../")) {
                    val cleanPath = rawName.trimStart('/')
                    val lowerPath = cleanPath.lowercase()
                    val ext = cleanPath.substringAfterLast('.', "").lowercase()

                    val isIgnored = IGNORED_PREFIXES.any { lowerPath.startsWith(it) || lowerPath.contains("/$it") } ||
                            IGNORED_FILENAMES.contains(lowerPath.substringAfterLast('/')) ||
                            BINARY_EXTENSIONS.contains(ext)

                    if (!isIgnored) {
                        val buffer = ByteArrayOutputStream()
                        val chunk = ByteArray(8192)
                        var bytesRead: Int
                        var currentSize = 0L

                        while (zipIn.read(chunk).also { bytesRead = it } != -1) {
                            currentSize += bytesRead
                            if (currentSize > MAX_FILE_SIZE_BYTES) {
                                break
                            }
                            buffer.write(chunk, 0, bytesRead)
                        }

                        val rawBytes = buffer.toByteArray()
                        val hasNulByte = rawBytes.take(1024).any { it == 0.toByte() }

                        if (!hasNulByte) {
                            val contentStr = String(rawBytes, StandardCharsets.UTF_8)
                                .removePrefix("\uFEFF")
                                .replace("\r\n", "\n")
                                .replace("\r", "\n")
                            rawFiles.add(RawZipFile(cleanPath, contentStr, currentSize))
                            count++
                        }
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        } finally {
            try {
                zipIn.close()
            } catch (_: Exception) {}
        }

        if (rawFiles.isEmpty()) {
            throw IllegalArgumentException("No valid bot files found in the ZIP archive. Make sure it contains bot code (.js, .ts, .py, etc.).")
        }

        // Detect if all files share a common root folder (e.g. GitHub archive "repo-name-main/index.js")
        val flattenedFiles = stripCommonRootDirectory(rawFiles)

        // Check if there is an exported bot_project.json metadata file
        val metadataFile = flattenedFiles.find { it.path == "bot_project.json" }
        if (metadataFile != null) {
            try {
                val json = JSONObject(metadataFile.content)
                val name = json.optString("name", fallbackName).ifBlank { fallbackName }
                val desc = json.optString("description", "Imported Discord bot from ZIP")
                val lang = json.optString("language", BotLanguage.JAVASCRIPT.name)
                val prefix = json.optString("prefix", "!")
                val token = json.optString("botToken", "")
                val clientId = json.optString("clientId", "")
                val status = json.optString("status", "Online")
                val activityType = json.optString("activityType", "PLAYING")
                val activityText = json.optString("activityText", "$prefix help")

                val botFiles = flattenedFiles
                    .filter { it.path != "bot_project.json" }
                    .map { raw ->
                        val isEntry = detectIsEntrypoint(raw.path, lang, null)
                        BotFile(
                            projectId = 0,
                            filePath = raw.path,
                            content = raw.content,
                            isEntrypoint = isEntry,
                            updatedAt = System.currentTimeMillis()
                        )
                    }

                val finalFiles = ensureEntrypointPresent(botFiles, lang)
                return ParsedBotPackage(
                    name = name,
                    description = desc,
                    language = lang,
                    prefix = prefix,
                    botToken = token,
                    clientId = clientId,
                    status = status,
                    activityType = activityType,
                    activityText = activityText,
                    files = finalFiles
                )
            } catch (_: Exception) {
                // Fallback to heuristic parser if JSON parsing fails
            }
        }

        // Heuristic analysis of generic bot zip (GitHub repos, custom bots, Discord.js, Discord.py, etc.)
        val packageJson = flattenedFiles.find { it.path == "package.json" }
        var parsedPkgName: String? = null
        var parsedPkgDesc: String? = null
        var parsedPkgMain: String? = null

        if (packageJson != null) {
            try {
                val pkgObj = JSONObject(packageJson.content)
                parsedPkgName = pkgObj.optString("name", "").takeIf { it.isNotBlank() }
                parsedPkgDesc = pkgObj.optString("description", "").takeIf { it.isNotBlank() }
                parsedPkgMain = pkgObj.optString("main", "").takeIf { it.isNotBlank() }
            } catch (_: Exception) {}
        }

        // Language detection
        val detectedLanguage = detectLanguage(flattenedFiles)

        // Token & Prefix extraction
        val (detectedToken, detectedPrefix, detectedClientId) = extractConfigSecrets(flattenedFiles)

        val finalName = parsedPkgName?.replace("-", " ")?.replace("_", " ")?.capitalizeWords()
            ?: fallbackName.removeSuffix(".zip").replace("-", " ").replace("_", " ").capitalizeWords()

        val finalDesc = parsedPkgDesc ?: "Imported ${detectedLanguage.displayName} Discord bot"
        val finalPrefix = detectedPrefix.ifBlank { "!" }

        val botFiles = flattenedFiles
            .filter { it.path != "bot_project.json" }
            .map { raw ->
                val isEntry = detectIsEntrypoint(raw.path, detectedLanguage.name, parsedPkgMain)
                BotFile(
                    projectId = 0,
                    filePath = raw.path,
                    content = raw.content,
                    isEntrypoint = isEntry,
                    updatedAt = System.currentTimeMillis()
                )
            }

        val finalFiles = ensureEntrypointPresent(botFiles, detectedLanguage.name)

        return ParsedBotPackage(
            name = finalName.ifBlank { "Imported Bot" },
            description = finalDesc,
            language = detectedLanguage.name,
            prefix = finalPrefix,
            botToken = detectedToken,
            clientId = detectedClientId,
            status = "Online",
            activityType = "PLAYING",
            activityText = "$finalPrefix help | ${detectedLanguage.displayName}",
            files = finalFiles
        )
    }

    /**
     * Packs all project files and metadata into a valid ZIP archive stream.
     */
    fun exportToZipStream(
        outputStream: OutputStream,
        project: BotProject,
        files: List<BotFile>
    ): ExportSummary {
        val zipOut = ZipOutputStream(BufferedOutputStream(outputStream))
        var fileCount = 0
        var totalBytes = 0L

        try {
            // 1. Write bot_project.json metadata
            val metadataObj = JSONObject().apply {
                put("formatVersion", 1)
                put("name", project.name)
                put("description", project.description)
                put("language", project.language)
                put("prefix", project.prefix)
                put("botToken", project.botToken)
                put("clientId", project.clientId)
                put("status", project.status)
                put("activityType", project.activityType)
                put("activityText", project.activityText)
                put("intentMessageContent", project.intentMessageContent)
                put("intentGuildMembers", project.intentGuildMembers)
                put("intentPresences", project.intentPresences)
                put("exportedAt", System.currentTimeMillis())
            }

            val metaBytes = metadataObj.toString(2).toByteArray(StandardCharsets.UTF_8)
            val metaEntry = ZipEntry("bot_project.json")
            zipOut.putNextEntry(metaEntry)
            zipOut.write(metaBytes)
            zipOut.closeEntry()
            fileCount++
            totalBytes += metaBytes.size

            // 2. Prepare, auto-fix, and ensure production-ready deployment files
            val baseFiles = if (files.isEmpty()) {
                listOf(
                    BotFile(
                        projectId = project.id,
                        filePath = if (project.language.equals(BotLanguage.PYTHON.name, true)) "bot.py" else "index.js",
                        content = "",
                        isEntrypoint = true
                    )
                )
            } else files

            val report = BotCodeAutoFixer.autoFixProject(project, baseFiles)
            val exportFiles = report.updatedFiles

            // Also write a safe .env.example with placeholders for secure sharing
            val hasEnvExample = exportFiles.any { it.filePath.equals(".env.example", true) }
            val finalExportFiles = if (!hasEnvExample) {
                exportFiles + BotFile(
                    projectId = project.id,
                    filePath = ".env.example",
                    content = "# Copy this file to .env and insert your real Discord credentials\nDISCORD_TOKEN=YOUR_BOT_TOKEN_HERE\nCLIENT_ID=${project.clientId}\nPREFIX=${project.prefix}\n",
                    isEntrypoint = false
                )
            } else exportFiles

            for (file in finalExportFiles) {
                val cleanPath = file.filePath.trimStart('/')
                val bytes = file.content.toByteArray(StandardCharsets.UTF_8)
                val entry = ZipEntry(cleanPath)
                zipOut.putNextEntry(entry)
                zipOut.write(bytes)
                zipOut.closeEntry()
                fileCount++
                totalBytes += bytes.size
            }

            zipOut.finish()
            zipOut.flush()

            return ExportSummary(
                success = true,
                message = "Successfully exported $fileCount files to ZIP archive (${totalBytes / 1024} KB).",
                fileCount = fileCount,
                totalBytes = totalBytes
            )
        } catch (e: Exception) {
            return ExportSummary(
                success = false,
                message = "Export failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        } finally {
            try {
                zipOut.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Creates a temporary ZIP file in cache directory for direct sharing via Intent.
     */
    fun createZipFileForSharing(context: Context, project: BotProject, files: List<BotFile>): File? {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val safeName = project.name
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .trim('_')
                .ifEmpty { "bot" }

            val zipFile = File(exportDir, "${safeName}.zip")
            val fos = FileOutputStream(zipFile)
            val summary = exportToZipStream(fos, project, files)
            if (summary.success) {
                zipFile
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getShareUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun stripCommonRootDirectory(files: List<RawZipFile>): List<RawZipFile> {
        if (files.isEmpty()) return files

        val paths = files.map { it.path }
        val firstSlash = paths.first().indexOf('/')
        if (firstSlash <= 0) return files

        val candidateRoot = paths.first().substring(0, firstSlash + 1)
        val allShareRoot = paths.all { it.startsWith(candidateRoot) }

        return if (allShareRoot) {
            files.map { it.copy(path = it.path.removePrefix(candidateRoot)) }
                .filter { it.path.isNotBlank() }
        } else {
            files
        }
    }

    private fun detectLanguage(files: List<RawZipFile>): BotLanguage {
        val paths = files.map { it.path.lowercase() }

        // TypeScript check
        if (paths.any { it.endsWith(".ts") || it == "tsconfig.json" }) {
            return BotLanguage.TYPESCRIPT
        }

        // Python check
        if (paths.any { it.endsWith(".py") || it == "requirements.txt" || it == "pyproject.toml" }) {
            return BotLanguage.PYTHON
        }

        // Rust check
        if (paths.any { it.endsWith(".rs") || it == "cargo.toml" }) {
            return BotLanguage.RUST
        }

        // Go check
        if (paths.any { it.endsWith(".go") || it == "go.mod" }) {
            return BotLanguage.GO
        }

        // Java check
        if (paths.any { it.endsWith(".java") || it == "pom.xml" || it.endsWith(".gradle") }) {
            return BotLanguage.JAVA
        }

        // C# check
        if (paths.any { it.endsWith(".cs") || it.endsWith(".csproj") }) {
            return BotLanguage.CSHARP
        }

        // Default: JavaScript
        return BotLanguage.JAVASCRIPT
    }

    private fun extractConfigSecrets(files: List<RawZipFile>): Triple<String, String, String> {
        var token = ""
        var prefix = ""
        var clientId = ""

        // Check .env files
        val envFile = files.find { it.path == ".env" || it.path.endsWith("/.env") }
        if (envFile != null) {
            for (line in envFile.content.lines()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("#") || !trimmed.contains("=")) continue
                val key = trimmed.substringBefore("=").trim().uppercase()
                var value = trimmed.substringAfter("=").trim().trim('"', '\'', '`')

                when (key) {
                    "DISCORD_TOKEN", "BOT_TOKEN", "TOKEN" -> if (token.isEmpty()) token = value
                    "PREFIX", "BOT_PREFIX", "COMMAND_PREFIX" -> if (prefix.isEmpty()) prefix = value
                    "CLIENT_ID", "BOT_ID", "APPLICATION_ID", "APP_ID" -> if (clientId.isEmpty()) clientId = value
                }
            }
        }

        // Check config.json or bot.json
        val configFile = files.find {
            it.path == "config.json" || it.path == "bot.json" || it.path == "settings.json" ||
                    it.path.endsWith("/config.json")
        }
        if (configFile != null) {
            try {
                val json = JSONObject(configFile.content)
                if (token.isEmpty()) {
                    token = json.optString("token", "").ifEmpty { json.optString("botToken", "") }
                }
                if (prefix.isEmpty()) {
                    prefix = json.optString("prefix", "")
                }
                if (clientId.isEmpty()) {
                    clientId = json.optString("clientId", "").ifEmpty { json.optString("client_id", "") }
                }
            } catch (_: Exception) {}
        }

        // Deep heuristic scan across all code files for hardcoded secrets/configs
        if (token.isEmpty() || prefix.isEmpty() || clientId.isEmpty()) {
            val tokenRegex = Regex("""(?:client\.login|bot\.run)\s*\(\s*["']([A-Za-z0-9_\-\.]{40,})["']\s*\)|(?:DISCORD_TOKEN|BOT_TOKEN|TOKEN|token|botToken)\s*[:=]\s*["']([A-Za-z0-9_\-\.]{40,})["']""")
            val prefixRegex = Regex("""(?:command_prefix|prefix|PREFIX|bot_prefix)\s*[:=]\s*["']([^"'\s]{1,5})["']""")
            val clientIdRegex = Regex("""(?:client_?id|CLIENT_ID|application_?id|APPLICATION_ID)\s*[:=]\s*["']?(\d{17,20})["']?""")

            for (file in files) {
                if (file.content.length > 500_000) continue
                if (token.isEmpty()) {
                    val m = tokenRegex.find(file.content)
                    if (m != null) {
                        val found = m.groupValues[1].ifEmpty { m.groupValues[2] }
                        if (found.isNotBlank() && !found.contains("YOUR_") && !found.contains("TOKEN_HERE") && !found.contains("process.env")) {
                            token = found
                        }
                    }
                }
                if (prefix.isEmpty()) {
                    val m = prefixRegex.find(file.content)
                    if (m != null) {
                        prefix = m.groupValues[1]
                    }
                }
                if (clientId.isEmpty()) {
                    val m = clientIdRegex.find(file.content)
                    if (m != null) {
                        clientId = m.groupValues[1]
                    }
                }
            }
        }

        return Triple(token, prefix, clientId)
    }

    private fun detectIsEntrypoint(path: String, languageName: String, packageMain: String?): Boolean {
        val clean = path.trimStart('/')
        if (packageMain != null && (clean == packageMain || clean == packageMain.removePrefix("./"))) {
            return true
        }

        val candidates = when (languageName.uppercase()) {
            "JAVASCRIPT" -> listOf("index.js", "src/index.js", "bot.js", "main.js", "app.js")
            "TYPESCRIPT" -> listOf("src/index.ts", "index.ts", "src/bot.ts", "bot.ts", "src/main.ts", "main.ts")
            "PYTHON" -> listOf("bot.py", "main.py", "app.py", "src/bot.py", "src/main.py")
            "RUST" -> listOf("src/main.rs", "main.rs")
            "GO" -> listOf("main.go", "bot.go")
            "JAVA" -> listOf("src/Main.java", "src/main/java/Main.java", "Main.java")
            "CSHARP" -> listOf("Program.cs", "src/Program.cs")
            else -> listOf("index.js", "bot.py", "main.js")
        }

        return candidates.any { clean.equals(it, ignoreCase = true) }
    }

    private fun ensureEntrypointPresent(files: List<BotFile>, languageName: String): List<BotFile> {
        if (files.any { it.isEntrypoint }) {
            return files
        }

        // None marked as entrypoint, pick first matching candidate or the first file
        val candidate = files.find {
            it.filePath.endsWith(".js") || it.filePath.endsWith(".ts") ||
                    it.filePath.endsWith(".py") || it.filePath.endsWith(".go") ||
                    it.filePath.endsWith(".rs") || it.filePath.endsWith(".java")
        } ?: files.firstOrNull()

        return if (candidate != null) {
            files.map { if (it.filePath == candidate.filePath) it.copy(isEntrypoint = true) else it }
        } else {
            files
        }
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
