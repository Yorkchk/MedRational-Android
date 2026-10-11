package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.data.api.MedRationalApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

// Number of files uploaded since the user last opened "What's new", for the home screen badges
class NoveltyBadgeViewModel(private val api: MedRationalApi) : ViewModel() {

    private val _unreadCount = MutableStateFlow(0L)
    val unreadCount: StateFlow<Long> = _unreadCount.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            try {
                val response = api.getNoveltyBadgeCount()
                val count = response.body()?.get("unreadCount")
                if (response.isSuccessful && count != null) {
                    _unreadCount.value = count
                }
            } catch (e: IOException) {
                // A badge isn't worth an error message; keep the last known count
            } catch (e: HttpException) {
                // Same as above
            }
        }
    }
}
