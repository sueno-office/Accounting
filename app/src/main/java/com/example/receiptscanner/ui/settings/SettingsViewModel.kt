package com.example.receiptscanner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.receiptscanner.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val soundEnabled = settingsRepository.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val skipReview = settingsRepository.skipReview
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val geminiApiKey = settingsRepository.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val stabilityDuration = settingsRepository.stabilityDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.5f)

    val categories = settingsRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
    }

    fun setSkipReview(skip: Boolean) {
        viewModelScope.launch { settingsRepository.setSkipReview(skip) }
    }

    fun setGeminiApiKey(key: String) {
        viewModelScope.launch { settingsRepository.setGeminiApiKey(key) }
    }

    fun setStabilityDuration(seconds: Float) {
        viewModelScope.launch { settingsRepository.setStabilityDuration(seconds) }
    }

    fun setCategories(cats: List<String>) {
        viewModelScope.launch { settingsRepository.setCategories(cats) }
    }
}
