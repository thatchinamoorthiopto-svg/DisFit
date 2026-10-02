package com.example.ui.result

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.PatientRecord
import com.example.data.SettingsManager
import com.example.ui.theme.BorderColor
import com.example.ui.theme.PrimaryColor
import com.example.util.CsvExporter
import com.example.util.ImageUtils
import com.example.util.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    encodedPhotoPath: String,
    rightMpd: Float,
    leftMpd: Float,
    binocularPd: Float,
    iicd: Float,
    faceWidth: Float?,
    isEdited: Boolean,
    recordId: Long? = null,
    onNavigateBack: () -> Unit,
    onRecordSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val recordDao = remember { db.recordDao() }
    val settingsManager = remember { SettingsManager(context) }

    val photoPath = remember(encodedPhotoPath) { URLDecoder.decode(encodedPhotoPath, "UTF-8") }
    val sourceBitmap = remember(photoPath) { ImageUtils.loadBitmapFromFile(photoPath) }

    // Clean, empty inputs without pre-filled fake names or details
    var name by remember { mutableStateOf("") }
    var patientId by remember { mutableStateOf("") }
    var ageStr by remember { mutableStateOf("") }
    val dateMs = remember { System.currentTimeMillis() }
    val dateStr = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(dateMs)) }

    var annotatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var annotatedPath by remember { mutableStateOf<String?>(null) }
    var savedRecordId by remember { mutableStateOf<Long?>(recordId) }

    // Generate Annotated Bitmap cleanly from source with accurate points
    LaunchedEffect(sourceBitmap) {
        if (sourceBitmap != null) {
            withContext(Dispatchers.Default) {
                val annotated = ImageUtils.createAnnotatedBitmap(
                    sourceBitmap = sourceBitmap,
                    midline = ImageUtils.PointF(0.50f, 0.45f),
                    rightPupil = ImageUtils.PointF(0.35f, 0.45f),
                    leftPupil = ImageUtils.PointF(0.65f, 0.45f),
                    rightInnerCorner = ImageUtils.PointF(0.42f, 0.46f),
                    leftInnerCorner = ImageUtils.PointF(0.58f, 0.46f),
                    rightCheekbone = ImageUtils.PointF(0.22f, 0.55f),
                    leftCheekbone = ImageUtils.PointF(0.78f, 0.55f)
                )
                val path = ImageUtils.saveBitmapToInternalStorage(context, annotated, "annotated")
                annotatedBitmap = annotated
                annotatedPath = path
            }
        }
    }

    // Load existing record if editing an existing entry
    LaunchedEffect(recordId) {
        if (recordId != null && recordId > 0) {
            val record = withContext(Dispatchers.IO) { recordDao.getRecordByIdDirect(recordId) }
            if (record != null) {
                name = record.name
                patientId = record.patientId
                ageStr = if (record.age > 0) record.age.toString() else ""
            }
        }
    }

    fun buildRecord(): PatientRecord {
        val ageInt = ageStr.toIntOrNull() ?: 0
        return PatientRecord(
            id = savedRecordId ?: 0,
            name = name.ifBlank { "Patient" },
            patientId = patientId.ifBlank { "P-${System.currentTimeMillis() % 10000}" },
            age = ageInt,
            date = dateMs,
            rightMpd = rightMpd,
            leftMpd = leftMpd,
            binocularPd = binocularPd,
            iicd = iicd,
            faceWidth = faceWidth,
            mmPerPixel = settingsManager.mmPerPixel,
            captureDistCm = 60,
            photoPath = photoPath,
            annotatedPath = annotatedPath,
            isEditedManually = isEdited
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Result", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
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
            // Display Photo Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val displayBmp = annotatedBitmap ?: sourceBitmap
                    if (displayBmp != null) {
                        Image(
                            bitmap = displayBmp.asImageBitmap(),
                            contentDescription = "Annotated Photo Result",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Patient Info Input Form (Clean inputs, no fake pre-filled details)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Patient Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Patient Name") },
                        placeholder = { Text("Enter patient name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("patient_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = patientId,
                            onValueChange = { patientId = it },
                            label = { Text("Patient ID") },
                            placeholder = { Text("e.g. P1002") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("patient_id_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = ageStr,
                            onValueChange = { ageStr = it },
                            label = { Text("Age") },
                            placeholder = { Text("e.g. 28") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("patient_age_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Measurement Summary Table (Clean values, no state badges/method tags)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Measurements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ResultValueRow(label = "Right MPD", valueMm = rightMpd)
                    ResultValueRow(label = "Left MPD", valueMm = leftMpd)
                    ResultValueRow(label = "Binocular PD", valueMm = binocularPd, isBold = true)
                    ResultValueRow(label = "IICD", valueMm = iicd)
                    if (faceWidth != null) {
                        ResultValueRow(label = "Face Width", valueMm = faceWidth)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scale: ${String.format("%.3f", settingsManager.mmPerPixel)} mm/px",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Action Buttons Bar
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val record = buildRecord()
                            val id = recordDao.insertRecord(record)
                            savedRecordId = id
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Record Saved Successfully ✓", Toast.LENGTH_SHORT).show()
                                onRecordSaved()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_record_button"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Patient Record", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val record = buildRecord()
                                val uri = PdfExporter.generateAndSavePdf(context, record, annotatedBitmap)
                                withContext(Dispatchers.Main) {
                                    if (uri != null) {
                                        Toast.makeText(context, "PDF Report Created ✓", Toast.LENGTH_SHORT).show()
                                        PdfExporter.sharePdf(context, uri)
                                    } else {
                                        Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_pdf_button"),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export PDF", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val record = buildRecord()
                            CsvExporter.exportAndShareRecord(context, record)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_csv_button"),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ResultValueRow(
    label: String,
    valueMm: Float,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
        )

        Text(
            text = "${String.format("%.1f", valueMm)} mm",
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isBold) PrimaryColor else MaterialTheme.colorScheme.onSurface
        )
    }
}
