package com.example.receiptscanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.receiptscanner.domain.model.ReceiptEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntry): Long

    @Query("SELECT * FROM receipts ORDER BY timestamp DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntry>>

    @Query("SELECT * FROM receipts WHERE sessionDate = :date ORDER BY photoNumber ASC")
    fun getReceiptsByDate(date: String): Flow<List<ReceiptEntry>>

    @Query("SELECT DISTINCT sessionDate FROM receipts ORDER BY sessionDate DESC")
    fun getSessionDates(): Flow<List<String>>

    @Query("SELECT MAX(photoNumber) FROM receipts")
    suspend fun getMaxPhotoNumber(): Int?

    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun deleteReceipt(id: Long)

    @Query("DELETE FROM receipts WHERE sessionDate = :date")
    suspend fun deleteReceiptsByDate(date: String)

    @Query("SELECT COUNT(*) FROM receipts WHERE sessionDate = :date")
    suspend fun getReceiptCountByDate(date: String): Int

    @Query("SELECT SUM(amountWithTax) FROM receipts WHERE sessionDate = :date")
    suspend fun getTotalAmountByDate(date: String): Long?
}
