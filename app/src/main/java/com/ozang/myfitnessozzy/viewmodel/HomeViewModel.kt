package com.ozang.myfitnessozzy.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.permission.HealthPermission
import com.ozang.myfitnessozzy.permissions.PermissionUtils
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val healthConnectClient = HealthConnectClient.getOrCreate(application)

    // Permission states
    private val _stepPermissionGranted = MutableStateFlow(false)
    val stepPermissionGranted: StateFlow<Boolean> = _stepPermissionGranted

    private val _cyclingPermissionGranted = MutableStateFlow(false)
    val cyclingPermissionGranted: StateFlow<Boolean> = _cyclingPermissionGranted

    private val _caloriesPermissionGranted = MutableStateFlow(false)
    val caloriesPermissionGranted: StateFlow<Boolean> = _caloriesPermissionGranted

    // Write permission states
    private val _cyclingWritePermissionGranted = MutableStateFlow(false)
    val cyclingWritePermissionGranted: StateFlow<Boolean> = _cyclingWritePermissionGranted

    private val _caloriesWritePermissionGranted = MutableStateFlow(false)
    val caloriesWritePermissionGranted: StateFlow<Boolean> = _caloriesWritePermissionGranted

    // Data states
    private val _stepsData = MutableStateFlow<List<StepsRecord>>(emptyList())
    val stepsData: StateFlow<List<StepsRecord>> = _stepsData

    private val _caloriesData = MutableStateFlow<List<TotalCaloriesBurnedRecord>>(emptyList())
    val caloriesData: StateFlow<List<TotalCaloriesBurnedRecord>> = _caloriesData

    private val _cyclingData = MutableStateFlow<List<CyclingPedalingCadenceRecord>>(emptyList())
    val cyclingData: StateFlow<List<CyclingPedalingCadenceRecord>> = _cyclingData

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingSteps = MutableStateFlow(false)
    val isLoadingSteps: StateFlow<Boolean> = _isLoadingSteps

    private val _isLoadingCalories = MutableStateFlow(false)
    val isLoadingCalories: StateFlow<Boolean> = _isLoadingCalories

    private val _isLoadingCycling = MutableStateFlow(false)
    val isLoadingCycling: StateFlow<Boolean> = _isLoadingCycling

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                delay(100) // Small delay to prevent rapid calls

                val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
                Log.d("HomeViewModel", "All granted permissions: $grantedPermissions")

                val stepPermissions = PermissionUtils.getStepPermissions()
                val cyclingPermissions = PermissionUtils.getCyclingPermissions()
                val caloriesPermissions = PermissionUtils.getCaloriesPermissions()

                _stepPermissionGranted.value = stepPermissions.any { grantedPermissions.contains(it) }
                _cyclingPermissionGranted.value = cyclingPermissions.any { grantedPermissions.contains(it) }
                _caloriesPermissionGranted.value = caloriesPermissions.any { grantedPermissions.contains(it) }

                // Check write permissions specifically
                _cyclingWritePermissionGranted.value = grantedPermissions.contains(
                    HealthPermission.getWritePermission(ExerciseSessionRecord::class)
                )
                _caloriesWritePermissionGranted.value = grantedPermissions.contains(
                    HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
                )

                Log.d("HomeViewModel", "Step permissions granted: ${_stepPermissionGranted.value}")
                Log.d("HomeViewModel", "Cycling permissions granted: ${_cyclingPermissionGranted.value}")
                Log.d("HomeViewModel", "Calories permissions granted: ${_caloriesPermissionGranted.value}")
                Log.d("HomeViewModel", "Cycling write permission: ${_cyclingWritePermissionGranted.value}")
                Log.d("HomeViewModel", "Calories write permission: ${_caloriesWritePermissionGranted.value}")

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error checking permissions", e)
                // Reset all permissions to false on error
                _stepPermissionGranted.value = false
                _cyclingPermissionGranted.value = false
                _caloriesPermissionGranted.value = false
                _cyclingWritePermissionGranted.value = false
                _caloriesWritePermissionGranted.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadStepsData() {
        if (!_stepPermissionGranted.value) {
            Log.d("HomeViewModel", "Step permissions not granted, skipping data load")
            return
        }

        viewModelScope.launch {
            _isLoadingSteps.value = true
            try {
                val now = Instant.now()
                val startOfDay = now.truncatedTo(ChronoUnit.DAYS)

                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )

                val response = healthConnectClient.readRecords(request)
                _stepsData.value = response.records

                Log.d("HomeViewModel", "Loaded ${response.records.size} step records")

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading steps data", e)
                _stepsData.value = emptyList()
            } finally {
                _isLoadingSteps.value = false
            }
        }
    }

    fun loadCaloriesData() {
        if (!_caloriesPermissionGranted.value) {
            Log.d("HomeViewModel", "Calories permissions not granted, skipping data load")
            return
        }

        viewModelScope.launch {
            _isLoadingCalories.value = true
            try {
                val now = Instant.now()
                val startOfDay = now.truncatedTo(ChronoUnit.DAYS)

                val request = ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )

                val response = healthConnectClient.readRecords(request)
                _caloriesData.value = response.records

                Log.d("HomeViewModel", "Loaded ${response.records.size} calories records")

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading calories data", e)
                _caloriesData.value = emptyList()
            } finally {
                _isLoadingCalories.value = false
            }
        }
    }

    fun loadCyclingData() {
        if (!_cyclingPermissionGranted.value) {
            Log.d("HomeViewModel", "Cycling permissions not granted, skipping data load")
            return
        }

        viewModelScope.launch {
            _isLoadingCycling.value = true
            try {
                val now = Instant.now()
                val startOfWeek = now.minus(7, ChronoUnit.DAYS)

                val request = ReadRecordsRequest(
                    recordType = CyclingPedalingCadenceRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfWeek, now)
                )

                val response = healthConnectClient.readRecords(request)
                _cyclingData.value = response.records

                Log.d("HomeViewModel", "Loaded ${response.records.size} cycling records")

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading cycling data", e)
                _cyclingData.value = emptyList()
            } finally {
                _isLoadingCycling.value = false
            }
        }
    }

    @SuppressLint("RestrictedApi")
    fun addCyclingSession(durationMinutes: Long) {
        if (!_cyclingWritePermissionGranted.value) {
            Log.e("HomeViewModel", "No write permission for cycling")
            return
        }

        viewModelScope.launch {
            try {
                val now = Instant.now()
                val startTime = now.minus(durationMinutes, ChronoUnit.MINUTES)
                val zoneOffset = java.time.ZoneId.systemDefault().rules.getOffset(now)

                val exerciseSession = ExerciseSessionRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset,
                    endTime = now,
                    endZoneOffset = zoneOffset,
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
                    title = "Bisiklet Antrenmanı"
                )

                healthConnectClient.insertRecords(listOf(exerciseSession))
                Log.d("HomeViewModel", "Added cycling session: $durationMinutes minutes ${ExerciseSessionRecord.EXERCISE_TYPE_BIKING} ")

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error adding cycling session", e)
            }
        }
    }

    fun addCaloriesBurned(calories: Double) {
        if (!_caloriesWritePermissionGranted.value) {
            Log.e("HomeViewModel", "No write permission for calories")
            return
        }

        viewModelScope.launch {
            try {
                val now = Instant.now()
                val startTime = now.minus(1, ChronoUnit.HOURS)
                val zoneOffset = java.time.ZoneId.systemDefault().rules.getOffset(now)

                val caloriesRecord = TotalCaloriesBurnedRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset,
                    endTime = now,
                    endZoneOffset = zoneOffset,
                    energy = Energy.kilocalories(calories)
                )

                healthConnectClient.insertRecords(listOf(caloriesRecord))
                Log.d("HomeViewModel", "Added calories: $calories kcal")


                delay(500)
                loadCaloriesData()

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error adding calories", e)
            }
        }
    }

    fun getTotalStepsToday(): Long {
        return try {
            _stepsData.value.sumOf { it.count }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error calculating total steps", e)
            0L
        }
    }

    fun getTotalCaloriesToday(): Double {
        return try {
            _caloriesData.value.sumOf { it.energy.inKilocalories }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error calculating total calories", e)
            0.0
        }
    }
}