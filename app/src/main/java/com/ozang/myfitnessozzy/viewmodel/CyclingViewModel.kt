package com.ozang.myfitnessozzy.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

class CyclingViewModel(application: Application) : AndroidViewModel(application) {

    private val client = HealthConnectClient.getOrCreate(application)

    private val _todayCyclingMinutes = MutableStateFlow(0L)
    val todayCyclingMinutes: StateFlow<Long> = _todayCyclingMinutes

    private val _last28DaysCyclingMinutes = MutableStateFlow(0L)
    val last28DaysCyclingMinutes: StateFlow<Long> = _last28DaysCyclingMinutes

    private val _last90DaysCyclingMinutes = MutableStateFlow(0L)
    val last90DaysCyclingMinutes: StateFlow<Long> = _last90DaysCyclingMinutes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        refreshCycling()
    }

    fun refreshCycling() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val now = Instant.now()
                val startOfToday = now.truncatedTo(ChronoUnit.DAYS)

                _todayCyclingMinutes.value = readCyclingDurationInRange(startOfToday, now)
                _last28DaysCyclingMinutes.value = readCyclingDurationInRange(now.minus(28, ChronoUnit.DAYS), now)
                _last90DaysCyclingMinutes.value = readCyclingDurationInRange(now.minus(90, ChronoUnit.DAYS), now)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun readCyclingDurationInRange(start: Instant, end: Instant): Long {
        try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            Log.d("BikingData","total biking value = ${response.records.filter {it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_BIKING}}")
            return response.records

                .filter { it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_BIKING }
                .sumOf {
                    val durationSeconds = it.endTime.epochSecond - it.startTime.epochSecond
                    durationSeconds / 60
                }
        } catch (e: Exception) {
            Log.d("BikingData","error $e")
            e.printStackTrace()
            return 0L
        }
    }
}