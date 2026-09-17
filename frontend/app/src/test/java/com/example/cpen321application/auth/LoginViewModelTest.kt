package com.example.cpen321application.auth

import com.example.cpen321application.info.InfoApi
import com.example.cpen321application.info.InfoApiException
import com.example.cpen321application.info.OwnerName
import com.example.cpen321application.ui.login.ConnectionInfo
import com.example.cpen321application.ui.login.InfoState
import com.example.cpen321application.ui.login.LoginUiState
import com.example.cpen321application.ui.login.LoginViewModel
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val user = GoogleUser("yichang@example.com", "Yichang", "Chen", "token")
    private val fixedNow = ZonedDateTime.of(2026, 9, 17, 12, 34, 56, 0, ZoneOffset.ofHoursMinutes(-7, 0))

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeAuthenticator(private val result: () -> GoogleUser) : GoogleAuthenticator {
        var signInCalls = 0
        var signOutCalls = 0
        override suspend fun signIn(): GoogleUser { signInCalls++; return result() }
        override suspend fun signOut() { signOutCalls++ }
    }

    private class FakeInfoApi(
        private val ip: () -> String = { "142.250.217.110" },
        private val time: () -> String = { "12:34:56 GMT+00:00" },
    ) : InfoApi {
        var calls = 0
        override suspend fun serverIp(): String { calls++; return ip() }
        override suspend fun serverTime(): String = time()
        override suspend fun ownerName() = OwnerName("Yichang", "Chen")
    }

    private fun newViewModel(auth: GoogleAuthenticator, api: InfoApi = FakeInfoApi()) =
        LoginViewModel(auth, api, clientIp = { "192.168.1.5" }, now = { fixedNow })

    @Test
    fun `initial state is signed out`() {
        assertEquals(LoginUiState.SignedOut, newViewModel(FakeAuthenticator { user }).uiState.value)
    }

    @Test
    fun `successful sign-in exposes the user, then loads the info table`() = runTest(dispatcher) {
        val auth = FakeAuthenticator { user }
        val api = FakeInfoApi()
        val vm = newViewModel(auth, api)

        vm.signIn()
        assertEquals(LoginUiState.SigningIn, vm.uiState.value)
        advanceUntilIdle()

        val expected = LoginUiState.SignedIn(
            user,
            InfoState.Loaded(
                ConnectionInfo(
                    serverIp = "142.250.217.110",
                    clientIp = "192.168.1.5",
                    serverTime = "12:34:56 GMT+00:00",
                    clientTime = "12:34:56 GMT-07:00",
                    owner = OwnerName("Yichang", "Chen"),
                ),
            ),
        )
        assertEquals(expected, vm.uiState.value)
        assertEquals(1, auth.signInCalls)
        assertEquals(1, api.calls)
    }

    @Test
    fun `server failure after sign-in is shown in the info section, not as a sign-in error`() = runTest(dispatcher) {
        val api = FakeInfoApi(ip = { throw InfoApiException("/api/server-ip: HTTP 502") })
        val vm = newViewModel(FakeAuthenticator { user }, api)

        vm.signIn()
        advanceUntilIdle()

        val state = vm.uiState.value as LoginUiState.SignedIn
        assertEquals(user, state.user)
        assertEquals(InfoState.Failed("/api/server-ip: HTTP 502"), state.info)
    }

    @Test
    fun `refresh re-queries the server`() = runTest(dispatcher) {
        val api = FakeInfoApi()
        val vm = newViewModel(FakeAuthenticator { user }, api)
        vm.signIn(); advanceUntilIdle()

        vm.loadInfo()
        advanceUntilIdle()

        assertEquals(2, api.calls)
        assertTrue((vm.uiState.value as LoginUiState.SignedIn).info is InfoState.Loaded)
    }

    @Test
    fun `sign-in failure exposes the message`() = runTest(dispatcher) {
        val vm = newViewModel(FakeAuthenticator { throw SignInException("Sign-in cancelled") })

        vm.signIn()
        advanceUntilIdle()

        assertEquals(LoginUiState.Error("Sign-in cancelled"), vm.uiState.value)
    }

    @Test
    fun `unexpected exception is reported as an error, not a crash`() = runTest(dispatcher) {
        val vm = newViewModel(FakeAuthenticator { throw IllegalStateException("boom") })

        vm.signIn()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is LoginUiState.Error)
        assertTrue((state as LoginUiState.Error).message.contains("boom"))
    }

    @Test
    fun `sign-in is ignored while one is already in progress`() = runTest(dispatcher) {
        val gate = CompletableDeferred<GoogleUser>()
        val auth = object : GoogleAuthenticator {
            var calls = 0
            override suspend fun signIn(): GoogleUser { calls++; return gate.await() }
            override suspend fun signOut() = Unit
        }
        val vm = newViewModel(auth)

        vm.signIn()
        vm.signIn()
        gate.complete(user)
        advanceUntilIdle()

        assertEquals(1, auth.calls)
        assertTrue(vm.uiState.value is LoginUiState.SignedIn)
    }

    @Test
    fun `sign-out returns to signed out and clears credentials`() = runTest(dispatcher) {
        val auth = FakeAuthenticator { user }
        val vm = newViewModel(auth)
        vm.signIn(); advanceUntilIdle()

        vm.signOut(); advanceUntilIdle()

        assertEquals(LoginUiState.SignedOut, vm.uiState.value)
        assertEquals(1, auth.signOutCalls)
    }
}
