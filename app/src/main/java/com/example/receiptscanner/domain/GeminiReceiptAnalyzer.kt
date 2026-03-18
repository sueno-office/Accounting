package com.example.receiptscanner.domain

import com.example.receiptscanner.domain.model.ParsedReceiptFields
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Gemini API を使ってレシートOCRテキストからフィールドを解析するクラス。
 * 無料枠 (gemini-1.5-flash): 15 req/min, 1M tokens/day
 */
@Singleton
class GeminiReceiptAnalyzer @Inject constructor() {

    private var generativeModel: GenerativeModel? = null

    @Serializable
    private data class GeminiResponse(
        val category: String = "",
        val supplier: String = "",
        val description: String = "",
        val amountWithTax: Long = 0,
        val taxRate: Int = 10,
        val confidence: Float = 0f
    )

    companion object {
        private val VALID_CATEGORIES = listOf(
            "交際費", "交通費", "消耗品費", "通信費", "会議費",
            "広告費", "雑費", "食費", "光熱費", "備品費", "その他"
        )

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    fun initialize(apiKey: String) {
        if (apiKey.isBlank()) return
        generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.1f
                responseMimeType = "application/json"
            }
        )
    }

    fun isInitialized(): Boolean = generativeModel != null

    /**
     * OCRテキストをGeminiに送信してフィールドを抽出する。
     * @return ParsedReceiptFields (失敗時はnull)
     */
    suspend fun analyze(ocrText: String, customCategories: List<String> = emptyList()): ParsedReceiptFields? {
        val model = generativeModel ?: return null
        val categories = (customCategories + VALID_CATEGORIES).distinct()

        val prompt = buildPrompt(ocrText, categories)

        return withContext(Dispatchers.IO) {
            try {
                val response = model.generateContent(prompt)
                val responseText = response.text ?: return@withContext null
                parseGeminiResponse(responseText, categories)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun buildPrompt(ocrText: String, categories: List<String>): String {
        val categoryList = categories.joinToString("/")
        return """
以下はレシート・領収書のOCR読み取りテキストです。
JSON形式で必要な情報を抽出してください。

抽出するフィールド:
- category: 経費科目。[$categoryList]から最も適切なものを選んでください
- supplier: 店名または取引先会社名（30文字以内）
- description: 購入した商品・サービスの概要（30文字以内）
- amountWithTax: 税込み合計金額（整数、円単位）
- taxRate: 消費税率（8 または 10 のいずれかの整数）
- confidence: 抽出の信頼度（0.0〜1.0の小数）

注意事項:
- 金額は税込み合計を抽出してください
- 消費税率が明記されていない場合は 10 としてください
- 確信が持てないフィールドは空文字列 "" にしてください
- 必ずJSON形式で回答してください（コードブロックは不要）

OCRテキスト:
$ocrText
        """.trimIndent()
    }

    private fun parseGeminiResponse(responseText: String, validCategories: List<String>): ParsedReceiptFields? {
        // JSONブロックを抽出 (```json ... ``` が含まれる場合に対応)
        val cleanedJson = responseText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val geminiResponse = json.decodeFromString<GeminiResponse>(cleanedJson)

            // categoryのバリデーション
            val category = if (geminiResponse.category in validCategories) {
                geminiResponse.category
            } else {
                "その他"
            }

            // taxRateのバリデーション
            val taxRate = if (geminiResponse.taxRate in listOf(8, 10)) {
                geminiResponse.taxRate
            } else {
                10
            }

            ParsedReceiptFields(
                category = category,
                supplier = geminiResponse.supplier.take(50),
                description = geminiResponse.description.take(50),
                amountWithTax = geminiResponse.amountWithTax.coerceAtLeast(0),
                taxRate = taxRate,
                confidence = geminiResponse.confidence.coerceIn(0f, 1f)
            )
        } catch (e: Exception) {
            null
        }
    }
}
