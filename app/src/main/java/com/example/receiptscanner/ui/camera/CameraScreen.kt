package com.example.receiptscanner.ui.camera

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.receiptscanner.domain.model.ScanState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onNavigateToReview: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToManualEntry: () -> Unit,
    onShareCsv: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scanState by viewModel.scanState.collectAsState()
    val scanCount by viewModel.scanCount.collectAsState()
    val stabilityProgress by viewModel.stabilityProgress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isScanningActive by viewModel.isScanningActive.collectAsState()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // ScanState.COMPLETEのとき確認画面へ遷移
    LaunchedEffect(scanState) {
        if (scanState == ScanState.COMPLETE) {
            onNavigateToReview()
        }
    }

    if (!cameraPermissionState.status.isGranted) {
        PermissionScreen(onRequestPermission = { cameraPermissionState.launchPermissionRequest() })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "出納帳スキャナー",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Filled.History, contentDescription = "履歴")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "設定")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // カメラプレビュー
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onImageCaptureReady = { capture -> imageCapture = capture },
                onFrameAnalysis = { bitmap ->
                    imageCapture?.let { capture ->
                        viewModel.onFrame(bitmap, capture)
                    }
                }
            )

            // ガイドオーバーレイ
            ScanningOverlay(
                scanState = scanState,
                stabilityProgress = stabilityProgress,
                modifier = Modifier.fillMaxSize()
            )

            // 処理中スピナー
            if (scanState == ScanState.PROCESSING) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Text(
                            text = "解析中...",
                            color = Color.White,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }

            // ステータスバー
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = scanStateMessage(scanState, isScanningActive),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                if (scanState == ScanState.STABILIZING || scanState == ScanState.DETECTING) {
                    LinearProgressIndicator(
                        progress = { stabilityProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = Color.Green
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "撮影枚数: ${scanCount}枚",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row {
                        OutlinedButton(
                            onClick = { viewModel.toggleScanning() },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            if (isScanningActive) {
                                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                                Spacer(Modifier.width(4.dp))
                                Text("停止")
                            } else {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Green)
                                Spacer(Modifier.width(4.dp))
                                Text("再開")
                            }
                        }

                        OutlinedButton(
                            onClick = onNavigateToManualEntry,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("手動入力")
                        }

                        OutlinedButton(onClick = onShareCsv) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CSV")
                        }
                    }
                }
            }

            // エラーメッセージ
            errorMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp, start = 16.dp, end = 16.dp)
                        .background(
                            Color.Red.copy(alpha = 0.8f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(12.dp)
                ) {
                    Text(text = msg, color = Color.White)
                }
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.clearError()
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onFrameAnalysis: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier,
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                onImageCaptureReady(imageCapture)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            val bitmap = imageProxy.toBitmap()
                            onFrameAnalysis(bitmap)
                            imageProxy.close()
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    // カメラバインドエラー
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@Composable
private fun ScanningOverlay(
    scanState: ScanState,
    stabilityProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val rectWidth = size.width * 0.85f
        val rectHeight = size.height * 0.5f
        val left = centerX - rectWidth / 2
        val top = centerY - rectHeight / 2

        // ガイド枠の色
        val borderColor = when (scanState) {
            ScanState.IDLE -> Color.White.copy(alpha = 0.6f)
            ScanState.DETECTING -> Color.Yellow.copy(alpha = 0.8f)
            ScanState.STABILIZING -> Color(0xFF00C853).copy(alpha = 0.9f)
            ScanState.CAPTURING, ScanState.COMPLETE -> Color.Green
            ScanState.PROCESSING -> Color.White.copy(alpha = 0.5f)
        }

        // 周囲を暗くする
        drawRect(Color.Black.copy(alpha = 0.4f))

        // 検出枠をクリア (透明にするためDrawScopeの操作)
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(rectWidth, rectHeight),
            cornerRadius = CornerRadius(12.dp.toPx()),
        )

        // 枠線
        drawRoundRect(
            color = borderColor,
            topLeft = Offset(left, top),
            size = Size(rectWidth, rectHeight),
            cornerRadius = CornerRadius(12.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )

        // 安定度に応じて枠の進捗を表示（コーナーハイライト）
        if (stabilityProgress > 0f) {
            val cornerLen = 40.dp.toPx() * stabilityProgress
            val stroke = Stroke(width = 5.dp.toPx())
            val c = Color.Green.copy(alpha = 0.9f)

            // 左上
            drawLine(c, Offset(left, top + cornerLen), Offset(left, top), strokeWidth = 5.dp.toPx())
            drawLine(c, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = 5.dp.toPx())

            // 右上
            drawLine(c, Offset(left + rectWidth - cornerLen, top), Offset(left + rectWidth, top), strokeWidth = 5.dp.toPx())
            drawLine(c, Offset(left + rectWidth, top), Offset(left + rectWidth, top + cornerLen), strokeWidth = 5.dp.toPx())

            // 左下
            drawLine(c, Offset(left, top + rectHeight - cornerLen), Offset(left, top + rectHeight), strokeWidth = 5.dp.toPx())
            drawLine(c, Offset(left, top + rectHeight), Offset(left + cornerLen, top + rectHeight), strokeWidth = 5.dp.toPx())

            // 右下
            drawLine(c, Offset(left + rectWidth - cornerLen, top + rectHeight), Offset(left + rectWidth, top + rectHeight), strokeWidth = 5.dp.toPx())
            drawLine(c, Offset(left + rectWidth, top + rectHeight - cornerLen), Offset(left + rectWidth, top + rectHeight), strokeWidth = 5.dp.toPx())
        }
    }
}

@Composable
private fun scanStateMessage(state: ScanState, isScanningActive: Boolean): String {
    if (!isScanningActive) return "スキャン停止中 - 「再開」で再スタート"
    return when (state) {
        ScanState.IDLE -> "レシートを枠内に合わせてください"
        ScanState.DETECTING -> "レシートを検出中..."
        ScanState.STABILIZING -> "静止してください..."
        ScanState.CAPTURING -> "読み取り中..."
        ScanState.PROCESSING -> "解析中..."
        ScanState.COMPLETE -> "読み取り完了！"
    }
}

@Composable
private fun PermissionScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "カメラの使用許可が必要です",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("権限を許可する")
            }
        }
    }
}
