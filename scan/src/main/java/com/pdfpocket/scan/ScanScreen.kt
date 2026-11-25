package com.pdfpocket.scan

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pdfpocket.ads.AdController
import com.pdfpocket.coreui.PrimaryButton
import com.pdfpocket.coreui.SecondaryButton
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun ScanScreen(modifier: Modifier = Modifier, onClose: () -> Unit = {}) {
    val vm: ScanViewModel = viewModel()
    val ui by vm.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProvider = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var ocrInput by remember { mutableStateOf("") }
    var margins by remember { mutableStateOf(intArrayOf(0, 0, 0, 0)) }
    var scanning by remember { mutableStateOf(true) }
    var detectionInFlight by remember { mutableStateOf(false) }
    val activity = context as? android.app.Activity

    val phase by animateFloatAsState(
        targetValue = 100f,
        animationSpec = infiniteRepeatable(tween(1400)),
        label = "scanBorder",
    )

    LaunchedEffect(ui.status) {
        if (ui.status.startsWith("Exported") && activity != null) {
            AdController.onUserAction(); AdController.onUserAction()
            AdController.maybeShowInterstitial(activity)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                val instance = ProcessCameraProvider.getInstance(context).await()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val capture = ImageCapture.Builder().build()
                instance.unbindAll()
                instance.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                cameraProvider.value = instance
                imageCapture.value = capture
                vm.onCameraReady()
            }
        } else {
            vm.onCameraError("Camera permission denied — enable it in Android settings")
        }
    }

    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    // Periodic edge detection while the camera is live.
    LaunchedEffect(ui.cameraReady, ui.reviewIndex) {
        if (!ui.cameraReady || ui.reviewIndex != null) return@LaunchedEffect
        while (true) {
            val capture = imageCapture.value
            if (capture != null && !detectionInFlight) {
                detectionInFlight = true
                kotlinx.coroutines.delay(1200)
                val detector = object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        try {
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            margins = ScanCropper.autoMargins(bytes)
                            scanning = false
                        } finally {
                            image.close()
                            detectionInFlight = false
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        detectionInFlight = false
                    }
                }
                capture.takePicture(executor, detector)
            } else {
                kotlinx.coroutines.delay(300)
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) vm.save(context, uri)
    }

    DisposableEffect(Unit) {
        onDispose { cameraProvider.value?.unbindAll(); executor.shutdown() }
    }

    Box(modifier.fillMaxSize()) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())

        // Active document border overlay.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val left = size.width * margins[3] / 100f
            val top = size.height * margins[0] / 100f
            val right = size.width * (100 - margins[1]) / 100f
            val bottom = size.height * (100 - margins[2]) / 100f
            drawRoundRect(
                color = if (scanning) Color(0xFF9CE8DE) else Color(0xFF18D3B6),
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f),
                style = Stroke(
                    width = 4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 14f), phase),
                ),
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (scanning) "Scanning for edges…" else "Document detected",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text("Done", color = Color.White) }
        }

        if (ui.reviewIndex == null) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = {
                        val capture = imageCapture.value
                        if (capture != null) {
                            capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val buffer = image.planes[0].buffer
                                    val bytes = ByteArray(buffer.remaining())
                                    buffer.get(bytes)
                                    image.close()
                                    vm.addPage(bytes)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    vm.onCameraError("Capture failed: ${exception.message}")
                                }
                            })
                        }
                    },
                    modifier = Modifier.size(84.dp),
                ) { Icon(Icons.Default.CameraAlt, contentDescription = "Capture") }
                Text(
                    ui.status + " · pages: " + ui.pageCount,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Text("Review crop", style = MaterialTheme.typography.titleLarge)
                    val bytes = vm.pageAt(ui.reviewIndex!!)
                    val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                    if (bitmap != null) {
                        Image(bitmap.asImageBitmap(), contentDescription = "Cropped page", modifier = Modifier.fillMaxWidth().height(200.dp))
                    }
                    CropSlider("Left", ui.margins[3]) { vm.adjustMargin(3, it) }
                    CropSlider("Right", ui.margins[1]) { vm.adjustMargin(1, it) }
                    CropSlider("Top", ui.margins[0]) { vm.adjustMargin(0, it) }
                    CropSlider("Bottom", ui.margins[2]) { vm.adjustMargin(2, it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.retake() }, modifier = Modifier.weight(1f)) { Text("Retake") }
                        Button(onClick = { vm.finishReview() }, modifier = Modifier.weight(1f)) { Text("Done cropping") }
                    }
                    if (ui.pageCount > 0 && ui.reviewIndex == null) {
                        PrimaryButton("Export ${ui.pageCount} page(s) to PDF") { vm.exportPdf() }
                        SecondaryButton("Save PDF", enabled = ui.output != null) { saveLauncher.launch("pdf-pocket-scan.pdf") }
                        OutlinedTextField(value = ocrInput, onValueChange = { ocrInput = it }, label = { Text("Editable OCR result") }, modifier = Modifier.fillMaxWidth())
                        SecondaryButton("Recognise text") {
                            val page = vm.lastPage()
                            if (page != null) Recognizer.recognize(context, page) { vm.setOcrText(it) }
                        }
                        SecondaryButton("Copy text") {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("OCR", ui.ocrText))
                        }
                        Text(ui.ocrText)
                    }
                }
            }
        }
    }
}

@Composable
private fun CropSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Text("$label: $value%", style = MaterialTheme.typography.bodySmall, color = Color.White)
    Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = 0f..30f)
}
