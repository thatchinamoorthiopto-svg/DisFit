package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patient_records")
data class PatientRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val patientId: String,
    val age: Int,
    val date: Long = System.currentTimeMillis(),
    val rightMpd: Float,
    val leftMpd: Float,
    val binocularPd: Float, // auto = rightMpd + leftMpd
    val iicd: Float,
    val faceWidth: Float? = null,
    val mmPerPixel: Float,
    val captureDistCm: Int = 60,
    val photoPath: String,
    val annotatedPath: String? = null,
    val isEditedManually: Boolean = false
)
