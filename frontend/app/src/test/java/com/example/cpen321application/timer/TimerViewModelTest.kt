package com.example.cpen321application.timer

import com.example.cpen321application.ui.timer.TimerUiState
import com.example.cpen321application.ui.timer.TimerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val start = IssPosition(0.0, 0.0, 420.0, 27_600.0)
    private val end = IssPosition(0.0, 1.0, 420.0, 27_600.0)

    /** Returns [start] on the first call and [end] afterwards. */
    private inner class FakeTracker : IssTracker {
        var calls = 0
        override suspend fun currentPosition(): IssPosition? = if (calls++ == 0) start else end
    }

    // Virtual clock driven by the test scheduler so delay() and clock() agree.
    private val clock: () -> Long = { 1_000_000L + dispatcher.scheduler.currentTime }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `starts idle and ignores a zero-length timer`() = runTest(dispatcher) {
        val vm = TimerViewModel(FakeTracker(), clock)
        assertEquals(TimerUiState.Idle, vm.uiState.value)

        vm.start(0, 0)
        advanceUntilIdle()
        assertEquals(TimerUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `counts down from the requested duration`() = runTest(dispatcher) {
        val vm = TimerViewModel(FakeTracker(), clock, tickMs = 250)

        vm.start(0, 3)
        runCurrent()
        assertEquals(TimerUiState.Running(3_000, 3_000), vm.uiState.value)

        advanceTimeBy(1_000); runCurrent()
        assertEquals(TimerUiState.Running(2_000, 3_000), vm.uiState.value)

        advanceTimeBy(1_500); runCurrent()
        assertEquals(TimerUiState.Running(500, 3_000), vm.uiState.value)
    }

    @Test
    fun `fires at zero, emits one event, then attaches facts`() = runTest(dispatcher) {
        val tracker = FakeTracker()
        val vm = TimerViewModel(tracker, clock, tickMs = 250)
        var fired = 0
        val collector = launch { vm.firedEvents.collect { fired++ } }

        vm.start(1, 5)
        advanceTimeBy(65_000); runCurrent()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("expected Fired but was $state", state is TimerUiState.Fired)
        state as TimerUiState.Fired
        assertEquals(65, state.elapsedSeconds)
        assertNotNull(state.facts)
        assertEquals(start, state.facts!!.issStart)
        assertEquals(end, state.facts!!.issEnd)
        assertEquals(2, tracker.calls)
        assertEquals(1, fired)
        collector.cancel()
    }

    @Test
    fun `cancel returns to idle and stops the countdown`() = runTest(dispatcher) {
        val vm = TimerViewModel(FakeTracker(), clock)
        vm.start(0, 10)
        advanceTimeBy(2_000); runCurrent()
        assertTrue(vm.uiState.value is TimerUiState.Running)

        vm.cancel()
        advanceTimeBy(20_000); advanceUntilIdle()
        assertEquals(TimerUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `start is ignored while already running`() = runTest(dispatcher) {
        val vm = TimerViewModel(FakeTracker(), clock)
        vm.start(0, 10)
        runCurrent()
        vm.start(5, 0)
        runCurrent()
        assertEquals(10_000L, (vm.uiState.value as TimerUiState.Running).totalMs)
        vm.cancel()
    }

    @Test
    fun `surprise still works when the ISS tracker is offline`() = runTest(dispatcher) {
        val offline = object : IssTracker { override suspend fun currentPosition(): IssPosition? = null }
        val vm = TimerViewModel(offline, clock)

        vm.start(0, 2)
        advanceUntilIdle()

        val fired = vm.uiState.first { it is TimerUiState.Fired } as TimerUiState.Fired
        assertNotNull(fired.facts)
        assertEquals(null, fired.facts!!.issDistanceKm)
        assertEquals(2, fired.facts!!.heartbeats)
    }
}
