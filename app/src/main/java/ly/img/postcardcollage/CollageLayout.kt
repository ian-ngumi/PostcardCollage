package ly.img.postcardcollage

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FrontHand
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import kotlinx.serialization.Serializable

val collageLayouts = listOf(
    CollageLayout.FullHalfHalf,
    CollageLayout.HalfHalfFull,
    CollageLayout.Christmas,
    CollageLayout.Bonjour,
    CollageLayout.MissingYou,
)

@Serializable
sealed class CollageLayout(
    val name: String = "",
    val scene: String = "",
) {

    @Serializable
    data object FullHalfHalf : CollageLayout(
        name = "full-half-half",
        scene = "file:///android_asset/full_half_half.scene",
    )

    @Serializable
    data object HalfHalfFull : CollageLayout(
        name = "half-half-full",
        scene = "file:///android_asset/half_half_full.scene",
    )

    @Serializable
    data object Christmas : CollageLayout(
        name = "Christmas",
        scene = "file:///android_asset/christmas.scene",
    )

    @Serializable
    data object Bonjour : CollageLayout(
        name = "Bonjour",
        scene = "file:///android_asset/bonjour.scene",
    )

    @Serializable
    data object MissingYou : CollageLayout(
        name = "Missing You",
        scene = "file:///android_asset/missing_you.scene",
    )

}

@Composable
fun CollageLayout.GetIcon(modifier: Modifier = Modifier) {

    Image(
        painter = painterResource(
            when (this) {
                CollageLayout.FullHalfHalf -> R.drawable.full_half
                CollageLayout.HalfHalfFull -> R.drawable.half_full
                CollageLayout.Christmas -> R.drawable.christmas
                CollageLayout.Bonjour -> R.drawable.bonjour
                CollageLayout.MissingYou -> R.drawable.missing_you
            }
        ),
        modifier = modifier,
        contentDescription = null,
    )
}