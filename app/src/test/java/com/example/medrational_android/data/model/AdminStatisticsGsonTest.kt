package com.example.medrational_android.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminStatisticsGsonTest {

    private val gson = Gson()

    @Test
    fun `dashboard response is read field for field`() {
        val stats = gson.fromJson(
            """
            {"generatedAt":"2026-10-13T15:00:00.123",
             "users":{"totalStudents":40,"weeklyActiveStudents":25,"weeklyActiveStudentsPreviousWeek":20,
                      "weeklySignUps":[{"periodStart":"2026-10-07","value":6}]},
             "usage":{"downloadsThisWeek":70,"dailyViews":[{"periodStart":"2026-10-13","value":3}]},
             "lastWeek":{"topDownloaded":[{"fileId":2,"fileName":"avc.pdf","reasoningId":5,
                         "categoryName":null,"metricValue":20,"avgRating":4.5,"totalRatings":2}],
                         "categories":[{"categoryId":1,"categoryName":"Cardiologie","downloads":7,"views":1,"favorites":1}],
                         "topHashtags":[{"name":"ecg","downloads":4}]},
             "trends":{"files":[{"id":2,"name":"avc.pdf","reasoningId":5,"thisWeek":20,"previousWeek":0}]},
             "engagement":{"averageRating":4.3,"openSuggestions":2,"openWorkshops":1}}
            """,
            AdminStatistics::class.java
        )

        assertEquals(40L, stats.users.totalStudents)
        assertEquals(StatPoint("2026-10-07", 6), stats.users.weeklySignUps.single())
        assertEquals(70L, stats.usage.downloadsThisWeek)
        val top = stats.lastWeek.topDownloaded.single()
        assertEquals(5L, top.reasoningId)
        assertNull(top.categoryName)
        assertEquals(20L, top.metricValue)
        assertEquals("Cardiologie", stats.lastWeek.categories.single().categoryName)
        assertEquals("ecg", stats.lastWeek.topHashtags.single().name)
        assertEquals(20L, stats.trends.files.single().thisWeek)
        assertEquals(4.3, stats.engagement.averageRating!!, 0.0)
    }

    @Test
    fun `missing sections fall back to empty values`() {
        val stats = gson.fromJson("""{"users":{"totalStudents":3}}""", AdminStatistics::class.java)

        assertEquals(3L, stats.users.totalStudents)
        assertTrue(stats.users.weeklySignUps.isEmpty())
        assertTrue(stats.allTime.topViewed.isEmpty())
        assertTrue(stats.trends.categories.isEmpty())
        assertNull(stats.engagement.averageRating)
    }
}
