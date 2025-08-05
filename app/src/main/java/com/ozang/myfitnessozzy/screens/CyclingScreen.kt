package com.ozang.myfitnessozzy.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ozang.myfitnessozzy.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyclingScreen(
    homeViewModel: HomeViewModel,
    onBack: () -> Unit,
    onRequestWritePermission: () -> Unit
) {
    val todayCycling by homeViewModel.todayCyclingMinutes.collectAsState()
    val last28DaysCycling by homeViewModel.last28DaysCyclingMinutes.collectAsState()
    val last90DaysCycling by homeViewModel.last90DaysCyclingMinutes.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val writePermissionGranted by homeViewModel.cyclingWritePermissionGranted.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var inputMinutes by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        homeViewModel.refreshCycling()
        homeViewModel.refreshPermissions()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Bisiklet Verileri") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                }
            },
            actions = {
                IconButton(onClick = {
                    if (writePermissionGranted) {
                        showDialog = true
                    } else {
                        onRequestWritePermission()
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Veri Ekle")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
        }
    }

    // AlertDialog (Aynı kalıyor)
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
                            homeViewModel.refreshCycling()
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