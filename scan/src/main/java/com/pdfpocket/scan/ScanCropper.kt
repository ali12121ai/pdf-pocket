package com.pdfpocket.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** On-device document crop: finds bright margins, then applies user adjustments. */
object ScanCropper {
    fun autoMargins(jpeg: ByteArray): IntArray {
        val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return intArrayOf(0, 0, 0, 0)
        val w = bitmap.width
        val h = bitmap.height
        if (w < 8 || h < 8) return intArrayOf(0, 0, 0, 0)

        fun bright(x: Int, y: Int): Boolean {
            val color = bitmap.getPixel(x, y)
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            return (r + g + b) / 3 > 215
        }

        fun rowIsBackground(y: Int): Boolean {
            var hits = 0
            var samples = 0
            for (x in 0 until w step max(1, w / 24)) {
                samples++
                if (bright(x, y)) hits++
            }
            return samples > 0 && hits.toFloat() / samples > 0.9f
        }

        fun colIsBackground(x: Int): Boolean {
            var hits = 0
            var samples = 0
            for (y in 0 until h step max(1, h / 24)) {
                samples++
                if (bright(x, y)) hits++
            }
            return samples > 0 && hits.toFloat() / samples > 0.9f
        }

        var top = 0
        while (top < h / 3 && rowIsBackground(top)) top++
        var bottom = 0
        while (bottom < h / 3 && rowIsBackground(h - 1 - bottom)) bottom++
        var left = 0
        while (left < w / 3 && colIsBackground(left)) left++
        var right = 0
        while (right < w / 3 && colIsBackground(w - 1 - right)) right++

        val pct = { value: Int, total: Int -> (value * 100 / total).coerceAtMost(30) }
        return intArrayOf(pct(top, h), pct(right, w), pct(bottom, h), pct(left, w))
    }

    fun crop(jpeg: ByteArray, margins: IntArray): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return jpeg
        val w = bitmap.width
        val h = bitmap.height
        val left = (margins[3].coerceAtLeast(0) * w) / 100
        val right = (margins[1].coerceAtLeast(0) * w) / 100
        val top = (margins[0].coerceAtLeast(0) * h) / 100
        val bottom = (margins[2].coerceAtLeast(0) * h) / 100
        val cropW = w - left - right
        val cropH = h - top - bottom
        if (cropW < 16 || cropH < 16) return jpeg
        val cropped = Bitmap.createBitmap(bitmap, left, top, cropW, cropH)
        val out = ByteArrayOutputStream()
        cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
        bitmap.recycle()
        return out.toByteArray()
    }
}
