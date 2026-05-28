/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vikiea.age.core.data.OperationRepository
import io.github.vikiea.age.core.model.OperationRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    private val operationRepository: OperationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val recordId: Long = savedStateHandle["recordId"] ?: 0L

    val operation: StateFlow<OperationRecord?> = operationRepository.getOperationById(recordId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun deleteOperation(onDone: () -> Unit) {
        viewModelScope.launch {
            operationRepository.deleteOperation(recordId)
            onDone()
        }
    }
}
