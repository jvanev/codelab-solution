package com.sap.codelab.view.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sap.codelab.model.GeofenceRegisterResult
import com.sap.codelab.model.Memo
import com.sap.codelab.repository.GeofenceRepository
import com.sap.codelab.repository.IGeofenceRepository
import com.sap.codelab.repository.IMemoRepository
import com.sap.codelab.repository.MemoRepository
import com.sap.codelab.utils.extensions.empty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for matching CreateMemo view. Handles user interactions.
 */
internal class CreateMemoViewModel(
    private val memoRepository: IMemoRepository,
    private val geofenceRepository: IGeofenceRepository,
) : ViewModel() {

    private var memo = Memo(
        id = 0,
        title = String.empty(),
        description = String.empty(),
        reminderDate = 0,
        reminderLatitude = NO_LOCATION,
        reminderLongitude = NO_LOCATION,
        isDone = false,
    )

    private val _memoRegisterResult = MutableStateFlow<GeofenceRegisterResult?>(null)
    val memoRegisterResult: StateFlow<GeofenceRegisterResult?> = _memoRegisterResult

    private val _memoStoreFailed = MutableStateFlow(false)
    val memoStoreFailed: StateFlow<Boolean> = _memoStoreFailed

    private var saveJob: Job? = null

    /**
     * Saves the memo in its current state.
     */
    fun saveMemo() {
        clearErrorMessage()

        if (saveJob?.isActive == true || _memoRegisterResult.value != null) {
            return
        }

        saveJob = viewModelScope.launch {
            val id = try {
                memoRepository.saveMemo(memo)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _memoStoreFailed.value = true
                return@launch
            }
            _memoRegisterResult.value = geofenceRepository.addGeofence(memo.copy(id = id))
        }
    }

    /**
     * Call this method to update the memo. This is usually needed when the user changed his input.
     */
    fun updateMemo(title: String, description: String, latitude: Double, longitude: Double) {
        memo = Memo(
            id = 0,
            title = title,
            description = description,
            reminderDate = 0,
            reminderLatitude = latitude,
            reminderLongitude = longitude,
            isDone = false,
        )
    }

    /**
     * Clears the error message of the failed save attempt.
     */
    fun clearErrorMessage() {
        _memoStoreFailed.value = false
    }

    /**
     * @return true if the title and content are not blank, and location is selected;
     *         false otherwise.
     */
    fun isMemoValid(): Boolean = !hasTitleError() && !hasTextError() && !hasLocationError()

    /**
     * @return true if the memo text is blank, false otherwise.
     */
    fun hasTextError() = memo.description.isBlank()

    /**
     * @return true if the memo title is blank, false otherwise.
     */
    fun hasTitleError() = memo.title.isBlank()

    /**
     * @return true if the memo's location is not set, false otherwise.
     */
    fun hasLocationError() = memo.reminderLatitude == NO_LOCATION ||
            memo.reminderLongitude == NO_LOCATION

    companion object {
        const val NO_LOCATION = Double.NEGATIVE_INFINITY

        val Factory = viewModelFactory {
            initializer { CreateMemoViewModel(MemoRepository, GeofenceRepository) }
        }
    }
}