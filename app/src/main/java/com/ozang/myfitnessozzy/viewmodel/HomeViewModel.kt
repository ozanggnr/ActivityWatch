package com.ozang.myfitnessozzy.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.ozang.myfitnessozzy.permissions.PermissionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private var healthConnectClient: HealthConnectClient? = null

    fun setHealthClient(client: HealthConnectClient) {
        this.healthConnectClient = client
    }

    suspend fun getGrantedPermissions(): Set<String> {
        return healthConnectClient?.permissionController?.getGrantedPermissions() ?: emptySet()
    }


    private val _stepPermissionGranted = MutableStateFlow(false)
    val stepPermissionGranted: StateFlow<Boolean> = _stepPermissionGranted

    private val _cyclingPermissionGranted = MutableStateFlow(false)
    val cyclingPermissionGranted: StateFlow<Boolean> = _cyclingPermissionGranted

    private val _caloriesPermissionGranted = MutableStateFlow(false)
    val caloriesPermissionGranted: StateFlow<Boolean> = _caloriesPermissionGranted

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

    fun refreshPermissions() {
        val client = healthConnectClient ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                delay(100)

                val grantedPermissions = client.permissionController.getGrantedPermissions()

                _stepPermissionGranted.value = PermissionUtils.getStepPermissions()
                    .any { grantedPermissions.contains(it) }

                _cyclingPermissionGranted.value = PermissionUtils.getCyclingPermissions()
                    .any { grantedPermissions.contains(it) }

                _caloriesPermissionGranted.value = PermissionUtils.getCaloriesPermissions()
                    .any { grantedPermissions.contains(it) }

                _cyclingWritePermissionGranted.value = grantedPermissions.contains(
                    HealthPermission.getWritePermission(ExerciseSessionRecord::class)
                )
                _caloriesWritePermissionGranted.value = grantedPermissions.contains(
                    HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
                )

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error checking permissions", e)
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
        if (!_stepPermissionGranted.value) return

        val client = healthConnectClient ?: return

        viewModelScope.launch {
            _isLoadingSteps.value = true
            try {
                val now = Instant.now()
                val startOfDay = now.truncatedTo(ChronoUnit.DAYS)

                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )
                _stepsData.value = response.records

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading steps data", e)
                _stepsData.value = emptyList()
            } finally {
                _isLoadingSteps.value = false
            }
        }
    }

    fun loadCaloriesData() {
        if (!_caloriesPermissionGranted.value) return

        val client = healthConnectClient ?: return

        viewModelScope.launch {
            _isLoadingCalories.value = true
            try {
                val now = Instant.now()
                val startOfDay = now.truncatedTo(ChronoUnit.DAYS)

                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = TotalCaloriesBurnedRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )
                _caloriesData.value = response.records

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading calories data", e)
                _caloriesData.value = emptyList()
            } finally {
                _isLoadingCalories.value = false
            }
        }
    }

    fun loadCyclingData() {
        if (!_cyclingPermissionGranted.value) return

        val client = healthConnectClient ?: return

        viewModelScope.launch {
            _isLoadingCycling.value = true
            try {
                val now = Instant.now()
                val startOfWeek = now.minus(7, ChronoUnit.DAYS)

                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = CyclingPedalingCadenceRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfWeek, now)
                    )
                )
                _cyclingData.value = response.records

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
        if (!_cyclingWritePermissionGranted.value) return

        val client = healthConnectClient ?: return

        viewModelScope.launch {
            try {
                val now = Instant.now()
                val startTime = now.minus(durationMinutes, ChronoUnit.MINUTES)
                val zoneOffset = java.time.ZoneId.systemDefault().rules.getOffset(now)

                val session = ExerciseSessionRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset,
                    endTime = now,
                    endZoneOffset = zoneOffset,
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
                    title = "Bisiklet Antrenmanı"
                )

                client.insertRecords(listOf(session))

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error adding cycling session", e)
            }
        }
    }

    fun addCaloriesBurned(calories: Double) {
        if (!_caloriesWritePermissionGranted.value) return

        val client = healthConnectClient ?: return

        viewModelScope.launch {
            try {
                val now = Instant.now()
                val startTime = now.minus(1, ChronoUnit.HOURS)
                val zoneOffset = java.time.ZoneId.systemDefault().rules.getOffset(now)

                val record = TotalCaloriesBurnedRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset,
                    endTime = now,
                    endZoneOffset = zoneOffset,
                    energy = Energy.kilocalories(calories)
                )

                client.insertRecords(listOf(record))

                delay(500)
                loadCaloriesData()

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error adding calories", e)
            }
        }
    }

    fun getTotalStepsToday(): Long = try {
        _stepsData.value.sumOf { it.count }
    } catch (e: Exception) {
        Log.e("HomeViewModel", "Error calculating steps", e)
        0L
    }

    fun getTotalCaloriesToday(): Double = try {
        _caloriesData.value.sumOf { it.energy.inKilocalories }
    } catch (e: Exception) {
        Log.e("HomeViewModel", "Error calculating calories", e)
        0.0
    }
}
