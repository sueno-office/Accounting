package com.example.receiptscanner.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.receiptscanner.data.repository.ReceiptRepository
import com.example.receiptscanner.data.repository.SettingsRepository
import com.example.receiptscanner.domain.CsvExporter
import com.example.receiptscanner.domain.model.ParsedReceiptFields
import com.example.receiptscanner.domain.model.ReceiptEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: ReceiptRepository,
    private val settingsRepository: SettingsRepository,
    private val csvExporter: CsvExporter
) : ViewModel() {

    private val _category = MutableStateFlow("")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _supplier = MutableStateFlow("")
    val supplier: StateFlow<String> = _supplier.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _amountWithTax = MutableStateFlow("")
    val amountWithTax: StateFlow<String> = _amountWithTax.asStateFlow()

    private val _taxRate = MutableStateFlow(10)
    val taxRate: StateFlow<Int> = _taxRate.asStateFlow()

    private val _photoNumber = MutableStateFlow(0)
    val photoNumber: StateFlow<Int> = _photoNumber.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
    private var photoPath: String = ""

    init {
        viewModelScope.launch {
            _categories.value = settingsRepository.categories.first()
        }
    }

    fun initialize(
        parsedFields: ParsedReceiptFields,
        photoPath: String,
        photoNumber: Int
    ) {
        this.photoPath = photoPath
        _category.value = parsedFields.category.ifBlank {
            _categories.value.firstOrNull() ?: "その他"
        }
        _supplier.value = parsedFields.supplier
        _description.value = parsedFields.description
        _amountWithTax.value = if (parsedFields.amountWithTax > 0) {
            parsedFields.amountWithTax.toString()
        } else ""
        _taxRate.value = parsedFields.taxRate
        _photoNumber.value = photoNumber
    }

    fun setCategory(value: String) { _category.value = value }
    fun setSupplier(value: String) { _supplier.value = value }
    fun setDescription(value: String) { _description.value = value }
    fun setAmountWithTax(value: String) { _amountWithTax.value = value }
    fun setTaxRate(value: Int) { _taxRate.value = value }

    fun save() {
        viewModelScope.launch {
            val sessionDate = dateFormat.format(Date())
            val amount = _amountWithTax.value.replace(",", "").toLongOrNull() ?: 0L

            val receipt = ReceiptEntry(
                photoNumber = _photoNumber.value,
                category = _category.value,
                supplier = _supplier.value,
                description = _description.value,
                amountWithTax = amount,
                taxRate = _taxRate.value,
                photoPath = photoPath,
                sessionDate = sessionDate
            )

            val id = repository.insertReceipt(receipt)
            csvExporter.appendToCsv(receipt.copy(id = id), sessionDate)
            _isSaved.value = true
        }
    }
}
