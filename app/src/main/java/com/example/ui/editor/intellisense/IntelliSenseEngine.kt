package com.example.ui.editor.intellisense

object IntelliSenseEngine {

    /**
     * Extracts completion suggestions based on code, cursor position, and language.
     */
    fun computeCompletions(
        code: String,
        cursorIndex: Int,
        filePath: String
    ): List<CompletionItem> {
        val safeCursor = cursorIndex.coerceIn(0, code.length)
        val textBeforeCursor = code.substring(0, safeCursor)
        val isPython = filePath.endsWith(".py", ignoreCase = true)

        // Find the current token being typed
        val lastWordMatch = Regex("([A-Za-z0-9_$.]+)$").find(textBeforeCursor)
        val currentToken = lastWordMatch?.value ?: ""

        val baseItems = if (isPython) {
            IntelliSenseRegistry.DISCORD_PY_ITEMS
        } else {
            IntelliSenseRegistry.DISCORD_JS_ITEMS
        }

        // If typing after a dot (e.g. "interaction.", "embed.", "client.")
        if (currentToken.contains('.')) {
            val prefix = currentToken.substringBeforeLast('.') + "."
            val memberPart = currentToken.substringAfterLast('.')

            val matchingItems = baseItems.filter { item ->
                item.label.startsWith(prefix, ignoreCase = true) ||
                (item.triggerPrefix.isNotEmpty() && prefix.endsWith(item.triggerPrefix, ignoreCase = true))
            }

            if (matchingItems.isNotEmpty()) {
                return matchingItems.filter { item ->
                    val memberName = item.label.substringAfterLast('.')
                    memberPart.isEmpty() || memberName.contains(memberPart, ignoreCase = true)
                }.sortedBy { item ->
                    val memberName = item.label.substringAfterLast('.')
                    if (memberName.startsWith(memberPart, ignoreCase = true)) 0 else 1
                }
            }
        }

        // Check if user is typing "new " for class instantiation
        val trimmedBefore = textBeforeCursor.trimEnd()
        if (trimmedBefore.endsWith("new") || textBeforeCursor.endsWith("new ")) {
            return baseItems.filter { it.kind == CompletionKind.CLASS }
        }

        // General word completion matching
        if (currentToken.isNotEmpty()) {
            val filtered = baseItems.filter { item ->
                val shortName = item.label.substringAfterLast('.')
                shortName.contains(currentToken, ignoreCase = true) ||
                item.label.contains(currentToken, ignoreCase = true)
            }.sortedWith(compareBy(
                { !it.label.startsWith(currentToken, ignoreCase = true) },
                { !it.label.substringAfterLast('.').startsWith(currentToken, ignoreCase = true) },
                { it.label.length }
            ))

            if (filtered.isNotEmpty()) return filtered
        }

        // Default: return top recommended items for fast access
        return baseItems.take(12)
    }

    /**
     * Determines if the cursor is currently inside the argument list of a known function call
     * and returns the signature help information.
     */
    fun computeSignatureHelp(
        code: String,
        cursorIndex: Int
    ): SignatureHelp? {
        val safeCursor = cursorIndex.coerceIn(0, code.length)
        val textBeforeCursor = code.substring(0, safeCursor)

        // Find the last unmatched '('
        val lastOpenParen = textBeforeCursor.lastIndexOf('(')
        if (lastOpenParen == -1) return null

        val lastCloseParen = textBeforeCursor.lastIndexOf(')')
        if (lastCloseParen > lastOpenParen) {
            // Cursor is outside parentheses
            return null
        }

        // Extract the function call expression preceding the '('
        val textBeforeParen = textBeforeCursor.substring(0, lastOpenParen).trimEnd()
        val funcNameMatch = Regex("([A-Za-z0-9_$.]+)$").find(textBeforeParen) ?: return null
        val funcCall = funcNameMatch.value
        val simpleFuncName = funcCall.substringAfterLast('.')

        val signature = IntelliSenseRegistry.SIGNATURE_HELPS[simpleFuncName] ?: return null

        // Count commas inside the current argument list to determine active parameter index
        val argsText = textBeforeCursor.substring(lastOpenParen + 1)
        val commaCount = argsText.count { it == ',' }
        val activeIndex = commaCount.coerceAtMost(signature.parameters.size - 1)

        return signature.copy(activeParameterIndex = activeIndex)
    }

    /**
     * Resolves the text to replace and new cursor offset when accepting an IntelliSense completion.
     */
    fun applyCompletion(
        currentCode: String,
        cursorIndex: Int,
        item: CompletionItem
    ): Pair<String, Int> {
        val safeCursor = cursorIndex.coerceIn(0, currentCode.length)
        val textBefore = currentCode.substring(0, safeCursor)
        val textAfter = currentCode.substring(safeCursor)

        // Find how much of the current token should be replaced
        val lastWordMatch = Regex("([A-Za-z0-9_$.]+)$").find(textBefore)
        val currentToken = lastWordMatch?.value ?: ""

        // If the token matches the item's prefix or suffix, replace it
        val insertTextClean = item.insertText.replace("\$1", "").replace("\$2", "").replace("\$3", "")
        val replaceStartIndex: Int
        val newText: String

        if (currentToken.isNotEmpty()) {
            if (currentToken.contains('.')) {
                // If dot exists and item has dot:
                val prefix = currentToken.substringBeforeLast('.') + "."
                val member = currentToken.substringAfterLast('.')
                if (item.label.startsWith(prefix)) {
                    val itemMember = item.insertText.substringAfter(prefix)
                    val cleanMember = itemMember.replace("\$1", "").replace("\$2", "").replace("\$3", "")
                    val start = safeCursor - member.length
                    newText = currentCode.substring(0, start) + cleanMember + textAfter
                    val newCursor = start + cleanMember.length
                    return Pair(newText, newCursor)
                }
            }

            replaceStartIndex = safeCursor - currentToken.length
        } else {
            replaceStartIndex = safeCursor
        }

        val prefixPart = currentCode.substring(0, replaceStartIndex)
        val result = prefixPart + insertTextClean + textAfter
        val newCursor = replaceStartIndex + insertTextClean.length

        return Pair(result, newCursor)
    }
}
