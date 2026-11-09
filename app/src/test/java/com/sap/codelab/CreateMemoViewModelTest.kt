package com.sap.codelab

import com.sap.codelab.model.GeofenceRegisterResult
import com.sap.codelab.view.create.CreateMemoViewModel
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

internal class CreateMemoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var memoRepository: FakeMemoRepository
    private lateinit var geofenceRepository: FakeGeofenceRepository
    private lateinit var viewModel: CreateMemoViewModel

    @Before
    fun setUp() {
        memoRepository = FakeMemoRepository()
        geofenceRepository = FakeGeofenceRepository()
        viewModel = CreateMemoViewModel(memoRepository, geofenceRepository)
    }

    private fun updateWithValidMemoData() {
        viewModel.updateMemo(
            title = "Palazzo del Casinò",
            description = "Such a nice place",
            latitude = 45.406137,
            longitude = 12.368409,
        )
    }

    @Test
    fun onValidInput_MemoShouldBeValid() {
        updateWithValidMemoData()

        assertTrue(viewModel.isMemoValid())
    }

    @Test
    fun onEmptyTitle_MemoShouldNotBeValid() {
        viewModel.updateMemo("  ", "Such a nice place", 45.406137, 12.368409)

        assertTrue(viewModel.hasTitleError())
        assertFalse(viewModel.isMemoValid())
    }

    @Test
    fun onEmptyDescription_MemoShouldNotBeValid() {
        viewModel.updateMemo("Palazzo del Casinò", "", 45.406137, 12.368409)

        assertTrue(viewModel.hasTextError())
        assertFalse(viewModel.isMemoValid())
    }

    @Test
    fun onNoLocation_MemoShouldNotBeValid() {
        viewModel.updateMemo(
            title = "Palazzo del Casinò",
            description = "Such a nice place",
            latitude = CreateMemoViewModel.NO_LOCATION,
            longitude = CreateMemoViewModel.NO_LOCATION,
        )

        assertTrue(viewModel.hasLocationError())
        assertFalse(viewModel.isMemoValid())
    }

    @Test
    fun onInit_ViewModelStartsWithNoLocation() {
        assertTrue(viewModel.hasLocationError())
    }

    @Test
    fun onSave_MemoShouldBeStoredAndGeofenceRegistered() {
        updateWithValidMemoData()
        viewModel.saveMemo()

        val saved = memoRepository.savedMemos.single()
        val registered = geofenceRepository.registeredMemos.single()

        assertEquals(saved.id, registered.id)
        assertTrue(registered.id != 0L)
        assertEquals("Palazzo del Casinò", registered.title)
    }

    @Test
    fun onSave_ReturnsGeofenceRegistrationResult() {
        geofenceRepository.result = GeofenceRegisterResult.MissingPermission

        updateWithValidMemoData()
        viewModel.saveMemo()

        assertEquals(GeofenceRegisterResult.MissingPermission, viewModel.memoRegisterResult.value)
        assertFalse(viewModel.memoStoreFailed.value)
    }

    @Test
    fun onGeofenceRegistrationFailure_MemoShouldBeSaved() {
        val failure = GeofenceRegisterResult.Failure(RuntimeException())
        geofenceRepository.result = failure

        updateWithValidMemoData()
        viewModel.saveMemo()

        assertEquals(1, memoRepository.savedMemos.size)
        assertEquals(failure, viewModel.memoRegisterResult.value)
    }

    @Test
    fun onMemoSaveFailure_GeofenceShouldNotBeRegistered() {
        memoRepository.failOnSave = true

        updateWithValidMemoData()
        viewModel.saveMemo()

        assertTrue(viewModel.memoStoreFailed.value)
        assertNull(viewModel.memoRegisterResult.value)
        assertTrue(geofenceRepository.registeredMemos.isEmpty())
    }

    @Test
    fun onMemoSaveFailure_ShouldAllowRetry() {
        memoRepository.failOnSave = true
        updateWithValidMemoData()
        viewModel.saveMemo()

        memoRepository.failOnSave = false
        viewModel.saveMemo()

        assertFalse(viewModel.memoStoreFailed.value)
        assertEquals(1, memoRepository.savedMemos.size)
        assertEquals(GeofenceRegisterResult.Success, viewModel.memoRegisterResult.value)
    }

    @Test
    fun onMemoSaveSuccess_ShouldIgnoreSubsequentUpdates() {
        updateWithValidMemoData()

        viewModel.saveMemo()
        viewModel.saveMemo()

        assertEquals(1, memoRepository.saveCount)
        assertEquals(1, geofenceRepository.registeredMemos.size)
    }

    @Test
    fun afterClearErrorMessage_FailureFlagShouldBeReset() {
        memoRepository.failOnSave = true

        updateWithValidMemoData()
        viewModel.saveMemo()
        viewModel.clearErrorMessage()

        assertFalse(viewModel.memoStoreFailed.value)
    }
}