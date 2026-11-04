package com.m7mdabaza.xogame.presentation.update

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.m7mdabaza.xogame.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The flexible Play update flow, driven by Google's FakeAppUpdateManager. */
@RunWith(AndroidJUnit4::class)
class InAppUpdatesTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fake = FakeAppUpdateManager(context)
    private val restart = context.getString(R.string.update_restart)
    private var prompts = 0

    private fun show(allowPrompt: Boolean) {
        val snackbar = SnackbarHostState()
        rule.setContent {
            InAppUpdates(snackbar, shouldPrompt = { allowPrompt }, onPrompted = { prompts++ }, manager = fake)
            SnackbarHost(snackbar)
        }
    }

    @Test
    fun anUpdateIsOfferedDownloadedAndInstalledOnRestart() {
        fake.setUpdateAvailable(99, AppUpdateType.FLEXIBLE)
        show(allowPrompt = true)

        rule.waitUntil(5_000) { fake.isConfirmationDialogVisible }
        assertEquals(1, prompts)

        rule.runOnUiThread {
            fake.userAcceptsUpdate()
            fake.downloadStarts()
            fake.downloadCompletes()
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithText(restart).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(restart).performClick()
        rule.waitUntil(5_000) { fake.isInstallSplashScreenVisible }
    }

    @Test
    fun noOfferWhenThePromptRuleSaysNo() {
        fake.setUpdateAvailable(99, AppUpdateType.FLEXIBLE)
        show(allowPrompt = false)
        rule.waitForIdle()
        assertFalse(fake.isConfirmationDialogVisible)
        assertEquals(0, prompts)
    }

    @Test
    fun nothingHappensWithoutAnUpdate() {
        fake.setUpdateNotAvailable()
        show(allowPrompt = true)
        rule.waitForIdle()
        assertFalse(fake.isConfirmationDialogVisible)
        assertEquals(0, prompts)
    }
}
