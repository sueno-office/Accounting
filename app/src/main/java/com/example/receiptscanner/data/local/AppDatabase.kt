package com.example.receiptscanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.receiptscanner.domain.model.ReceiptEntry

@Database(
    entities = [ReceiptEntry::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
}
