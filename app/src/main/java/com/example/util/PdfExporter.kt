package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.PatientRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun generateAndSavePdf(context: Context, record: PatientRecord, annotatedBitmap: Bitmap?): Uri? {
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val primaryPaint = Paint().apply {
            color = Color.parseColor("#1565C0")
            isAntiAlias = true
        }

        val textPrimary = Paint().apply {
            color = Color.parseColor("#1A1A1A")
            textSize = 12f
            isAntiAlias = true
        }

        val textTitle = Paint().apply {
            color = Color.parseColor("#1565C0")
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val textSubtitle = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 11f
            isAntiAlias = true
        }

        val textHeader = Paint().apply {
            color = Color.parseColor("#1565C0")
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val tableHeaderBg = Paint().apply {
            color = Color.parseColor("#F7F9FC")
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val successPaint = Paint().apply {
            color = Color.parseColor("#16A34A")
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val warningPaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }

        var currentY = 40f

        // --- 1. HEADER ---
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 12f, primaryPaint)

        canvas.drawText("FACIAL MEASUREMENT REPORT", 40f, currentY + 16f, textTitle)
        currentY += 24f
        canvas.drawText("Facial Measurement & Spectacle Fitting System • Accurate • Personalized", 40f, currentY, textSubtitle)
        currentY += 20f

        canvas.drawLine(40f, currentY, pageWidth - 40f, currentY, borderPaint)
        currentY += 16f

        // --- 2. PATIENT DETAILS ---
        canvas.drawText("Patient Details", 40f, currentY, textHeader)
        currentY += 16f

        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(record.date))

        val detailsRect = RectF(40f, currentY, pageWidth - 40f, currentY + 54f)
        canvas.drawRoundRect(detailsRect, 8f, 8f, tableHeaderBg)
        canvas.drawRoundRect(detailsRect, 8f, 8f, borderPaint)

        val col1X = 52f
        val col2X = 300f
        var detailY = currentY + 20f

        canvas.drawText("Name  : ${record.name}", col1X, detailY, textPrimary)
        canvas.drawText("Age     : ${record.age} yrs", col2X, detailY, textPrimary)
        detailY += 20f
        canvas.drawText("ID        : ${record.patientId}", col1X, detailY, textPrimary)
        canvas.drawText("Date    : $dateStr", col2X, detailY, textPrimary)

        currentY += 70f

        // --- 3. MEASUREMENTS TABLE ---
        canvas.drawText("Measurements", 40f, currentY, textHeader)
        currentY += 12f

        val tableLeft = 40f
        val tableRight = pageWidth - 40f
        val rowHeight = 24f

        // Table Header
        val headerRect = RectF(tableLeft, currentY, tableRight, currentY + rowHeight)
        canvas.drawRect(headerRect, tableHeaderBg)
        canvas.drawRect(headerRect, borderPaint)

        val colNameX = tableLeft + 12f
        val colValueX = tableLeft + 260f
        val colStatusX = tableLeft + 380f

        textPrimary.isFakeBoldText = true
        canvas.drawText("Parameter", colNameX, currentY + 16f, textPrimary)
        canvas.drawText("Measured Value", colValueX, currentY + 16f, textPrimary)
        canvas.drawText("Status / Method", colStatusX, currentY + 16f, textPrimary)
        textPrimary.isFakeBoldText = false

        currentY += rowHeight

        val rows = listOf(
            Triple("Right Monocular PD", "${String.format("%.1f", record.rightMpd)} mm", if (Calibration.isRightMpdNormal(record.rightMpd)) "✓ Normal" else "⚠ Out of range"),
            Triple("Left Monocular PD", "${String.format("%.1f", record.leftMpd)} mm", if (Calibration.isLeftMpdNormal(record.leftMpd)) "✓ Normal" else "⚠ Out of range"),
            Triple("Binocular PD", "${String.format("%.1f", record.binocularPd)} mm", "(auto = Right + Left)"),
            Triple("Inter-Canthal Dist. (IICD)", "${String.format("%.1f", record.iicd)} mm", if (Calibration.isIicdNormal(record.iicd)) "✓ Normal" else "⚠ Out of range"),
            Triple(
                "Face Width",
                if (record.faceWidth != null) "${String.format("%.1f", record.faceWidth)} mm" else "--",
                if (record.faceWidth == null) "Not measured" else if (Calibration.isFaceWidthNormal(record.faceWidth)) "✓ Normal" else "⚠ Out of range"
            )
        )

        for ((name, valStr, status) in rows) {
            val rRect = RectF(tableLeft, currentY, tableRight, currentY + rowHeight)
            canvas.drawRect(rRect, borderPaint)

            canvas.drawText(name, colNameX, currentY + 16f, textPrimary)
            canvas.drawText(valStr, colValueX, currentY + 16f, textPrimary)

            val stPaint = when {
                status.startsWith("✓") -> successPaint
                status.startsWith("⚠") -> warningPaint
                else -> textSubtitle
            }
            canvas.drawText(status, colStatusX, currentY + 16f, stPaint)

            currentY += rowHeight
        }

        currentY += 12f

        // Calibration & Notes
        canvas.drawText("Calibration Scale  : ${String.format("%.3f", record.mmPerPixel)} mm/pixel", 40f, currentY + 12f, textSubtitle)
        canvas.drawText("Capture Distance   : ${record.captureDistCm} cm (Expected uncertainty: ±1.0 mm)", 300f, currentY + 12f, textSubtitle)
        if (record.isEditedManually) {
            currentY += 14f
            canvas.drawText("* Note: Points were manually adjusted/fine-tuned on photo.", 40f, currentY + 12f, warningPaint)
        }
        currentY += 28f

        // --- 4. ANNOTATED PHOTO ---
        if (annotatedBitmap != null) {
            canvas.drawText("Annotated Facial Photo", 40f, currentY, textHeader)
            currentY += 10f

            val availableWidth = (pageWidth - 80).toFloat()
            val availableHeight = pageHeight - currentY - 50f

            val bitmapRatio = annotatedBitmap.height.toFloat() / annotatedBitmap.width.toFloat()
            var targetWidth = availableWidth
            var targetHeight = targetWidth * bitmapRatio

            if (targetHeight > availableHeight) {
                targetHeight = availableHeight
                targetWidth = targetHeight / bitmapRatio
            }

            val photoLeft = (pageWidth - targetWidth) / 2f
            val dstRect = RectF(photoLeft, currentY, photoLeft + targetWidth, currentY + targetHeight)

            canvas.drawBitmap(annotatedBitmap, null, dstRect, null)
            canvas.drawRect(dstRect, borderPaint)
            currentY += targetHeight + 10f
        }

        // --- 5. FOOTER ---
        canvas.drawLine(40f, pageHeight - 35f, pageWidth - 40f, pageHeight - 35f, borderPaint)
        canvas.drawText("Generated by Facial Measurement App • $dateStr", 40f, pageHeight - 20f, textSubtitle)

        document.finishPage(page)

        // Save PDF to Downloads directory
        return savePdfToStorage(context, document, "Report_${record.patientId}_${System.currentTimeMillis()}.pdf")
    }

    private fun savePdfToStorage(context: Context, document: PdfDocument, filename: String): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/FacialMeasurement")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        document.writeTo(out)
                    }
                    document.close()
                    uri
                } else {
                    document.close()
                    null
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(downloadsDir, "FacialMeasurement")
                if (!appDir.exists()) appDir.mkdirs()

                val pdfFile = File(appDir, filename)
                FileOutputStream(pdfFile).use { out ->
                    document.writeTo(out)
                }
                document.close()

                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }

    fun sharePdf(context: Context, uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Measurement Report PDF"))
    }
}
