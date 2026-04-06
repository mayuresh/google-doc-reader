package com.docreader.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.docreader.app.ui.screens.DriveScreen
import com.docreader.app.ui.screens.LoginScreen
import com.docreader.app.ui.screens.ReaderScreen
import com.docreader.app.viewmodel.AuthViewModel
import com.docreader.app.viewmodel.DriveViewModel
import com.docreader.app.viewmodel.ReaderViewModel
import com.docreader.app.viewmodel.VoiceViewModel
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val LOGIN = "login"
    const val DRIVE = "drive"
    const val READER = "reader/{docId}/{docTitle}"

    fun readerRoute(docId: String, docTitle: String) =
        "reader/${URLEncoder.encode(docId, "UTF-8")}/${URLEncoder.encode(docTitle, "UTF-8")}"
}

@Composable
fun NavGraph(
    authViewModel: AuthViewModel,
    driveViewModel: DriveViewModel,
    readerViewModel: ReaderViewModel,
    voiceViewModel: VoiceViewModel
) {
    val navController = rememberNavController()

    fun logout() {
        authViewModel.signOut()
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.DRIVE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DRIVE) {
            DriveScreen(
                viewModel = driveViewModel,
                onDocSelected = { driveItem ->
                    navController.navigate(Routes.readerRoute(driveItem.id, driveItem.name))
                },
                onLogout = { logout() }
            )
        }

        composable(
            route = Routes.READER,
            arguments = listOf(
                navArgument("docId") { type = NavType.StringType },
                navArgument("docTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val docId = URLDecoder.decode(backStackEntry.arguments?.getString("docId") ?: "", "UTF-8")
            val docTitle = URLDecoder.decode(backStackEntry.arguments?.getString("docTitle") ?: "", "UTF-8")

            ReaderScreen(
                docId = docId,
                docTitle = docTitle,
                readerViewModel = readerViewModel,
                voiceViewModel = voiceViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
