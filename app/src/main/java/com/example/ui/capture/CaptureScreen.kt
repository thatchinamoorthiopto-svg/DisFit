package com.example.ui.capture

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.PhotoCameraFront
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.PrimaryColor
import com.example.util.ImageUtils
import java.io.File
import java.net.URLEncoder
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun CaptureScreen(
    onNavigateBack: () -> Unit,
    onPhotoCaptured: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isFlashOn by remember { mutableStateOf(false) }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var tempPhotoPath by remember { mutableStateOf<String?>(null) }

    var isCropMode by remember { mutableStateOf(false) }

    // CameraProvider reference & PreviewView reference for clean flip camera rebinding
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setFlashMode(if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
            .build()
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraProviderRef?.unbindAll()
        }
    }

    // Rebind camera whenever lensFacing, isFlashOn, or permission changes
    LaunchedEffect(lensFacing, isFlashOn, hasCameraPermission) {
        val pv = previewViewRef
        if (hasCameraPermission && pv != null) {
            imageCapture.flashMode = if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                cameraProviderRef = cameraProvider
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(pv.surfaceProvider)
                }
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("CaptureScreen", "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    // Crop box coordinates in normalized floats [0..1]
    var cropLeft by remember { mutableFloatStateOf(0.1f) }
    var cropTop by remember { mutableFloatStateOf(0.1f) }
    var cropRight by remember { mutableFloatStateOf(0.9f) }
    var cropBottom by remember { mutableFloatStateOf(0.9f) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val copiedPath = ImageUtils.copyUriToInternalStorage(context, uri)
            if (copiedPath != null) {
                tempPhotoPath = copiedPath
                capturedBitmap = ImageUtils.loadBitmapFromFile(copiedPath)
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (capturedBitmap != null) {
                if (isCropMode) {
                    // Crop Mode Overlay
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            val bmp = capturedBitmap!!.asImageBitmap()

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val canvasW = size.width
                                val canvasH = size.height

                                drawImage(
                                    image = bmp,
                                    dstSize = IntSize(canvasW.roundToInt(), canvasH.roundToInt())
                                )

                                val leftPx = cropLeft * canvasW
                                val topPx = cropTop * canvasH
                                val rightPx = cropRight * canvasW
                                val bottomPx = cropBottom * canvasH

                                // Dark semi-transparent overlay outside crop box
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    size = size
                                )

                                // Clear crop rect
                                drawRect(
                                    color = Color.Transparent,
                                    topLeft = Offset(leftPx, topPx),
                                    size = androidx.compose.ui.geometry.Size(rightPx - leftPx, bottomPx - topPx)
                                )

                                // Crop box outline & grid lines
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(leftPx, topPx),
                                    size = androidx.compose.ui.geometry.Size(rightPx - leftPx, bottomPx - topPx),
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // Grid lines inside crop
                                val gridW = (rightPx - leftPx) / 3f
                                val gridH = (bottomPx - topPx) / 3f
                                drawLine(Color.White.copy(alpha = 0.5f), Offset(leftPx + gridW, topPx), Offset(leftPx + gridW, bottomPx), 1.dp.toPx())
                                drawLine(Color.White.copy(alpha = 0.5f), Offset(leftPx + gridW * 2, topPx), Offset(leftPx + gridW * 2, bottomPx), 1.dp.toPx())
                                drawLine(Color.White.copy(alpha = 0.5f), Offset(leftPx, topPx + gridH), Offset(rightPx, topPx + gridH), 1.dp.toPx())
                                drawLine(Color.White.copy(alpha = 0.5f), Offset(leftPx, topPx + gridH * 2), Offset(rightPx, topPx + gridH * 2), 1.dp.toPx())

                                // Corner handles
                                val handleRadius = 10.dp.toPx()
                                drawCircle(PrimaryColor, handleRadius, Offset(leftPx, topPx))
                                drawCircle(PrimaryColor, handleRadius, Offset(rightPx, topPx))
                                drawCircle(PrimaryColor, handleRadius, Offset(leftPx, bottomPx))
                                drawCircle(PrimaryColor, handleRadius, Offset(rightPx, bottomPx))
                            }

                            // Interactive drag detection for crop corners
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()

                                            val dx = dragAmount.x / w
                                            val dy = dragAmount.y / h

                                            val touchX = change.position.x / w
                                            val touchY = change.position.y / h

                                            val distTL = hypot(touchX - cropLeft, touchY - cropTop)
                                            val distTR = hypot(touchX - cropRight, touchY - cropTop)
                                            val distBL = hypot(touchX - cropLeft, touchY - cropBottom)
                                            val distBR = hypot(touchX - cropRight, touchY - cropBottom)

                                            val minDist = minOf(distTL, distTR, distBL, distBR)

                                            when (minDist) {
                                                distTL -> {
                                                    cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - 0.1f)
                                                    cropTop = (cropTop + dy).coerceIn(0f, cropBottom - 0.1f)
                                                }
                                                distTR -> {
                                                    cropRight = (cropRight + dx).coerceIn(cropLeft + 0.1f, 1f)
                                                    cropTop = (cropTop + dy).coerceIn(0f, cropBottom - 0.1f)
                                                }
                                                distBL -> {
                                                    cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - 0.1f)
                                                    cropBottom = (cropBottom + dy).coerceIn(cropTop + 0.1f, 1f)
                                                }
                                                distBR -> {
                                                    cropRight = (cropRight + dx).coerceIn(cropLeft + 0.1f, 1f)
                                                    cropBottom = (cropBottom + dy).coerceIn(cropTop + 0.1f, 1f)
                                                }
                                            }
                                        }
                                    }
                            )
                        }

                        // Crop Action Bar with Portrait Auto-Crop Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    cropLeft = 0.15f
                                    cropTop = 0.10f
                                    cropRight = 0.85f
                                    cropBottom = 0.90f
                                },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Rounded.PhotoCameraFront, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auto Portrait", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val cropped = ImageUtils.cropBitmap(
                                        sourceBitmap = capturedBitmap!!,
                                        cropXNorm = cropLeft,
                                        cropYNorm = cropTop,
                                        cropWidthNorm = cropRight - cropLeft,
                                        cropHeightNorm = cropBottom - cropTop
                                    )
                                    val savedPath = ImageUtils.saveBitmapToInternalStorage(context, cropped, "cropped")
                                    capturedBitmap = cropped
                                    tempPhotoPath = savedPath
                                    isCropMode = false
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                            ) {
                                Icon(Icons.Rounded.Crop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Apply Crop")
                            }
                        }
                    }
                } else {
                    // Post-Capture Preview Mode
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured Photo",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Retake / Crop / Confirm Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    capturedBitmap = null
                                    tempPhotoPath = null
                                },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("retake_button")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retake")
                            }

                            OutlinedButton(
                                onClick = { isCropMode = true },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("crop_photo_button")
                            ) {
                                Icon(Icons.Rounded.Crop, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Crop")
                            }

                            Button(
                                onClick = {
                                    tempPhotoPath?.let { path ->
                                        val encodedPath = URLEncoder.encode(path, "UTF-8")
                                        onPhotoCaptured(encodedPath)
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                modifier = Modifier.testTag("confirm_photo_button")
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Confirm")
                            }
                        }
                    }
                }
            } else {
                // Live Camera View
                if (hasCameraPermission) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // CameraX Preview
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                previewViewRef = previewView
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top Row: Left Close Button ('X') & Right Flash ON/OFF Toggle Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("close_camera_button")
                            ) {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }

                            // Flash ON/OFF Toggle Button
                            Surface(
                                shape = CircleShape,
                                color = if (isFlashOn) PrimaryColor else Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("toggle_flash_button")
                            ) {
                                IconButton(onClick = { isFlashOn = !isFlashOn }) {
                                    Icon(
                                        imageVector = if (isFlashOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                                        contentDescription = "Toggle Flash",
                                        tint = if (isFlashOn) Color.Yellow else Color.White
                                    )
                                }
                            }
                        }

                        // Top Instruction Text
                        Text(
                            text = "Keep eyes on the line, face inside the circle.\n60 cm from phone.",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 28.dp)
                        )

                        // Circular Alignment Overlay with Crosshairs
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val strokeWidthPx = 2.dp.toPx()
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)

                            val centerX = w / 2f
                            val centerY = h * 0.45f
                            val radius = w * 0.38f

                            // Outer Guide Circle
                            drawCircle(
                                color = Color.White,
                                center = Offset(centerX, centerY),
                                radius = radius,
                                style = Stroke(width = strokeWidthPx)
                            )

                            // Horizontal Crosshair Line
                            drawLine(
                                color = Color.White.copy(alpha = 0.8f),
                                start = Offset(centerX - radius - 20.dp.toPx(), centerY),
                                end = Offset(centerX + radius + 20.dp.toPx(), centerY),
                                strokeWidth = strokeWidthPx,
                                pathEffect = dashEffect
                            )

                            // Vertical Crosshair Line
                            drawLine(
                                color = Color.White.copy(alpha = 0.8f),
                                start = Offset(centerX, centerY - radius - 20.dp.toPx()),
                                end = Offset(centerX, centerY + radius + 20.dp.toPx()),
                                strokeWidth = strokeWidthPx,
                                pathEffect = dashEffect
                            )

                            // Center Bullseye Target
                            drawCircle(
                                color = Color.White,
                                center = Offset(centerX, centerY),
                                radius = 12.dp.toPx(),
                                style = Stroke(width = strokeWidthPx)
                            )
                        }

                        // White Bottom Section
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            color = Color.White,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp, horizontal = 28.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Upload Button
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .testTag("upload_photo_button")
                                ) {
                                    IconButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(Color(0xFFF0F4F8), CircleShape)
                                    ) {
                                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Upload", tint = Color(0xFF64748B))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Upload", fontSize = 12.sp, color = Color(0xFF64748B))
                                }

                                // Capture Button (Big Blue Circle)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.testTag("capture_photo_button")
                                ) {
                                    Surface(
                                        modifier = Modifier.size(72.dp),
                                        shape = CircleShape,
                                        color = PrimaryColor,
                                        shadowElevation = 4.dp
                                    ) {
                                        IconButton(
                                            onClick = {
                                                takePhoto(context, imageCapture) { bitmap, path ->
                                                    val portraitBmp = ImageUtils.cropBitmap(bitmap, 0.10f, 0.05f, 0.80f, 0.90f)
                                                    val savedPath = ImageUtils.saveBitmapToInternalStorage(context, portraitBmp, "portrait_capture")
                                                    capturedBitmap = portraitBmp
                                                    tempPhotoPath = savedPath
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Rounded.CameraAlt,
                                                contentDescription = "Capture",
                                                tint = Color.White,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Capture", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryColor)
                                }

                                // Flip Button
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .testTag("flip_camera_button")
                                ) {
                                    IconButton(
                                        onClick = {
                                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                                CameraSelector.LENS_FACING_FRONT
                                            } else {
                                                CameraSelector.LENS_FACING_BACK
                                            }
                                        },
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(Color(0xFFF0F4F8), CircleShape)
                                    ) {
                                        Icon(Icons.Rounded.FlipCameraAndroid, contentDescription = "Flip Camera", tint = PrimaryColor)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Flip", fontSize = 12.sp, color = PrimaryColor, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                } else {
                    // Fallback when camera permission denied
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Camera permission is required for live photo capture.",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                        ) {
                            Text("Grant Permission")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Text("Upload Photo from Gallery", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onCaptured: (Bitmap, String) -> Unit
) {
    val photoFile = File(context.filesDir, "capture_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val bitmap = ImageUtils.loadBitmapFromFile(photoFile.absolutePath)
                if (bitmap != null) {
                    onCaptured(bitmap, photoFile.absolutePath)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CaptureScreen", "Photo capture failed: ${exception.message}", exception)
            }
        }
    )
}
