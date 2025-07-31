package com.ozang.myfitnessozzy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ozang.myfitnessozzy.screens.*
import com.ozang.myfitnessozzy.ui.theme.MyFitnessOzzyTheme
import com.ozang.myfitnessozzy.viewmodel.HomeViewModel
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.ozang.myfitnessozzy.permissions.PermissionUtils
import kotlinx.coroutines.delay
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    private fun isHealthConnectInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("com.google.android.apps.healthdata", 0)
            true
        } catch (e: Exception) {
            false
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
            Log.e("MainActivity", "Play Store açılamadı", e)
            Toast.makeText(this, "Play Store açılamadı", Toast.LENGTH_SHORT).show()
        }
    }


    private lateinit var healthConnectClient: HealthConnectClient
    val homeViewModel: HomeViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions: Set<String> ->
        Log.d("MainActivity", "Permission result: $grantedPermissions")
        try {
            if (grantedPermissions.isNotEmpty()) {
                homeViewModel.refreshPermissions()
                Toast.makeText(this, "İzinler güncellendi", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "İzin verilmedi", Toast.LENGTH_SHORT).show()
                homeViewModel.refreshPermissions()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error handling permission result", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val healthConnectClient = HealthConnectClient.getOrCreate(this)
        homeViewModel.setHealthClient(healthConnectClient)

        try {
            // Health Connect Check
            val availabilityStatus = HealthConnectClient.getSdkStatus(this)
            Log.d("MainActivity", "Health Connect status: $availabilityStatus")

            if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE && !isHealthConnectInstalled()) {
                Toast.makeText(this, "Health Connect yüklü değil, Play Store'a yönlendiriliyorsunuz.", Toast.LENGTH_LONG).show()
                redirectToHealthConnectPlayStore()
                return
            }

            this@MainActivity.healthConnectClient = HealthConnectClient.getOrCreate(this)

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
                                    try {
                                        Log.d("MainActivity", "Requesting permissions: $permissions")
                                        permissionLauncher.launch(permissions)
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Error requesting permissions", e)
                                        Toast.makeText(this@MainActivity, "İzin isteme hatası", Toast.LENGTH_SHORT).show()
                                    }
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
                                    try {
                                        val writePermissions = PermissionUtils.getCyclingPermissions()
                                            .filter { it.contains("WRITE") }
                                            .toSet()
                                        if (writePermissions.isNotEmpty()) {
                                            permissionLauncher.launch(writePermissions)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Error requesting cycling permissions", e)
                                    }
                                }
                            )
                        }
                        composable("calories") {
                            CaloriesScreen(
                                homeViewModel = homeViewModel,
                                onBack = { navController.popBackStack() },
                                onRequestWritePermission = {
                                    try {
                                        val writePermissions = PermissionUtils.getCaloriesPermissions()
                                            .filter { it.contains("WRITE") }
                                            .toSet()
                                        if (writePermissions.isNotEmpty()) {
                                            permissionLauncher.launch(writePermissions)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Error requesting calories permissions", e)
                                    }
                                }
                            )
                        }
                    }

                    LaunchedEffect(Unit) {
                        try {
                            delay(500)

                            val allPermissions = PermissionUtils.getAllPermissions()
                            val granted = homeViewModel.getGrantedPermissions()
                            val missing = allPermissions.filterNot { granted.contains(it) }.toSet()

                            Log.d("MainActivity", "Granted permissions: $granted")
                            Log.d("MainActivity", "Missing permissions: $missing")

                            homeViewModel.refreshPermissions()

                            if (missing.isNotEmpty()) {
                                Log.d("MainActivity", "Requesting missing permissions: $missing")
                                delay(1000)
                                permissionLauncher.launch(missing)
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error in permission check", e)
                            Toast.makeText(this@MainActivity, "İzin kontrolü hatası: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate", e)
            Toast.makeText(this, "Uygulama başlatma hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            homeViewModel.refreshPermissions()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onResume", e)
        }
    }
}