package com.example.medrational_android.data.reminders

import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.ProjectStatus
import retrofit2.HttpException
import java.io.IOException

/**
 * Brings the device's reminders in line with the backend: every UPCOMING and OPEN project gets its
 * reminders and the others lose theirs. Run once per app start for a signed-in user, so reminders
 * also reach users who never open the Projects screen. Returns false when the backend couldn't be
 * reached, leaving the existing reminders as they were.
 */
suspend fun syncWorkshopReminders(api: MedRationalApi, scheduler: WorkshopReminderScheduler): Boolean = try {
    val upcoming = api.getWorkshops(ProjectStatus.UPCOMING)
    val open = api.getWorkshops(ProjectStatus.OPEN)
    val upcomingBody = upcoming.body()
    val openBody = open.body()
    if (upcoming.isSuccessful && open.isSuccessful && upcomingBody != null && openBody != null) {
        scheduler.replaceAll(upcomingBody.content + openBody.content)
        true
    } else {
        false
    }
} catch (e: IOException) {
    false
} catch (e: HttpException) {
    false
}
