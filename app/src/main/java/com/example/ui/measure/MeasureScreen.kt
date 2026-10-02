package com.example.ui.measure

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ui.theme.WarningColor
import com.example.util.Calibration
import com.example.util.ImageUtils
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class MeasurementMode(val label: String) {
    RIGHT_MPD("Right MPD"),
    LEFT_MPD("Left MPD"),
    IICD("IICD"),
    FACE_WIDTH("Face Width")
}

enum class PointTheme(val label: String) {
    CROSSHAIR("Crosshair +"),
    MICRO_DOT("Micro Dot •"),
    LINE_TICK("Line Ticks ──")
}

data class Point2D(val x: Float, val y: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasureScreen(
    encodedPhotoPath: String,
    onNavigateBack: () -> Unit,
    onNavigateToResult: (
        photoPath: String,
        rightMpd: Float,
        leftMpd: Float,
        binocularPd: Float,
        iicd: Float,
        faceWidth: Float?,
        isEdited: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val photoPath = remember(encodedPhotoPath) { URLDecoder.decode(encodedPhotoPath, "UTF-8") }
    val bitmap = remember(photoPath) { ImageUtils.loadBitmapFromFile(photoPath) }
    val settingsManager = remember { SettingsManager(context) }

    // Dynamic scale for this photo
    var activeMmPerPixel by remember { mutableFloatStateOf(settingsManager.mmPerPixel) }

    var activeMode by remember { mutableStateOf(MeasurementMode.RIGHT_MPD) }
    var activePointTheme by remember { mutableStateOf(PointTheme.CROSSHAIR) }
    var isFullScreenPhoto by remember { mutableStateOf(false) }
    var isPanelExpanded by remember { mutableStateOf(true) }

    // Theme state: Crisp White Theme by default
    var isWhiteThemeCanvas by remember { mutableStateOf(true) }

    // Option to hide/show measurement lines and dots ("un-visible" option)
    var isOverlayVisible by remember { mutableStateOf(true) }

    // Iris Mini-Calibration Modal Dialog State
    var showSetScaleDialog by remember { mutableStateOf(false) }
    var irisP1 by remember { mutableStateOf<Point2D?>(null) }
    var irisP2 by remember { mutableStateOf<Point2D?>(null) }
    var calculatedIrisScale by remember { mutableFloatStateOf(0f) }

    // Dialog Zoom and Pan States for Iris Calibration
    var dialogScale by remember { mutableFloatStateOf(1f) }
    var dialogOffsetX by remember { mutableFloatStateOf(0f) }
    var dialogOffsetY by remember { mutableFloatStateOf(0f) }

    // Normalized points [0..1]
    var midline by remember { mutableStateOf(Point2D(0.50f, 0.45f)) }
    var rightPupil by remember { mutableStateOf(Point2D(0.35f, 0.45f)) }
    var leftPupil by remember { mutableStateOf(Point2D(0.65f, 0.45f)) }
    var rightInnerCorner by remember { mutableStateOf(Point2D(0.42f, 0.46f)) }
    var leftInnerCorner by remember { mutableStateOf(Point2D(0.58f, 0.46f)) }
    var rightCheekbone by remember { mutableStateOf(Point2D(0.22f, 0.55f)) }
    var leftCheekbone by remember { mutableStateOf(Point2D(0.78f, 0.55f)) }

    // STEP-BY-STEP CONFIRMATION STATES (No auto fill!)
    var isRightMpdConfirmed by remember { mutableStateOf(false) }
    var rightMpdConfirmedVal by remember { mutableFloatStateOf(0f) }

    var isLeftMpdConfirmed by remember { mutableStateOf(false) }
    var leftMpdConfirmedVal by remember { mutableFloatStateOf(0f) }

    var isIicdConfirmed by remember { mutableStateOf(false) }
    var iicdConfirmedVal by remember { mutableFloatStateOf(0f) }

    var isFaceWidthConfirmed by remember { mutableStateOf(false) }
    var faceWidthConfirmedVal by remember { mutableFloatStateOf(0f) }

    var isEditedManually by remember { mutableStateOf(false) }

    // Magnifier state
    var isDraggingPoint by remember { mutableStateOf(false) }
    var dragPointLocation by remember { mutableStateOf(Offset.Zero) }

    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Computed live values for active points
    val imageWidthPx = bitmap?.width?.toFloat() ?: 1000f
    val imageHeightPx = bitmap?.height?.toFloat() ?: 1000f

    fun calcDistMm(p1: Point2D, p2: Point2D): Float {
        val dx = (p2.x - p1.x) * imageWidthPx
        val dy = (p2.y - p1.y) * imageHeightPx
        val distPx = hypot(dx, dy)
        return Calibration.calculateMmFromPixels(distPx, activeMmPerPixel)
    }

    val liveRightMpd = calcDistMm(midline, rightPupil)
    val liveLeftMpd = calcDistMm(midline, leftPupil)
    val liveIicd = calcDistMm(rightInnerCorner, leftInnerCorner)
    val liveFaceWidth = calcDistMm(rightCheekbone, leftCheekbone)

    // Binocular PD is auto calculated ONLY when BOTH Right MPD and Left MPD are confirmed!
    val isBinocularConfirmed = isRightMpdConfirmed && isLeftMpdConfirmed
    val binocularPdVal = if (isBinocularConfirmed) Math.round((rightMpdConfirmedVal + leftMpdConfirmedVal) * 10.0f) / 10.0f else 0f

    val isSanityAlert = remember(isRightMpdConfirmed, rightMpdConfirmedVal, activeMmPerPixel) {
        if (isRightMpdConfirmed) Calibration.isSanityWarning(rightMpdConfirmedVal, leftMpdConfirmedVal, iicdConfirmedVal, if (isFaceWidthConfirmed) faceWidthConfirmedVal else null) else false
    }

    val photoHeight by animateDpAsState(
        targetValue = if (isFullScreenPhoto) 580.dp else if (!isPanelExpanded) 480.dp else 290.dp,
        label = "photoHeight"
    )

    fun confirmCurrentModeMeasurement() {
        when (activeMode) {
            MeasurementMode.RIGHT_MPD -> {
                rightMpdConfirmedVal = liveRightMpd
                isRightMpdConfirmed = true
                activeMode = MeasurementMode.LEFT_MPD
            }
            MeasurementMode.LEFT_MPD -> {
                leftMpdConfirmedVal = liveLeftMpd
                isLeftMpdConfirmed = true
                activeMode = MeasurementMode.IICD
            }
            MeasurementMode.IICD -> {
                iicdConfirmedVal = liveIicd
                isIicdConfirmed = true
                activeMode = MeasurementMode.FACE_WIDTH
            }
            MeasurementMode.FACE_WIDTH -> {
                faceWidthConfirmedVal = liveFaceWidth
                isFaceWidthConfirmed = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Measure", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // White / Dark Theme Canvas Toggle Button
                    IconButton(onClick = { isWhiteThemeCanvas = !isWhiteThemeCanvas }) {
                        Icon(
                            imageVector = if (isWhiteThemeCanvas) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                            contentDescription = "Toggle White/Dark Theme",
                            tint = PrimaryColor
                        )
                    }

                    // Small "⚙ Set Scale" Button
                    OutlinedButton(
                        onClick = {
                            irisP1 = null
                            irisP2 = null
                            calculatedIrisScale = 0f
                            dialogScale = 1f
                            dialogOffsetX = 0f
                            dialogOffsetY = 0f
                            showSetScaleDialog = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("set_scale_button")
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("⚙ Set Scale", fontSize = 12.sp)
                    }

                    IconButton(onClick = { isFullScreenPhoto = !isFullScreenPhoto }) {
                        Icon(
                            imageVector = if (isFullScreenPhoto) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                            contentDescription = "Toggle Fullscreen Photo",
                            tint = PrimaryColor
                        )
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
        ) {
            // Interactive Photo Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(photoHeight)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isWhiteThemeCanvas) Color(0xFFF1F5F9) else Color.Black
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    if (bitmap != null) {
                        val imageBitmap = bitmap.asImageBitmap()

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                        ) {
                            // Display Photo Canvas
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val canvasW = size.width
                                val canvasH = size.height

                                drawImage(
                                    image = imageBitmap,
                                    dstSize = IntSize(canvasW.roundToInt(), canvasH.roundToInt())
                                )

                                // Draw fine, precise measurement line and dots ONLY if isOverlayVisible is true
                                if (isOverlayVisible) {
                                    val pt0 = when (activeMode) {
                                        MeasurementMode.RIGHT_MPD -> Offset(midline.x * canvasW, midline.y * canvasH)
                                        MeasurementMode.LEFT_MPD -> Offset(midline.x * canvasW, midline.y * canvasH)
                                        MeasurementMode.IICD -> Offset(rightInnerCorner.x * canvasW, rightInnerCorner.y * canvasH)
                                        MeasurementMode.FACE_WIDTH -> Offset(rightCheekbone.x * canvasW, rightCheekbone.y * canvasH)
                                    }

                                    val pt1 = when (activeMode) {
                                        MeasurementMode.RIGHT_MPD -> Offset(rightPupil.x * canvasW, rightPupil.y * canvasH)
                                        MeasurementMode.LEFT_MPD -> Offset(leftPupil.x * canvasW, leftPupil.y * canvasH)
                                        MeasurementMode.IICD -> Offset(leftInnerCorner.x * canvasW, leftInnerCorner.y * canvasH)
                                        MeasurementMode.FACE_WIDTH -> Offset(leftCheekbone.x * canvasW, leftCheekbone.y * canvasH)
                                    }

                                    // Ultra-thin connecting line (0.9 dp)
                                    drawLine(
                                        color = PrimaryColor,
                                        start = pt0,
                                        end = pt1,
                                        strokeWidth = 0.9.dp.toPx()
                                    )

                                    // Render Point Handles based on activePointTheme
                                    val dotRadiusPx = 2.0.dp.toPx()
                                    val borderStrokePx = 0.8.dp.toPx()
                                    val crosshairSize = 7.dp.toPx()

                                    listOf(pt0, pt1).forEach { pt ->
                                        when (activePointTheme) {
                                            PointTheme.CROSSHAIR -> {
                                                // Ultra-thin Red Reticle Crosshairs + Micro Dot
                                                drawLine(Color.Red, Offset(pt.x - crosshairSize, pt.y), Offset(pt.x + crosshairSize, pt.y), strokeWidth = 0.8.dp.toPx())
                                                drawLine(Color.Red, Offset(pt.x, pt.y - crosshairSize), Offset(pt.x, pt.y + crosshairSize), strokeWidth = 0.8.dp.toPx())

                                                drawCircle(color = PrimaryColor, center = pt, radius = dotRadiusPx)
                                                drawCircle(color = Color.White, center = pt, radius = dotRadiusPx, style = androidx.compose.ui.graphics.drawscope.Stroke(width = borderStrokePx))
                                            }
                                            PointTheme.MICRO_DOT -> {
                                                // Micro dot theme
                                                drawCircle(color = PrimaryColor, center = pt, radius = dotRadiusPx)
                                                drawCircle(color = Color.White, center = pt, radius = dotRadiusPx, style = androidx.compose.ui.graphics.drawscope.Stroke(width = borderStrokePx))
                                            }
                                            PointTheme.LINE_TICK -> {
                                                // Vertical end tick mark
                                                drawLine(Color.White, Offset(pt.x, pt.y - 5.dp.toPx()), Offset(pt.x, pt.y + 5.dp.toPx()), strokeWidth = 1.6.dp.toPx())
                                                drawLine(PrimaryColor, Offset(pt.x, pt.y - 5.dp.toPx()), Offset(pt.x, pt.y + 5.dp.toPx()), strokeWidth = 0.9.dp.toPx())
                                            }
                                        }
                                    }
                                }
                            }

                            // Touch & Drag Handles Overlay
                            if (isOverlayVisible) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(activeMode) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    isDraggingPoint = true
                                                    dragPointLocation = offset
                                                },
                                                onDragEnd = { isDraggingPoint = false },
                                                onDragCancel = { isDraggingPoint = false },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    isEditedManually = true
                                                    val w = size.width.toFloat()
                                                    val h = size.height.toFloat()

                                                    val currPosition = change.position
                                                    dragPointLocation = currPosition

                                                    val normDx = (dragAmount.x / scale) / w
                                                    val normDy = (dragAmount.y / scale) / h

                                                    val normTouchX = ((currPosition.x - dragAmount.x) / scale) / w
                                                    val normTouchY = ((currPosition.y - dragAmount.y) / scale) / h

                                                    when (activeMode) {
                                                        MeasurementMode.RIGHT_MPD -> {
                                                            val distMid = hypot(normTouchX - midline.x, normTouchY - midline.y)
                                                            val distRPupil = hypot(normTouchX - rightPupil.x, normTouchY - rightPupil.y)

                                                            if (distMid < distRPupil) {
                                                                midline = Point2D((midline.x + normDx).coerceIn(0f, 1f), (midline.y + normDy).coerceIn(0f, 1f))
                                                            } else {
                                                                rightPupil = Point2D((rightPupil.x + normDx).coerceIn(0f, 1f), (rightPupil.y + normDy).coerceIn(0f, 1f))
                                                            }
                                                        }
                                                        MeasurementMode.LEFT_MPD -> {
                                                            val distMid = hypot(normTouchX - midline.x, normTouchY - midline.y)
                                                            val distLPupil = hypot(normTouchX - leftPupil.x, normTouchY - leftPupil.y)

                                                            if (distMid < distLPupil) {
                                                                midline = Point2D((midline.x + normDx).coerceIn(0f, 1f), (midline.y + normDy).coerceIn(0f, 1f))
                                                            } else {
                                                                leftPupil = Point2D((leftPupil.x + normDx).coerceIn(0f, 1f), (leftPupil.y + normDy).coerceIn(0f, 1f))
                                                            }
                                                        }
                                                        MeasurementMode.IICD -> {
                                                            val distR = hypot(normTouchX - rightInnerCorner.x, normTouchY - rightInnerCorner.y)
                                                            val distL = hypot(normTouchX - leftInnerCorner.x, normTouchY - leftInnerCorner.y)

                                                            if (distR < distL) {
                                                                rightInnerCorner = Point2D((rightInnerCorner.x + normDx).coerceIn(0f, 1f), (rightInnerCorner.y + normDy).coerceIn(0f, 1f))
                                                            } else {
                                                                leftInnerCorner = Point2D((leftInnerCorner.x + normDx).coerceIn(0f, 1f), (leftInnerCorner.y + normDy).coerceIn(0f, 1f))
                                                            }
                                                        }
                                                        MeasurementMode.FACE_WIDTH -> {
                                                            val distR = hypot(normTouchX - rightCheekbone.x, normTouchY - rightCheekbone.y)
                                                            val distL = hypot(normTouchX - leftCheekbone.x, normTouchY - leftCheekbone.y)

                                                            if (distR < distL) {
                                                                rightCheekbone = Point2D((rightCheekbone.x + normDx).coerceIn(0f, 1f), (rightCheekbone.y + normDy).coerceIn(0f, 1f))
                                                            } else {
                                                                leftCheekbone = Point2D((leftCheekbone.x + normDx).coerceIn(0f, 1f), (leftCheekbone.y + normDy).coerceIn(0f, 1f))
                                                            }
                                                        }
                                                    }

                                                    // Update step values live as points move
                                                    rightMpdConfirmedVal = calcDistMm(midline, rightPupil)
                                                    leftMpdConfirmedVal = calcDistMm(midline, leftPupil)
                                                    iicdConfirmedVal = calcDistMm(rightInnerCorner, leftInnerCorner)
                                                    faceWidthConfirmedVal = calcDistMm(rightCheekbone, leftCheekbone)
                                                }
                                            )
                                        }
                                )
                            }

                            // Magnifier Lens
                            if (isOverlayVisible && isDraggingPoint) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .size(105.dp)
                                        .border(2.dp, PrimaryColor, CircleShape)
                                        .clip(CircleShape),
                                    shadowElevation = 8.dp
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawImage(
                                            image = imageBitmap,
                                            dstSize = IntSize(size.width.roundToInt() * 3, size.height.roundToInt() * 3)
                                        )
                                        drawLine(PrimaryColor, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 2f)
                                        drawLine(PrimaryColor, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 2f)
                                        drawCircle(PrimaryColor, center = Offset(size.width / 2f, size.height / 2f), radius = 3.dp.toPx())
                                    }
                                }
                            }

                            // Patient Side Badge
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp),
                                color = Color.White.copy(alpha = 0.95f),
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = "Patient's right = viewer's left",
                                    fontSize = 10.sp,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Floating Zoom Bar (Zoom In +, Zoom Out -, Reset 100%)
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = Color.White,
                                    shape = CircleShape,
                                    shadowElevation = 4.dp
                                ) {
                                    IconButton(
                                        onClick = { scale = (scale + 0.5f).coerceAtMost(5f) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = "Zoom In", tint = PrimaryColor, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Surface(
                                    color = Color.White,
                                    shape = CircleShape,
                                    shadowElevation = 4.dp
                                ) {
                                    IconButton(
                                        onClick = {
                                            scale = (scale - 0.5f).coerceAtLeast(1f)
                                            if (scale == 1f) {
                                                offsetX = 0f
                                                offsetY = 0f
                                            }
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Rounded.Remove, contentDescription = "Zoom Out", tint = PrimaryColor, modifier = Modifier.size(18.dp))
                                    }
                                }

                                if (scale > 1f) {
                                    Surface(
                                        color = PrimaryColor,
                                        shape = CircleShape,
                                        shadowElevation = 4.dp
                                    ) {
                                        IconButton(
                                            onClick = {
                                                scale = 1f
                                                offsetX = 0f
                                                offsetY = 0f
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset Zoom", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }

                            // Controls Row on Photo (Hide/Show Toggle, Fullscreen)
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    color = Color.White,
                                    shape = CircleShape,
                                    shadowElevation = 4.dp
                                ) {
                                    IconButton(
                                        onClick = { isOverlayVisible = !isOverlayVisible },
                                        modifier = Modifier.testTag("toggle_overlay_visibility_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isOverlayVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                            contentDescription = "Toggle Overlay",
                                            tint = if (isOverlayVisible) PrimaryColor else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    color = Color.White,
                                    shape = CircleShape,
                                    shadowElevation = 4.dp
                                ) {
                                    IconButton(onClick = { isFullScreenPhoto = !isFullScreenPhoto }) {
                                        Icon(
                                            imageVector = if (isFullScreenPhoto) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                            contentDescription = "Full Screen",
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sanity Warning Box
            if (isSanityAlert) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("sanity_warning_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningColor.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Warning",
                            tint = WarningColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Value out of range (${String.format("%.1f", rightMpdConfirmedVal)} mm)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "The scale may be wrong. Tap 'Set Scale' to fix.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Button(
                            onClick = {
                                irisP1 = null
                                irisP2 = null
                                calculatedIrisScale = 0f
                                dialogScale = 1f
                                dialogOffsetX = 0f
                                dialogOffsetY = 0f
                                showSetScaleDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Set Scale", fontSize = 11.sp, color = Color.Black)
                        }
                    }
                }
            }

            // Expandable / Sliding Bottom Options Sheet Panel (Crisp White Card)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Slide Handle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPanelExpanded = !isPanelExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.Gray.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier
                                .width(36.dp)
                                .height(5.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isPanelExpanded) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                            contentDescription = "Expand/Collapse",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Sliding Measurement Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MeasurementMode.entries.forEach { mode ->
                            val isModeConfirmed = when (mode) {
                                MeasurementMode.RIGHT_MPD -> isRightMpdConfirmed
                                MeasurementMode.LEFT_MPD -> isLeftMpdConfirmed
                                MeasurementMode.IICD -> isIicdConfirmed
                                MeasurementMode.FACE_WIDTH -> isFaceWidthConfirmed
                            }

                            FilterChip(
                                selected = activeMode == mode,
                                onClick = {
                                    activeMode = mode
                                    if (!isPanelExpanded) isPanelExpanded = true
                                },
                                label = {
                                    Text(
                                        text = if (isModeConfirmed) "${mode.label} ✓" else mode.label,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryColor,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.background
                                ),
                                modifier = Modifier.testTag("chip_${mode.name.lowercase()}")
                            )
                        }
                    }

                    // Line Theme Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Text("Style:", fontSize = 11.sp, color = Color.Gray)
                        PointTheme.entries.forEach { theme ->
                            FilterChip(
                                selected = activePointTheme == theme,
                                onClick = { activePointTheme = theme },
                                label = { Text(theme.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryColor.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryColor,
                                    containerColor = MaterialTheme.colorScheme.background
                                )
                            )
                        }
                    }

                    // Step-by-Step Confirmed Measurements Table
                    AnimatedVisibility(
                        visible = isPanelExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))

                            StepMeasurementRow(
                                label = "Right MPD",
                                isConfirmed = isRightMpdConfirmed,
                                valueMm = rightMpdConfirmedVal,
                                liveValMm = liveRightMpd,
                                isNormal = Calibration.isRightMpdNormal(if (isRightMpdConfirmed) rightMpdConfirmedVal else liveRightMpd)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            StepMeasurementRow(
                                label = "Left MPD",
                                isConfirmed = isLeftMpdConfirmed,
                                valueMm = leftMpdConfirmedVal,
                                liveValMm = liveLeftMpd,
                                isNormal = Calibration.isLeftMpdNormal(if (isLeftMpdConfirmed) leftMpdConfirmedVal else liveLeftMpd)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            StepMeasurementRow(
                                label = "Binocular PD",
                                isConfirmed = isBinocularConfirmed,
                                valueMm = binocularPdVal,
                                liveValMm = 0f,
                                isNormal = Calibration.isBinocularPdNormal(binocularPdVal),
                                isAuto = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            StepMeasurementRow(
                                label = "IICD",
                                isConfirmed = isIicdConfirmed,
                                valueMm = iicdConfirmedVal,
                                liveValMm = liveIicd,
                                isNormal = Calibration.isIicdNormal(if (isIicdConfirmed) iicdConfirmedVal else liveIicd)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            StepMeasurementRow(
                                label = "Face Width",
                                isConfirmed = isFaceWidthConfirmed,
                                valueMm = faceWidthConfirmedVal,
                                liveValMm = liveFaceWidth,
                                isNormal = Calibration.isFaceWidthNormal(if (isFaceWidthConfirmed) faceWidthConfirmedVal else liveFaceWidth)
                            )

                            // "Adjust Scale" Slider Card
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Adjust Scale",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${String.format("%.3f", activeMmPerPixel)} mm/px",
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryColor,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Slider(
                                        value = activeMmPerPixel,
                                        onValueChange = {
                                            activeMmPerPixel = it
                                            if (isRightMpdConfirmed) rightMpdConfirmedVal = calcDistMm(midline, rightPupil)
                                            if (isLeftMpdConfirmed) leftMpdConfirmedVal = calcDistMm(midline, leftPupil)
                                            if (isIicdConfirmed) iicdConfirmedVal = calcDistMm(rightInnerCorner, leftInnerCorner)
                                            if (isFaceWidthConfirmed) faceWidthConfirmedVal = calcDistMm(rightCheekbone, leftCheekbone)
                                        },
                                        valueRange = 0.05f..0.30f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = PrimaryColor,
                                            activeTrackColor = PrimaryColor
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("adjust_scale_slider")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Actions Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        confirmCurrentModeMeasurement()
                        Toast.makeText(context, "${activeMode.label} Confirmed ✓", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_measurement_step_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm ${activeMode.label}", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            midline = Point2D(0.50f, 0.45f)
                            rightPupil = Point2D(0.35f, 0.45f)
                            leftPupil = Point2D(0.65f, 0.45f)
                            rightInnerCorner = Point2D(0.42f, 0.46f)
                            leftInnerCorner = Point2D(0.58f, 0.46f)
                            rightCheekbone = Point2D(0.22f, 0.55f)
                            leftCheekbone = Point2D(0.78f, 0.55f)
                            isRightMpdConfirmed = false
                            isLeftMpdConfirmed = false
                            isIicdConfirmed = false
                            isFaceWidthConfirmed = false
                            activeMmPerPixel = settingsManager.mmPerPixel
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("reset_points_button"),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset All")
                    }

                    Button(
                        onClick = {
                            // Always calculate exact live values directly from current point locations & current scale
                            val finalRightMpd = calcDistMm(midline, rightPupil)
                            val finalLeftMpd = calcDistMm(midline, leftPupil)
                            val finalBinocularPd = Math.round((finalRightMpd + finalLeftMpd) * 10.0f) / 10.0f
                            val finalIicd = calcDistMm(rightInnerCorner, leftInnerCorner)
                            val finalFaceWidth = calcDistMm(rightCheekbone, leftCheekbone)

                            var targetPath = photoPath
                            if (bitmap != null) {
                                val annotatedBmp = ImageUtils.createAnnotatedBitmap(
                                    sourceBitmap = bitmap,
                                    midline = ImageUtils.PointF(midline.x, midline.y),
                                    rightPupil = ImageUtils.PointF(rightPupil.x, rightPupil.y),
                                    leftPupil = ImageUtils.PointF(leftPupil.x, leftPupil.y),
                                    rightInnerCorner = ImageUtils.PointF(rightInnerCorner.x, rightInnerCorner.y),
                                    leftInnerCorner = ImageUtils.PointF(leftInnerCorner.x, leftInnerCorner.y),
                                    rightCheekbone = ImageUtils.PointF(rightCheekbone.x, rightCheekbone.y),
                                    leftCheekbone = ImageUtils.PointF(leftCheekbone.x, leftCheekbone.y)
                                )
                                val savedAnnotatedPath = ImageUtils.saveBitmapToInternalStorage(context, annotatedBmp, "measured_annotated")
                                if (savedAnnotatedPath != null) {
                                    targetPath = savedAnnotatedPath
                                }
                            }

                            onNavigateToResult(
                                URLEncoder.encode(targetPath, "UTF-8"),
                                finalRightMpd,
                                finalLeftMpd,
                                finalBinocularPd,
                                finalIicd,
                                finalFaceWidth,
                                isEditedManually
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_measurements_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Finish Result", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // "Set Scale" Iris Mini-Calibration Dialog Mode with FREELY DRAGGABLE POINTS & ZOOM!
    if (showSetScaleDialog && bitmap != null) {
        // Initialize default points over iris if not set
        if (irisP1 == null) irisP1 = Point2D(0.38f, 0.45f)
        if (irisP2 == null) irisP2 = Point2D(0.44f, 0.45f)

        // Calculate initial scale if not set
        val initialDx = (irisP2!!.x - irisP1!!.x) * imageWidthPx
        val initialDy = (irisP2!!.y - irisP1!!.y) * imageHeightPx
        val initialIrisPx = hypot(initialDx, initialDy)
        if (initialIrisPx > 2f && calculatedIrisScale == 0f) {
            calculatedIrisScale = Calibration.AVERAGE_IRIS_DIAMETER_MM / initialIrisPx
        }

        AlertDialog(
            onDismissRequest = { showSetScaleDialog = false },
            title = {
                Column {
                    Text("Set Scale (Iris Calibration)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Drag P1 & P2 freely to align with the left and right iris edges.", fontSize = 12.sp, color = Color.Gray)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(if (isWhiteThemeCanvas) Color(0xFFF1F5F9) else Color.Black)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        val bmp = bitmap.asImageBitmap()

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = dialogScale,
                                    scaleY = dialogScale,
                                    translationX = dialogOffsetX,
                                    translationY = dialogOffsetY
                                )
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        dialogScale = (dialogScale * zoom).coerceIn(1f, 6f)
                                        if (dialogScale > 1f) {
                                            dialogOffsetX += pan.x
                                            dialogOffsetY += pan.y
                                        } else {
                                            dialogOffsetX = 0f
                                            dialogOffsetY = 0f
                                        }
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val w = size.width.toFloat()
                                        val h = size.height.toFloat()

                                        val normDx = (dragAmount.x / dialogScale) / w
                                        val normDy = (dragAmount.y / dialogScale) / h

                                        val currPos = change.position
                                        val normTouchX = ((currPos.x - dragAmount.x) / dialogScale) / w
                                        val normTouchY = ((currPos.y - dragAmount.y) / dialogScale) / h

                                        val p1 = irisP1 ?: Point2D(0.38f, 0.45f)
                                        val p2 = irisP2 ?: Point2D(0.44f, 0.45f)

                                        val dist1 = hypot(normTouchX - p1.x, normTouchY - p1.y)
                                        val dist2 = hypot(normTouchX - p2.x, normTouchY - p2.y)

                                        if (dist1 <= dist2) {
                                            irisP1 = Point2D((p1.x + normDx).coerceIn(0f, 1f), (p1.y + normDy).coerceIn(0f, 1f))
                                        } else {
                                            irisP2 = Point2D((p2.x + normDx).coerceIn(0f, 1f), (p2.y + normDy).coerceIn(0f, 1f))
                                        }

                                        // Recalculate scale live as points move
                                        val dx = ((irisP2?.x ?: 0f) - (irisP1?.x ?: 0f)) * imageWidthPx
                                        val dy = ((irisP2?.y ?: 0f) - (irisP1?.y ?: 0f)) * imageHeightPx
                                        val irisPx = hypot(dx, dy)
                                        if (irisPx > 2f) {
                                            calculatedIrisScale = Calibration.AVERAGE_IRIS_DIAMETER_MM / irisPx
                                        }
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(bmp, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))

                                val p1 = irisP1 ?: Point2D(0.38f, 0.45f)
                                val p2 = irisP2 ?: Point2D(0.44f, 0.45f)

                                val pt1 = Offset(p1.x * size.width, p1.y * size.height)
                                val pt2 = Offset(p2.x * size.width, p2.y * size.height)

                                // Connecting line between P1 and P2
                                drawLine(PrimaryColor, pt1, pt2, strokeWidth = 2.dp.toPx())

                                // Draggable Handle P1
                                drawCircle(Color.Red, radius = 6.dp.toPx(), center = pt1)
                                drawCircle(Color.White, radius = 6.dp.toPx(), center = pt1, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))

                                // Draggable Handle P2
                                drawCircle(Color.Red, radius = 6.dp.toPx(), center = pt2)
                                drawCircle(Color.White, radius = 6.dp.toPx(), center = pt2, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
                            }
                        }

                        // Floating Zoom Buttons inside Iris Calibration Dialog
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = Color.White,
                                shape = CircleShape,
                                shadowElevation = 4.dp
                            ) {
                                IconButton(
                                    onClick = { dialogScale = (dialogScale + 1f).coerceAtMost(6f) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Zoom In", tint = PrimaryColor, modifier = Modifier.size(16.dp))
                                }
                            }

                            Surface(
                                color = Color.White,
                                shape = CircleShape,
                                shadowElevation = 4.dp
                            ) {
                                IconButton(
                                    onClick = {
                                        dialogScale = (dialogScale - 1f).coerceAtLeast(1f)
                                        if (dialogScale == 1f) {
                                            dialogOffsetX = 0f
                                            dialogOffsetY = 0f
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Zoom Out", tint = PrimaryColor, modifier = Modifier.size(16.dp))
                                }
                            }

                            if (dialogScale > 1f) {
                                Surface(
                                    color = PrimaryColor,
                                    shape = CircleShape,
                                    shadowElevation = 4.dp
                                ) {
                                    IconButton(
                                        onClick = {
                                            dialogScale = 1f
                                            dialogOffsetX = 0f
                                            dialogOffsetY = 0f
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset Zoom", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (calculatedIrisScale > 0f) {
                        Text(
                            text = "Scale: ${String.format("%.3f", calculatedIrisScale)} mm/pixel",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryColor,
                            fontSize = 15.sp
                        )
                    } else {
                        Text(
                            text = "Drag P1 to left iris edge and P2 to right iris edge",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (calculatedIrisScale > 0f) {
                            activeMmPerPixel = calculatedIrisScale
                            settingsManager.mmPerPixel = calculatedIrisScale
                            Toast.makeText(context, "Scale Applied: ${String.format("%.3f", calculatedIrisScale)} mm/px ✓", Toast.LENGTH_SHORT).show()
                        }
                        showSetScaleDialog = false
                    },
                    enabled = calculatedIrisScale > 0f,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Text("Apply Scale")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetScaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StepMeasurementRow(
    label: String,
    isConfirmed: Boolean,
    valueMm: Float,
    liveValMm: Float,
    isNormal: Boolean,
    isAuto: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isConfirmed) {
                Text(
                    text = "${String.format("%.1f", valueMm)} mm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))

                if (isAuto) {
                    Text(
                        text = "(auto)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else if (isNormal) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Normal",
                        tint = SuccessColor,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "Out of range",
                        tint = WarningColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Text(
                    text = "-- mm",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    fontWeight = FontWeight.Normal
                )
                if (isAuto) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("(auto)", fontSize = 11.sp, color = Color.Gray)
                } else if (liveValMm > 0f) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("(${String.format("%.1f", liveValMm)} live)", fontSize = 11.sp, color = PrimaryColor)
                }
            }
        }
    }
}
