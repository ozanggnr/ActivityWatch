package com.ozang.myfitnessozzy.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class StepsViewModel(application: Application) : AndroidViewModel(application) {

    private val client = HealthConnectClient.getOrCreate(application)

    private val _todaySteps = MutableStateFlow(0L)
    val todaySteps: StateFlow<Long> = _todaySteps

    private val _last28DaysSteps = MutableStateFlow(0L)
    val last28DaysSteps: StateFlow<Long> = _last28DaysSteps

    private val _last90DaysSteps = MutableStateFlow(0L)
    val last90DaysSteps: StateFlow<Long> = _last90DaysSteps

    init {
        refreshSteps()
    }

    fun refreshSteps() {
        viewModelScope.launch {
            try {
                val now = Instant.now()
                val startOfToday = now.truncatedTo(ChronoUnit.DAYS)
                _todaySteps.value = readStepsInRange(startOfToday, now)

                _last28DaysSteps.value = readStepsInRange(now.minus(28, ChronoUnit.DAYS), now)

                _last90DaysSteps.value = readStepsInRange(now.minus(90, ChronoUnit.DAYS), now)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun readStepsInRange(start: Instant, end: Instant): Long {
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response.records.sumOf { it.count }
    }
}
