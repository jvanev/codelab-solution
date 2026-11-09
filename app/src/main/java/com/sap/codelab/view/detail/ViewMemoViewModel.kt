package com.sap.codelab.view.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sap.codelab.model.Memo
import com.sap.codelab.repository.IMemoRepository
import com.sap.codelab.repository.MemoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for matching ViewMemo view.
 */
internal class ViewMemoViewModel(
    private val repository: IMemoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val memoId: Long = savedStateHandle[ViewMemo.BUNDLE_MEMO_ID] ?: -1L

    private val _memo: MutableStateFlow<Memo?> = MutableStateFlow(null)
    val memo: StateFlow<Memo?> = _memo

    init {
        loadMemo(memoId)
    }

    /**
     * Loads the memo whose id matches the given memoId from the database.
     */
    private fun loadMemo(memoId: Long) {
        viewModelScope.launch {
            _memo.value = repository.getMemoById(memoId)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ViewMemoViewModel(MemoRepository, createSavedStateHandle())
            }
        }
    }
}