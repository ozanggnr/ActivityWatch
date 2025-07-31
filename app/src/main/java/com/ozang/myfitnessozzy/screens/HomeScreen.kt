package com.ozang.myfitnessozzy.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.ozang.myfitnessozzy.permissions.PermissionUtils
import com.ozang.myfitnessozzy.viewmodel.HomeViewModel


@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onNavigateToSteps: () -> Unit,
    onNavigateToCycling: () -> Unit,
    onNavigateToCalories: () -> Unit,
    onRequestPermissions: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prefs", Context.MODE_PRIVATE) }

    var showInitialDialog by remember { mutableStateOf(false) }
    var hasShownDialog by remember { mutableStateOf(prefs.getBoolean("shownHealthDialog", false)) }

    val stepGranted by homeViewModel.stepPermissionGranted.collectAsState()
    val cyclingGranted by homeViewModel.cyclingPermissionGranted.collectAsState()
    val caloriesGranted by homeViewModel.caloriesPermissionGranted.collectAsState()


    LaunchedEffect(Unit) {
        if (!hasShownDialog && (!stepGranted || !cyclingGranted || !caloriesGranted)) {
            showInitialDialog = true
        }
    }

    if (showInitialDialog) {
        AlertDialog(
            onDismissRequest = { showInitialDialog = false },
            title = { Text("İzin Gerekiyor") },
            text = {
                Text("Uygulamanın sağlık verilerinize erişebilmesi için izin vermeniz gerekiyor. Bu sadece bir kez sorulacak.")
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("shownHealthDialog", true).apply()
                    hasShownDialog = true
                    showInitialDialog = false
                    onRequestPermissions(PermissionUtils.getAllPermissions())
                }) {
                    Text("İzin Ver")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("shownHealthDialog", true).apply()
                    hasShownDialog = true
                    showInitialDialog = false
                }) {
                    Text("Hayır")
                }
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "My Fitness Ozzy",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionButton(
            enabled = stepGranted,
            label = "Adımlar",
            onClick = onNavigateToSteps,
            showPermissionIcon = !stepGranted,
            onRequestPermission = {
                onRequestPermissions(PermissionUtils.getStepPermissions())
            }
        )

        PermissionButton(
            enabled = cyclingGranted,
            label = "Bisiklet Verileri",
            onClick = onNavigateToCycling,
            showPermissionIcon = !cyclingGranted,
            onRequestPermission = {
                onRequestPermissions(PermissionUtils.getCyclingPermissions())
            }
        )

        PermissionButton(
            enabled = caloriesGranted,
            label = "Yaktığınız Kalori",
            onClick = onNavigateToCalories,
            showPermissionIcon = !caloriesGranted,
            onRequestPermission = {
                onRequestPermissions(PermissionUtils.getCaloriesPermissions())
            }
        )
    }
}

@Composable
fun PermissionButton(
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
    showPermissionIcon: Boolean,
    onRequestPermission: () -> Unit
) {
    Button(
        onClick = {
            if (enabled) {
                onClick()
            } else {
                onRequestPermission()
            }
        },
        enabled = true,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                color = if (enabled) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showPermissionIcon) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Permission needed",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}