package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("facial_measure_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_CALIBRATED = "is_calibrated"
        private const val KEY_MM_PER_PIXEL = "mm_per_pixel"
        const val DEFAULT_MM_PER_PIXEL = 0.124f
        const val DEFAULT_CAPTURE_DIST_CM = 60
    }

    var isCalibrated: Boolean
        get() = prefs.getBoolean(KEY_IS_CALIBRATED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_CALIBRATED, value).apply()

    var mmPerPixel: Float
        get() = prefs.getFloat(KEY_MM_PER_PIXEL, DEFAULT_MM_PER_PIXEL)
        set(value) {
            prefs.edit().putFloat(KEY_MM_PER_PIXEL, value).apply()
            isCalibrated = true
        }

    fun saveCalibration(scale: Float) {
        mmPerPixel = scale
        isCalibrated = true
    }

    fun getStatusString(): String {
        return if (isCalibrated) {
            "Phone calibrated ✓ (${String.format("%.3f", mmPerPixel)} mm/px)"
        } else {
            "Not calibrated — tap Calibrate"
        }
    }
}
