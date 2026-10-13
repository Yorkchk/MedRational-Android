package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.TestStrings.en
import com.example.medrational_android.TestStrings.fr
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.AdminStatistics
import com.example.medrational_android.data.model.ContentStats
import com.example.medrational_android.data.model.FileMetric
import com.example.medrational_android.data.model.UserStats
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class StatisticsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()

    private val stats = AdminStatistics(
        users = UserStats(totalStudents = 40, weeklyActiveStudents = 25),
        lastWeek = ContentStats(topDownloaded = listOf(FileMetric(fileId = 1, fileName = "week.pdf"))),
        allTime = ContentStats(topDownloaded = listOf(FileMetric(fileId = 2, fileName = "ever.pdf")))
    )

    private fun error(code: Int) = Response.error<AdminStatistics>(code, "".toResponseBody(null))

    private fun StatisticsViewModel.success() = uiState.value as StatisticsUiState.Success

    @Test
    fun `statistics load on creation with the last 7 days selected`() {
        coEvery { api.getAdminStatistics() } returns Response.success(stats)

        val viewModel = StatisticsViewModel(api)

        val state = viewModel.success()
        assertEquals(40L, state.stats.users.totalStudents)
        assertEquals(StatRange.WEEK, state.range)
        assertEquals("week.pdf", state.content.topDownloaded.single().fileName)
    }

    @Test
    fun `switching the range shows the all-time leaderboards without reloading`() {
        coEvery { api.getAdminStatistics() } returns Response.success(stats)
        val viewModel = StatisticsViewModel(api)

        viewModel.selectRange(StatRange.ALL_TIME)

        assertEquals("ever.pdf", viewModel.success().content.topDownloaded.single().fileName)
        coVerify(exactly = 1) { api.getAdminStatistics() }
    }

    @Test
    fun `no connection shows the network error`() {
        coEvery { api.getAdminStatistics() } throws IOException("offline")

        val viewModel = StatisticsViewModel(api)

        val state = viewModel.uiState.value as StatisticsUiState.Error
        assertEquals("Network error. Check your connection and try again.", en(state.message))
    }

    @Test
    fun `server error shows the status in both languages`() {
        coEvery { api.getAdminStatistics() } returns error(500)

        val viewModel = StatisticsViewModel(api)

        val message = (viewModel.uiState.value as StatisticsUiState.Error).message
        assertEquals("Couldn't load the statistics (HTTP 500).", en(message))
        assertEquals("Impossible de charger les statistiques (HTTP 500).", fr(message))
    }

    @Test
    fun `retry after an error loads again`() {
        coEvery { api.getAdminStatistics() } throws IOException("offline") andThen Response.success(stats)
        val viewModel = StatisticsViewModel(api)

        viewModel.retry()

        assertEquals(40L, viewModel.success().stats.users.totalStudents)
    }

    @Test
    fun `refresh replaces the figures and keeps the chosen range`() {
        val newer = stats.copy(users = UserStats(totalStudents = 41))
        coEvery { api.getAdminStatistics() } returns Response.success(stats) andThen Response.success(newer)
        val viewModel = StatisticsViewModel(api)
        viewModel.selectRange(StatRange.ALL_TIME)

        viewModel.refresh()

        val state = viewModel.success()
        assertEquals(41L, state.stats.users.totalStudents)
        assertEquals(StatRange.ALL_TIME, state.range)
        assertFalse(state.refreshing)
        assertNull(state.refreshError)
    }

    @Test
    fun `failed refresh keeps the previous figures and reports once`() {
        coEvery { api.getAdminStatistics() } returns Response.success(stats) andThen error(503)
        val viewModel = StatisticsViewModel(api)

        viewModel.refresh()

        val state = viewModel.success()
        assertSame(stats, state.stats)
        assertFalse(state.refreshing)
        assertEquals("Couldn't load the statistics (HTTP 503).", en(state.refreshError))

        viewModel.refreshErrorShown()
        assertNull(viewModel.success().refreshError)
    }

    @Test
    fun `refresh from the error screen does a full load`() {
        coEvery { api.getAdminStatistics() } returns error(500) andThen Response.success(stats)
        val viewModel = StatisticsViewModel(api)

        viewModel.refresh()

        assertTrue(viewModel.uiState.value is StatisticsUiState.Success)
    }
}
