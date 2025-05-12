package com.example.myapplication.ui.theme.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.model.Category
import com.example.myapplication.model.Task
import com.example.myapplication.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskApp(viewModel: TaskViewModel = viewModel()) {
    var showDialog by remember { mutableStateOf(false) }
    var newTaskName by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var showError by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val tasks by viewModel.tasks.observeAsState(emptyList())
    val categories by viewModel.allCategories.observeAsState(emptyList())
    var newCategoryName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Task Organizer", style = MaterialTheme.typography.titleLarge) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues).padding(16.dp)) {
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                label = { Text("Add New Category") },
                trailingIcon = {
                    IconButton(onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCategory(newCategoryName.trim())
                            newCategoryName = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                val grouped = tasks.groupBy { it.categoryId }
                categories.forEach { category ->
                    val categoryTasks = grouped[category.id].orEmpty()
                    if (categoryTasks.isNotEmpty()) {
                        item {
                            Text(category.name, style = MaterialTheme.typography.titleMedium)
                        }
                        items(categoryTasks) { task ->
                            TaskCard(task, category, viewModel)
                        }
                    }
                }

                val uncategorized = grouped[null].orEmpty()
                if (uncategorized.isNotEmpty()) {
                    item {
                        Text("No Category", style = MaterialTheme.typography.titleMedium)
                    }
                    items(uncategorized) { task ->
                        TaskCard(task, null, viewModel)
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Task") },
            text = {
                Column {
                    TextField(
                        value = newTaskName,
                        onValueChange = {
                            newTaskName = it
                            if (showError && it.isNotBlank()) showError = false
                        },
                        label = { Text("Task name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category selector
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text(categories.find { it.id == selectedCategoryId }?.name ?: "Select Category")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategoryId = category.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Due date selector
                    val context = LocalContext.current
                    Button(onClick = {
                        val calendar = Calendar.getInstance().apply {
                            dueDate?.let { timeInMillis = it }
                        }

                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val selectedCalendar = Calendar.getInstance().apply {
                                    set(year, month, day)
                                }

                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        selectedCalendar.set(Calendar.HOUR_OF_DAY, hour)
                                        selectedCalendar.set(Calendar.MINUTE, minute)
                                        dueDate = selectedCalendar.timeInMillis
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Text("Select Due Date")
                    }

                    dueDate?.let {
                        val formatted = SimpleDateFormat("EEE, d MMM yyyy 'at' h:mm a", Locale.getDefault()).format(it)
                        Text("Due: $formatted", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }

                    if (showError) {
                        Text(
                            "Task name cannot be empty",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTaskName.isNotBlank()) {
                        viewModel.addTask(newTaskName.trim(), selectedCategoryId, dueDate)
                        newTaskName = ""
                        selectedCategoryId = null
                        dueDate = null
                        showDialog = false
                        showError = false
                    } else {
                        showError = true
                    }
                }) {
                    Text("Add Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun TaskCard(
    task: Task,
    category: Category?,
    viewModel: TaskViewModel
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { viewModel.toggleTask(task, it) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                )
                if (category != null) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = { viewModel.removeTask(task) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Task")
            }
        }
    }
}


@Composable
fun TaskItem(
    task: Task,
    category: Category?,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = onCheckedChange
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${task.name} (Category: ${category?.name ?: "No Category"})",
                style = if (task.isDone) {
                    TextStyle(textDecoration = TextDecoration.LineThrough)
                } else {
                    TextStyle.Default
                },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Task")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTaskItem() {
    TaskItem(
        task = Task(name = "Sample Task", isDone = false),
        category = Category(id = 1L, name = "Work"),
        onCheckedChange = {},
        onDelete = {}
    )
}
