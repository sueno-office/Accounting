package com.example.receiptscanner.ui.history

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.receiptscanner.data.repository.ReceiptRepository
import com.example.receiptscanner.domain.CsvExporter
import com.example.receiptscanner.domain.model.ReceiptEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ReceiptRepository,
    private val csvExporter: CsvExporter
) : ViewModel() {

    private val _sessionDates = MutableStateFlow<List<String>>(emptyList())
    val sessionDates: StateFlow<List<String>> = _sessionDates.asStateFlow()

    private val _receiptsByDate = MutableStateFlow<Map<String, List<ReceiptEntry>>>(emptyMap())
    val receiptsByDate: StateFlow<Map<String, List<ReceiptEntry>>> = _receiptsByDate.asStateFlow()

    private val _totals = MutableStateFlow<Map<String, Long>>(emptyMap())
    val totals: StateFlow<Map<String, Long>> = _totals.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSessionDates().collect { dates ->
                _sessionDates.value = dates
                loadReceiptsForDates(dates)
            }
        }
    }

    private suspend fun loadReceiptsForDates(dates: List<String>) {
        val map = mutableMapOf<String, List<ReceiptEntry>>()
        val totals = mutableMapOf<String, Long>()
        for (date in dates) {
            val receipts = repository.getReceiptsByDate(date).first()
            map[date] = receipts
            totals[date] = receipts.sumOf { it.amountWithTax }
        }
        _receiptsByDate.value = map
        _totals.value = totals
    }

    fun shareCsv(date: String) {
        viewModelScope.launch {
            val receipts = repository.getReceiptsByDate(date).first()
            val csvFile = csvExporter.exportAll(receipts, date)
            shareFile(csvFile)
        }
    }

    fun deleteSession(date: String) {
        viewModelScope.launch {
            repository.deleteReceiptsByDate(date)
        }
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "CSV を共有").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
