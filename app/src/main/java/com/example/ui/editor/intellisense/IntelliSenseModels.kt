package com.example.ui.editor.intellisense

enum class CompletionKind(val symbol: String, val label: String) {
    METHOD("ƒ", "Method"),
    PROPERTY("p", "Property"),
    CLASS("C", "Class"),
    KEYWORD("k", "Keyword"),
    SNIPPET("S", "Snippet"),
    EVENT("⚡", "Event"),
    CONSTANT("c", "Constant"),
    INTERFACE("I", "Interface"),
    MODULE("M", "Module")
}

data class CompletionItem(
    val label: String,
    val insertText: String,
    val kind: CompletionKind,
    val detail: String,
    val documentation: String,
    val example: String? = null,
    val category: String = "Discord.js",
    val triggerPrefix: String = "",
    val replaceLength: Int = 0
)

data class SignatureHelp(
    val functionName: String,
    val signature: String,
    val parameters: List<ParameterInfo>,
    val activeParameterIndex: Int,
    val documentation: String
)

data class ParameterInfo(
    val name: String,
    val type: String,
    val documentation: String,
    val isOptional: Boolean = false
)
