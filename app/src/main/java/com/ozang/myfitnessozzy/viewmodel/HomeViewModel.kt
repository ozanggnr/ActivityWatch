package com.ozang.myfitnessozzy.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
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
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private var healthConnectClient: HealthConnectClient? = null

    fun setHealthClient(client: HealthConnectClient) {
        this.healthConnectClient = client
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


    private val _stepsData = MutableStateFlow<List<StepsRecord>>(emptyList())
    val stepsData: StateFlow<List<StepsRecord>> = _stepsData

    private val _caloriesData = MutableStateFlow<List<TotalCaloriesBurnedRecord>>(emptyList())
    val caloriesData: StateFlow<List<TotalCaloriesBurnedRecord>> = _caloriesData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingSteps = MutableStateFlow(false)
    val isLoadingSteps: StateFlow<Boolean> = _isLoadingSteps

    private val _isLoadingCalories = MutableStateFlow(false)
    val isLoadingCalories: StateFlow<Boolean> = _isLoadingCalories

    fun refreshPermissions() {
        val client = healthConnectClient ?: return

        viewModelScope.launch {
            _isLoading.value = true

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

            _isLoading.value = false
        }
    }

    fun loadStepsData() {
        if (!_stepPermissionGranted.value) return
        val client = healthConnectClient ?: return

        viewModelScope.launch {
            _isLoadingSteps.value = true

            val now = Instant.now()
            val startOfDay = now.truncatedTo(ChronoUnit.DAYS)

            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            _stepsData.value = response.records
            _isLoadingSteps.value = false
        }
    }

    fun loadCaloriesData() {
        if (!_caloriesPermissionGranted.value) return
        val client = healthConnectClient ?: return

        viewModelScope.launch {
            _isLoadingCalories.value = true

            val now = Instant.now()
            val startOfDay = now.truncatedTo(ChronoUnit.DAYS)

            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            _caloriesData.value = response.records
            _isLoadingCalories.value = false
        }
    }

    @SuppressLint("RestrictedApi")
    fun addCyclingSession(durationMinutes: Long) {
        if (!_cyclingWritePermissionGranted.value) return
        val client = healthConnectClient ?: return

        viewModelScope.launch {
            val now = Instant.now()
            val startTime = now.minus(durationMinutes, ChronoUnit.MINUTES)

            val zoneOffset = ZoneId.systemDefault().rules.getOffset(now)

            val session = ExerciseSessionRecord(
                startTime = startTime,
                startZoneOffset = zoneOffset,
                endTime = now,
                endZoneOffset = zoneOffset,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
                title = "Bisiklet Antrenmanı"
            )

            client.insertRecords(listOf(session))
        }
    }

    fun addCaloriesBurned(calories: Double) {
        if (!_caloriesWritePermissionGranted.value) return
        val client = healthConnectClient ?: return

        viewModelScope.launch {
            val now = Instant.now()
            val startTime = now.minus(1, ChronoUnit.HOURS)

            val zoneOffset = ZoneId.systemDefault().rules.getOffset(now)

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
        }
    }

    fun getTotalStepsToday(): Long = _stepsData.value.sumOf { it.count }

    fun getTotalCaloriesToday(): Double = _caloriesData.value.sumOf { it.energy.inKilocalories }
}