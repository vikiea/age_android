package io.github.vikiea.age.feature.history

import io.github.vikiea.age.core.data.OperationRepository
import io.github.vikiea.age.core.model.OperationType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class HistoryViewModelTest {
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setup() {
        val operationRepository = mockk<OperationRepository> {
            every { getAllOperations() } returns flowOf(emptyList())
        }
        viewModel = HistoryViewModel(operationRepository)
    }

    @Test
    fun `next filter advances from all to encrypt to decrypt`() {
        assertNull(viewModel.uiState.value.filterType)

        viewModel.showNextFilter()
        assertEquals(OperationType.ENCRYPT, viewModel.uiState.value.filterType)

        viewModel.showNextFilter()
        assertEquals(OperationType.DECRYPT, viewModel.uiState.value.filterType)
    }

    @Test
    fun `filter swipe stays inside available tabs`() {
        viewModel.showPreviousFilter()
        assertNull(viewModel.uiState.value.filterType)

        viewModel.setFilter(OperationType.DECRYPT)
        viewModel.showNextFilter()
        assertEquals(OperationType.DECRYPT, viewModel.uiState.value.filterType)
    }
}
