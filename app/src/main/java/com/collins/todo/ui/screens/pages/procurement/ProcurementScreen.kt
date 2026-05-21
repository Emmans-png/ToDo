package com.collins.todo.ui.screens.pages.procurement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.collins.todo.data.Models.MaterialOrder
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProcurementScreen(
    onBack: () -> Unit,
    onNavigateToLiveTracking: (Int) -> Unit,
    onNavigateToMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ProcurementViewModel = viewModel()
    val orders by viewModel.orders
    val isLoading by viewModel.isLoading
    val unreadMessages by viewModel.unreadMessageCount

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "PROCUREMENT GATE",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToMessages) {
                        BadgedBox(
                            badge = {
                                if (unreadMessages > 0) {
                                    Badge { Text(unreadMessages.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, "Message Driver", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddOrderClick() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Order")
            }
        },
        containerColor = Color.Black
    ) { padding ->
        if (viewModel.showAddOrder) {
            AddOrderDialog(
                viewModel = viewModel
            )
        }

        viewModel.editingOrder?.let { order ->
            EditOrderDialog(
                order = order,
                onDismiss = { viewModel.onDismissEditOrder() },
                onOrderUpdated = { updatedOrder ->
                    viewModel.updateOrder(updatedOrder)
                    viewModel.onDismissEditOrder()
                }
            )
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Stage-Gate Procurement",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Approve orders based on construction stage gates",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (orders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active material orders.", color = MaterialTheme.colorScheme.tertiary)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val grouped = orders.groupBy { it.requiredStage }
                    grouped.forEach { (stage, stageOrders) ->
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    stage.uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        items(stageOrders) { order ->
                            OrderCard(
                                order = order,
                                onTrack = { order.id?.let { onNavigateToLiveTracking(it) } },
                                onEdit = { viewModel.onEditOrderClick(order) },
                                onDelete = { order.id?.let { viewModel.deleteOrder(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditOrderDialog(
    order: MaterialOrder,
    onDismiss: () -> Unit,
    onOrderUpdated: (MaterialOrder) -> Unit
) {
    var materialName by remember { mutableStateOf(order.materialName) }
    var quantity by remember { mutableStateOf(order.quantity.toString()) }
    var unitPrice by remember { mutableStateOf(order.unitPrice.toString()) }
    var supplier by remember { mutableStateOf(order.supplierName) }
    var status by remember { mutableStateOf(order.status) }
    var earnings by remember { mutableStateOf(order.earnings?.toString() ?: "") }
    
    val statuses = listOf("Pending", "Dispatched", "Ongoing", "Arrived", "Unloading", "Delivered")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Material Order", color = Color.White) },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = materialName, onValueChange = { materialName = it }, label = { Text("Material Name") })
                TextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Qty") })
                TextField(value = unitPrice, onValueChange = { unitPrice = it }, label = { Text("Unit Price ($)") })
                TextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Supplier") })
                TextField(value = earnings, onValueChange = { earnings = it }, label = { Text("Earnings for Driver ($)") })
                
                Text("Status", color = Color.White, fontSize = 12.sp)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    statuses.forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onOrderUpdated(
                    order.copy(
                        materialName = materialName,
                        quantity = quantity.toDoubleOrNull() ?: order.quantity,
                        unitPrice = unitPrice.toDoubleOrNull() ?: order.unitPrice,
                        supplierName = supplier,
                        status = status,
                        earnings = earnings.toDoubleOrNull()
                    )
                )
            }) { Text("Update Order") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddOrderDialog(
    viewModel: ProcurementViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = { viewModel.onDismissAddOrder() },
        title = { 
            Column {
                Text("NEW MATERIAL ORDER", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("Logistics Dispatch", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF1A1A1A), // Slightly lighter than black for depth
        shape = RoundedCornerShape(16.dp),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                // Project Selection
                if (viewModel.selectedProjectIdForNewOrder == null && viewModel.projects.value.isNotEmpty()) {
                    var expandedProject by remember { mutableStateOf(false) }
                    val selectedProject = viewModel.projects.value.find { it.id == viewModel.selectedProjectIdForNewOrder }
                    
                    Column {
                        Text("SELECT PROJECT", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                        Box {
                            OutlinedButton(
                                onClick = { expandedProject = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedProject?.name ?: "Tap to choose project...", color = if (selectedProject == null) Color.Gray else Color.White, fontSize = 14.sp)
                                    Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            DropdownMenu(
                                expanded = expandedProject, 
                                onDismissRequest = { expandedProject = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface).fillMaxWidth(0.7f)
                            ) {
                                viewModel.projects.value.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.name, color = Color.White) },
                                        onClick = {
                                            viewModel.onAddOrderClick(p.id)
                                            expandedProject = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                OrderInputField("Material Name", viewModel.materialName, Icons.Default.Inventory) { viewModel.materialName = it }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OrderInputField("Qty", viewModel.quantity, Icons.Default.ProductionQuantityLimits) { viewModel.quantity = it }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OrderInputField("Price ($)", viewModel.unitPrice, Icons.Default.Payments) { viewModel.unitPrice = it }
                    }
                }
                
                OrderInputField("Transport Earning ($)", viewModel.earnings, Icons.Default.LocalShipping) { viewModel.earnings = it }
                OrderInputField("Supplier", viewModel.supplier, Icons.Default.Store) { viewModel.supplier = it }
                
                Column {
                    Text("REQUIRED STAGE", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    val stages = listOf("Foundation", "Walling", "Roofing", "Finishing")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        stages.forEach { s ->
                            val isSelected = viewModel.requiredStage == s
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.requiredStage = s },
                                label = { Text(s, fontSize = 11.sp) },
                                shape = RoundedCornerShape(4.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    labelColor = Color.Gray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color.White.copy(alpha = 0.1f),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)) {
                viewModel.statusMessage?.let {
                    Text(
                        it, 
                        color = if (it.startsWith("Error") || it.startsWith("Failed")) Color.Red else Color.Green,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Button(
                    enabled = !viewModel.isSaving && (viewModel.selectedProjectIdForNewOrder != null || viewModel.projects.value.isNotEmpty()) && viewModel.materialName.isNotBlank(),
                    onClick = { viewModel.createOrder() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { 
                    if (viewModel.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("ADD ORDER", fontWeight = FontWeight.Bold) 
                }
            }
        },
        dismissButton = { 
            TextButton(
                enabled = !viewModel.isSaving,
                onClick = { viewModel.onDismissAddOrder() },
                modifier = Modifier.padding(bottom = 8.dp)
            ) { Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold) } 
        }
    )
}

@Composable
fun OrderInputField(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 10.sp) },
        leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = Color.Gray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        singleLine = true
    )
}

@Composable
fun OrderCard(
    order: MaterialOrder,
    onTrack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Inventory, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.materialName, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Supplier: ${order.supplierName}", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                    Text("${order.quantity} ${order.unit} @ $${order.unitPrice}", color = Color.White, fontSize = 14.sp)
                    if (order.earnings != null && order.earnings > 0) {
                        Text("Transport: $${order.earnings}", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (order.estimatedDays != null) {
                        Text("ETA: ${order.estimatedDays} days", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }

                Surface(
                    color = when (order.status) {
                        "Delivered" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        "Dispatched" -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        "Ongoing" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        "Arrived" -> Color(0xFF9C27B0).copy(alpha = 0.2f)
                        "Unloading" -> Color(0xFFFF5722).copy(alpha = 0.2f)
                        else -> Color(0xFFFFA000).copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        order.status,
                        color = when (order.status) {
                            "Delivered" -> Color(0xFF4CAF50)
                            "Dispatched" -> Color(0xFF2196F3)
                            "Ongoing" -> MaterialTheme.colorScheme.primary
                            "Arrived" -> Color(0xFF9C27B0)
                            "Unloading" -> Color(0xFFFF5722)
                            else -> Color(0xFFFFA000)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (order.status == "Ongoing" || order.status == "Dispatched" || order.status == "Arrived" || order.status == "Unloading") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onTrack,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("VIEW LIVE MOVEMENT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
