/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vikiea.age.core.data.OperationRepository
import io.github.vikiea.age.core.model.OperationRecord
import io.github.vikiea.age.core.model.OperationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val filterType: OperationType? = null,
    val showClearDialog: Boolean = false
)

private val historyFilterOrder = listOf<OperationType?>(null, OperationType.ENCRYPT, OperationType.DECRYPT)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val operationRepository: OperationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    val operations = operationRepository.getAllOperations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(type: OperationType?) { _uiState.update { it.copy(filterType = type) } }
    fun showPreviousFilter() { moveFilterBy(-1) }
    fun showNextFilter() { moveFilterBy(1) }
    fun showClearDialog() { _uiState.update { it.copy(showClearDialog = true) } }
    fun hideClearDialog() { _uiState.update { it.copy(showClearDialog = false) } }

    fun clearHistory() {
        viewModelScope.launch {
            operationRepository.clearAll()
            _uiState.update { it.copy(showClearDialog = false) }
        }
    }

    fun deleteOperation(id: Long) {
        viewModelScope.launch {
            operationRepository.deleteOperation(id)
        }
    }

    private fun moveFilterBy(delta: Int) {
        _uiState.update { state ->
            val currentIndex = historyFilterOrder.indexOf(state.filterType).takeIf { it >= 0 } ?: 0
            val nextIndex = (currentIndex + delta).coerceIn(historyFilterOrder.indices)
            state.copy(filterType = historyFilterOrder[nextIndex])
        }
    }
}
