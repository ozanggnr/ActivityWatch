package com.ozang.myfitnessozzy.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ozang.myfitnessozzy.viewmodel.CyclingViewModel
import com.ozang.myfitnessozzy.viewmodel.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CyclingScreen(
    cyclingViewModel: CyclingViewModel = viewModel(),
    homeViewModel: HomeViewModel,
    onBack: () -> Unit,
    onRequestWritePermission: () -> Unit
) {
    val todayCycling by cyclingViewModel.todayCyclingMinutes.collectAsState()
    val last28DaysCycling by cyclingViewModel.last28DaysCyclingMinutes.collectAsState()
    val last90DaysCycling by cyclingViewModel.last90DaysCyclingMinutes.collectAsState()
    val isLoading by cyclingViewModel.isLoading.collectAsState()
    val writePermissionGranted by homeViewModel.cyclingWritePermissionGranted.collectAsState()
    val homeLoading by homeViewModel.isLoading.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var inputMinutes by remember { mutableStateOf("") }


    LaunchedEffect(Unit) {
        cyclingViewModel.refreshCycling()
        homeViewModel.refreshPermissions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
            }
            Text(
                text = "Bisiklet Verileri",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))


        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            CyclingDataCard("Bugün Bisiklet Sürme Süresi", "$todayCycling dk")
            CyclingDataCard("Son 28 Günde Toplam", "$last28DaysCycling dk")
            CyclingDataCard("Son 90 Günde Toplam", "$last90DaysCycling dk")
        }

        Spacer(modifier = Modifier.weight(1f))

        // Veri girme (ÇALIŞMIYO BU)
        Button(
            onClick = {
                if (writePermissionGranted) {
                    showDialog = true
                } else {
                    onRequestWritePermission()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !homeLoading
        ) {
            if (homeLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text("+ Veri Ekle")
            }
        }
    }


    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                inputMinutes = ""
            },
            title = { Text("Bisiklet Süresi Ekle") },
            text = {
                Column {
                    Text("Bugün kaç dakika bisiklet sürdünüz?")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputMinutes,
                        onValueChange = { inputMinutes = it },
                        label = { Text("Dakika") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minutes = inputMinutes.toLongOrNull()
                        if (minutes != null && minutes > 0) {
                            homeViewModel.addCyclingSession(minutes)
                            showDialog = false
                            inputMinutes = ""

                            cyclingViewModel.refreshCycling()
                        }
                    },
                    enabled = inputMinutes.toLongOrNull()?.let { it > 0 } == true
                ) {
                    Text("Ekle")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    inputMinutes = ""
                }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun CyclingDataCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}