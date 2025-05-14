package ly.img.postcardcollage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage

fun imageUrl(index: Int) = "https://yavuzceliker.github.io/sample-images/image-${index}.jpg"

@Composable
fun PhotoSelect(
    navHostController: NavHostController,
    modifier: Modifier = Modifier
) {

    val selectedImages = remember { mutableStateListOf<String>() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(
                    topStart = 32.dp,
                    topEnd = 32.dp,
                ),
            ) {

                AnimatedContent(
                    targetState = selectedImages.size == 3,
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center,
                    transitionSpec = {
                        slideInVertically { -it / 3 } + fadeIn() togetherWith slideOutVertically { it / 3 } + fadeOut()
                    }
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (it) {
                            Row(
                                modifier = Modifier.horizontalScroll(
                                    state = rememberScrollState()
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                collageLayouts.forEach {
                                    Column(
                                        modifier = Modifier
                                            .clickable {
                                                navHostController.navigate(
                                                    EditorPayload(
                                                        urls = selectedImages,
                                                        collageLayout = it.scene
                                                    )
                                                )
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
                        } else {
                            Text("Select 3 images")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->

        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Fixed(3),
            contentPadding = paddingValues,
        ) {
            items(50) { index ->
                val url = remember(index) { imageUrl(index + 1) }
                SelectableImage(
                    selected = selectedImages.contains(url),
                    imageUrl = url,
                    enabled = selectedImages.size < 3,
                    onSelect = {
                        if (selectedImages.contains(url)) {
                            selectedImages.remove(url)
                        } else {
                            if (selectedImages.size < 3)
                                selectedImages.add(url)
                        }
                    }
                )
            }
        }

    }

}

@Composable
fun SelectableImage(
    modifier: Modifier = Modifier,
    imageUrl: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: (Boolean) -> Unit,
) {

    val innerPadding by animateDpAsState(
        targetValue = if (selected) 4.dp else 0.dp
    )

    val alpha by animateFloatAsState(
        targetValue = if (selected || enabled) 1f else .2f
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .aspectRatio(1f)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onSelect(!selected) }
            .padding(8.dp)
//            .border(
//                width = borderWidth,
//                color = if (selected) Color.Red else Color.Transparent,
//                shape = RoundedCornerShape(32.dp)
//            )
            .padding(innerPadding)
            .background(
                color = Color.Red,
                shape = RoundedCornerShape(28.dp)
            )
    ) {

        Box(
            modifier = Modifier.clip(
                shape = RoundedCornerShape(16.dp)
            )
        ) {
            AsyncImage(
                model = imageUrl,
                modifier = Modifier.fillMaxSize(),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
                    .offset(8.dp, 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

    }

}

@Preview(showBackground = true)
@Composable
private fun SelectableImageChecked() {
    SelectableImage(
        modifier = Modifier.width(200.dp),
        imageUrl = "",
        enabled = true,
        selected = true
    ) { }
}

@Preview(showBackground = true)
@Composable
private fun SelectableImageUnchecked() {
    SelectableImage(
        modifier = Modifier.width(200.dp),
        imageUrl = "",
        enabled = true,
        selected = false
    ) { }
}