package com.example.ui.visualbuilder

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Main Visual Builder screen with dot-grid canvas, vertical Scratch block stacking,
 * interactive parameter capsules, recipe starters, live interactive Discord simulator,
 * multi-language code export (Python & JavaScript), and undo/redo capabilities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualBuilderScreen(
    viewModel: VisualBuilderViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val blocks by viewModel.blocks.collectAsState()
    val isDragging by viewModel.isDragging.collectAsState()
    val draggedBlock by viewModel.draggedBlock.collectAsState()
    val dragPosition by viewModel.dragPosition.collectAsState()
    val dropTargetIndex by viewModel.dropTargetIndex.collectAsState()
    val activeExecutingBlockId by viewModel.activeExecutingBlockId.collectAsState()

    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isPaletteExpanded by viewModel.isPaletteExpanded.collectAsState()
    val selectedBlockForEdit by viewModel.selectedBlockForEdit.collectAsState()
    val isExportDialogOpen by viewModel.isExportDialogOpen.collectAsState()
    val selectedExportLang by viewModel.selectedExportLanguage.collectAsState()
    val isRecipeDialogOpen by viewModel.isRecipeDialogOpen.collectAsState()

    val showSimulationSheet by viewModel.showSimulationSheet.collectAsState()
    val simulationLogs by viewModel.simulationLogs.collectAsState()
    val simulatedMessages by viewModel.simulatedMessages.collectAsState()
    val isSimulationRunning by viewModel.isSimulationRunning.collectAsState()
    val canvasZoom by viewModel.canvasZoom.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    // Listen to snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("visual_builder_scaffold"),
        containerColor = VisualBuilderThemeColors.Background,
        topBar = {
            VisualBuilderTopBar(
                blockCount = blocks.size,
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onClearCanvas = { showClearDialog = true },
                onOpenRecipes = { viewModel.openRecipeDialog() },
                onBackClick = onNavigateBack,
                onTestClick = { viewModel.openSimulation() },
                onSaveClick = { viewModel.saveScript() },
                onExportClick = { viewModel.openExportDialog() }
            )
        },
        bottomBar = {
            BlockPalette(
                isExpanded = isPaletteExpanded,
                onToggleExpand = { viewModel.togglePalette() },
                selectedCategory = selectedCategory,
                onSelectCategory = { viewModel.selectCategory(it) },
                onBlockDragStart = { template, offset ->
                    viewModel.startDragFromPalette(template, offset)
                },
                onBlockDrag = { delta ->
                    viewModel.updateDrag(delta)
                },
                onBlockDragEnd = {
                    viewModel.endDrag(success = true)
                },
                onBlockClick = { template ->
                    viewModel.addBlockFromPalette(template)
                },
                onOpenRecipes = { viewModel.openRecipeDialog() },
                isDragging = isDragging
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            ) { data ->
                Surface(
                    color = VisualBuilderThemeColors.SurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, VisualBuilderThemeColors.Border),
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = data.visuals.message,
                        color = VisualBuilderThemeColors.OnBackground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(VisualBuilderThemeColors.Background)
        ) {
            // Background Canvas: 24.dp Dot Grid Pattern (#252A35)
            DotGridCanvas(
                modifier = Modifier.fillMaxSize()
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Validation Warning Banner: checks if script starts with an Event block
                val hasEvent = blocks.any { it.category == BlockCategory.EVENT }
                if (blocks.isNotEmpty() && !hasEvent) {
                    Surface(
                        color = Color(0xFFF0B429).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFF0B429).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color(0xFFF0B429),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notice: Script needs an Event trigger block at top (e.g. \"When message received\").",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFFF0B429)
                            )
                        }
                    }
                }

                // Canvas Content: Empty message or Block Stack
                if (blocks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "🟦 Canvas is empty",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = VisualBuilderThemeColors.OnBackground
                            )
                            Text(
                                text = "Tap any block in the palette below to snap it in,\nor load a pre-built starter recipe.",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = VisualBuilderThemeColors.OnSurfaceVariant,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { viewModel.openRecipeDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = VisualBuilderThemeColors.Accent),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Browse Recipes", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = canvasZoom
                                scaleY = canvasZoom
                                transformOrigin = TransformOrigin(0.5f, 0f)
                            }
                            .testTag("canvas_block_stack"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = blocks,
                            key = { _, block -> block.id }
                        ) { index, block ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(
                                        fadeInSpec = tween(180),
                                        fadeOutSpec = tween(150),
                                        placementSpec = spring(dampingRatio = 0.8f)
                                    )
                            ) {
                                // Pulsing drop zone outline when dragging near this index
                                if (isDragging && dropTargetIndex == index) {
                                    DropTargetIndicator(category = draggedBlock?.category ?: BlockCategory.EVENT)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                BlockCard(
                                    category = block.category,
                                    title = block.title,
                                    subtitle = block.displaySubtitle,
                                    icon = block.icon,
                                    indentLevel = block.indentLevel,
                                    inlineTokens = block.inlineTokens,
                                    isCurrentlyExecuting = (activeExecutingBlockId == block.id),
                                    canMoveUp = (index > 0),
                                    canMoveDown = (index < blocks.size - 1),
                                    onMoveUp = { viewModel.moveBlockUp(block.id) },
                                    onMoveDown = { viewModel.moveBlockDown(block.id) },
                                    onLongPress = {
                                        viewModel.startDragCanvasBlock(block, Offset.Zero)
                                    },
                                    onClick = {
                                        viewModel.openEditSheet(block)
                                    },
                                    onPillClick = {
                                        viewModel.openEditSheet(block)
                                    },
                                    onDelete = {
                                        viewModel.removeBlock(block.id)
                                    },
                                    onDuplicate = {
                                        viewModel.duplicateBlock(block.id)
                                    }
                                )

                                // Render closing cap block for containers
                                if (block.isContainer) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    ContainerEndCapBlock(
                                        category = block.category,
                                        indentLevel = block.indentLevel
                                    )
                                }
                            }
                        }

                        // Bottom drop target indicator when dragging at end of list
                        if (isDragging && (dropTargetIndex == null || dropTargetIndex == blocks.size)) {
                            item {
                                DropTargetIndicator(category = draggedBlock?.category ?: BlockCategory.EVENT)
                            }
                        }
                    }
                }
            }

            // Actively dragged floating block overlay
            if (isDragging && draggedBlock != null) {
                val scale by animateFloatAsState(
                    targetValue = 1.04f,
                    animationSpec = tween(120),
                    label = "dragScale"
                )
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (dragPosition.x - 60f).roundToInt(),
                                y = (dragPosition.y - 36f).roundToInt()
                            )
                        }
                        .scale(scale)
                        .shadow(elevation = 10.dp, shape = RoundedCornerShape(10.dp), ambientColor = Color.Black.copy(alpha = 0.4f))
                ) {
                    BlockCard(
                        category = draggedBlock!!.category,
                        title = draggedBlock!!.title,
                        subtitle = draggedBlock!!.subtitle,
                        icon = draggedBlock!!.icon,
                        inlineTokens = draggedBlock!!.inlineTokens,
                        modifier = Modifier.width(280.dp)
                    )
                }
            }

            // Canvas Minimap (Top-Right)
            if (blocks.isNotEmpty()) {
                CanvasMinimap(
                    blocks = blocks,
                    activeBlockId = activeExecutingBlockId,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 12.dp)
                )
            }

            // Canvas Zoom Controls (Bottom-Right)
            CanvasZoomControls(
                zoom = canvasZoom,
                onZoomIn = { viewModel.zoomIn() },
                onZoomOut = { viewModel.zoomOut() },
                onResetZoom = { viewModel.resetZoom() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 12.dp, end = 12.dp)
            )

            // Trash Drop-Zone (Pulsing Red Bar when dragging)
            if (isDragging && draggedBlock != null) {
                TrashDropZone(
                    onDiscard = {
                        viewModel.dropOnTrash(draggedBlock!!.id)
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }
        }
    }

    // Modal Bottom Sheet: Block Configuration Editor
    if (selectedBlockForEdit != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeEditSheet() },
            sheetState = sheetState,
            containerColor = VisualBuilderThemeColors.Surface,
            scrimColor = Color.Black.copy(alpha = 0.65f)
        ) {
            BlockEditSheetContent(
                block = selectedBlockForEdit!!,
                onSave = { updated -> viewModel.updateBlock(updated) },
                onCancel = { viewModel.closeEditSheet() }
            )
        }
    }

    // Starter Recipes Dialog
    if (isRecipeDialogOpen) {
        Dialog(onDismissRequest = { viewModel.closeRecipeDialog() }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                shape = RoundedCornerShape(12.dp),
                color = VisualBuilderThemeColors.Surface,
                border = BorderStroke(1.dp, VisualBuilderThemeColors.Border),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VisualBuilderThemeColors.Primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Starter Bot Recipes",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = VisualBuilderThemeColors.OnBackground
                            )
                        }
                        IconButton(onClick = { viewModel.closeRecipeDialog() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = VisualBuilderThemeColors.OnSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Load a pre-configured stack of blocks to get started instantly:",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = VisualBuilderThemeColors.OnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PaletteCatalog.starterRecipes.forEach { recipe ->
                            Surface(
                                color = VisualBuilderThemeColors.SurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, VisualBuilderThemeColors.Border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.loadRecipe(recipe) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = recipe.emoji, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = recipe.name,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = VisualBuilderThemeColors.OnBackground
                                        )
                                        Text(
                                            text = recipe.description,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = VisualBuilderThemeColors.OnSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${recipe.blocks.size} blocks",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = VisualBuilderThemeColors.Primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Multi-Language Code Export Monospace Viewer (Python & JavaScript)
    if (isExportDialogOpen) {
        val pythonCode = remember(blocks) { viewModel.exportToDiscordPy() }
        val jsCode = remember(blocks) { viewModel.exportToDiscordJs() }
        val activeCode = if (selectedExportLang == "Python") pythonCode else jsCode

        Dialog(onDismissRequest = { viewModel.closeExportDialog() }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp),
                shape = RoundedCornerShape(12.dp),
                color = VisualBuilderThemeColors.Surface,
                border = BorderStroke(1.dp, VisualBuilderThemeColors.Border),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📦 Export Code",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = VisualBuilderThemeColors.OnBackground
                        )
                        IconButton(onClick = { viewModel.closeExportDialog() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = VisualBuilderThemeColors.OnSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Language Selector Tabs: Python vs JavaScript
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Python", "JavaScript").forEach { lang ->
                            val isSelected = (selectedExportLang == lang)
                            Surface(
                                color = if (isSelected) VisualBuilderThemeColors.Accent else VisualBuilderThemeColors.SurfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, VisualBuilderThemeColors.Border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clickable { viewModel.selectExportLanguage(lang) }
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (lang == "Python") "🐍 Python (discord.py)" else "⚡ JavaScript (v14)",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else VisualBuilderThemeColors.OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = VisualBuilderThemeColors.Background,
                        border = BorderStroke(1.dp, VisualBuilderThemeColors.Border)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = activeCode,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (selectedExportLang == "Python") VisualBuilderThemeColors.Primary else Color(0xFFF0B429),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.closeExportDialog() }) {
                            Text("Done", fontFamily = FontFamily.Monospace, color = VisualBuilderThemeColors.OnSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Discord Bot Code", activeCode)
                                clipboard.setPrimaryClip(clip)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Copied $selectedExportLang code to clipboard!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VisualBuilderThemeColors.Accent),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Code", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Live Interactive Discord Simulator
    if (showSimulationSheet) {
        val simSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var simTab by remember { mutableIntStateOf(0) } // 0 = Discord Chat, 1 = Execution Logs
        var testInputText by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = { viewModel.closeSimulationSheet() },
            sheetState = simSheetState,
            containerColor = VisualBuilderThemeColors.Surface,
            scrimColor = Color.Black.copy(alpha = 0.65f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .heightIn(min = 380.dp, max = 560.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🤖 Discord Bot Simulator",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = VisualBuilderThemeColors.OnBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isSimulationRunning) {
                            Text(
                                text = "RUNNING",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = VisualBuilderThemeColors.Primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(VisualBuilderThemeColors.Primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    TextButton(onClick = { viewModel.closeSimulationSheet() }) {
                        Text("Close", fontFamily = FontFamily.Monospace, color = VisualBuilderThemeColors.OnSurfaceVariant)
                    }
                }

                // Tabs: Discord Chat vs Logs
                TabRow(
                    selectedTabIndex = simTab,
                    containerColor = VisualBuilderThemeColors.Surface,
                    contentColor = VisualBuilderThemeColors.Primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[simTab]),
                            color = VisualBuilderThemeColors.Primary
                        )
                    }
                ) {
                    Tab(
                        selected = (simTab == 0),
                        onClick = { simTab = 0 },
                        text = {
                            Text(
                                "💬 Discord Chat (#general)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = if (simTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = (simTab == 1),
                        onClick = { simTab = 1 },
                        text = {
                            Text(
                                "📜 Step Logs (${simulationLogs.size})",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = if (simTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (simTab == 0) {
                    // Discord Chat View
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF313338), // Authentic Discord Chat background
                        border = BorderStroke(1.dp, VisualBuilderThemeColors.Border)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Channel header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2B2D31))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF80848E))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("general", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2F3F5), fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("|  Visual Builder live test channel", fontSize = 10.sp, color = Color(0xFF949BA4), fontFamily = FontFamily.Monospace)
                            }

                            // Messages list
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                simulatedMessages.forEach { msg ->
                                    SimulatedDiscordMessageRow(msg)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick trigger suggestion pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("!ping", "Hello!", "!help", "discord.gg/invite").forEach { sample ->
                            Surface(
                                color = VisualBuilderThemeColors.SurfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, VisualBuilderThemeColors.Border),
                                modifier = Modifier.clickable {
                                    testInputText = sample
                                    viewModel.sendSimulatedMessage(sample)
                                    testInputText = ""
                                }
                            ) {
                                Text(
                                    text = "Send \"$sample\"",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = VisualBuilderThemeColors.OnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Message input field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = testInputText,
                            onValueChange = { testInputText = it },
                            placeholder = { Text("Message #general (test bot)...", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = VisualBuilderThemeColors.Primary,
                                unfocusedBorderColor = VisualBuilderThemeColors.Border,
                                focusedContainerColor = Color(0xFF383A40),
                                unfocusedContainerColor = Color(0xFF383A40)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (testInputText.isNotBlank()) {
                                    viewModel.sendSimulatedMessage(testInputText)
                                    testInputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(VisualBuilderThemeColors.Primary, CircleShape)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = VisualBuilderThemeColors.Background,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    // Logs View
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = VisualBuilderThemeColors.Background,
                        border = BorderStroke(1.dp, VisualBuilderThemeColors.Border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            simulationLogs.forEach { log ->
                                Text(
                                    text = log,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = when {
                                        log.contains("[EVENT]") -> Color(0xFF3D7EFF)
                                        log.contains("[MESSAGE]") -> Color(0xFF00E676)
                                        log.contains("[LOGIC]") -> Color(0xFFF0B429)
                                        log.contains("[COMPLETE]") -> Color(0xFF00E676)
                                        else -> VisualBuilderThemeColors.OnBackground
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog: Clear Canvas
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Canvas?", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = VisualBuilderThemeColors.OnBackground) },
            text = { Text("This will remove all blocks from the canvas. You can undo this action later.", fontFamily = FontFamily.Monospace, color = VisualBuilderThemeColors.OnSurfaceVariant, fontSize = 12.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCanvas()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Clear All", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", fontFamily = FontFamily.Monospace, color = VisualBuilderThemeColors.OnSurfaceVariant)
                }
            },
            containerColor = VisualBuilderThemeColors.Surface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

/**
 * Renders an individual simulated message in authentic Discord dark style.
 */
@Composable
fun SimulatedDiscordMessageRow(msg: SimulatedDiscordMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(Color(android.graphics.Color.parseColor(msg.avatarColorHex)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (msg.isBot) "🤖" else msg.authorName.take(1),
                fontSize = if (msg.isBot) 16.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            // Author row + Bot badge + Timestamp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = msg.authorName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (msg.isBot) Color(0xFF5865F2) else Color(0xFFF2F3F5),
                    fontFamily = FontFamily.Monospace
                )
                if (msg.isBot) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF5865F2), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "BOT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = msg.timestamp,
                    fontSize = 9.sp,
                    color = Color(0xFF949BA4),
                    fontFamily = FontFamily.Monospace
                )
            }

            // Message text
            if (msg.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = msg.content,
                    fontSize = 12.sp,
                    color = Color(0xFFDBDEE1),
                    fontFamily = FontFamily.Monospace
                )
            }

            // Rich Embed card
            if (msg.embedTitle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF2B2D31),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, Color(0xFF35373C)),
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Left embed color line
                        val embedColor = try {
                            Color(android.graphics.Color.parseColor(msg.embedColorHex ?: "#3D7EFF"))
                        } catch (e: Exception) {
                            Color(0xFF3D7EFF)
                        }
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .heightIn(min = 40.dp)
                                .background(embedColor)
                        )
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = msg.embedTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            if (msg.embedDescription != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.embedDescription,
                                    fontSize = 11.sp,
                                    color = Color(0xFFDBDEE1),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Reactions row
            if (msg.reactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    msg.reactions.forEach { react ->
                        Surface(
                            color = Color(0xFF2B2D31),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF35373C))
                        ) {
                            Text(
                                text = react,
                                fontSize = 10.sp,
                                color = Color(0xFFDBDEE1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top App Bar for the Visual Builder canvas with action buttons, undo/redo, and stats.
 */
@Composable
fun VisualBuilderTopBar(
    blockCount: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClearCanvas: () -> Unit,
    onOpenRecipes: () -> Unit,
    onBackClick: () -> Unit,
    onTestClick: () -> Unit,
    onSaveClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("visual_builder_top_bar"),
        color = VisualBuilderThemeColors.Surface,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back arrow (min 48dp touch target)
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 44.dp, minHeight = 48.dp)
                        .clickable(onClick = onBackClick)
                        .testTag("btn_visual_back"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VisualBuilderThemeColors.OnSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Title + Block count pill
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Visual Builder",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = VisualBuilderThemeColors.OnBackground,
                        maxLines = 1
                    )
                    Text(
                        text = "$blockCount blocks",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = VisualBuilderThemeColors.Primary
                    )
                }

                // Undo Button
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) VisualBuilderThemeColors.OnBackground else VisualBuilderThemeColors.Border,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Redo Button
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) VisualBuilderThemeColors.OnBackground else VisualBuilderThemeColors.Border,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Clear button
                IconButton(
                    onClick = onClearCanvas,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Clear",
                        tint = VisualBuilderThemeColors.OnSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // "Export" button
                TextButton(
                    onClick = onExportClick,
                    modifier = Modifier.testTag("btn_export_code")
                ) {
                    Text(
                        text = "Export",
                        color = Color(0xFFA78BFA),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // "▶ Test" button
                Button(
                    onClick = onTestClick,
                    colors = ButtonDefaults.buttonColors(containerColor = VisualBuilderThemeColors.Primary),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_test_script")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = VisualBuilderThemeColors.Background, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Test",
                        color = VisualBuilderThemeColors.Background,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // 1.dp bottom border #252A35
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(VisualBuilderThemeColors.Border)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * 24.dp Dot Grid Pattern Canvas for authentic IDE editor canvas feeling.
 */
@Composable
fun DotGridCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val spacingPx = 24.dp.toPx()
        val dotRadius = 1.dp.toPx()
        val dotColor = Color(0xFF252A35)

        val cols = (size.width / spacingPx).toInt() + 1
        val rows = (size.height / spacingPx).toInt() + 1

        for (i in 0..cols) {
            for (j in 0..rows) {
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(i * spacingPx, j * spacingPx)
                )
            }
        }
    }
}

/**
 * Visual indicator highlighting where a dropped block will snap.
 */
@Composable
fun DropTargetIndicator(
    category: BlockCategory,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(
                BorderStroke(2.dp, category.color.copy(alpha = alpha)),
                RoundedCornerShape(8.dp)
            )
            .background(category.color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "＋ Snap block here",
            color = category.color,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Content for the block configuration ModalBottomSheet with tailored fields for all Discord blocks.
 */
@Composable
fun BlockEditSheetContent(
    block: VisualBlock,
    onSave: (VisualBlock) -> Unit,
    onCancel: () -> Unit
) {
    var messageText by remember { mutableStateOf(block.messageContent) }
    var conditionField by remember { mutableStateOf(block.conditionField) }
    var conditionOp by remember { mutableStateOf(block.conditionOperator) }
    var conditionVal by remember { mutableStateOf(block.conditionValue) }
    var waitSecs by remember { mutableIntStateOf(block.waitSeconds) }
    var targetChannel by remember { mutableStateOf(block.targetChannel) }
    var targetUser by remember { mutableStateOf(block.targetUser) }
    var roleName by remember { mutableStateOf(block.roleName) }
    var embedTitle by remember { mutableStateOf(block.embedTitle) }
    var embedDesc by remember { mutableStateOf(block.embedDescription) }
    var embedColor by remember { mutableStateOf(block.embedColorHex) }
    var reactionEmoji by remember { mutableStateOf(block.reactionEmoji) }
    var timeoutMins by remember { mutableIntStateOf(block.timeoutMinutes) }
    var purgeCount by remember { mutableIntStateOf(block.purgeCount) }
    var varName by remember { mutableStateOf(block.variableName) }
    var varVal by remember { mutableStateOf(block.variableValue) }
    var slashCmdName by remember { mutableStateOf(block.slashCommandName) }
    var slashCmdDesc by remember { mutableStateOf(block.slashCommandDesc) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Sheet Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = block.icon, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Edit ${block.title}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = VisualBuilderThemeColors.OnBackground
                    )
                    Text(
                        text = "Category: ${block.category.title}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = block.category.color
                    )
                }
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = VisualBuilderThemeColors.OnSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic form inputs
        when {
            // Slash Command
            block.category == BlockCategory.EVENT && block.title.contains("slash", ignoreCase = true) -> {
                Text("Command Name (without /)", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = slashCmdName,
                    onValueChange = { slashCmdName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Command Description", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = slashCmdDesc,
                    onValueChange = { slashCmdDesc = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }

            // Rich Embed
            block.category == BlockCategory.MESSAGE && block.title.contains("embed", ignoreCase = true) -> {
                Text("Embed Title", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = embedTitle,
                    onValueChange = { embedTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Embed Description", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = embedDesc,
                    onValueChange = { embedDesc = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Target Channel", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = targetChannel,
                    onValueChange = { targetChannel = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }

            // Standard Message / Reply / DM
            block.category == BlockCategory.MESSAGE && !block.title.contains("reaction", ignoreCase = true) && !block.title.contains("Delete", ignoreCase = true) -> {
                Text("Message Content", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Target Channel / User", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = targetChannel,
                    onValueChange = { targetChannel = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }

            // Add Reaction
            block.category == BlockCategory.MESSAGE && block.title.contains("reaction", ignoreCase = true) -> {
                Text("Emoji Reaction", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("⭐", "🏓", "✅", "❤️", "🔥", "🎉").forEach { em ->
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { reactionEmoji = em },
                            shape = RoundedCornerShape(6.dp),
                            color = if (reactionEmoji == em) VisualBuilderThemeColors.Primary else VisualBuilderThemeColors.SurfaceVariant,
                            border = BorderStroke(1.dp, VisualBuilderThemeColors.Border)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(em, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            // Roles
            block.category == BlockCategory.ACTION && block.title.contains("role", ignoreCase = true) -> {
                Text("Role Name", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = roleName,
                    onValueChange = { roleName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Target User (Author / Mentioned)", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = targetUser,
                    onValueChange = { targetUser = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }

            // Timeout
            block.category == BlockCategory.ACTION && block.title.contains("Timeout", ignoreCase = true) -> {
                Text("Duration (Minutes)", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { if (timeoutMins > 1) timeoutMins -= 5 }, modifier = Modifier.background(VisualBuilderThemeColors.SurfaceVariant, CircleShape)) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
                    }
                    Text("$timeoutMins minutes", fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VisualBuilderThemeColors.Primary)
                    IconButton(onClick = { timeoutMins += 5 }, modifier = Modifier.background(VisualBuilderThemeColors.SurfaceVariant, CircleShape)) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
                    }
                }
            }

            // Logic: If condition
            block.category == BlockCategory.LOGIC && block.title.contains("If", ignoreCase = true) -> {
                Text("Condition Field", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = conditionField,
                    onValueChange = { conditionField = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Operator", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("==", "!=", "contains", "startswith").forEach { op ->
                        Surface(
                            modifier = Modifier
                                .height(32.dp)
                                .clickable { conditionOp = op },
                            shape = RoundedCornerShape(6.dp),
                            color = if (conditionOp == op) VisualBuilderThemeColors.Primary else VisualBuilderThemeColors.SurfaceVariant,
                            border = BorderStroke(1.dp, VisualBuilderThemeColors.Border)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = op,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = if (conditionOp == op) VisualBuilderThemeColors.Background else VisualBuilderThemeColors.OnBackground
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Expected Value", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = conditionVal,
                    onValueChange = { conditionVal = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }

            // Logic: Wait
            block.category == BlockCategory.LOGIC && block.title.contains("Wait", ignoreCase = true) -> {
                Text("Wait Duration (Seconds)", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { if (waitSecs > 1) waitSecs-- }, modifier = Modifier.background(VisualBuilderThemeColors.SurfaceVariant, CircleShape)) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
                    }
                    Text("$waitSecs sec", fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VisualBuilderThemeColors.Primary)
                    IconButton(onClick = { waitSecs++ }, modifier = Modifier.background(VisualBuilderThemeColors.SurfaceVariant, CircleShape)) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
                    }
                }
            }

            // Variables
            block.category == BlockCategory.VARIABLE -> {
                Text("Variable Name", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = varName,
                    onValueChange = { varName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Value", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = varVal,
                    onValueChange = { varVal = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }

            // Destructive: Purge
            block.category == BlockCategory.DESTRUCTIVE && block.title.contains("Purge", ignoreCase = true) -> {
                Text("Number of Messages to Purge (1-100)", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VisualBuilderThemeColors.OnSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { if (purgeCount > 5) purgeCount -= 5 }, modifier = Modifier.background(VisualBuilderThemeColors.SurfaceVariant, CircleShape)) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
                    }
                    Text("$purgeCount msgs", fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5555))
                    IconButton(onClick = { if (purgeCount < 100) purgeCount += 5 }, modifier = Modifier.background(VisualBuilderThemeColors.SurfaceVariant, CircleShape)) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
                    }
                }
            }

            else -> {
                Text(
                    text = "Configure parameters for this block below:",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = VisualBuilderThemeColors.OnSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Save Button
        Button(
            onClick = {
                val updated = block.copy(
                    messageContent = messageText,
                    targetChannel = targetChannel,
                    targetUser = targetUser,
                    roleName = roleName,
                    embedTitle = embedTitle,
                    embedDescription = embedDesc,
                    embedColorHex = embedColor,
                    reactionEmoji = reactionEmoji,
                    timeoutMinutes = timeoutMins,
                    purgeCount = purgeCount,
                    conditionField = conditionField,
                    conditionOperator = conditionOp,
                    conditionValue = conditionVal,
                    waitSeconds = waitSecs,
                    variableName = varName,
                    variableValue = varVal,
                    slashCommandName = slashCmdName,
                    slashCommandDesc = slashCmdDesc
                )
                onSave(updated)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VisualBuilderThemeColors.Primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Apply Changes",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = VisualBuilderThemeColors.Background,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Minimap in top-right corner showing small color bars for each block in the stack and active execution glow.
 */
@Composable
fun CanvasMinimap(
    blocks: List<VisualBlock>,
    activeBlockId: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(84.dp)
            .heightIn(max = 140.dp)
            .testTag("canvas_minimap"),
        color = VisualBuilderThemeColors.Surface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, VisualBuilderThemeColors.Border),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MINIMAP",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = VisualBuilderThemeColors.OnSurfaceVariant
                )
                Text(
                    text = "${blocks.size}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = VisualBuilderThemeColors.Primary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            blocks.take(15).forEach { block ->
                val isActive = block.id == activeBlockId
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (1f - (block.indentLevel * 0.15f)).coerceIn(0.5f, 1f))
                        .height(if (isActive) 5.dp else 3.dp)
                        .background(
                            if (isActive) VisualBuilderThemeColors.Primary else block.category.color,
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

/**
 * Floating Zoom Controls (+ / - / reset) for tap users.
 */
@Composable
fun CanvasZoomControls(
    zoom: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("canvas_zoom_controls"),
        color = VisualBuilderThemeColors.Surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, VisualBuilderThemeColors.Border),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    tint = VisualBuilderThemeColors.OnBackground,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "${(zoom * 100).roundToInt()}%",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (zoom != 1.0f) VisualBuilderThemeColors.Primary else VisualBuilderThemeColors.OnSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onResetZoom() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = VisualBuilderThemeColors.OnBackground,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Pulsing trash drop zone overlay that appears while dragging blocks.
 */
@Composable
fun TrashDropZone(
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "trashPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trashAlpha"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onDiscard() }
            .testTag("trash_drop_zone"),
        color = Color(0xFFD32F2F).copy(alpha = 0.25f * alpha),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, Color(0xFFFF5555).copy(alpha = alpha)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Trash",
                tint = Color(0xFFFF5555),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "TRASH ZONE • DROP TO DELETE",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF5555)
            )
        }
    }
}
