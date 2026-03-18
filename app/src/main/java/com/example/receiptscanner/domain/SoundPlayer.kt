package com.example.receiptscanner.domain

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.receiptscanner.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * レシート読み取り完了時の効果音を管理するクラス。
 */
@Singleton
class SoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var soundPool: SoundPool? = null
    private var scanCompleteSoundId: Int = 0
    private var isLoaded = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, status ->
            isLoaded = status == 0
        }

        scanCompleteSoundId = soundPool?.load(context, R.raw.scan_complete, 1) ?: 0
    }

    /**
     * スキャン完了音を再生する。
     */
    fun playScanComplete() {
        if (isLoaded && scanCompleteSoundId != 0) {
            soundPool?.play(
                scanCompleteSoundId,
                /* leftVolume  = */ 1.0f,
                /* rightVolume = */ 1.0f,
                /* priority    = */ 1,
                /* loop        = */ 0,
                /* rate        = */ 1.0f
            )
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}
