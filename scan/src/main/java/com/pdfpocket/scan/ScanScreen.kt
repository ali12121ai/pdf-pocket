package com.pdfpocket.scan

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pdfpocket.coreui.PrimaryButton
import com.pdfpocket.coreui.SecondaryButton
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@Composable
fun ScanScreen(modifier: Modifier = Modifier, onClose: () -> Unit = {}) {
    val vm: ScanViewModel = viewModel()
    val ui by vm.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }
    val cameraProvider = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var margins by remember { mutableStateOf(intArrayOf(0, 0, 0, 0)) }
    var scanning by remember { mutableStateOf(true) }
    var showDiscard by remember { mutableStateOf(false) }
    var ocrInput by remember { mutableStateOf("") }
    val activity = context as? android.app.Activity

    val transition = rememberInfiniteTransition(label = "scanBorder")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 76f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "dashPhase",
    )

    LaunchedEffect(Unit) {
        if (activity != null) {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            WindowInsetsControllerCompat(activity.window, activity.window.decorView).hide(
                androidx.core.view.WindowInsetsCompat.Type.systemBars(),
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider.value?.unbindAll()
            executor.shutdown()
            if (activity != null) {
                WindowCompat.setDecorFitsSystemWindows(activity.window, true)
                WindowInsetsControllerCompat(activity.window, activity.window.decorView).show(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars(),
                )
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) vm.save(context, uri)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                val instance = ProcessCameraProvider.getInstance(context).await()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val capture = ImageCapture.Builder().build()
                var lastAnalysis = 0L
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { image: ImageProxy ->
                    val now = System.currentTimeMillis()
                    if (now - lastAnalysis < 200) {
                        image.close()
                        return@setAnalyzer
                    }
                    lastAnalysis = now
                    try {
                        val plane = image.planes[0]
                        val w = (image.width / 4).coerceAtLeast(1)
                        val h = (image.height / 4).coerceAtLeast(1)
                        val luma = ByteArray(w * h)
                        for (y in 0 until h) {
                            for (x in 0 until w) {
                                luma[y * w + x] = plane.buffer.get(y * plane.rowStride + x * plane.pixelStride)
                            }
                        }
                        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
                        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(luma))
                        val out = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                        bitmap.recycle()
                        margins = ScanCropper.autoMargins(out.toByteArray())
                        scanning = false
                    } catch (_: Exception) {
                        scanning = true
                    } finally {
                        image.close()
                    }
                }
                instance.unbindAll()
                instance.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture, analysis)
                cameraProvider.value = instance
                imageCapture.value = capture
                vm.onCameraReady()
            }
        } else {
            vm.onCameraError("Camera permission denied — open Settings to allow it")
        }
    }
    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    fun requestClose() {
        if (ui.pageCount > 0 && ui.output == null) showDiscard = true else onClose()
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Discard ${ui.pageCount} pages?") },
            text = { Text("Captured pages have not been exported.") },
            confirmButton = {
                TextButton(onClick = { vm.discard(); showDiscard = false; onClose() }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Keep scanning") } },
        )
    }

    when {
        ui.output != null -> {
            ScanResultScreen(
                count = ui.pageCount,
                onSave = { saveLauncher.launch("scan-${System.currentTimeMillis()}.pdf") },
                onShare = {
                    val bytes = ui.output ?: return@ScanResultScreen
                    val file = java.io.File(context.cacheDir, "scan-share.pdf")
                    file.writeBytes(bytes)
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share scan"))
                },
                onDone = { requestClose() },
                modifier = modifier,
            )
        }
        ui.pagesOpen -> {
            PagesScreen(vm, ui, ocrInput, { ocrInput = it }, context, modifier)
        }
        ui.reviewIndex != null -> {
            ReviewScreen(vm, ui, modifier)
        }
        else -> {
            CameraScreen(
                vm, ui, previewView, imageCapture, executor,
                { margins }, { scanning }, { requestClose() }, phase,
                modifier,
            )
        }
    }
}

@Composable
private fun CameraScreen(
    vm: ScanViewModel,
    ui: ScanUiState,
    previewView: PreviewView,
    imageCapture: androidx.compose.runtime.MutableState<ImageCapture?>,
    executor: java.util.concurrent.ExecutorService,
    marginsProvider: () -> IntArray,
    scanningProvider: () -> Boolean,
    onClose: () -> Unit,
    phase: Float,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val margins = marginsProvider()
    val scanning = scanningProvider()
    Box(modifier.fillMaxSize()) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val left = size.width * margins[3] / 100f
            val top = size.height * margins[0] / 100f
            val right = size.width * (100 - margins[1]) / 100f
            val bottom = size.height * (100 - margins[2]) / 100f
            drawRoundRect(
                color = if (scanning) Color(0xFF9CE8DE) else Color(0xFF18D3B6),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f),
                style = Stroke(
                    width = 4.dp.toPx(),
                    pathEffect = if (scanning) PathEffect.dashPathEffect(floatArrayOf(24f, 14f), -phase) else null,
                ),
            )
        }
        Row(Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopStart), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (scanning) "Scanning for edges…" else "Document found",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text("Close", color = Color.White) }
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = {
                    imageCapture.value?.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
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
                },
                modifier = Modifier.size(84.dp),
            ) { Icon(Icons.Default.CameraAlt, contentDescription = "Capture") }
            Text(
                ui.status + " · pages: " + ui.pageCount,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
            if (ui.pageCount > 0) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.openPages() }) { Text("Done (${ui.pageCount})") }
            }
            if (!ui.cameraReady && ui.status.contains("denied")) {
                TextButton(onClick = {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:${context.packageName}"),
                    )
                    context.startActivity(intent)
                }) {
                    Text("Open settings", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ReviewScreen(vm: ScanViewModel, ui: ScanUiState, modifier: Modifier) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxSize().padding(12.dp),
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Review crop", style = MaterialTheme.typography.headlineSmall)
            val bytes = ui.reviewIndex?.let { vm.pageAt(it) }
            val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), contentDescription = "Cropped page", modifier = Modifier.fillMaxWidth().height(280.dp))
            }
            CropSlider("Left", ui.margins[3]) { vm.adjustMargin(3, it) }
            CropSlider("Right", ui.margins[1]) { vm.adjustMargin(1, it) }
            CropSlider("Top", ui.margins[0]) { vm.adjustMargin(0, it) }
            CropSlider("Bottom", ui.margins[2]) { vm.adjustMargin(2, it) }
            Text("Drag the corner sliders until the document fits.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.retake() }, Modifier.weight(1f)) { Text("Retake") }
                Button(onClick = { vm.finishReview() }, Modifier.weight(1f)) { Text("Add page") }
            }
        }
    }
}

@Composable
private fun PagesScreen(
    vm: ScanViewModel,
    ui: ScanUiState,
    ocrInput: String,
    onOcrInput: (String) -> Unit,
    context: Context,
    modifier: Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Pages (${ui.pageCount})", style = MaterialTheme.typography.headlineSmall)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().height(320.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ui.pageCount) { index ->
                val bitmap = vm.pageAt(index)?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                Box(Modifier.size(90.dp).clickable { vm.removeAt(index) }) {
                    if (bitmap != null) Image(bitmap.asImageBitmap(), contentDescription = "Page ${index + 1}", modifier = Modifier.fillMaxSize())
                    Text("${index + 1} · remove", color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Export PDF") { vm.exportPdf() }
        SecondaryButton("Recognise text") {
            val page = vm.lastPage()
            if (page != null) Recognizer.recognize(context, page) { vm.setOcrText(it) }
        }
        OutlinedTextField(value = ocrInput, onValueChange = onOcrInput, label = { Text("Editable OCR result") }, modifier = Modifier.fillMaxWidth())
        SecondaryButton("Copy text") {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("OCR", ui.ocrText))
        }
        Text(ui.ocrText)
        TextButton(onClick = { vm.closePages() }) { Text("Back to camera") }
    }
}

@Composable
private fun ScanResultScreen(
    count: Int,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Scan result", style = MaterialTheme.typography.headlineSmall)
        Text("$count page(s) exported to PDF")
        PrimaryButton("Save PDF") { onSave() }
        SecondaryButton("Share PDF") { onShare() }
        TextButton(onDone) { Text("Done") }
    }
}

@Composable
private fun CropSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Text("$label: $value%", style = MaterialTheme.typography.bodySmall)
    Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = 0f..30f)
}
