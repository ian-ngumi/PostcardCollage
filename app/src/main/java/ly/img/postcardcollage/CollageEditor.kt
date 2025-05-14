package ly.img.postcardcollage

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
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
import ly.img.engine.DesignBlockType
import ly.img.engine.FillType


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

    val editorConfiguration = EditorConfiguration.rememberForDesign(
        dock = {
            Dock.rememberForDesign(
                listBuilder = Dock.ListBuilder.remember {
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
                                                            .padding(horizontal = 32.dp)
                                                        ,
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
                                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                                verticalArrangement = Arrangement.Center,
                                                                horizontalAlignment = Alignment.CenterHorizontally
                                                            ) {
                                                                it.GetIcon(Modifier.weight(1f))
                                                                Spacer(Modifier.height(4.dp))
                                                                Text(it.name, style = MaterialTheme.typography.labelSmall)
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
                                                                        backgroundColor.value = it
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
