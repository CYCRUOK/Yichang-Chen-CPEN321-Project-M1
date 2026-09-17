package com.example.cpen321application.auth

import com.example.cpen321application.ui.login.LoginUiState
import com.example.cpen321application.ui.login.LoginViewModel
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

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeAuthenticator(
        private val result: () -> GoogleUser,
    ) : GoogleAuthenticator {
        var signInCalls = 0
        var signOutCalls = 0
        override suspend fun signIn(): GoogleUser { signInCalls++; return result() }
        override suspend fun signOut() { signOutCalls++ }
    }

    @Test
    fun `initial state is signed out`() {
        val vm = LoginViewModel(FakeAuthenticator { user })
        assertEquals(LoginUiState.SignedOut, vm.uiState.value)
    }

    @Test
    fun `successful sign-in exposes the user`() = runTest(dispatcher) {
        val auth = FakeAuthenticator { user }
        val vm = LoginViewModel(auth)

        vm.signIn()
        assertEquals(LoginUiState.SigningIn, vm.uiState.value)
        advanceUntilIdle()

        assertEquals(LoginUiState.SignedIn(user), vm.uiState.value)
        assertEquals(1, auth.signInCalls)
    }

    @Test
    fun `sign-in failure exposes the message`() = runTest(dispatcher) {
        val vm = LoginViewModel(FakeAuthenticator { throw SignInException("Sign-in cancelled") })

        vm.signIn()
        advanceUntilIdle()

        assertEquals(LoginUiState.Error("Sign-in cancelled"), vm.uiState.value)
    }

    @Test
    fun `unexpected exception is reported as an error, not a crash`() = runTest(dispatcher) {
        val vm = LoginViewModel(FakeAuthenticator { throw IllegalStateException("boom") })

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
        val vm = LoginViewModel(auth)

        vm.signIn()
        vm.signIn()
        gate.complete(user)
        advanceUntilIdle()

        assertEquals(1, auth.calls)
        assertEquals(LoginUiState.SignedIn(user), vm.uiState.value)
    }

    @Test
    fun `sign-out returns to signed out and clears credentials`() = runTest(dispatcher) {
        val auth = FakeAuthenticator { user }
        val vm = LoginViewModel(auth)
        vm.signIn(); advanceUntilIdle()

        vm.signOut(); advanceUntilIdle()

        assertEquals(LoginUiState.SignedOut, vm.uiState.value)
        assertEquals(1, auth.signOutCalls)
    }
}
