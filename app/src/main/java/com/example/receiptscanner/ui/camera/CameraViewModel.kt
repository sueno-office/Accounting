package com.example.receiptscanner.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.receiptscanner.data.repository.ReceiptRepository
import com.example.receiptscanner.data.repository.SettingsRepository
import com.example.receiptscanner.domain.CsvExporter
import com.example.receiptscanner.domain.GeminiReceiptAnalyzer
import com.example.receiptscanner.domain.ReceiptParser
import com.example.receiptscanner.domain.SoundPlayer
import com.example.receiptscanner.domain.StabilityDetector
import com.example.receiptscanner.domain.model.ParsedReceiptFields
import com.example.receiptscanner.domain.model.ScanState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stabilityDetector: StabilityDetector,
    private val receiptParser: ReceiptParser,
    private val geminiAnalyzer: GeminiReceiptAnalyzer,
    private val soundPlayer: SoundPlayer,
    private val csvExporter: CsvExporter,
    private val repository: ReceiptRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _scanCount = MutableStateFlow(0)
    val scanCount: StateFlow<Int> = _scanCount.asStateFlow()

    private val _parsedFields = MutableStateFlow<ParsedReceiptFields?>(null)
    val parsedFields: StateFlow<ParsedReceiptFields?> = _parsedFields.asStateFlow()

    private val _capturedPhotoPath = MutableStateFlow<String?>(null)
    val capturedPhotoPath: StateFlow<String?> = _capturedPhotoPath.asStateFlow()

    private val _nextPhotoNumber = MutableStateFlow(1)
    val nextPhotoNumber: StateFlow<Int> = _nextPhotoNumber.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val stabilityProgress = stabilityDetector.stabilityProgress

    private val executor = Executors.newSingleThreadExecutor()
    private val textRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
    private var isCapturing = false

    init {
        viewModelScope.launch {
            // 設定を読み込んでGeminiを初期化
            val apiKey = settingsRepository.geminiApiKey.first()
            if (apiKey.isNotBlank()) {
                geminiAnalyzer.initialize(apiKey)
            }

            val stabilityDuration = settingsRepository.stabilityDuration.first()
            stabilityDetector.setStabilityDuration(stabilityDuration)

            // 次の写真番号を初期化
            _nextPhotoNumber.value = repository.getNextPhotoNumber()
        }
    }

    /**
     * 毎フレーム呼び出される。安定判定して必要ならキャプチャする。
     */
    fun onFrame(bitmap: Bitmap, imageCapture: ImageCapture) {
        if (isCapturing || _scanState.value == ScanState.PROCESSING) return

        when (_scanState.value) {
            ScanState.IDLE, ScanState.DETECTING, ScanState.STABILIZING -> {
                _scanState.value = ScanState.DETECTING
                val isStable = stabilityDetector.analyzeFrame(bitmap)
                if (isStable) {
                    triggerCapture(imageCapture)
                } else {
                    val progress = stabilityDetector.stabilityProgress.value
                    _scanState.value = if (progress > 0.1f) ScanState.STABILIZING else ScanState.DETECTING
                }
            }
            else -> { /* 他の状態では何もしない */ }
        }
    }

    private fun triggerCapture(imageCapture: ImageCapture) {
        isCapturing = true
        _scanState.value = ScanState.CAPTURING

        val sessionDate = dateFormat.format(Date())
        val photoNumber = _nextPhotoNumber.value
        val photoFile = csvExporter.getNextPhotoFile(sessionDate, photoNumber)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    viewModelScope.launch {
                        // 効果音再生
                        val soundEnabled = settingsRepository.soundEnabled.first()
                        if (soundEnabled) {
                            soundPlayer.playScanComplete()
                        }

                        _scanState.value = ScanState.PROCESSING
                        _capturedPhotoPath.value = photoFile.absolutePath

                        // OCR実行
                        runOcr(photoFile, sessionDate, photoNumber)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    viewModelScope.launch {
                        _errorMessage.value = "撮影に失敗しました: ${exception.message}"
                        _scanState.value = ScanState.IDLE
                        isCapturing = false
                        stabilityDetector.reset()
                    }
                }
            }
        )
    }

    private suspend fun runOcr(photoFile: File, sessionDate: String, photoNumber: Int) {
        try {
            val bitmap = loadAndCorrectBitmap(photoFile)
            val image = InputImage.fromBitmap(bitmap, 0)

            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    viewModelScope.launch {
                        val ocrText = visionText.text
                        analyzeWithGemini(ocrText, sessionDate, photoNumber)
                    }
                }
                .addOnFailureListener { e ->
                    viewModelScope.launch {
                        // OCR失敗時はルールベースのみ
                        val fallback = receiptParser.parse("")
                        completeScan(fallback, sessionDate, photoNumber)
                    }
                }
        } catch (e: Exception) {
            val fallback = receiptParser.parse("")
            completeScan(fallback, sessionDate, photoNumber)
        }
    }

    private suspend fun analyzeWithGemini(
        ocrText: String,
        sessionDate: String,
        photoNumber: Int
    ) {
        val categories = settingsRepository.categories.first()

        val geminiResult = if (geminiAnalyzer.isInitialized()) {
            geminiAnalyzer.analyze(ocrText, categories)
        } else null

        val parsedFields = geminiResult ?: receiptParser.parse(ocrText)
        completeScan(parsedFields, sessionDate, photoNumber)
    }

    private suspend fun completeScan(
        fields: ParsedReceiptFields,
        sessionDate: String,
        photoNumber: Int
    ) {
        _parsedFields.value = fields
        _scanCount.value = _scanCount.value + 1
        _scanState.value = ScanState.COMPLETE
        isCapturing = false

        val skipReview = settingsRepository.skipReview.first()
        val hasLowConfidence = fields.confidence < 0.6f ||
            fields.category.isBlank() || fields.amountWithTax == 0L

        // スキップ設定ONかつ信頼度が高い場合のみ自動保存
        if (skipReview && !hasLowConfidence) {
            // ViewModelはナビゲーションを直接行えないので、状態でUIに通知
            // UIはCOMPLETEを検知して確認画面をスキップして自動保存する
        }
    }

    /**
     * カメラを再開してスキャン待機状態に戻す。
     */
    fun resetForNextScan() {
        _scanState.value = ScanState.IDLE
        _parsedFields.value = null
        _capturedPhotoPath.value = null
        stabilityDetector.reset()
        isCapturing = false

        viewModelScope.launch {
            _nextPhotoNumber.value = repository.getNextPhotoNumber()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun loadAndCorrectBitmap(file: File): Bitmap {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
        executor.shutdown()
        stabilityDetector.fullReset()
    }
}
