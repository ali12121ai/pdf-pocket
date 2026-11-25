package com.pdfpocket.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File

/** English via ML Kit, Arabic via on-device Tesseract with bundled ara.traineddata. */
object Recognizer {
    fun recognize(context: Context, jpeg: ByteArray, onResult: (String) -> Unit) {
        val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        if (bitmap == null) {
            onResult("OCR failed: image could not be decoded")
            return
        }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { latin ->
                val arabic = runCatching { arabicText(context, bitmap) }.onFailure { android.util.Log.e("PDFPocketOcr", "arabic failed", it) }.getOrDefault("")
                val parts = listOf(latin.text, arabic)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                onResult(parts.joinToString("\n").ifBlank { "No text found" })
            }
            .addOnFailureListener { error ->
                val arabic = runCatching { arabicText(context, bitmap) }.onFailure { android.util.Log.e("PDFPocketOcr", "arabic failed", it) }.getOrDefault("")
                onResult(arabic.ifBlank { "OCR failed: ${error.message}" })
            }
    }

    private fun arabicText(context: Context, bitmap: Bitmap): String {
        android.util.Log.d("PDFPocketOcr", "arabicText start")
        val root = File(context.filesDir, "tesseract")
        val tessData = File(root, "tessdata")
        tessData.mkdirs()
        (context.assets.list("tessdata") ?: emptyArray()).forEach { name ->
            val target = File(tessData, name)
            val size = context.assets.open("tessdata/$name").use { it.available() }
            if (!target.exists() || target.length() != size.toLong()) {
                context.assets.open("tessdata/$name").use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
        val api = TessBaseAPI()
        return try {
            val ok = api.init(root.absolutePath, "ara")
            if (!ok) return ""
            api.setImage(bitmap)
            val text = api.getUTF8Text() ?: ""
            text
        } finally {
            api.end()
        }
    }
}
