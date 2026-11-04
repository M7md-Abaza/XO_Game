package com.m7mdabaza.xogame.data.prompts

import com.m7mdabaza.xogame.data.billing.FakeDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PromptsRepositoryImplTest {

    @Test
    fun `stores the last review request and update prompt separately`() = runTest {
        val repo = PromptsRepositoryImpl(FakeDataStore())
        assertNull(repo.reviewRequestedAt.first())
        assertNull(repo.updatePromptedAt.first())

        repo.setReviewRequestedAt(100)
        repo.setUpdatePromptedAt(200)
        repo.setReviewRequestedAt(300)

        assertEquals(300L, repo.reviewRequestedAt.first())
        assertEquals(200L, repo.updatePromptedAt.first())
    }
}
