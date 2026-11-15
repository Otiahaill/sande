package com.example.sande.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sande.data.repository.OutboxRepository
import com.example.sande.domain.sync.SyncEngine
import com.example.sande.domain.sync.SyncResult
import com.example.sande.domain.sync.SyncState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SyncViewModel(
    private val outboxRepository: OutboxRepository,
    private val syncEngine: SyncEngine
) : ViewModel() {

    val state: StateFlow<SyncState> = syncEngine.state
    val pendingCount: StateFlow<Int> = syncEngine.pendingCount
    val lastResult: StateFlow<SyncResult?> = syncEngine.lastResult

    fun sync(businessId: String) {
        viewModelScope.launch {
            syncEngine.push(businessId)
        }
    }

    fun resolveConflict(saleId: String, keepServerCopy: Boolean) {
        viewModelScope.launch {
            syncEngine.resolveConflict(saleId, keepServerCopy)
        }
    }
}
