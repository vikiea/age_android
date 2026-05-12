/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.age.android.core.data.OperationRepository
import com.age.android.core.model.OperationRecord
import com.age.android.core.model.OperationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val filterType: OperationType? = null,
    val showClearDialog: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val operationRepository: OperationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    val operations = operationRepository.getAllOperations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(type: OperationType?) { _uiState.update { it.copy(filterType = type) } }
    fun showClearDialog() { _uiState.update { it.copy(showClearDialog = true) } }
    fun hideClearDialog() { _uiState.update { it.copy(showClearDialog = false) } }

    fun clearHistory() {
        viewModelScope.launch {
            operationRepository.clearAll()
            _uiState.update { it.copy(showClearDialog = false) }
        }
    }
}
