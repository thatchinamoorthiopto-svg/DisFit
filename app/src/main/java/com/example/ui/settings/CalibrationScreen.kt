package com.example.ui.settings

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsManager
import com.example.ui.theme.BorderColor
import com.example.ui.theme.PrimaryColor
import com.example.ui.theme.SuccessColor
import com.example.util.ImageUtils
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    var currentScale by remember { mutableStateOf(settingsManager.mmPerPixel) }
    var isCalibrated by remember { mutableStateOf(settingsManager.isCalibrated) }
    var rulerBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var p1 by remember { mutableStateOf<Offset?>(null) }
    var p2 by remember { mutableStateOf<Offset?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val copiedPath = ImageUtils.copyUriToInternalStorage(context, uri)
            if (copiedPath != null) {
                rulerBitmap = ImageUtils.loadBitmapFromFile(copiedPath)
                p1 = null
                p2 = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calibrate Phone", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Phone Calibration Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Mount phone on tripod at exactly 60 cm from a physical ruler.", style = MaterialTheme.typography.bodyLarge)
                    Text("2. Upload or take a photo of the ruler.", style = MaterialTheme.typography.bodyLarge)
                    Text("3. Tap two points on the photo exactly 10 cm (100 mm) apart.", style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Current Scale Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCalibrated) SuccessColor.copy(alpha = 0.08f) else PrimaryColor.copy(alpha = 0.08f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isCalibrated) SuccessColor.copy(alpha = 0.3f) else PrimaryColor.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (isCalibrated) SuccessColor else PrimaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Scale: ${String.format("%.3f", currentScale)} mm/pixel ${if (isCalibrated) "— Saved ✓" else ""}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "100 mm = ${(100f / currentScale).roundToInt()} pixels at 60 cm",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Ruler Photo Tap-to-Measure Area
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("upload_ruler_photo_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Ruler Photo")
            }

            if (rulerBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                if (p1 == null) {
                                    p1 = offset
                                } else if (p2 == null) {
                                    p2 = offset
                                    // Compute scale
                                    val distPx = hypot(p2!!.x - p1!!.x, p2!!.y - p1!!.y)
                                    if (distPx > 10f) {
                                        // Image scale adjustment factor
                                        val calculatedScale = 100.0f / (distPx * (rulerBitmap!!.width.toFloat() / size.width))
                                        currentScale = calculatedScale
                                    }
                                } else {
                                    // Reset taps
                                    p1 = offset
                                    p2 = null
                                }
                            }
                        }
                ) {
                    val bmp = rulerBitmap!!.asImageBitmap()
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawImage(
                            image = bmp,
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                        )

                        p1?.let {
                            drawCircle(Color.Red, radius = 10.dp.toPx(), center = it)
                        }
                        p2?.let {
                            drawCircle(Color.Red, radius = 10.dp.toPx(), center = it)
                            drawLine(Color.Yellow, p1!!, p2!!, strokeWidth = 3.dp.toPx())
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (p1 == null) "Tap first point on 0 cm" else if (p2 == null) "Tap second point on 10 cm" else "Points set! Tap again to reset.",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    settingsManager.saveCalibration(currentScale)
                    isCalibrated = true
                    Toast.makeText(context, "Calibration Saved ✓ (${String.format("%.3f", currentScale)} mm/px)", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_calibration_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Calibration", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
