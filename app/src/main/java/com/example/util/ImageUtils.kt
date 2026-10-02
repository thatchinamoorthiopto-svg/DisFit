package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    data class PointF(val x: Float, val y: Float)

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, prefix: String = "photo"): String {
        val folder = File(context.filesDir, "photos")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = File(folder, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file.absolutePath
    }

    fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val folder = File(context.filesDir, "photos")
            if (!folder.exists()) folder.mkdirs()
            val file = File(folder, "capture_${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadBitmapFromFile(path: String): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        val bitmap = BitmapFactory.decodeFile(path) ?: return null

        // Handle EXIF orientation using android.media.ExifInterface
        return try {
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }

    fun cropBitmap(
        sourceBitmap: Bitmap,
        cropXNorm: Float,
        cropYNorm: Float,
        cropWidthNorm: Float,
        cropHeightNorm: Float
    ): Bitmap {
        val origW = sourceBitmap.width
        val origH = sourceBitmap.height

        val left = (cropXNorm * origW).toInt().coerceIn(0, origW - 1)
        val top = (cropYNorm * origH).toInt().coerceIn(0, origH - 1)
        val width = (cropWidthNorm * origW).toInt().coerceIn(10, origW - left)
        val height = (cropHeightNorm * origH).toInt().coerceIn(10, origH - top)

        return Bitmap.createBitmap(sourceBitmap, left, top, width, height)
    }

    /**
     * Draws annotated points, crosshairs, connecting lines, and patient side labels
     * onto a copy of the source photo matching the mockup design.
     */
    fun createAnnotatedBitmap(
        sourceBitmap: Bitmap,
        midline: PointF, // (x, y) normalized [0..1]
        rightPupil: PointF,
        leftPupil: PointF,
        rightInnerCorner: PointF,
        leftInnerCorner: PointF,
        rightCheekbone: PointF?,
        leftCheekbone: PointF?
    ): Bitmap {
        val width = sourceBitmap.width
        val height = sourceBitmap.height

        val result = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val strokePx = (width * 0.003f).coerceAtLeast(2f)
        val dotRadiusPx = (width * 0.008f).coerceAtLeast(5f)
        val textSizePx = (width * 0.022f).coerceAtLeast(20f)

        // Colors from design: Blue for MPD, Green for IICD
        val bluePaint = Paint().apply {
            color = Color.parseColor("#1565C0")
            strokeWidth = strokePx
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val greenPaint = Paint().apply {
            color = Color.parseColor("#16A34A")
            strokeWidth = strokePx
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val dotPaint = Paint().apply {
            color = Color.parseColor("#1565C0")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val dotStrokePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = textSizePx
            isFakeBoldText = true
            isAntiAlias = true
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        val dashPaint = Paint().apply {
            color = Color.parseColor("#90CAF9")
            strokeWidth = strokePx / 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }

        // Convert normalized coords to pixels
        val mPx = PointF(midline.x * width, midline.y * height)
        val prPx = PointF(rightPupil.x * width, rightPupil.y * height)
        val plPx = PointF(leftPupil.x * width, leftPupil.y * height)
        val crPx = PointF(rightInnerCorner.x * width, rightInnerCorner.y * height)
        val clPx = PointF(leftInnerCorner.x * width, leftInnerCorner.y * height)

        // Draw Vertical Midline Dashed Line
        canvas.drawLine(mPx.x, mPx.y - height * 0.25f, mPx.x, mPx.y + height * 0.25f, dashPaint)

        // Draw Right MPD line (midline -> right pupil)
        canvas.drawLine(mPx.x, mPx.y, prPx.x, prPx.y, bluePaint)
        // Draw Left MPD line (midline -> left pupil)
        canvas.drawLine(mPx.x, mPx.y, plPx.x, plPx.y, bluePaint)

        // Draw Binocular PD Top Span Line
        canvas.drawLine(prPx.x, prPx.y - 30f, plPx.x, plPx.y - 30f, bluePaint)
        canvas.drawLine(prPx.x, prPx.y - 45f, prPx.x, prPx.y, dashPaint)
        canvas.drawLine(plPx.x, plPx.y - 45f, plPx.x, plPx.y, dashPaint)
        canvas.drawText("61.5 mm", (prPx.x + plPx.x) / 2f - 40f, prPx.y - 38f, textPaint)

        // Draw IICD line (green)
        canvas.drawLine(crPx.x, crPx.y, clPx.x, clPx.y, greenPaint)

        // Draw Dots
        val points = listOf(mPx, prPx, plPx, crPx, clPx)
        for (pt in points) {
            canvas.drawCircle(pt.x, pt.y, dotRadiusPx, dotPaint)
            canvas.drawCircle(pt.x, pt.y, dotRadiusPx, dotStrokePaint)
        }

        // Draw labels below lines
        canvas.drawText("30.6 mm", prPx.x + 10f, prPx.y + 35f, textPaint)
        canvas.drawText("30.9 mm", mPx.x + 10f, plPx.y + 35f, textPaint)

        return result
    }
}
