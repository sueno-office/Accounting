package com.example.receiptscanner.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class ReceiptEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val photoNumber: Int,           // 写真番号 (連番)
    val category: String,           // 科目
    val supplier: String,           // 取引先
    val description: String,        // 内容
    val amountWithTax: Long,        // 税込み価格 (円)
    val taxRate: Int,               // 消費税率 (8 or 10)
    val photoPath: String,          // 保存した写真のパス
    val sessionDate: String,        // セッション日付 (YYYY-MM-DD)
    val timestamp: Long = System.currentTimeMillis(), // 撮影日時
    val confidence: Float = 0f      // Gemini解析の信頼度 (0.0-1.0)
)

data class ParsedReceiptFields(
    val category: String = "",
    val supplier: String = "",
    val description: String = "",
    val amountWithTax: Long = 0,
    val taxRate: Int = 10,
    val confidence: Float = 0f
)

enum class ScanState {
    IDLE,           // 待機中 - "レシートを枠内に合わせてください"
    DETECTING,      // 検出中 - "レシートを検出中..."
    STABILIZING,    // 安定判定中 - "静止してください..."
    CAPTURING,      // キャプチャ中 - 効果音
    PROCESSING,     // 解析中 - "解析中..."
    COMPLETE        // 完了 - 確認画面へ
}
