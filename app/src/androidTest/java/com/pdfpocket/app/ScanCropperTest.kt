package com.pdfpocket.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdfpocket.scan.ScanCropper
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class ScanCropperTest {
    @Test
    fun detectsBrightMarginsAndCrops() {
        val bitmap = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawRect(90f, 120f, 510f, 680f, android.graphics.Paint().apply { color = Color.DKGRAY })

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        val jpeg = out.toByteArray()

        val margins = ScanCropper.autoMargins(jpeg)
        assertTrue("top=${margins[0]} right=${margins[1]} bottom=${margins[2]} left=${margins[3]}", margins[0] > 0 && margins[3] > 0)

        val cropped = ScanCropper.crop(jpeg, margins)
        val result = android.graphics.BitmapFactory.decodeByteArray(cropped, 0, cropped.size)
        assertTrue(result.width < 600)
        assertTrue(result.height < 800)
    }
}
