package ly.img.postcardcollage

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ly.img.postcardcollage.ui.theme.PostcardCollageTheme

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PostcardCollageTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {

                    val navHostController = rememberNavController()

                    NavHost(
                        navController = navHostController,
                        startDestination = "main",
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                    ) {

                        composable(
                            route = "main"
                        ) {
                            PhotoSelect(navHostController)

                        }

                        composable<EditorPayload> {
                            CollageEditor(

                            ) {
                                navHostController.popBackStack()
                            }
                        }

                    }


                }
            }
        }
    }
}