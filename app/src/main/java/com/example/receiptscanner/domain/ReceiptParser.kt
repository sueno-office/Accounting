package com.example.receiptscanner.domain

import com.example.receiptscanner.domain.model.ParsedReceiptFields
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OCRで取得したテキストからレシートの各フィールドを抽出するルールベースパーサー。
 * Gemini APIが利用できない場合のフォールバックとして使用。
 */
@Singleton
class ReceiptParser @Inject constructor() {

    companion object {
        // 税込み価格のパターン (例: ¥1,080 / 合計 1,080円 / 小計：3,300)
        private val AMOUNT_PATTERNS = listOf(
            Regex("""合計[　 ]*[¥￥]?\s*([0-9,]+)"""),
            Regex("""お会計[　 ]*[¥￥]?\s*([0-9,]+)"""),
            Regex("""小計[　 ]*[¥￥]?\s*([0-9,]+)"""),
            Regex("""総合計[　 ]*[¥￥]?\s*([0-9,]+)"""),
            Regex("""[¥￥]\s*([0-9,]+)"""),
            Regex("""([0-9,]+)\s*円""")
        )

        // 消費税率のパターン
        private val TAX_8_PATTERN = Regex("""8\s*%|軽減税率""")
        private val TAX_10_PATTERN = Regex("""10\s*%|標準税率""")

        // 取引先名の候補パターン (先頭行や「様」の前など)
        private val SUPPLIER_HINT_PATTERNS = listOf(
            Regex("""^(.{2,20})(株式会社|有限会社|合同会社|店|商店|マート|ストア)"""),
            Regex("""(株式会社|有限会社|合同会社)\s*(.{1,20})"""),
            Regex("""領収書\s*\n(.+)""")
        )

        // 日付パターン (取引先名特定のヒント)
        private val DATE_PATTERN = Regex("""(\d{4})[年/\-](\d{1,2})[月/\-](\d{1,2})日?""")
    }

    /**
     * OCRテキストからフィールドをルールベースで抽出する。
     * 金額・税率は高精度だが、科目・取引先・内容は精度が低い。
     */
    fun parse(ocrText: String): ParsedReceiptFields {
        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val amount = extractAmount(ocrText)
        val taxRate = extractTaxRate(ocrText)
        val supplier = extractSupplier(lines, ocrText)
        val confidence = calculateConfidence(amount, taxRate, supplier)

        return ParsedReceiptFields(
            category = "",              // ルールベースでは分類不可 → Gemini or 手動
            supplier = supplier,
            description = "",           // ルールベースでは内容特定不可 → Gemini or 手動
            amountWithTax = amount,
            taxRate = taxRate,
            confidence = confidence
        )
    }

    private fun extractAmount(text: String): Long {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(text) ?: continue
            val amountStr = match.groupValues[1].replace(",", "").replace("，", "")
            val amount = amountStr.toLongOrNull() ?: continue
            // 妥当な範囲 (1円〜1000万円)
            if (amount in 1..10_000_000) {
                return amount
            }
        }
        return 0L
    }

    private fun extractTaxRate(text: String): Int {
        return when {
            TAX_8_PATTERN.containsMatchIn(text) -> 8
            TAX_10_PATTERN.containsMatchIn(text) -> 10
            else -> 10  // デフォルト10%
        }
    }

    private fun extractSupplier(lines: List<String>, fullText: String): String {
        // 最初の非空行から取引先候補を探す
        for (pattern in SUPPLIER_HINT_PATTERNS) {
            val match = pattern.find(fullText) ?: continue
            val candidate = match.groupValues.lastOrNull { it.length in 2..20 } ?: continue
            if (candidate.isNotEmpty()) return candidate.trim()
        }

        // フォールバック: 最初の行（レシート先頭は店名が多い）
        return lines.firstOrNull { line ->
            line.length in 2..30 &&
            !line.matches(Regex("""^\d+.*""")) &&
            !DATE_PATTERN.containsMatchIn(line) &&
            !line.contains("領収書") &&
            !line.contains("レシート")
        } ?: ""
    }

    private fun calculateConfidence(amount: Long, taxRate: Int, supplier: String): Float {
        var score = 0f
        if (amount > 0) score += 0.5f
        if (taxRate == 8 || taxRate == 10) score += 0.2f
        if (supplier.isNotEmpty()) score += 0.3f
        return score
    }
}
