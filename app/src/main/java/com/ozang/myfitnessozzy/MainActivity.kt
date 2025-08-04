package com.ozang.myfitnessozzy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ozang.myfitnessozzy.screens.*
import com.ozang.myfitnessozzy.ui.theme.MyFitnessOzzyTheme
import com.ozang.myfitnessozzy.viewmodel.HomeViewModel
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.core.net.toUri
import com.ozang.myfitnessozzy.permissions.PermissionUtils

class MainActivity : ComponentActivity() {

    private var healthConnectClient: HealthConnectClient? = null
    private var isHealthConnectAvailable = false
    val homeViewModel: HomeViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions: Set<String> ->
        homeViewModel.refreshPermissions()
    }

    private fun initializeHealthConnect() {
        try {
            val sdkStatus = HealthConnectClient.getSdkStatus(this)

            when (sdkStatus) {
                HealthConnectClient.SDK_AVAILABLE -> {
                    healthConnectClient = HealthConnectClient.getOrCreate(this)
                    homeViewModel.setHealthClient(healthConnectClient!!)
                    isHealthConnectAvailable = true
                }

                HealthConnectClient.SDK_UNAVAILABLE -> {
                    isHealthConnectAvailable = false
                    Toast.makeText(
                        this,
                        "Bu cihaz Health Connect'i desteklemiyor",
                        Toast.LENGTH_LONG
                    ).show()
                }

                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    isHealthConnectAvailable = false
                    redirectToHealthConnectPlayStore()
                }
            }
        } catch (e: Exception) {
            isHealthConnectAvailable = false
        }
    }

    private fun redirectToHealthConnectPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data =
                    "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata".toUri()
                setPackage("com.android.vending")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Lütfen Play Store'u yükleyin", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestPermissionsSafely(permissions: Set<String>) {
        if (!isHealthConnectAvailable) {
            val sdkStatus = HealthConnectClient.getSdkStatus(this)

            when (sdkStatus) {
                HealthConnectClient.SDK_AVAILABLE -> {

                    Log.w("MainActivity", "SDK is available but client is null")
                    return
                }

                HealthConnectClient.SDK_UNAVAILABLE -> {
                    Toast.makeText(
                        this,
                        "Bu cihaz Health Connect'i desteklemiyor",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    Toast.makeText(
                        this,
                        "Health Connect yüklü değil veya güncelleme gerekli",
                        Toast.LENGTH_LONG
                    ).show()
                    redirectToHealthConnectPlayStore()
                    return
                }
            }
            return
        }

        permissionLauncher.launch(permissions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeHealthConnect()
        setupUI()

    }

    private fun setupUI() {
        setContent {
            MyFitnessOzzyTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            homeViewModel = homeViewModel,
                            onNavigateToSteps = { navController.navigate("steps") },
                            onNavigateToCycling = { navController.navigate("cycling") },
                            onNavigateToCalories = { navController.navigate("calories") },
                            onRequestPermissions = { permissions ->
                                requestPermissionsSafely(permissions)
                            }
                        )
                    }
                    composable("steps") {
                        StepsScreen(
                            homeViewModel = homeViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("cycling") {
                        CyclingScreen(
                            homeViewModel = homeViewModel,
                            onBack = { navController.popBackStack() },
                            onRequestWritePermission = {
                                requestWritePermissions(PermissionUtils.getCyclingPermissions())
                            }
                        )
                    }
                    composable("calories") {
                        CaloriesScreen(
                            homeViewModel = homeViewModel,
                            onBack = { navController.popBackStack() },
                            onRequestWritePermission = {
                                requestWritePermissions(PermissionUtils.getCaloriesPermissions())
                            }
                        )
                    }
                }
            }
        }
    }

    private fun requestWritePermissions(allPermissions: Set<String>) {
        if (isHealthConnectAvailable) {
            val writePermissions = allPermissions.filter { it.contains("WRITE") }.toSet()
            requestPermissionsSafely(writePermissions)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isHealthConnectAvailable) {
            homeViewModel.refreshPermissions()
        }
    }
}