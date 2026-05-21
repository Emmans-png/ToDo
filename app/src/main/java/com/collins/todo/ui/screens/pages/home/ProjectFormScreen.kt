package com.collins.todo.ui.screens.pages.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.collins.todo.data.Models.ConstructionProject
import com.collins.todo.data.repository.ConstructionRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProjectFormScreen(
    projectId: Int? = null,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var landCost by remember { mutableStateOf("") }
    var rentalIncome by remember { mutableStateOf("") }
    var currentStage by remember { mutableStateOf("Foundation") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val repository = ConstructionRepository()
    val scope = rememberCoroutineScope()

    LaunchedEffect(projectId) {
        if (projectId != null) {
            isLoading = true
            try {
                val project = repository.getProjectById(projectId)
                if (project != null) {
                    name = project.name
                    location = project.location
                    budget = project.totalBudget.toString()
                    landCost = project.landCost.toString()
                    rentalIncome = project.targetRentalIncome.toString()
                    currentStage = project.currentStage
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load project details."
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (projectId == null) "NEW PROJECT" else "EDIT PROJECT", 
                            fontWeight = FontWeight.Black, 
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                        Text("Venture Details", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 24.dp)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ProjectInputField("Project Name", name, Icons.Default.Business) { name = it }
                ProjectInputField("Location", location, Icons.Default.LocationOn) { location = it }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        ProjectInputField("Budget ($)", budget, Icons.Default.Payments) { budget = it }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ProjectInputField("Land Cost ($)", landCost, Icons.Default.Terrain) { landCost = it }
                    }
                }
                
                ProjectInputField("Target Monthly Rental ($)", rentalIncome, Icons.Default.MonetizationOn) { rentalIncome = it }
                
                if (projectId != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("CURRENT CONSTRUCTION STAGE", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    val stages = listOf("Foundation", "Walling", "Roofing", "Finishing")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stages.forEach { stage ->
                            val isSelected = currentStage == stage
                            FilterChip(
                                selected = isSelected,
                                onClick = { currentStage = stage },
                                label = { Text(stage, fontSize = 11.sp) },
                                shape = RoundedCornerShape(4.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    labelColor = Color.Gray,
                                    disabledContainerColor = Color.Transparent
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color.Gray.copy(alpha = 0.3f),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text(
                            errorMessage!!, 
                            color = MaterialTheme.colorScheme.error, 
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val project = ConstructionProject(
                                id = projectId,
                                name = name,
                                location = location,
                                totalBudget = budget.toDoubleOrNull() ?: 0.0,
                                landCost = landCost.toDoubleOrNull() ?: 0.0,
                                targetRentalIncome = rentalIncome.toDoubleOrNull() ?: 0.0,
                                currentStage = currentStage
                            )
                            
                            val result = if (projectId == null) {
                                repository.createProject(project)
                            } else {
                                repository.updateProject(project)
                            }

                            if (result != null) {
                                onSaveSuccess()
                            } else {
                                errorMessage = "Failed to save project. Please try again."
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            errorMessage = "Error: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading && name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.2f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        if (projectId == null) "SAVE VENTURE" else "UPDATE VENTURE", 
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectInputField(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onValueChange: (String) -> Unit) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label, fontSize = 13.sp) },
            leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
    }
}
