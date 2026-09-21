package com.example.ui.editor.intellisense

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DiscordBackground
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.DiscordDarker
import com.example.ui.theme.DiscordElevated
import com.example.ui.theme.DiscordGreen
import com.example.ui.theme.DiscordHover
import com.example.ui.theme.DiscordRed
import com.example.ui.theme.DiscordSurface
import com.example.ui.theme.DiscordTextMuted
import com.example.ui.theme.DiscordTextPrimary
import com.example.ui.theme.DiscordTextSecondary
import com.example.ui.theme.DiscordYellow

@Composable
fun CompletionKindBadge(kind: CompletionKind) {
    val (color, bgColor) = when (kind) {
        CompletionKind.METHOD -> DiscordBlurple to DiscordBlurple.copy(alpha = 0.2f)
        CompletionKind.PROPERTY -> DiscordGreen to DiscordGreen.copy(alpha = 0.2f)
        CompletionKind.CLASS -> Color(0xFF00B0F4) to Color(0xFF00B0F4).copy(alpha = 0.2f)
        CompletionKind.KEYWORD -> DiscordYellow to DiscordYellow.copy(alpha = 0.2f)
        CompletionKind.SNIPPET -> Color(0xFFFF73FA) to Color(0xFFFF73FA).copy(alpha = 0.2f)
        CompletionKind.EVENT -> Color(0xFFFEE75C) to Color(0xFFFEE75C).copy(alpha = 0.2f)
        CompletionKind.CONSTANT -> Color(0xFFEB459E) to Color(0xFFEB459E).copy(alpha = 0.2f)
        CompletionKind.INTERFACE -> Color(0xFF57F287) to Color(0xFF57F287).copy(alpha = 0.2f)
        CompletionKind.MODULE -> Color(0xFFFFA500) to Color(0xFFFFA500).copy(alpha = 0.2f)
    }

    Box(
        modifier = Modifier
            .size(18.dp)
            .background(bgColor, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = kind.symbol,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Docked suggestion bar for IntelliSense directly above the helper keyboard keys.
 */
@Composable
fun IntelliSenseBar(
    suggestions: List<CompletionItem>,
    onSelectSuggestion: (CompletionItem) -> Unit,
    onShowDoc: (CompletionItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = suggestions.isNotEmpty(),
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Surface(
            color = DiscordDarker,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DiscordBlurple.copy(alpha = 0.4f)),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // IntelliSense Label pill
                Surface(
                    color = DiscordBlurple.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = "IntelliSense",
                        color = DiscordBlurple,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                // Horizontal scroll list of suggestions
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    suggestions.forEach { item ->
                        Surface(
                            color = DiscordElevated,
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, DiscordHover),
                            modifier = Modifier
                                .clickable { onSelectSuggestion(item) }
                                .testTag("intellisense_item_${item.label}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompletionKindBadge(item.kind)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = item.label,
                                    color = DiscordTextPrimary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onShowDoc(item) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Details",
                                        tint = DiscordTextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Close suggestions button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close suggestions",
                        tint = DiscordTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Signature help banner displaying parameter hints when typing inside function calls.
 */
@Composable
fun SignatureHelpBar(
    signatureHelp: SignatureHelp?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (signatureHelp == null) return

    Surface(
        color = DiscordSurface,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DiscordGreen.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Signature with active parameter highlight
                val annotatedSignature = buildAnnotatedString {
                    withStyle(SpanStyle(color = DiscordTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)) {
                        append("${signatureHelp.functionName}(")
                    }

                    signatureHelp.parameters.forEachIndexed { index, param ->
                        val isActive = index == signatureHelp.activeParameterIndex
                        if (isActive) {
                            withStyle(
                                SpanStyle(
                                    color = DiscordGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            ) {
                                append("${param.name}: ${param.type}")
                            }
                        } else {
                            withStyle(
                                SpanStyle(
                                    color = DiscordTextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            ) {
                                append("${param.name}: ${param.type}")
                            }
                        }

                        if (index < signatureHelp.parameters.size - 1) {
                            withStyle(SpanStyle(color = DiscordTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)) {
                                append(", ")
                            }
                        }
                    }

                    withStyle(SpanStyle(color = DiscordTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)) {
                        append(")")
                    }
                }

                Text(text = annotatedSignature)

                // Active parameter description
                if (signatureHelp.activeParameterIndex in signatureHelp.parameters.indices) {
                    val activeParam = signatureHelp.parameters[signatureHelp.activeParameterIndex]
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "👉 ${activeParam.name}: ${activeParam.documentation}",
                        color = DiscordTextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = DiscordTextMuted,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Detailed documentation dialog for an IntelliSense completion item.
 */
@Composable
fun IntelliSenseDocDialog(
    item: CompletionItem?,
    onDismiss: () -> Unit,
    onInsert: (CompletionItem) -> Unit
) {
    if (item == null) return
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompletionKindBadge(item.kind)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = item.label,
                        color = DiscordTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${item.kind.label} • ${item.category}",
                        color = DiscordTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                // Signature / Type detail
                Surface(
                    color = DiscordDarker,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.detail,
                        color = DiscordGreen,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Documentation text
                Text(
                    text = item.documentation,
                    color = DiscordTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                // Code Example if available
                if (item.example != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "EXAMPLE USAGE",
                        color = DiscordBlurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = DiscordDarker,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.example,
                            color = DiscordTextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onInsert(item)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
            ) {
                Text("Insert Code", color = Color.White)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clean = item.insertText.replace("\$1", "").replace("\$2", "").replace("\$3", "")
                        clipboard.setPrimaryClip(ClipData.newPlainText("Code", clean))
                        Toast.makeText(context, "Copied snippet", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copy", color = DiscordTextSecondary)
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = DiscordTextSecondary)
                }
            }
        },
        containerColor = DiscordSurface
    )
}

/**
 * Full-screen / Dialog explorer for the complete IntelliSense and API Reference Catalog.
 */
@Composable
fun IntelliSenseExplorerDialog(
    isPython: Boolean,
    onDismiss: () -> Unit,
    onInsert: (CompletionItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedItemForDoc by remember { mutableStateOf<CompletionItem?>(null) }

    val allItems = remember(isPython) {
        if (isPython) IntelliSenseRegistry.DISCORD_PY_ITEMS
        else IntelliSenseRegistry.DISCORD_JS_ITEMS
    }

    val categories = remember(allItems) {
        listOf("All") + allItems.map { it.category }.distinct()
    }

    val filteredItems = remember(allItems, searchQuery, selectedCategory) {
        allItems.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    item.label.contains(searchQuery, ignoreCase = true) ||
                    item.detail.contains(searchQuery, ignoreCase = true) ||
                    item.documentation.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "IntelliSense",
                        tint = DiscordBlurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPython) "Discord.py IntelliSense" else "Discord.js v14 IntelliSense",
                        color = DiscordTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = DiscordTextMuted, modifier = Modifier.size(16.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search classes, methods, properties...", color = DiscordTextMuted, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = DiscordTextMuted, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = DiscordTextMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DiscordBlurple,
                        unfocusedBorderColor = DiscordHover,
                        focusedContainerColor = DiscordDarker,
                        unfocusedContainerColor = DiscordDarker
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            color = if (isSelected) DiscordBlurple else DiscordElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.White else DiscordTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // List of completion items
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredItems) { item ->
                        Surface(
                            color = DiscordDarker,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, DiscordHover),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CompletionKindBadge(item.kind)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = item.label,
                                            color = DiscordTextPrimary,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = item.detail,
                                            color = DiscordGreen,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { selectedItemForDoc = item },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Documentation",
                                            tint = DiscordBlurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            onInsert(item)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Insert", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = DiscordTextSecondary)
            }
        },
        containerColor = DiscordSurface
    )

    // Detailed doc dialog if clicked
    selectedItemForDoc?.let { item ->
        IntelliSenseDocDialog(
            item = item,
            onDismiss = { selectedItemForDoc = null },
            onInsert = {
                onInsert(it)
                onDismiss()
            }
        )
    }
}
