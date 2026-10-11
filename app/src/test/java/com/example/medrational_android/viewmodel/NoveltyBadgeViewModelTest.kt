package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.data.api.MedRationalApi
import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class NoveltyBadgeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()

    @Test
    fun `count starts at zero until refreshed`() {
        assertEquals(0L, NoveltyBadgeViewModel(api).unreadCount.value)
    }

    @Test
    fun `refresh reads the unread count`() {
        coEvery { api.getNoveltyBadgeCount() } returns Response.success(mapOf("unreadCount" to 4L))

        val viewModel = NoveltyBadgeViewModel(api)
        viewModel.refresh()

        assertEquals(4L, viewModel.unreadCount.value)
    }

    @Test
    fun `refresh after the feed was seen clears the count`() {
        coEvery { api.getNoveltyBadgeCount() } returns
            Response.success(mapOf("unreadCount" to 4L)) andThen Response.success(mapOf("unreadCount" to 0L))

        val viewModel = NoveltyBadgeViewModel(api)
        viewModel.refresh()
        viewModel.refresh()

        assertEquals(0L, viewModel.unreadCount.value)
    }

    @Test
    fun `failures keep the last known count`() {
        coEvery { api.getNoveltyBadgeCount() } returns
            Response.success(mapOf("unreadCount" to 2L)) andThenThrows IOException("offline") andThen
            Response.error(500, "".toResponseBody(null))

        val viewModel = NoveltyBadgeViewModel(api)
        repeat(3) { viewModel.refresh() }

        assertEquals(2L, viewModel.unreadCount.value)
    }
}
