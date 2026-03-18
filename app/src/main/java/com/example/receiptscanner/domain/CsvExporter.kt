package com.example.receiptscanner.domain

import android.content.Context
import android.os.Environment
import com.example.receiptscanner.domain.model.ReceiptEntry
import com.opencsv.CSVWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * レシートデータをCSV形式でエクスポートするクラス。
 * UTF-8 with BOM (Excelで文字化けしないよう) で出力する。
 */
@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val BASE_FOLDER = "出納帳スキャナー"
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd", Locale.JAPAN)
        private val DATETIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN)

        val CSV_HEADER = arrayOf(
            "写真番号", "科目", "取引先", "内容", "税込み価格", "消費税率", "撮影日時"
        )
    }

    /**
     * レシートデータをCSVに追記する。
     * @return 保存したCSVファイルのパス
     */
    fun appendToCsv(receipt: ReceiptEntry, sessionDate: String): File {
        val folder = getSessionFolder(sessionDate)
        val csvFile = File(folder, "receipts_${sessionDate.replace("-", "")}.csv")

        val isNewFile = !csvFile.exists()

        FileOutputStream(csvFile, true).use { fos ->
            // BOM (UTF-8 with BOM) - 新規ファイルのみ
            if (isNewFile) {
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            }

            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                CSVWriter(writer).use { csv ->
                    if (isNewFile) {
                        csv.writeNext(CSV_HEADER)
                    }
                    csv.writeNext(receipt.toCsvRow())
                }
            }
        }

        return csvFile
    }

    /**
     * 指定日のすべてのレシートをCSVとして書き出す（上書き）。
     */
    fun exportAll(receipts: List<ReceiptEntry>, sessionDate: String): File {
        val folder = getSessionFolder(sessionDate)
        val csvFile = File(folder, "receipts_${sessionDate.replace("-", "")}.csv")

        FileOutputStream(csvFile, false).use { fos ->
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                CSVWriter(writer).use { csv ->
                    csv.writeNext(CSV_HEADER)
                    receipts.forEach { csv.writeNext(it.toCsvRow()) }
                }
            }
        }

        return csvFile
    }

    fun getSessionFolder(sessionDate: String): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val folder = File(baseDir, "$BASE_FOLDER/$sessionDate")
        if (!folder.exists()) folder.mkdirs()
        return folder
    }

    fun getNextPhotoFile(sessionDate: String, photoNumber: Int): File {
        val folder = getSessionFolder(sessionDate)
        return File(folder, "receipt_%04d.jpg".format(photoNumber))
    }

    private fun ReceiptEntry.toCsvRow(): Array<String> = arrayOf(
        "%04d".format(photoNumber),
        category,
        supplier,
        description,
        amountWithTax.toString(),
        "${taxRate}%",
        DATETIME_FORMAT.format(Date(timestamp))
    )
}
