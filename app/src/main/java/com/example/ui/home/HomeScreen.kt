package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.PatientRecord
import com.example.data.SettingsManager
import com.example.ui.theme.BorderColor
import com.example.ui.theme.PrimaryColor
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.WarningColor
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToRecords: () -> Unit,
    onNavigateToCalibration: () -> Unit,
    onSelectRecord: ((String, Float, Float, Float, Float, Float?, Boolean, Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val db = remember { AppDatabase.getDatabase(context) }
    val recordDao = remember { db.recordDao() }

    val recentRecords by recordDao.getAllRecords().collectAsState(initial = emptyList())

    var isCalibrated by remember { mutableStateOf(false) }
    var mmPerPixel by remember { mutableStateOf(0.124f) }

    LaunchedEffect(Unit) {
        isCalibrated = settingsManager.isCalibrated
        mmPerPixel = settingsManager.mmPerPixel
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryColor,
                        selectedTextColor = PrimaryColor,
                        indicatorColor = PrimaryColor.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToCamera,
                    icon = { Icon(Icons.Rounded.PhotoCamera, contentDescription = "Capture") },
                    label = { Text("Capture", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_capture")
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToRecords,
                    icon = { Icon(Icons.Rounded.BarChart, contentDescription = "Records") },
                    label = { Text("Records", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_records")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // DisFit Branded Hero Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PrimaryColor,
                        modifier = Modifier.size(52.dp),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Visibility,
                                contentDescription = "DisFit Logo",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "DisFit",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Facial & Spectacle Fitting System",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = PrimaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Precision Eyewear Dispensing • Photo Pupil Distance",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }

            item {
                // Calibration Status Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToCalibration() }
                        .testTag("calibration_status_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCalibrated) SuccessColor.copy(alpha = 0.08f) else WarningColor.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isCalibrated) SuccessColor.copy(alpha = 0.3f) else WarningColor.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isCalibrated) SuccessColor else WarningColor,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isCalibrated) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = if (isCalibrated) "Phone Calibrated ✓" else "Not Calibrated",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Scale: ${String.format("%.3f", mmPerPixel)} mm/pixel • 60 cm",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            item {
                // Action Pill Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onNavigateToCamera,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("new_measurement_button"),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Measurement", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onNavigateToRecords,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("view_records_button"),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.Rounded.FormatListBulleted, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Records", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onNavigateToCalibration,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("calibrate_phone_button"),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calibrate Phone", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Recent Patients",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            // Recent Records Items
            val displayRecords = if (recentRecords.isNotEmpty()) recentRecords.take(5) else getSampleRecentRecords()

            items(displayRecords) { record ->
                RecentRecordCard(
                    record = record,
                    onClick = {
                        if (onSelectRecord != null && record.id > 0) {
                            val encodedPath = URLEncoder.encode(record.photoPath, "UTF-8")
                            onSelectRecord(
                                encodedPath,
                                record.rightMpd,
                                record.leftMpd,
                                record.binocularPd,
                                record.iicd,
                                record.faceWidth,
                                record.isEditedManually,
                                record.id
                            )
                        } else {
                            onNavigateToRecords()
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RecentRecordCard(
    record: PatientRecord,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(record.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = PrimaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = record.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = record.patientId,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun getSampleRecentRecords(): List<PatientRecord> {
    val now = System.currentTimeMillis()
    val day = 86400000L
    return listOf(
        PatientRecord(
            id = 0,
            name = "Ramesh Kumar",
            patientId = "P00123",
            age = 28,
            date = now,
            rightMpd = 30.6f,
            leftMpd = 30.9f,
            binocularPd = 61.5f,
            iicd = 31.8f,
            faceWidth = null,
            mmPerPixel = 0.124f,
            photoPath = ""
        ),
        PatientRecord(
            id = 0,
            name = "Anita Rao",
            patientId = "P00124",
            age = 32,
            date = now - day,
            rightMpd = 31.2f,
            leftMpd = 31.6f,
            binocularPd = 62.8f,
            iicd = 32.1f,
            faceWidth = null,
            mmPerPixel = 0.124f,
            photoPath = ""
        ),
        PatientRecord(
            id = 0,
            name = "John Mathew",
            patientId = "P00125",
            age = 45,
            date = now - (day * 3),
            rightMpd = 29.8f,
            leftMpd = 30.1f,
            binocularPd = 59.9f,
            iicd = 31.0f,
            faceWidth = null,
            mmPerPixel = 0.124f,
            photoPath = ""
        )
    )
}
