package com.sap.codelab.view.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sap.codelab.model.Memo
import com.sap.codelab.repository.GeofenceRepository
import com.sap.codelab.repository.IGeofenceRepository
import com.sap.codelab.repository.IMemoRepository
import com.sap.codelab.repository.MemoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel for the Home Activity.
 */
internal class HomeViewModel(
    private val memoRepository: IMemoRepository,
    private val geofenceRepository: IGeofenceRepository,
) : ViewModel() {

    private var _isShowAll = MutableStateFlow(false)
    val isShowAll: StateFlow<Boolean> = _isShowAll

    @OptIn(ExperimentalCoroutinesApi::class)
    val memos: StateFlow<List<Memo>> = _isShowAll.flatMapLatest { showAll ->
        if (showAll) {
            memoRepository.getAll()
        } else {
            memoRepository.getOpen()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), emptyList())

    /**
     * Loads all memos.
     */
    fun loadAllMemos() {
        _isShowAll.value = true
    }

    /**
     * Loads all open (not done) memos.
     */
    fun loadOpenMemos() {
        _isShowAll.value = false
    }

    /**
     * Updates the given memo, marking it as done if isChecked is true.
     *
     * @param memo      - the memo to update.
     * @param isChecked - whether the memo has been checked (marked as done).
     */
    fun updateMemo(memo: Memo, isChecked: Boolean) {
        viewModelScope.launch {
            // We'll only forward the update if the memo has been checked, since we don't offer to uncheck memos right now
            if (isChecked) {
                memoRepository.saveMemo(memo.copy(isDone = true))
                geofenceRepository.removeGeofence(memo.id)
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel(MemoRepository, GeofenceRepository) }
        }
    }
}