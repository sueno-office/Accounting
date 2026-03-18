package com.example.receiptscanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.receiptscanner.domain.model.ParsedReceiptFields
import com.example.receiptscanner.ui.camera.CameraScreen
import com.example.receiptscanner.ui.camera.CameraViewModel
import com.example.receiptscanner.ui.history.HistoryScreen
import com.example.receiptscanner.ui.review.ReviewScreen
import com.example.receiptscanner.ui.settings.SettingsScreen

object Routes {
    const val CAMERA = "camera"
    const val REVIEW = "review"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val cameraViewModel: CameraViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = Routes.CAMERA) {

        composable(Routes.CAMERA) {
            CameraScreen(
                viewModel = cameraViewModel,
                onNavigateToReview = {
                    navController.navigate(Routes.REVIEW) {
                        launchSingleTop = true
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.HISTORY)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToManualEntry = {
                    navController.navigate(Routes.REVIEW) {
                        launchSingleTop = true
                    }
                },
                onShareCsv = {
                    // TODO: 現在セッションのCSV共有
                }
            )
        }

        composable(Routes.REVIEW) {
            val parsedFields by cameraViewModel.parsedFields.collectAsState()
            val photoPath by cameraViewModel.capturedPhotoPath.collectAsState()
            val photoNumber by cameraViewModel.nextPhotoNumber.collectAsState()

            ReviewScreen(
                parsedFields = parsedFields ?: ParsedReceiptFields(),
                photoPath = photoPath ?: "",
                photoNumber = photoNumber,
                onSaved = {
                    cameraViewModel.resetForNextScan()
                    navController.popBackStack(Routes.CAMERA, inclusive = false)
                },
                onDiscard = {
                    cameraViewModel.resetForNextScan()
                    navController.popBackStack(Routes.CAMERA, inclusive = false)
                }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
