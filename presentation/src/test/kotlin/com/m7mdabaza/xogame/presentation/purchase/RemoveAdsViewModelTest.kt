package com.m7mdabaza.xogame.presentation.purchase

import com.m7mdabaza.xogame.domain.model.PurchaseResult
import com.m7mdabaza.xogame.domain.model.RestoreResult
import com.m7mdabaza.xogame.domain.usecase.ObserveAdsRemovedUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveRemoveAdsOfferUseCase
import com.m7mdabaza.xogame.domain.usecase.PurchaseRemoveAdsUseCase
import com.m7mdabaza.xogame.domain.usecase.RestorePurchasesUseCase
import com.m7mdabaza.xogame.domain.usecase.SimulateRemoveAdsUseCase
import com.m7mdabaza.xogame.presentation.testing.FakePurchaseRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoveAdsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val purchases = FakePurchaseRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** A ViewModel whose state is collected (stateIn(WhileSubscribed) needs a collector). */
    private fun TestScope.viewModel() = RemoveAdsViewModel(
        ObserveAdsRemovedUseCase(purchases),
        ObserveRemoveAdsOfferUseCase(purchases),
        PurchaseRemoveAdsUseCase(purchases),
        RestorePurchasesUseCase(purchases),
        SimulateRemoveAdsUseCase(purchases),
    ).also { it.uiState.launchIn(backgroundScope) }

    // --- Badge ---------------------------------------------------------------------------

    @Test
    fun `the badge shows while ads do and Play offers Remove ads`() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(RemoveAdsUiState(price = "$1.99", canSimulatePurchase = true), vm.uiState.value)
        assertTrue(vm.uiState.value.showBadge)
    }

    @Test
    fun `no badge once Remove ads is bought`() = runTest(dispatcher) {
        val vm = viewModel()
        purchases.adsRemoved.value = true
        assertFalse(vm.uiState.value.showBadge)
    }

    @Test
    fun `no badge when Play can't sell Remove ads`() = runTest(dispatcher) {
        val vm = viewModel()
        purchases.removeAdsOffer.value = null
        assertFalse(vm.uiState.value.showBadge)
    }

    // --- Dialog --------------------------------------------------------------------------

    @Test
    fun `the dialog opens and closes`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.openDialog()
        assertTrue(vm.uiState.value.showDialog)
        vm.dismissDialog()
        assertFalse(vm.uiState.value.showDialog)
    }

    @Test
    fun `buying closes the dialog, thanks the player and removes the badge`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.openDialog()
        vm.buy()
        val state = vm.uiState.value
        assertEquals(RemoveAdsMessage.PURCHASED, state.message)
        assertFalse(state.showDialog)
        assertFalse(state.showBadge)
        vm.onMessageShown()
        assertEquals(null, vm.uiState.value.message)
    }

    @Test
    fun `a cancelled purchase says nothing and leaves the dialog open`() = runTest(dispatcher) {
        val vm = viewModel()
        purchases.purchaseResult = PurchaseResult.CANCELLED
        vm.openDialog()
        vm.buy()
        assertEquals(null, vm.uiState.value.message)
        assertTrue(vm.uiState.value.showDialog)
        assertFalse(vm.uiState.value.busy)
    }

    @Test
    fun `the dialog closes when ads are removed elsewhere`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.openDialog()
        purchases.adsRemoved.value = true
        assertFalse(vm.uiState.value.showDialog)
    }

    @Test
    fun `busy while Google Play works, and a second tap doesn't start another purchase`() = runTest(dispatcher) {
        val vm = viewModel()
        val gate = CompletableDeferred<Unit>()
        purchases.purchaseGate = gate
        vm.buy()
        assertTrue(vm.uiState.value.busy)
        vm.buy()
        vm.restore()
        assertEquals(1, purchases.purchaseCalls)

        gate.complete(Unit)
        assertFalse(vm.uiState.value.busy)
        assertEquals(RemoveAdsMessage.PURCHASED, vm.uiState.value.message)
    }

    // --- Results -------------------------------------------------------------------------

    @Test
    fun `every purchase outcome maps to its message`() = runTest(dispatcher) {
        val vm = viewModel()
        val expected = mapOf(
            PurchaseResult.CANCELLED to null,
            PurchaseResult.PENDING to RemoveAdsMessage.PENDING,
            PurchaseResult.ALREADY_OWNED to RemoveAdsMessage.ALREADY_OWNED,
            PurchaseResult.UNAVAILABLE to RemoveAdsMessage.UNAVAILABLE,
            PurchaseResult.ERROR to RemoveAdsMessage.PURCHASE_ERROR,
            PurchaseResult.PURCHASED to RemoveAdsMessage.PURCHASED,
        )
        for ((result, message) in expected) {
            purchases.purchaseResult = result
            vm.openDialog()
            vm.buy()
            assertEquals("for $result", message, vm.uiState.value.message)
            assertEquals("dialog for $result", message == null, vm.uiState.value.dialogOpen)
            assertFalse(vm.uiState.value.busy)
            vm.onMessageShown()
        }
    }

    @Test
    fun `restore reports what it found`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.restore()
        assertEquals(RemoveAdsMessage.NOTHING_TO_RESTORE, vm.uiState.value.message)

        purchases.restoreResult = RestoreResult.ERROR
        vm.restore()
        assertEquals(RemoveAdsMessage.RESTORE_ERROR, vm.uiState.value.message)

        purchases.restoreResult = RestoreResult.RESTORED
        vm.openDialog()
        vm.restore()
        assertEquals(RemoveAdsMessage.RESTORED, vm.uiState.value.message)
        assertTrue(vm.uiState.value.adsRemoved)
        assertFalse(vm.uiState.value.showDialog)
    }

    @Test
    fun `the debug switch simulates the purchase`() = runTest(dispatcher) {
        val vm = viewModel()
        assertTrue(vm.uiState.value.canSimulatePurchase)
        vm.simulate(true)
        assertTrue(vm.uiState.value.adsRemoved)
        vm.simulate(false)
        assertFalse(vm.uiState.value.adsRemoved)
    }
}
