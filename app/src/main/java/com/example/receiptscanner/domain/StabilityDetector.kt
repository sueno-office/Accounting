package com.example.receiptscanner.domain

import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * フレーム差分に基づいてカメラ映像の安定性を検出するクラス。
 * 指定時間（デフォルト1.5秒）以上静止したら安定と判断してトリガーする。
 */
@Singleton
class StabilityDetector @Inject constructor() {

    companion object {
        private const val DIFF_THRESHOLD = 0.025f  // 2.5%差分以内で安定と判断
        private const val SAMPLE_SIZE = 64          // ダウンサンプリングサイズ
    }

    private var lastBitmap: Bitmap? = null
    private var stableFrameCount = 0
    private var totalFrameCount = 0
    private var requiredStableFrames = 45  // 1.5秒 @ 30fps

    private val _stabilityProgress = MutableStateFlow(0f)
    val stabilityProgress: StateFlow<Float> = _stabilityProgress.asStateFlow()

    private val _isStable = MutableStateFlow(false)
    val isStable: StateFlow<Boolean> = _isStable.asStateFlow()

    fun setStabilityDuration(seconds: Float) {
        requiredStableFrames = (seconds * 30).toInt()
    }

    /**
     * フレームを解析して安定性を判定する。
     * @return true: 安定判定が完了してキャプチャすべき
     */
    fun analyzeFrame(bitmap: Bitmap): Boolean {
        totalFrameCount++
        val downsampled = Bitmap.createScaledBitmap(bitmap, SAMPLE_SIZE, SAMPLE_SIZE, false)
        val diff = calculateFrameDiff(downsampled, lastBitmap)

        lastBitmap?.recycle()
        lastBitmap = downsampled

        return if (diff < DIFF_THRESHOLD) {
            stableFrameCount++
            val progress = (stableFrameCount.toFloat() / requiredStableFrames).coerceIn(0f, 1f)
            _stabilityProgress.value = progress

            if (stableFrameCount >= requiredStableFrames && !_isStable.value) {
                _isStable.value = true
                reset()
                true
            } else {
                false
            }
        } else {
            stableFrameCount = 0
            _stabilityProgress.value = 0f
            _isStable.value = false
            false
        }
    }

    fun reset() {
        stableFrameCount = 0
        _stabilityProgress.value = 0f
        _isStable.value = false
    }

    fun fullReset() {
        reset()
        lastBitmap?.recycle()
        lastBitmap = null
        totalFrameCount = 0
    }

    private fun calculateFrameDiff(current: Bitmap, previous: Bitmap?): Float {
        if (previous == null) return Float.MAX_VALUE
        if (current.width != previous.width || current.height != previous.height) return Float.MAX_VALUE

        var totalDiff = 0L
        val width = current.width
        val height = current.height
        val pixelCount = width * height

        for (x in 0 until width) {
            for (y in 0 until height) {
                val c1 = current.getPixel(x, y)
                val c2 = previous.getPixel(x, y)
                val rDiff = abs(Color.red(c1) - Color.red(c2))
                val gDiff = abs(Color.green(c1) - Color.green(c2))
                val bDiff = abs(Color.blue(c1) - Color.blue(c2))
                totalDiff += (rDiff + gDiff + bDiff)
            }
        }

        // 最大差分: 255 * 3 * pixelCount
        return totalDiff.toFloat() / (255f * 3f * pixelCount)
    }
}
