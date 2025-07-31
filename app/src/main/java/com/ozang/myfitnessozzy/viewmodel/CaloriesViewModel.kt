package com.ozang.myfitnessozzy.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit


class CaloriesViewModel(application: Application) : AndroidViewModel(application) {

    private val client = HealthConnectClient.getOrCreate(application)

    private val _todayCalories = MutableStateFlow(0.0)
    val todayCalories: StateFlow<Double> = _todayCalories

    private val _last28DaysCalories = MutableStateFlow(0.0)
    val last28DaysCalories: StateFlow<Double> = _last28DaysCalories

    private val _last90DaysCalories = MutableStateFlow(0.0)
    val last90DaysCalories: StateFlow<Double> = _last90DaysCalories

    init {
        refreshCalories()
    }

    fun refreshCalories() {
        viewModelScope.launch {
            try {
                val now = Instant.now()
                val startOfToday = now.truncatedTo(ChronoUnit.DAYS)
                _todayCalories.value = readCaloriesInRange(startOfToday, now)

                _last28DaysCalories.value = readCaloriesInRange(now.minus(28, ChronoUnit.DAYS), now)

                _last90DaysCalories.value = readCaloriesInRange(now.minus(90, ChronoUnit.DAYS), now)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun readCaloriesInRange(start: Instant, end: Instant): Double {
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response.records.sumOf { it.energy.inKilocalories }
    }
}

