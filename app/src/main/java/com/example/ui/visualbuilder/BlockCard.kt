package com.example.ui.visualbuilder

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable drag Modifier for Scratch blocks.
 * Initiates drag gesture upon long press and tracks drag displacement.
 */
fun Modifier.draggableBlock(
    onDragStart: (Offset) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {}
): Modifier = this.pointerInput(Unit) {
    detectDragGesturesAfterLongPress(
        onDragStart = { offset -> onDragStart(offset) },
        onDrag = { change, dragAmount ->
            change.consume()
            onDrag(dragAmount)
        },
        onDragEnd = onDragEnd,
        onDragCancel = onDragCancel
    )
}

/**
 * Scratch-style Block Composable.
 * Renders an authentic Scratch-shaped puzzle block with top/bottom interlocking notches,
 * category color coding, interactive parameter bubble capsules, quick action shortcuts,
 * and execution highlighting.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockCard(
    category: BlockCategory,
    title: String,
    subtitle: String = "",
    icon: String,
    indentLevel: Int = 0,
    inlineTokens: List<InlineToken> = emptyList(),
    isCurrentlyExecuting: Boolean = false,
    canMoveUp: Boolean = true,
    canMoveDown: Boolean = true,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onClick: () -> Unit = {},
    onPillClick: (String) -> Unit = { onClick() },
    onDelete: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    // Pulsing neon execution glow animation
    val executionTransition = rememberInfiniteTransition(label = "execGlow")
    val glowAlpha by executionTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val isHatBlock = (category == BlockCategory.EVENT)
    val cardShape = if (isHatBlock) {
        // Scratch Event Hat shape: rounded high dome at top
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
    } else {
        RoundedCornerShape(8.dp)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = (indentLevel * 18).dp)
            .testTag("block_card_$title"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nesting vertical spine indicator if indented (C-block inner slot)
        if (indentLevel > 0) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .heightIn(min = 46.dp)
                    .background(category.color, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Main block body
        Surface(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 46.dp)
                .clip(cardShape)
                .clickable(onClick = onClick)
                .then(
                    if (isCurrentlyExecuting) {
                        Modifier.border(
                            BorderStroke(2.dp, Color.White.copy(alpha = glowAlpha)),
                            cardShape
                        )
                    } else {
                        Modifier
                    }
                ),
            shape = cardShape,
            color = category.color,
            shadowElevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Top puzzle notch (bump) for non-hat blocks
                if (!isHatBlock) {
                    Box(
                        modifier = Modifier
                            .size(width = 24.dp, height = 4.dp)
                            .align(Alignment.TopCenter)
                            .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(VisualBuilderThemeColors.Background.copy(alpha = 0.4f))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left edge 4.dp vertical darker category color strip
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(category.darkStripColor)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Block Icon (⚡, 💬, ⚙️, 🔀, etc.)
                    Text(
                        text = icon,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Content Column: Title / Inline Tokens + Subtitle
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 6.dp)
                    ) {
                        // Title row or inline tokens
                        if (inlineTokens.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                inlineTokens.forEach { token ->
                                    when (token) {
                                        is InlineToken.Text -> {
                                            Text(
                                                text = token.text,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Monospace,
                                                color = VisualBuilderThemeColors.TextInsideBlock,
                                                modifier = Modifier.align(Alignment.CenterVertically)
                                            )
                                        }
                                        is InlineToken.Chip -> {
                                            // Scratch-style rounded capsule bubble chip
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.16f),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f)),
                                                modifier = Modifier
                                                    .align(Alignment.CenterVertically)
                                                    .clickable { onPillClick(token.fieldKey) }
                                            ) {
                                                Text(
                                                    text = token.value,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = VisualBuilderThemeColors.TextInsideBlock,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                color = VisualBuilderThemeColors.TextInsideBlock,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Subtitle descriptor (if present and distinct)
                        if (subtitle.isNotEmpty() && inlineTokens.isEmpty()) {
                            Text(
                                text = subtitle,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = VisualBuilderThemeColors.TextInsideBlock.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Quick Reorder Controls (▲ / ▼) for ergonomic one-tap reordering
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = canMoveUp,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("btn_block_move_up")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move block up",
                                tint = if (canMoveUp) VisualBuilderThemeColors.TextInsideBlock.copy(alpha = 0.8f) else Color.Transparent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onMoveDown,
                            enabled = canMoveDown,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("btn_block_move_down")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move block down",
                                tint = if (canMoveDown) VisualBuilderThemeColors.TextInsideBlock.copy(alpha = 0.8f) else Color.Transparent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Options Menu Button ("⋯")
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("btn_block_options")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Block options",
                                tint = VisualBuilderThemeColors.TextInsideBlock.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(VisualBuilderThemeColors.Surface)
                                .border(BorderStroke(1.dp, VisualBuilderThemeColors.Border))
                                .testTag("menu_block_options")
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = VisualBuilderThemeColors.OnBackground, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Edit Parameters", fontFamily = FontFamily.Monospace, color = VisualBuilderThemeColors.OnBackground, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onClick()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = VisualBuilderThemeColors.OnBackground, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Duplicate", fontFamily = FontFamily.Monospace, color = VisualBuilderThemeColors.OnBackground, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onDuplicate()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5555), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Delete", fontFamily = FontFamily.Monospace, color = Color(0xFFFF5555), fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }

                // Scratch-style puzzle bottom cutout notch
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 4.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(VisualBuilderThemeColors.Background)
                )
            }
        }
    }
}

/**
 * End cap closing block for container blocks (If / Repeat / Loop).
 */
@Composable
fun ContainerEndCapBlock(
    category: BlockCategory,
    indentLevel: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = (indentLevel * 18).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .height(20.dp)
                .fillMaxWidth(0.55f),
            shape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp),
            color = category.color,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 3.dp)
                        .background(category.darkStripColor, RoundedCornerShape(1.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "end",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = VisualBuilderThemeColors.TextInsideBlock.copy(alpha = 0.75f)
                )
            }
        }
    }
}
