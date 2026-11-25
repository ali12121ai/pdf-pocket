package com.pdfpocket.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdfpocket.scan.Recognizer
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class OcrLanguageTest {
    @Test
    fun recognizesEnglishAndArabicText() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmap = Bitmap.createBitmap(1000, 320, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint()
        paint.color = Color.BLACK
        paint.textSize = 100f
        canvas.drawText("HELLO", 60f, 110f, paint)
        val arabicLayout = android.text.StaticLayout.Builder
            .obtain("مرحبا", 0, 5, android.text.TextPaint(paint), 900)
            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(60f, 160f)
        arabicLayout.draw(canvas)
        canvas.restore()

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        val latch = CountDownLatch(1)
        var result = ""
        Recognizer.recognize(context, out.toByteArray()) { text ->
            result = text
            latch.countDown()
        }
        assertTrue("OCR callback timed out", latch.await(30, TimeUnit.SECONDS))
        assertTrue("English missing. OCR output: $result", result.contains("HELLO"))
        val hasArabicScript = result.any { it.code in 0x0600..0x06FF }
        assertTrue("No Arabic script in OCR output: $result", hasArabicScript)
    }
}
