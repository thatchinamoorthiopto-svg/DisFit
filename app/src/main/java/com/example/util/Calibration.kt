package com.example.util

import kotlin.math.hypot

object Calibration {

    const val AVERAGE_IRIS_DIAMETER_MM = 11.7f

    data class NormalRange(
        val mean: Float,
        val stdDev: Float
    ) {
        val minNormal: Float get() = mean - stdDev
        val maxNormal: Float get() = mean + stdDev

        fun isNormal(value: Float): Boolean {
            return value in minNormal..maxNormal
        }
    }

    val RIGHT_MPD_RANGE = NormalRange(30.7f, 4.2f)
    val LEFT_MPD_RANGE = NormalRange(30.8f, 3.8f)
    val BINOCULAR_PD_RANGE = NormalRange(61.6f, 7.6f)
    val IICD_RANGE = NormalRange(31.9f, 6.6f)
    val FACE_WIDTH_RANGE = NormalRange(115.8f, 16.6f)

    fun calculateMm(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        mmPerPixel: Float,
        imageWidthPx: Float
    ): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val distPx = hypot(dx, dy)
        val valueMm = distPx * mmPerPixel
        return (Math.round(valueMm * 10.0f) / 10.0f)
    }

    fun calculateMmFromPixels(distPx: Float, mmPerPixel: Float): Float {
        val valueMm = distPx * mmPerPixel
        return (Math.round(valueMm * 10.0f) / 10.0f)
    }

    fun isRightMpdNormal(valMm: Float): Boolean = RIGHT_MPD_RANGE.isNormal(valMm)
    fun isLeftMpdNormal(valMm: Float): Boolean = LEFT_MPD_RANGE.isNormal(valMm)
    fun isBinocularPdNormal(valMm: Float): Boolean = BINOCULAR_PD_RANGE.isNormal(valMm)
    fun isIicdNormal(valMm: Float): Boolean = IICD_RANGE.isNormal(valMm)
    fun isFaceWidthNormal(valMm: Float): Boolean = FACE_WIDTH_RANGE.isNormal(valMm)

    fun isSanityWarning(rightMpd: Float, leftMpd: Float, iicd: Float, faceWidth: Float?): Boolean {
        val isRightMpdOff = rightMpd < 25.0f || rightMpd > 38.0f
        val isLeftMpdOff = leftMpd < 25.0f || leftMpd > 38.0f
        val isIicdOff = iicd < 25.0f || iicd > 40.0f
        val isFaceWidthOff = faceWidth != null && (faceWidth < 100.0f || faceWidth > 140.0f)
        return isRightMpdOff || isLeftMpdOff || isIicdOff || isFaceWidthOff
    }
}
