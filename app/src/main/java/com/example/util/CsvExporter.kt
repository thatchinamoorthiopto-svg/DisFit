package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.PatientRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportAndShareRecord(context: Context, record: PatientRecord) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(record.date))

        val csvContent = StringBuilder().apply {
            append("Parameter,Value,Unit,Method / Status\n")
            append("Patient Name,${record.name},,\n")
            append("Patient ID,${record.patientId},,\n")
            append("Age,${record.age},years,\n")
            append("Date,$dateStr,,\n")
            append("Right Monocular PD,${String.format("%.1f", record.rightMpd)},mm,Photo Measurement\n")
            append("Left Monocular PD,${String.format("%.1f", record.leftMpd)},mm,Photo Measurement\n")
            append("Binocular PD,${String.format("%.1f", record.binocularPd)},mm,Auto Calculated\n")
            append("Inter-Canthal Distance (IICD),${String.format("%.1f", record.iicd)},mm,Photo Measurement\n")
            append("Face Width,${if (record.faceWidth != null) String.format("%.1f", record.faceWidth) else "N/A"},mm,Photo Measurement\n")
            append("Calibration Scale,${String.format("%.3f", record.mmPerPixel)},mm/px,\n")
            append("Capture Distance,${record.captureDistCm},cm,Tripod 60cm\n")
            append("Manually Adjusted,${record.isEditedManually},,\n")
        }.toString()

        try {
            val file = File(context.cacheDir, "Record_${record.patientId}_${System.currentTimeMillis()}.csv")
            FileOutputStream(file).use {
                it.write(csvContent.toByteArray())
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Measurement CSV"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
