package com.ozang.myfitnessozzy.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ozang.myfitnessozzy.viewmodel.HomeViewModel
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaloriesScreen(
    homeViewModel: HomeViewModel, onBack: () -> Unit, onRequestWritePermission: () -> Unit
) {
    val caloriesData by homeViewModel.caloriesData.collectAsState()
    val isLoading by homeViewModel.isLoadingCalories.collectAsState()
    val caloriesPermissionGranted by homeViewModel.caloriesPermissionGranted.collectAsState()
    val caloriesWritePermissionGranted by homeViewModel.caloriesWritePermissionGranted.collectAsState()
    val totalCalories = homeViewModel.getTotalCaloriesToday()

    var showDialog by remember { mutableStateOf(false) }
    var inputCalories by remember { mutableStateOf("") }

    LaunchedEffect(caloriesPermissionGranted) {
        if (caloriesPermissionGranted) {
            homeViewModel.loadCaloriesData()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(title = { Text("Yaktığınız Kalori") }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
            }
        }, actions = {
            if (caloriesPermissionGranted) {
                IconButton(onClick = {
                    if (caloriesWritePermissionGranted) {
                        showDialog = true
                    } else {
                        onRequestWritePermission()
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Kalori Ekle")
                }
            }
        })

        if (!caloriesPermissionGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Kalori verilerine erişim için izin gerekli",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRequestWritePermission) {
                    Text("İzin Ver")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Bugün Yakılan Toplam Kalori",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = totalCalories.roundToInt().toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "kcal",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Text(
                        text = "Detaylı Veriler (${caloriesData.size} kayıt)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (caloriesData.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Bugün için kalori verisi bulunamadı",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(caloriesData) { record ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${record.energy.inKilocalories.roundToInt()} kcal",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                text = record.startTime.atZone(java.time.ZoneId.systemDefault())
                                                    .format(DateTimeFormatter.ofPattern("HH:mm")),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 14.sp
                                            )
                                        }

                                        val startTime =
                                            record.startTime.atZone(java.time.ZoneId.systemDefault())
                                        val endTime =
                                            record.endTime.atZone(java.time.ZoneId.systemDefault())
                                        Text(
                                            text = "${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${
                                                endTime.format(
                                                    DateTimeFormatter.ofPattern("HH:mm")
                                                )
                                            }",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Kalori ekleme
    if (showDialog) {
        AlertDialog(onDismissRequest = {
            showDialog = false
            inputCalories = ""
        }, title = { Text("Kalori Ekle") }, text = {
            Column {
                Text("Kaç kalori yaktınız?")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = inputCalories,
                    onValueChange = { inputCalories = it },
                    label = { Text("Kalori (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        }, confirmButton = {
            TextButton(onClick = {
                val calories = inputCalories.toDoubleOrNull()
                if (calories != null && calories > 0) {
                    homeViewModel.addCaloriesBurned(calories)
                    showDialog = false
                    inputCalories = ""
                }
            }, enabled = inputCalories.toDoubleOrNull()?.let { it > 0 } == true) {
                Text("Ekle")
            }
        }, dismissButton = {
            TextButton(onClick = {
                showDialog = false
                inputCalories = ""
            }) {
                Text("İptal")
            }
        })
    }
}