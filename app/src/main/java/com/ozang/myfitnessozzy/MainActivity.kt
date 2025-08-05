package com.ozang.myfitnessozzy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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

    // Health Connect durumu için state
    private var showHealthConnectDialog by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions: Set<String> ->
        homeViewModel.refreshPermissions()
    }

    private fun redirectToHealthConnectPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data =
                    "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata".toUri()
                setPackage("com.android.vending")
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Lütfen Play Store'u yükleyin", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestPermissionsSafely(permissions: Set<String>) {
        val sdkStatus = HealthConnectClient.getSdkStatus(this)

        when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> {
                try {
                    healthConnectClient = HealthConnectClient.getOrCreate(this)
                    homeViewModel.setHealthClient(healthConnectClient!!)
                    isHealthConnectAvailable = true
                    permissionLauncher.launch(permissions)
                } catch (e: Exception) {
                    Log.e("MainActivity", "getOrCreate failed", e)
                    showHealthConnectDialog = true
                }
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                Log.w("HealthConnect", "Health Connect needs update or installation")
                showHealthConnectDialog = true
            }
            HealthConnectClient.SDK_UNAVAILABLE -> {
                Log.w("HealthConnect", "Health Connect unavailable on this device")
                Toast.makeText(this, "Bu cihazda Health Connect desteklenmiyor", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        setContent {
            MyFitnessOzzyTheme {
                val navController = rememberNavController()

                // Health Connect Dialog
                if (showHealthConnectDialog) {
                    HealthConnectDialog(
                        onConfirm = {
                            showHealthConnectDialog = false
                            redirectToHealthConnectPlayStore()
                        },
                        onDismiss = {
                            showHealthConnectDialog = false
                        }
                    )
                }

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            homeViewModel = homeViewModel,
                            onNavigateToSteps = { navController.navigate("steps") },
                            onNavigateToCycling = { navController.navigate("cycling") },
                            onNavigateToCalories = { navController.navigate("calories") },
                            onRequestPermissions = { permissions ->
                                requestPermissionsSafely(permissions)
                            })
                    }
                    composable("steps") {
                        StepsScreen(
                            homeViewModel = homeViewModel,
                            onBack = { navController.popBackStack() })
                    }
                    composable("cycling") {
                        CyclingScreen(
                            homeViewModel = homeViewModel,
                            onBack = { navController.popBackStack() },
                            onRequestWritePermission = {
                                requestWritePermissions(PermissionUtils.getCyclingPermissions())
                            })
                    }
                    composable("calories") {
                        CaloriesScreen(
                            homeViewModel = homeViewModel,
                            onBack = { navController.popBackStack() },
                            onRequestWritePermission = {
                                requestWritePermissions(PermissionUtils.getCaloriesPermissions())
                            })
                    }
                }
            }
        }
    }

    @Composable
    private fun HealthConnectDialog(
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = "Health Connect Gerekli")
            },
            text = {
                Text(text = "Telefonunuzda Health Connect yok. Bu uygulamanın çalışması için Health Connect'in yüklenmesi gerekiyor. Play Store'dan yüklemek ister misiniz?")
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Play Store'a Git")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("İptal")
                }
            }
        )
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