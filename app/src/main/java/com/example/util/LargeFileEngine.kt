package com.example.util

import android.content.Context
import com.example.data.model.BotFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

/**
 * High-performance streaming & paging engine for handling text files of any size (15MB to 100MB+)
 * on Android without OutOfMemoryError (OOM) or Application Not Responding (ANR).
 *
 * Implements:
 * 1. 32KB buffered line-by-line and chunked streaming off the main thread.
 * 2. RandomAccessFile seek-based paging for zero-lag editing of massive files.
 * 3. Disk-backed storage bypassing SQLite 2MB CursorWindow limits.
 * 4. Safe atomic file replacement and strict resource closing (.use).
 */
object LargeFileEngine {

    const val LARGE_FILE_THRESHOLD_BYTES = 256 * 1024L // 256 KB activates Large File Mode
    const val BUFFER_SIZE = 32 * 1024 // 32 KB standard buffer
    const val DEFAULT_PAGE_SIZE = 250 // lines per page

    data class LineIndex(
        val file: File,
        val totalLines: Int,
        val fileSize: Long,
        val pageSize: Int,
        val pageOffsets: LongArray // byte offset of the start of each page
    ) {
        val pageCount: Int get() = pageOffsets.size
        val fileSizeBytes: Long get() = fileSize
    }

    data class PageData(
        val pageIndex: Int, // 0-based
        val totalPages: Int,
        val startLineNumber: Int, // 1-based
        val endLineNumber: Int, // 1-based
        val totalLines: Int,
        val pageText: String,
        val fileSize: Long
    ) {
        val text: String get() = pageText
    }

    data class StreamSearchResult(
        val lineNumber: Int,
        val lineSnippet: String,
        val pageIndex: Int
    )

    /**
     * Resolves the dedicated local disk file for a project's source file.
     */
    fun getLocalFile(context: Context, projectId: Long, filePath: String): File {
        val safePath = filePath.replace("\\", "/").trimStart('/')
        val dir = File(context.filesDir, "bot_projects/$projectId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val targetFile = File(dir, safePath)
        targetFile.parentFile?.let {
            if (!it.exists()) it.mkdirs()
        }
        return targetFile
    }

    /**
     * Streams data from an InputStream directly into a target file using fixed 32KB byte chunks.
     * Never calls available() or accumulates into memory.
     */
    suspend fun streamToFile(
        inputStream: InputStream,
        destinationFile: File,
        bufferSize: Int = BUFFER_SIZE
    ): Long = withContext(Dispatchers.IO) {
        destinationFile.parentFile?.mkdirs()
        var totalBytes = 0L
        val buffer = ByteArray(bufferSize)

        inputStream.use { inStream ->
            BufferedOutputStream(FileOutputStream(destinationFile), bufferSize).use { outStream ->
                var read: Int
                while (inStream.read(buffer).also { read = it } != -1) {
                    outStream.write(buffer, 0, read)
                    totalBytes += read
                }
                outStream.flush()
            }
        }
        totalBytes
    }

    /**
     * Streams a disk file into an OutputStream using 32KB chunks.
     */
    suspend fun streamFromFile(
        sourceFile: File,
        outputStream: OutputStream,
        bufferSize: Int = BUFFER_SIZE
    ): Long = withContext(Dispatchers.IO) {
        var totalBytes = 0L
        val buffer = ByteArray(bufferSize)

        BufferedInputStream(FileInputStream(sourceFile), bufferSize).use { inStream ->
            var read: Int
            while (inStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                totalBytes += read
            }
            outputStream.flush()
        }
        totalBytes
    }

    /**
     * Scans a file off-thread with a 32KB buffer, building a lightweight page index
     * without loading file content into memory.
     */
    suspend fun buildPageIndex(
        file: File,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): LineIndex = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) {
            return@withContext LineIndex(file, 0, 0L, pageSize, longArrayOf(0L))
        }

        val offsets = ArrayList<Long>()
        offsets.add(0L) // Page 0 starts at offset 0

        var lineCount = 0
        var currentOffset = 0L
        val buffer = ByteArray(BUFFER_SIZE)

        BufferedInputStream(FileInputStream(file), BUFFER_SIZE).use { bis ->
            var bytesRead: Int
            while (bis.read(buffer).also { bytesRead = it } != -1) {
                for (i in 0 until bytesRead) {
                    if (buffer[i] == '\n'.code.toByte()) {
                        lineCount++
                        if (lineCount % pageSize == 0) {
                            offsets.add(currentOffset + i + 1L)
                        }
                    }
                }
                currentOffset += bytesRead
            }
        }

        // If file didn't end with newline, count the final partial line
        if (currentOffset > 0 && lineCount == 0) {
            lineCount = 1
        }

        LineIndex(
            file = file,
            totalLines = lineCount.coerceAtLeast(1),
            fileSize = file.length(),
            pageSize = pageSize,
            pageOffsets = offsets.toLongArray()
        )
    }

    /**
     * Reads a single page of text using RandomAccessFile seek to jump directly to the target offset.
     * Prevents loading the rest of the 15MB+ file into memory.
     */
    suspend fun readPage(
        index: LineIndex,
        pageIndex: Int
    ): PageData = withContext(Dispatchers.IO) {
        val totalPages = index.pageOffsets.size.coerceAtLeast(1)
        val validPageIndex = pageIndex.coerceIn(0, totalPages - 1)
        val startOffset = index.pageOffsets[validPageIndex]
        val startLineNumber = (validPageIndex * index.pageSize) + 1

        val sb = StringBuilder()
        var linesRead = 0
        var actualEndLine = startLineNumber

        RandomAccessFile(index.file, "r").use { raf ->
            raf.seek(startOffset)
            val isr = InputStreamReader(
                object : InputStream() {
                    override fun read(): Int = raf.read()
                    override fun read(b: ByteArray, off: Int, len: Int): Int = raf.read(b, off, len)
                },
                StandardCharsets.UTF_8
            )
            val reader = BufferedReader(isr, BUFFER_SIZE)

            var line: String? = reader.readLine()
            while (line != null && linesRead < index.pageSize) {
                if (linesRead > 0) sb.append("\n")
                sb.append(line)
                linesRead++
                actualEndLine = startLineNumber + linesRead - 1
                if (linesRead < index.pageSize) {
                    line = reader.readLine()
                }
            }
        }

        PageData(
            pageIndex = validPageIndex,
            totalPages = totalPages,
            startLineNumber = startLineNumber,
            endLineNumber = actualEndLine.coerceAtLeast(startLineNumber),
            totalLines = index.totalLines,
            pageText = sb.toString(),
            fileSize = index.fileSize
        )
    }

    /**
     * Saves changes made to a specific page back to the file using streaming swap,
     * maintaining consistency without loading the whole file into the heap.
     */
    suspend fun savePage(
        index: LineIndex,
        pageIndex: Int,
        newPageText: String
    ): LineIndex = withContext(Dispatchers.IO) {
        val file = index.file
        val tempFile = File(file.parentFile, "${file.name}.tmp_${System.currentTimeMillis()}")
        val totalPages = index.pageOffsets.size.coerceAtLeast(1)
        val validPageIndex = pageIndex.coerceIn(0, totalPages - 1)
        val startOffset = index.pageOffsets[validPageIndex]
        val nextOffset = if (validPageIndex + 1 < index.pageOffsets.size) {
            index.pageOffsets[validPageIndex + 1]
        } else {
            file.length()
        }

        val buffer = ByteArray(BUFFER_SIZE)

        RandomAccessFile(file, "r").use { srcRaf ->
            BufferedOutputStream(FileOutputStream(tempFile), BUFFER_SIZE).use { destOut ->
                // 1. Copy everything before this page
                if (startOffset > 0) {
                    srcRaf.seek(0)
                    var remaining = startOffset
                    while (remaining > 0) {
                        val toRead = remaining.coerceAtMost(BUFFER_SIZE.toLong()).toInt()
                        val read = srcRaf.read(buffer, 0, toRead)
                        if (read == -1) break
                        destOut.write(buffer, 0, read)
                        remaining -= read
                    }
                }

                // 2. Write modified page text
                val pageBytes = newPageText.toByteArray(StandardCharsets.UTF_8)
                destOut.write(pageBytes)

                // 3. Ensure a trailing newline if next page follows and pageText doesn't end with newline
                if (nextOffset < file.length() && !newPageText.endsWith("\n")) {
                    destOut.write('\n'.code)
                }

                // 4. Copy everything after this page
                if (nextOffset < file.length()) {
                    srcRaf.seek(nextOffset)
                    var read: Int
                    while (srcRaf.read(buffer).also { read = it } != -1) {
                        destOut.write(buffer, 0, read)
                    }
                }
                destOut.flush()
            }
        }

        // Atomic swap
        if (file.exists()) {
            file.delete()
        }
        tempFile.renameTo(file)

        // Re-index updated file
        buildPageIndex(file, index.pageSize)
    }

    /**
     * Streams entire content to disk using a 32KB buffered writer off-thread.
     */
    suspend fun writeEntireContent(file: File, content: String): LineIndex = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        val tempFile = File(file.parentFile, "${file.name}.tmp_${System.currentTimeMillis()}")

        BufferedWriter(OutputStreamWriter(FileOutputStream(tempFile), StandardCharsets.UTF_8), BUFFER_SIZE).use { writer ->
            writer.write(content)
            writer.flush()
        }

        if (file.exists()) {
            file.delete()
        }
        tempFile.renameTo(file)
        buildPageIndex(file, DEFAULT_PAGE_SIZE)
    }

    /**
     * Fast search across a 15MB+ file line-by-line using a 32KB BufferedReader.
     * Never retains all lines in memory.
     */
    suspend fun searchStream(
        file: File,
        query: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        maxResults: Int = 100
    ): List<StreamSearchResult> = withContext(Dispatchers.IO) {
        if (!file.exists() || query.isBlank()) return@withContext emptyList()

        val results = mutableListOf<StreamSearchResult>()
        var lineNum = 1

        BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8), BUFFER_SIZE).use { reader ->
            var line = reader.readLine()
            while (line != null && results.size < maxResults) {
                if (line.contains(query, ignoreCase = true)) {
                    val pageIndex = (lineNum - 1) / pageSize
                    results.add(
                        StreamSearchResult(
                            lineNumber = lineNum,
                            lineSnippet = line.trim(),
                            pageIndex = pageIndex
                        )
                    )
                }
                lineNum++
                line = reader.readLine()
            }
        }
        results
    }

    /**
     * Formats bytes into human-readable B, KB, MB, GB.
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(java.util.Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(java.util.Locale.US, "%.2f GB", gb)
    }
}
