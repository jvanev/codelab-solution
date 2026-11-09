package com.sap.codelab

import com.sap.codelab.model.Memo
import com.sap.codelab.view.home.HomeViewModel
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

internal class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var memoRepository: FakeMemoRepository
    private lateinit var geofenceRepository: FakeGeofenceRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        memoRepository = FakeMemoRepository()
        geofenceRepository = FakeGeofenceRepository()
        viewModel = HomeViewModel(memoRepository, geofenceRepository)
    }

    private fun createMemo(title: String, isDone: Boolean = false) = Memo(
        id = 0,
        title = title,
        description = "Description of $title",
        reminderDate = 0,
        reminderLatitude = 43.210614,
        reminderLongitude = 27.890899,
        isDone = isDone,
    )

    private fun TestScope.startCollecting() {
        backgroundScope.launch { viewModel.memos.collect {} }
    }

    private fun visibleTitles() = viewModel.memos.value.map { it.title }.sorted()

    @Test
    fun shouldStartWithOpenMemos() = runTest(mainDispatcherRule.dispatcher) {
        memoRepository.saveMemo(createMemo("Open"))
        memoRepository.saveMemo(createMemo("Done", isDone = true))
        startCollecting()

        assertFalse(viewModel.isShowAll.value)
        assertEquals(listOf("Open"), visibleTitles())
    }

    @Test
    fun onLoadAllMemos_ShouldIncludeClosedMemos() = runTest(mainDispatcherRule.dispatcher) {
        memoRepository.saveMemo(createMemo("Open"))
        memoRepository.saveMemo(createMemo("Done", isDone = true))
        startCollecting()
        viewModel.loadAllMemos()

        assertTrue(viewModel.isShowAll.value)
        assertEquals(listOf("Done", "Open"), visibleTitles())
    }

    @Test
    fun afterLoadAllMemos_LoadOpenMemosShouldSwitchBackToOpenMemosOnly() =
        runTest(mainDispatcherRule.dispatcher) {
            memoRepository.saveMemo(createMemo("Open"))
            memoRepository.saveMemo(createMemo("Done", isDone = true))
            startCollecting()
            viewModel.loadAllMemos()
            viewModel.loadOpenMemos()

            assertFalse(viewModel.isShowAll.value)
            assertEquals(listOf("Open"), visibleTitles())
        }

    @Test
    fun afterNewMemoSaved_ShouldUpdateResults() = runTest(mainDispatcherRule.dispatcher) {
        startCollecting()
        memoRepository.saveMemo(createMemo("Added later"))

        assertEquals(listOf("Added later"), visibleTitles())
    }

    @Test
    fun onMemoChecked_ShouldUpdateIsDoneAndRemoveGeofence() =
        runTest(mainDispatcherRule.dispatcher) {
            val id = memoRepository.saveMemo(createMemo("Madrid"))
            startCollecting()
            val shown = viewModel.memos.value.single()

            viewModel.updateMemo(shown, isChecked = true)

            assertTrue(memoRepository.getMemoById(id)!!.isDone)
            assertEquals(listOf(id), geofenceRepository.removedIds)
        }

    @Test
    fun onMemoChecked_ShouldBeRemovedFromTheOpenList() = runTest(mainDispatcherRule.dispatcher) {
        memoRepository.saveMemo(createMemo("Barcelona"))
        memoRepository.saveMemo(createMemo("Venice"))
        startCollecting()

        val barcelona = viewModel.memos.value.first { it.title == "Barcelona" }

        viewModel.updateMemo(barcelona, isChecked = true)

        assertEquals(listOf("Venice"), visibleTitles())
    }

    @Test
    fun shouldNotUpdateCheckedMemoState() = runTest(mainDispatcherRule.dispatcher) {
        val id = memoRepository.saveMemo(createMemo("Rome"))
        val saveCountBefore = memoRepository.saveCount

        viewModel.updateMemo(memoRepository.getMemoById(id)!!, isChecked = false)

        assertEquals(saveCountBefore, memoRepository.saveCount)
        assertTrue(geofenceRepository.removedIds.isEmpty())
    }
}
