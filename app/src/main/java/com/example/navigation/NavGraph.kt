package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.capture.CaptureScreen
import com.example.ui.home.HomeScreen
import com.example.ui.measure.MeasureScreen
import com.example.ui.records.RecordsScreen
import com.example.ui.result.ResultScreen
import com.example.ui.settings.CalibrationScreen

object NavRoutes {
    const val HOME = "home"
    const val CAMERA = "camera"
    const val MEASURE = "measure/{encodedPhotoPath}"
    const val RESULT = "result/{encodedPhotoPath}?rightMpd={rightMpd}&leftMpd={leftMpd}&binocularPd={binocularPd}&iicd={iicd}&faceWidth={faceWidth}&isEdited={isEdited}&recordId={recordId}"
    const val RECORDS = "records"
    const val CALIBRATION = "calibration"

    fun createMeasureRoute(encodedPhotoPath: String) = "measure/$encodedPhotoPath"

    fun createResultRoute(
        encodedPhotoPath: String,
        rightMpd: Float,
        leftMpd: Float,
        binocularPd: Float,
        iicd: Float,
        faceWidth: Float?,
        isEdited: Boolean,
        recordId: Long? = null
    ): String {
        val fwStr = faceWidth?.toString() ?: "-1"
        val recIdStr = recordId?.toString() ?: "-1"
        return "result/$encodedPhotoPath?rightMpd=$rightMpd&leftMpd=$leftMpd&binocularPd=$binocularPd&iicd=$iicd&faceWidth=$fwStr&isEdited=$isEdited&recordId=$recIdStr"
    }
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToCamera = { navController.navigate(NavRoutes.CAMERA) },
                onNavigateToRecords = { navController.navigate(NavRoutes.RECORDS) },
                onNavigateToCalibration = { navController.navigate(NavRoutes.CALIBRATION) },
                onSelectRecord = { path, rightMpd, leftMpd, binocularPd, iicd, faceWidth, isEdited, recordId ->
                    navController.navigate(
                        NavRoutes.createResultRoute(path, rightMpd, leftMpd, binocularPd, iicd, faceWidth, isEdited, recordId)
                    )
                }
            )
        }

        composable(NavRoutes.CAMERA) {
            CaptureScreen(
                onNavigateBack = { navController.popBackStack() },
                onPhotoCaptured = { encodedPath ->
                    navController.navigate(NavRoutes.createMeasureRoute(encodedPath))
                }
            )
        }

        composable(
            route = NavRoutes.MEASURE,
            arguments = listOf(navArgument("encodedPhotoPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val photoPath = backStackEntry.arguments?.getString("encodedPhotoPath") ?: ""
            MeasureScreen(
                encodedPhotoPath = photoPath,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResult = { path, rightMpd, leftMpd, binocularPd, iicd, faceWidth, isEdited ->
                    navController.navigate(
                        NavRoutes.createResultRoute(path, rightMpd, leftMpd, binocularPd, iicd, faceWidth, isEdited)
                    )
                }
            )
        }

        composable(
            route = NavRoutes.RESULT,
            arguments = listOf(
                navArgument("encodedPhotoPath") { type = NavType.StringType },
                navArgument("rightMpd") { type = NavType.FloatType; defaultValue = 30.6f },
                navArgument("leftMpd") { type = NavType.FloatType; defaultValue = 30.9f },
                navArgument("binocularPd") { type = NavType.FloatType; defaultValue = 61.5f },
                navArgument("iicd") { type = NavType.FloatType; defaultValue = 31.8f },
                navArgument("faceWidth") { type = NavType.FloatType; defaultValue = -1f },
                navArgument("isEdited") { type = NavType.BoolType; defaultValue = false },
                navArgument("recordId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val photoPath = backStackEntry.arguments?.getString("encodedPhotoPath") ?: ""
            val rightMpd = backStackEntry.arguments?.getFloat("rightMpd") ?: 30.6f
            val leftMpd = backStackEntry.arguments?.getFloat("leftMpd") ?: 30.9f
            val binocularPd = backStackEntry.arguments?.getFloat("binocularPd") ?: 61.5f
            val iicd = backStackEntry.arguments?.getFloat("iicd") ?: 31.8f
            val rawFaceWidth = backStackEntry.arguments?.getFloat("faceWidth") ?: -1f
            val faceWidth = if (rawFaceWidth < 0) null else rawFaceWidth
            val isEdited = backStackEntry.arguments?.getBoolean("isEdited") ?: false
            val rawRecordId = backStackEntry.arguments?.getLong("recordId") ?: -1L
            val recordId = if (rawRecordId < 0) null else rawRecordId

            ResultScreen(
                encodedPhotoPath = photoPath,
                rightMpd = rightMpd,
                leftMpd = leftMpd,
                binocularPd = binocularPd,
                iicd = iicd,
                faceWidth = faceWidth,
                isEdited = isEdited,
                recordId = recordId,
                onNavigateBack = { navController.popBackStack() },
                onRecordSaved = {
                    navController.navigate(NavRoutes.RECORDS) {
                        popUpTo(NavRoutes.HOME)
                    }
                }
            )
        }

        composable(NavRoutes.RECORDS) {
            RecordsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCamera = { navController.navigate(NavRoutes.CAMERA) },
                onSelectRecord = { path, rightMpd, leftMpd, binocularPd, iicd, faceWidth, isEdited, recordId ->
                    navController.navigate(
                        NavRoutes.createResultRoute(path, rightMpd, leftMpd, binocularPd, iicd, faceWidth, isEdited, recordId)
                    )
                }
            )
        }

        composable(NavRoutes.CALIBRATION) {
            CalibrationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
