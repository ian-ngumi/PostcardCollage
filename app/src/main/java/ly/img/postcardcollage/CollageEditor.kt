package ly.img.postcardcollage

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BorderStyle
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.SwapVerticalCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.toRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ly.img.editor.DesignEditor
import ly.img.editor.EditorConfiguration
import ly.img.editor.EditorDefaults
import ly.img.editor.EngineConfiguration
import ly.img.editor.core.EditorScope
import ly.img.editor.core.component.Dock
import ly.img.editor.core.component.EditorComponentId
import ly.img.editor.core.component.rememberForDesign
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.library.LibraryCategory
import ly.img.editor.core.sheet.SheetStyle
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.rememberForDesign
import ly.img.engine.ContentFillMode
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.FillType
import kotlin.math.roundToInt


@Composable
fun CollageEditor(
    onClose: () -> Unit
) {

    val scope = rememberCoroutineScope()
    val margin = remember { mutableFloatStateOf(1f) }
    val cornerRadius = remember { mutableFloatStateOf(1f) }
    val backgroundColor = remember { mutableStateOf(Color.White) }

    val viewModel = viewModel<EditorViewModel>()

    val config = EngineConfiguration.remember(
        license = Secrets.LICENSE,
        onCreate = {
            EditorDefaults.onCreate(
                engine = editorContext.engine,
                eventHandler = editorContext.eventHandler,
                sceneUri = EngineConfiguration.defaultDesignSceneUri,
            )
            setLayout(
                scope,
                margin,
                cornerRadius,
                backgroundColor,
                Uri.parse(viewModel.editorPayload.collageLayout),
                viewModel.editorPayload.urls
            )
        }
    )

    var reorderMode by remember { mutableStateOf(false) }

    val editorConfiguration = EditorConfiguration.rememberForDesign(
        overlay = {
            var currentDragArea by remember { mutableStateOf(Rect.Zero) }
            val density = LocalDensity.current

            if (reorderMode) {
                Box(
                    modifier = Modifier
                        .zIndex(0f)
                        .statusBarsPadding()
                        .fillMaxSize()
                ) {

                    var invalidatePositions by remember { mutableStateOf(System.currentTimeMillis()) }
                    val placeholderPositions by remember(invalidatePositions) {
                        derivedStateOf {
                            val engine = editorContext.engine
                            val placeholders = engine.getPlaceholders()
                            placeholders.map { block ->
                                block to engine.getGlobalRect(density, block)
                            }
                        }
                    }

                    placeholderPositions.forEach { (currentBlock, position) ->
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        position.left.roundToInt(),
                                        position.top.roundToInt()
                                    )
                                }
                                .width(with(LocalDensity.current) { position.width.toDp() })
                                .height(with(LocalDensity.current) { position.height.toDp() })
                                .border(
                                    width = 2.dp,
                                    color = Color.Red.copy(alpha = .7f),
                                    shape = RoundedCornerShape(size = cornerRadius.floatValue.dp * 2)
                                )
                                .pointerInput(position) {
                                    val engine = editorContext.engine

                                    var currentOffset = Offset.Zero
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { touchOffset ->
                                            currentOffset =
                                                touchOffset + Offset(position.left, position.top)
                                            currentDragArea = position
                                        },
                                        onDragEnd = {
                                            val placeholders = engine.getPlaceholders()
                                            placeholders.map { block ->
                                                block to engine.getGlobalRect(density, block)
                                            }.find { (_, rect) ->
                                                rect.contains(currentOffset)
                                            }?.let { (block, _) ->
                                                engine.swapBlockPositions(currentBlock, block)
                                            }

                                            currentDragArea = Rect.Zero
                                            currentOffset = Offset.Zero

                                            invalidatePositions = System.currentTimeMillis()

                                        },
                                        onDrag = { change, dragAmount ->
                                            currentOffset += dragAmount
                                            currentDragArea = currentDragArea.copy(
                                                left = currentDragArea.left + dragAmount.x,
                                                top = currentDragArea.top + dragAmount.y,
                                                right = currentDragArea.right + dragAmount.x,
                                                bottom = currentDragArea.bottom + dragAmount.y,
                                            )
                                        }
                                    )
                                }
                        )
                    }


                    if (!currentDragArea.isEmpty) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset {
                                    IntOffset(
                                        currentDragArea.left.roundToInt(),
                                        currentDragArea.top.roundToInt()
                                    )
                                }
                                .width(with(LocalDensity.current) { currentDragArea.width.toDp() })
                                .height(with(LocalDensity.current) { currentDragArea.height.toDp() })
                                .background(
                                    color = Color.Red.copy(alpha = .2f),
                                    shape = RoundedCornerShape(size = cornerRadius.floatValue.dp * 2)
                                )
                                .border(
                                    width = 2.dp,
                                    color = Color.Red.copy(alpha = .7f),
                                    shape = RoundedCornerShape(size = cornerRadius.floatValue.dp * 2)
                                )
                        )
                    }

                }
            }
        },
        dock = {
            Dock.rememberForDesign(
                horizontalArrangement = {
                    if (reorderMode)
                        Arrangement.Center
                    else
                        Arrangement.SpaceAround
                },
                listBuilder = if (reorderMode) {
                    Dock.ListBuilder.remember {
                        add {
                            Dock.Custom.remember(
                                id = EditorComponentId("reorder_confirmation"),
                                scope = Dock.ItemScope(this),
                            ) {
                                Row (
                                    Modifier
                                        .padding(horizontal = 12.dp)
                                        .fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                )  {
                                    Text("Long press & drag to reorder")
                                    Spacer(Modifier.width(64.dp))

                                    IconButton(
                                        onClick = {
                                            reorderMode = false
                                            editorContext.engine.editor.addUndoStep()
                                            editorContext.engine.editor.undo()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Cancel,
                                            contentDescription = "Cancel Reorder"
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            editorContext.engine.editor.addUndoStep()
                                            reorderMode = false
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Apply Reorder"
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Dock.ListBuilder.remember {
                        add {
                            Dock.Button.remember(
                                id = EditorComponentId("margin"),
                                text = { Text("Margin") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.BorderStyle,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    editorContext.eventHandler.send(
                                        EditorEvent.Sheet.Open(
                                            SheetType.Custom(
                                                style = SheetStyle(),
                                                content = {
                                                    Column(
                                                        modifier = Modifier.padding(32.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                "Edit margins",
                                                                style = MaterialTheme.typography.labelLarge
                                                            )

                                                            Spacer(Modifier.weight(1f))

                                                            TextButton(
                                                                onClick = {
                                                                    editorContext.eventHandler.send(
                                                                        EditorEvent.Sheet.Close(true)
                                                                    )
                                                                }
                                                            ) {
                                                                Text("Done")
                                                            }
                                                        }

                                                        Spacer(Modifier.height(8.dp))

                                                        Slider(
                                                            value = margin.floatValue,
                                                            onValueChange = {
                                                                margin.floatValue = it
                                                            },
                                                            onValueChangeFinished = {
                                                                editorContext.engine.editor.addUndoStep()
                                                            },
                                                            valueRange = 0f..10f
                                                        )

                                                        Spacer(Modifier.height(8.dp))

                                                        Slider(
                                                            value = cornerRadius.floatValue,
                                                            onValueChange = {
                                                                cornerRadius.floatValue = it
                                                            },
                                                            onValueChangeFinished = {
                                                                editorContext.engine.editor.addUndoStep()
                                                            },
                                                            valueRange = 0f..20f
                                                        )

                                                        Spacer(Modifier.height(16.dp))

                                                    }

                                                }
                                            )
                                        )
                                    )
                                }
                            )
                        }



                        add {
                            Dock.Button.remember(
                                id = EditorComponentId("layout"),
                                text = { Text("Layout") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.GridView,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    editorContext.eventHandler.send(
                                        EditorEvent.Sheet.Open(
                                            SheetType.Custom(
                                                style = SheetStyle(),
                                                content = {
                                                    Column(
                                                        modifier = Modifier.padding(vertical = 32.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 32.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                "Edit layout",
                                                                style = MaterialTheme.typography.labelLarge
                                                            )

                                                            Spacer(Modifier.weight(1f))

                                                            TextButton(
                                                                onClick = {
                                                                    editorContext.eventHandler.send(
                                                                        EditorEvent.Sheet.Close(true)
                                                                    )
                                                                }
                                                            ) {
                                                                Text("Done")
                                                            }
                                                        }

                                                        Spacer(Modifier.height(8.dp))

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(100.dp)
                                                                .horizontalScroll(
                                                                    state = rememberScrollState()
                                                                )
                                                                .padding(horizontal = 32.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                10.dp, Alignment.CenterHorizontally,
                                                            )
                                                        ) {
                                                            collageLayouts.forEach {
                                                                Column(
                                                                    modifier = Modifier
                                                                        .clickable {
                                                                            editorContext.apply {
                                                                                scope.launch {
                                                                                    setLayout(
                                                                                        scope,
                                                                                        margin,
                                                                                        cornerRadius,
                                                                                        backgroundColor,
                                                                                        Uri.parse(it.scene),
                                                                                        imageUrls = viewModel.editorPayload.urls
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                        .fillMaxHeight()
                                                                        .padding(
                                                                            horizontal = 12.dp,
                                                                            vertical = 8.dp
                                                                        ),
                                                                    verticalArrangement = Arrangement.Center,
                                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                                ) {
                                                                    it.GetIcon(Modifier.weight(1f))
                                                                    Spacer(Modifier.height(4.dp))
                                                                    Text(
                                                                        it.name,
                                                                        style = MaterialTheme.typography.labelSmall
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        Spacer(Modifier.height(16.dp))

                                                    }

                                                }
                                            )
                                        )
                                    )
                                }
                            )
                        }


                        add {
                            Dock.Button.remember(
                                id = EditorComponentId("background-color"),
                                text = { Text("Color") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.FormatColorFill,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    editorContext.eventHandler.send(
                                        EditorEvent.Sheet.Open(
                                            SheetType.Custom(
                                                style = SheetStyle(),
                                                content = {
                                                    Column(
                                                        modifier = Modifier.padding(32.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                "Change Background Color",
                                                                style = MaterialTheme.typography.labelLarge
                                                            )

                                                            Spacer(Modifier.weight(1f))

                                                            TextButton(
                                                                onClick = {
                                                                    editorContext.eventHandler.send(
                                                                        EditorEvent.Sheet.Close(true)
                                                                    )
                                                                }
                                                            ) {
                                                                Text("Done")
                                                            }
                                                        }

                                                        Spacer(Modifier.height(8.dp))

                                                        Row(
                                                            modifier = Modifier
                                                                .horizontalScroll(state = rememberScrollState())
                                                                .fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                10.dp, Alignment.CenterHorizontally,
                                                            )
                                                        ) {
                                                            listOf(
                                                                Color.White,
                                                                Color.Red,
                                                                Color.Yellow,
                                                                Color.Green,
                                                                Color.Blue,
                                                                Color.Magenta,
                                                                Color.DarkGray,
                                                            ).forEach {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clickable {
                                                                            backgroundColor.value =
                                                                                it
                                                                        }
                                                                        .padding(4.dp)
                                                                        .size(48.dp)
                                                                        .background(it, CircleShape)
                                                                )
                                                            }
                                                        }

                                                        Spacer(Modifier.height(16.dp))

                                                    }

                                                }
                                            )
                                        )
                                    )
                                }
                            )
                        }


                        add {
                            Dock.Button.remember(
                                id = EditorComponentId("stickers"),
                                text = { Text("Stickers") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.EmojiEmotions,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    editorContext.eventHandler.send(
                                        EditorEvent.Sheet.Open(
                                            SheetType.LibraryAdd(
                                                style = SheetStyle(),
                                                libraryCategory = LibraryCategory.Stickers,
                                            )
                                        )
                                    )
                                }
                            )
                        }

                        add {
                            Dock.Button.remember(
                                id = EditorComponentId("reorder"),
                                text = { Text("Reorder") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.SwapVerticalCircle,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    editorContext.engine.editor.addUndoStep()
                                    reorderMode = !reorderMode
                                }
                            )
                        }


                    }

                }
            )
        }
    )


    DesignEditor(
        engineConfiguration = config,
        editorConfiguration = editorConfiguration,
    ) {
        onClose()
    }

}


private var resizerJob: Job? = null
private suspend fun EditorScope.setLayout(
    scope: CoroutineScope,
    margin: State<Float>,
    cornerRadius: State<Float>,
    backgroundColor: State<Color>,
    baseUri: Uri,
    imageUrls: List<String>
) {
    val engine = editorContext.engine

    engine.scene.load(baseUri)


    val imagePlaceholders = engine.block.findAllPlaceholders()
        .filter {
            engine.block.getType(it) == DesignBlockType.Graphic.key
        }
    imagePlaceholders
        .sortedBy { (engine.block.getPositionX(it) * 2) + engine.block.getPositionY(it) }
        .forEachIndexed { index, placeholder ->
            val fill = engine.block.createFill(FillType.Image)
            engine.block.setString(
                fill,
                "fill/image/imageFileURI",
                imageUrls[index]
            )
            engine.block.setFill(placeholder, fill)
            engine.block.setContentFillMode(placeholder, mode = ContentFillMode.COVER)
        }


    val group = engine.block.group(imagePlaceholders)

    engine.block.setScopeEnabled(group, "editor/select", false)

    val groupSize = Size(
        engine.block.getWidth(group),
        engine.block.getHeight(group),
    )

    resizerJob?.cancel()
    resizerJob = scope.launch {
        val initialSizes = buildMap {
            imagePlaceholders.forEach {
                set(it, Size(engine.block.getWidth(it), engine.block.getHeight(it)))
            }
        }

        val initialPositions = buildMap {
            imagePlaceholders.forEach {
                set(
                    it,
                    Offset(engine.block.getPositionX(it), engine.block.getPositionY(it))
                )
            }
        }
        launch {
            snapshotFlow { margin.value }
                .collect { newMargin ->
                    imagePlaceholders
                        .forEach { placeholder ->
                            initialSizes[placeholder]?.let { size ->
                                engine.block.setWidth(
                                    placeholder,
                                    size.width - newMargin
                                )

                                engine.block.setHeight(
                                    placeholder,
                                    size.height - newMargin
                                )
                            }

                            initialPositions[placeholder]?.let { position ->
                                engine.block.setPositionX(
                                    placeholder,
                                    position.x + (newMargin / 2)
                                )

                                engine.block.setPositionY(
                                    placeholder,
                                    position.y + (newMargin / 2)
                                )
                            }

                            listOf("TL", "TR", "BL", "BR").forEach { corner ->
                                engine.block.setFloat(
                                    engine.block.getShape(placeholder),
                                    "shape/rect/cornerRadius$corner",
                                    cornerRadius.value,
                                )
                            }
                        }

                    engine.block.scale(
                        group,
                        scale = (groupSize.width - newMargin) / groupSize.width,
                        anchorX = .5f,
                        anchorY = .5f,
                    )


                }
        }

        launch {
            snapshotFlow { cornerRadius.value }
                .collect { newRadius ->
                    imagePlaceholders
                        .forEach { placeholder ->
                            listOf("TL", "TR", "BL", "BR").forEach { corner ->
                                engine.block.setFloat(
                                    engine.block.getShape(placeholder),
                                    "shape/rect/cornerRadius$corner",
                                    newRadius,
                                )
                            }
                        }

                }
        }

        launch {
            snapshotFlow { backgroundColor.value }
                .collect { color ->

                    val colorFill = engine.block.createFill(FillType.Color)
                    engine.block.setColor(
                        block = colorFill,
                        property = "fill/color/value",
                        value = ly.img.engine.Color.fromColor(color.toArgb()),
                    )

                    engine.block.setFill(
                        engine.scene.getCurrentPage()!!,
                        colorFill
                    )


                }
        }
    }

    engine.scene.zoomToBlock(engine.scene.getCurrentPage()!!)
    engine.editor.addUndoStep()
}

fun Engine.getPlaceholders() = this
    .block
    .findAllPlaceholders()
    .filter {
        this.block.getType(it) == DesignBlockType.Graphic.key
    }

fun Engine.swapBlockPositions(currentBlock: DesignBlock, block: DesignBlock) {
    if (currentBlock == block) return
    val originX = this.block.getPositionX(currentBlock)
    val originY = this.block.getPositionY(currentBlock)
    val originWidth = this.block.getWidth(currentBlock)
    val originHeight = this.block.getHeight(currentBlock)

    val targetX = this.block.getPositionX(block)
    val targetY = this.block.getPositionY(block)
    val targetWidth = this.block.getWidth(block)
    val targetHeight = this.block.getHeight(block)

    this.block.setPositionX(currentBlock, targetX)
    this.block.setPositionY(currentBlock, targetY)
    this.block.setWidth(currentBlock, targetWidth)
    this.block.setHeight(currentBlock, targetHeight)

    this.block.setPositionX(block, originX)
    this.block.setPositionY(block, originY)
    this.block.setWidth(block, originWidth)
    this.block.setHeight(block, originHeight)
}

fun Engine.getGlobalRect(density: Density, block: DesignBlock): Rect {
    val rect = this.block
        .getScreenSpaceBoundingBoxRect(listOf(block))
    val left = with(density) { rect.left.dp.toPx() }
    val top = with(density) { rect.top.dp.toPx() }
    val right = with(density) { rect.right.dp.toPx() }
    val bottom = with(density) { rect.bottom.dp.toPx() }
    return Rect(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
    )
}


@Serializable
data class EditorPayload(
    val urls: List<String>,
    val collageLayout: String,
)


class EditorViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val editorPayload = savedStateHandle.toRoute<EditorPayload>()
}
