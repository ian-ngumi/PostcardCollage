package ly.img.postcardcollage

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.toRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
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

    var engine: Engine? by remember { mutableStateOf(null) }

    val config = EngineConfiguration.remember(
        license = Secrets.LICENSE,
        onCreate = {
            engine = editorContext.engine
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


    val density = LocalDensity.current
    val editorConfiguration = EditorConfiguration.rememberForDesign(
        overlay = {
            var selectedBlock: DesignBlock? by remember {
                mutableStateOf(null)
            }
            var selectedRegion by remember {
                mutableStateOf(Rect.Zero)
            }

            LaunchedEffect(engine) {
                val engine = engine ?: return@LaunchedEffect
                engine.block.onSelectionChanged()
                    .map { engine.block.findAllSelected() }
                    .collect {
                        println("Selected $it")
                        it.firstOrNull()?.let {
                            selectedBlock = it
                            selectedRegion = engine.getGlobalRect(density = density, it)
                        } ?: kotlin.run {
                            selectedBlock = null
                            selectedRegion = Rect.Zero
                        }
                    }
            }
            Box(
                Modifier
                    .statusBarsPadding()
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .applyRectDimensions(selectedRegion)
                        .pointerInput(Unit) {
                            detectTransformGestures(
                                panZoomLock = false
                            ) { centroid, pan, zoom, rotation ->
                                val engine = engine ?: return@detectTransformGestures
                                selectedBlock?.let { block ->
                                    if (engine.block.supportsCrop(block)) {
                                        /**
                                         * Collect gesture data and apply it if we have a block
                                         * currently selected
                                         */

                                        engine.block.setCropRotation(
                                            block,
                                            engine.block.getCropRotation(block) + (rotation * (Math.PI / 180f)).toFloat()
                                        )

                                        engine.block.setCropScaleRatio(
                                            block,
                                            engine.block.getCropScaleRatio(block) - (1f - zoom)
                                        )

                                        engine.block.setCropTranslationX(
                                            block,
                                            engine.block.getCropTranslationX(
                                                block
                                            ) + (with(density) { pan.x.toDp().value } / size.width)
                                        )

                                        engine.block.setCropTranslationY(
                                            block,
                                            engine.block.getCropTranslationY(
                                                block
                                            ) + (with(density) { pan.y.toDp().value } / size.height)
                                        )

                                        /**
                                         * This is to ensure that the crop gestures do not cause the image
                                         * to go off-screen
                                         */
                                        engine.block.adjustCropToFillFrame(
                                            block,
                                            engine.block.getCropScaleRatio(block)
                                        )
                                    }
                                }
                            }
                        }
                )
            }
        },
        dock = {
            Dock.rememberForDesign(
                listBuilder =
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
                }
            )
        }
    )


    var currentDragArea by remember { mutableStateOf(Rect.Zero) }
    var currentDragTarget by remember { mutableStateOf(Rect.Zero) }

    val placeholderPositions: MutableList<Pair<DesignBlock, Rect>> = remember {
        mutableStateListOf()
    }

    LaunchedEffect(engine) {
        val engine = engine ?: return@LaunchedEffect
        fun invalidatePositions() {
            engine.getPlaceholders().map { block ->
                block to engine.getGlobalRect(density, block)
            }.let {
                placeholderPositions.clear()
                placeholderPositions.addAll(it)
            }
        }

        scope.launch {
            invalidatePositions()
            engine.event
                .subscribe(
                    engine.getCamera()?.let {
                        engine.getPlaceholders() + it
                    } ?: engine.getPlaceholders()
                )
                .collect { _ ->
                    invalidatePositions()
                }
        }
    }

    val hapticFeedback = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .statusBarsPadding()
            .pointerInput(Unit, engine) {
                val engine = engine ?: return@pointerInput
                awaitEachGesture {
                    val event = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Final,
                    )
                    val position = event.position
                    placeholderPositions.find { (_, rect) ->
                        rect.contains(position)
                    }?.let { (currentBlock, rect) ->
                        awaitLongPressOrCancellation(event.id)?.let { longPress ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            var currentOffset = position
                            var currentBlockTarget = currentBlock
                            currentDragArea = rect

                            drag(longPress.id) { dragChange ->
                                val dragAmount = dragChange.positionChange()
                                currentOffset += dragAmount
                                currentDragArea = currentDragArea.copy(
                                    left = currentDragArea.left + dragAmount.x,
                                    top = currentDragArea.top + dragAmount.y,
                                    right = currentDragArea.right + dragAmount.x,
                                    bottom = currentDragArea.bottom + dragAmount.y,
                                )

                                engine.getPlaceholders().map { block ->
                                    block to engine.getGlobalRect(density, block)
                                }.find { (_, rect) ->
                                    rect.contains(currentOffset)
                                }?.let { (block, rect) ->
                                    if (currentBlockTarget != block) {
                                        currentDragTarget = rect
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        currentBlockTarget = block
                                    }
                                }

                                dragChange.consume()
                            }
                            longPress.consume()

                            engine.getPlaceholders().map { block ->
                                block to engine.getGlobalRect(density, block)
                            }.find { (_, rect) ->
                                rect.contains(currentOffset)
                            }?.let { (block, _) ->
                                scope.launch {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    engine.swapBlockPositions(currentBlock, block)
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    delay(10)
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }

                            currentDragArea = Rect.Zero
                            currentDragTarget = Rect.Zero
                            currentOffset = Offset.Zero
                        }
                    }
                }
            }
    ) {
        DesignEditor(
            engineConfiguration = config,
            editorConfiguration = editorConfiguration,
        ) {
            onClose()
        }


        Box(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxSize()
        ) {
            listOf(currentDragTarget, currentDragArea).forEach { rect ->
                Box(
                    modifier = Modifier
                        .applyRectDimensions(rect)
                        .background(
                            color = Color(0xFF4A67FF).copy(alpha = .3f),
                            shape = RoundedCornerShape(size = cornerRadius.floatValue.dp * 2)
                        )
                        .border(
                            width = 2.dp,
                            color = Color(0xFF4A67FF).copy(alpha = .7f),
                            shape = RoundedCornerShape(size = cornerRadius.floatValue.dp * 2)
                        )
                )
            }
        }

    }

}

@ReadOnlyComposable
@Composable
fun Modifier.applyRectDimensions(rect: Rect) =
    this
        .alpha(if (rect.isEmpty) 0f else 1f)
        .offset {
            IntOffset(
                rect.left.roundToInt(),
                rect.top.roundToInt()
            )
        }
        .width(with(LocalDensity.current) { rect.width.toDp() })
        .height(with(LocalDensity.current) { rect.height.toDp() })

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
    engine.block.setScopeEnabled(group, "layer/move", false)

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

suspend fun Engine.swapBlockPositions(currentBlock: DesignBlock, block: DesignBlock) {
    if (currentBlock == block) return
    val originX = this.block.getPositionX(currentBlock)
    val originY = this.block.getPositionY(currentBlock)
    val originWidth = this.block.getWidth(currentBlock)
    val originHeight = this.block.getHeight(currentBlock)

    val targetX = this.block.getPositionX(block)
    val targetY = this.block.getPositionY(block)
    val targetWidth = this.block.getWidth(block)
    val targetHeight = this.block.getHeight(block)

    val animation = Animatable(0f)
    val engine = this
    animation.animateTo(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        block = {
            engine.block.setPositionX(currentBlock, lerp(originX, targetX, value))
            engine.block.setPositionY(currentBlock, lerp(originY, targetY, value))
            engine.block.setWidth(currentBlock, lerp(originWidth, targetWidth, value))
            engine.block.setHeight(currentBlock, lerp(originHeight, targetHeight, value))

            engine.block.setPositionX(block, lerp(targetX, originX, value))
            engine.block.setPositionY(block, lerp(targetY, originY, value))
            engine.block.setWidth(block, lerp(targetWidth, originWidth, value))
            engine.block.setHeight(block, lerp(targetHeight, originHeight, value))

            engine.block.setContentFillMode(currentBlock, ContentFillMode.COVER)
            engine.block.setContentFillMode(block, ContentFillMode.COVER)
        }
    )
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

fun Engine.getCamera(): DesignBlock? {
    return block.findByType(DesignBlockType.Camera).firstOrNull()
}
