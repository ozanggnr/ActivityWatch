package com.ozang.myfitnessozzy

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.core.net.toUri
import com.ozang.myfitnessozzy.permissions.PermissionUtils
import com.ozang.myfitnessozzy.screens.*
import com.ozang.myfitnessozzy.ui.theme.MyFitnessOzzyTheme
import com.ozang.myfitnessozzy.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private var healthConnectClient: HealthConnectClient? = null
    private var isHealthConnectAvailable by mutableStateOf(false)
    private var showHealthConnectDialog by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions: Set<String> ->
        homeViewModel.refreshPermissions()
    }

    fun isHealthConnectAvailable(context: Context): Boolean {
        val sdkStatus = HealthConnectClient.getSdkStatus(context)

        when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> {
                return true
            }

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                return true
            }

            HealthConnectClient.SDK_UNAVAILABLE -> {
                return checkHealthConnectPackageInstalled(context)
            }
        }
        return false
    }

    private fun checkHealthConnectPackageInstalled(context: Context): Boolean {
        val packageManager = context.packageManager

        val possiblePackages = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                listOf(
                    "com.android.healthconnect.controller",
                    "com.google.android.apps.healthdata",
                    "com.google.android.healthconnect",
                    "com.android.healthconnect"
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                listOf(
                    "com.google.android.apps.healthdata",
                    "com.android.healthconnect.controller",
                    "com.google.android.healthconnect"
                )
            }

            else -> {
                listOf("com.google.android.apps.healthdata")
            }
        }

        for (packageName in possiblePackages) {
            try {
                packageManager.getPackageInfo(packageName, 0)
                return true
            } catch (_: PackageManager.NameNotFoundException) {
                continue
            }
        }
        return false
    }

    private fun redirectToHealthConnectPlayStore() {
        try {
            val packageName = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    "com.google.android.apps.healthdata"
                }

                else -> {
                    "com.google.android.apps.healthdata"
                }
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "https://play.google.com/store/apps/details?id=$packageName".toUri()
                setPackage("com.android.vending")
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Lütfen Play Store'u yükleyin", Toast.LENGTH_LONG).show()
        }
    }

    fun requestPermissionsSafely(permissions: Set<String>) {
        if (!isHealthConnectAvailable(this)) {
            showHealthConnectDialog = true
            return
        }

        val sdkStatus = HealthConnectClient.getSdkStatus(this)

        when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> {
                try {
                    if (healthConnectClient == null) {
                        healthConnectClient = HealthConnectClient.getOrCreate(this)
                        homeViewModel.setHealthClient(healthConnectClient!!)
                    }
                    isHealthConnectAvailable = true
                    permissionLauncher.launch(permissions)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Health Connect bağlantısı kurulamadı: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                showHealthConnectDialog = true
            }

            HealthConnectClient.SDK_UNAVAILABLE -> {
                if (checkHealthConnectPackageInstalled(this)) {
                    Toast.makeText(
                        this,
                        "Health Connect yüklü ancak bu cihazda desteklenmiyor",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    showHealthConnectDialog = true
                }
            }
        }
    }

    @Composable
    private fun HealthConnectDialog(
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        val dialogText = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                "Health Connect bu cihazda mevcut değil veya güncelleme gerekiyor. " +
                        "Bu uygulamanın çalışması için Health Connect gereklidir. " +
                        "Play Store'dan yüklemek veya güncellemek ister misiniz?"
            }

            else -> {
                "Telefonunuzda Health Connect yok veya güncel değil. " +
                        "Bu uygulamanın çalışması için Health Connect'in yüklenmesi gerekiyor. " +
                        "Play Store'dan yüklemek ister misiniz?"
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "Health Connect Gerekli") },
            text = { Text(dialogText) },
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isHealthConnectAvailable(this)) {
            val sdkStatus = HealthConnectClient.getSdkStatus(this)
            if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
                try {
                    healthConnectClient = HealthConnectClient.getOrCreate(this)
                    homeViewModel.setHealthClient(healthConnectClient!!)
                    isHealthConnectAvailable = true
                } catch (_: Exception) {

                }
            }
        }

        setContent {
            MyFitnessOzzyTheme {
                val navController = rememberNavController()

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
                                requestPermissionsSafely(PermissionUtils.getCyclingPermissions())
                            }
                        )
                    }
                    composable("calories") {
                        CaloriesScreen(
                            homeViewModel = homeViewModel,
                            onBack = { navController.popBackStack() },
                            onRequestWritePermission = {
                                requestPermissionsSafely(PermissionUtils.getCaloriesPermissions())
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (isHealthConnectAvailable(this)) {
            val sdkStatus = HealthConnectClient.getSdkStatus(this)
            if (sdkStatus == HealthConnectClient.SDK_AVAILABLE && healthConnectClient != null) {
                homeViewModel.refreshPermissions()
                isHealthConnectAvailable = true
            }
        }
    }
}