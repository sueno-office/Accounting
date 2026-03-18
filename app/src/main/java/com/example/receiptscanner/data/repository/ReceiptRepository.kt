package com.example.receiptscanner.data.repository

import com.example.receiptscanner.data.local.ReceiptDao
import com.example.receiptscanner.domain.model.ReceiptEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRepository @Inject constructor(
    private val dao: ReceiptDao
) {
    fun getAllReceipts(): Flow<List<ReceiptEntry>> = dao.getAllReceipts()

    fun getReceiptsByDate(date: String): Flow<List<ReceiptEntry>> =
        dao.getReceiptsByDate(date)

    fun getSessionDates(): Flow<List<String>> = dao.getSessionDates()

    suspend fun insertReceipt(receipt: ReceiptEntry): Long = dao.insertReceipt(receipt)

    suspend fun deleteReceipt(id: Long) = dao.deleteReceipt(id)

    suspend fun deleteReceiptsByDate(date: String) = dao.deleteReceiptsByDate(date)

    suspend fun getNextPhotoNumber(): Int {
        val max = dao.getMaxPhotoNumber() ?: 0
        return max + 1
    }

    suspend fun getReceiptCountByDate(date: String): Int =
        dao.getReceiptCountByDate(date)

    suspend fun getTotalAmountByDate(date: String): Long =
        dao.getTotalAmountByDate(date) ?: 0L
}
