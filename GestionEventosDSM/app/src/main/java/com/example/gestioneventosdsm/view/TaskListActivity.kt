package com.example.gestioneventosdsm.view

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gestioneventosdsm.R
import com.example.gestioneventosdsm.controller.TaskController
import com.example.gestioneventosdsm.model.Task
import com.example.gestioneventosdsm.model.TaskRepository
import com.example.gestioneventosdsm.network.ApiService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*

class TaskListActivity : AppCompatActivity() {

    // --- Views ---
    private lateinit var eventRecyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter
    private lateinit var addTaskButton: FloatingActionButton
    private lateinit var toolbar: Toolbar

    // --- Controller & Data ---
    private val taskController = TaskController(TaskRepository(ApiService.create()))
    private var selectedTask: Task? = null // To store the long-clicked task

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_list)

        // --- Initialize Views ---
        toolbar = findViewById(R.id.toolbar)
        addTaskButton = findViewById(R.id.addTaskButton)
        eventRecyclerView = findViewById(R.id.eventRecyclerView) // FIX: Correct ID

        // --- Set Toolbar ---
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Eventos"

        // --- Setup RecyclerView ---
        setupRecyclerView()

        // --- Load Initial Data ---
        loadTasks()

        // --- Setup Listeners ---
        addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun setupRecyclerView() {
        // Initialize the adapter with an empty list and click handlers
        eventAdapter = EventAdapter(
            events = emptyList(),
            onItemClick = { task ->
                // Handle short click
                Toast.makeText(this, "Task: ${task.title}", Toast.LENGTH_SHORT).show()
                // You can navigate to a detail screen here if you want
            },
            onItemLongClick = { task, view ->
                // Handle long click
                selectedTask = task
                registerForContextMenu(view) // Register the view for a context menu
                openContextMenu(view) // Open the menu
                true // Indicate that the long click is consumed
            }
        )

        eventRecyclerView.layoutManager = LinearLayoutManager(this)
        eventRecyclerView.adapter = eventAdapter
    }

    // --- Context Menu for RecyclerView ---
    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.task_item_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        // Use the 'selectedTask' variable we stored during the long click
        val task = selectedTask ?: return false // If no task is selected, do nothing

        return when (item.itemId) {
            R.id.action_update -> {
                val intent = Intent(this, TaskDetailActivity::class.java).apply {
                    putExtra("taskId", task.id)
                    putExtra("title", task.title)
                    putExtra("description", task.description)
                    putExtra("dueDate", task.dueDate)
                    putExtra("priority", task.priority)
                }
                startActivity(intent)
                true
            }
            R.id.action_delete -> {
                showDeleteConfirmationDialog(task)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    // --- Data Handling Methods (Copied from your original file, no changes needed) ---

    private fun loadTasks() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = taskController.getTasks()
                withContext(Dispatchers.Main) {
                    eventAdapter.updateData(tasks) // Update the new adapter
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TaskListActivity, "Error loading tasks: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteTask(task: Task) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                taskController.deleteTask(task.id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TaskListActivity, "Tarea Eliminada con Exito", Toast.LENGTH_SHORT).show()
                }
                loadTasks() // Refresh list
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TaskListActivity, "Error Eliminar Tarea: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addTask(task: Task) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                taskController.addTask(task)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TaskListActivity, "Tarea Agregada con Exito", Toast.LENGTH_SHORT).show()
                }
                loadTasks() // Refresh list
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TaskListActivity, "Error Agregar Tarea: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- Dialogs (Copied from your original file, no changes needed) ---
    private fun showDeleteConfirmationDialog(task: Task) {
        AlertDialog.Builder(this)
            .setMessage("¿Estás seguro de que quieres eliminar esta tarea?")
            .setCancelable(false)
            .setPositiveButton("Sí") { _, _ -> deleteTask(task) }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun showAddTaskDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Agregar Nueva Tarea")

        val inputLayout = LinearLayout(this)
        inputLayout.orientation = LinearLayout.VERTICAL

        val titleEditText = EditText(this).apply { hint = "Título" }
        inputLayout.addView(titleEditText)

        val descriptionEditText = EditText(this).apply { hint = "Descripción" }
        inputLayout.addView(descriptionEditText)

        val fechaEditText = EditText(this).apply { hint = "Fecha Cierre" }
        inputLayout.addView(fechaEditText)

        val prioridadEditText = EditText(this).apply { hint = "Prioridad" }
        inputLayout.addView(prioridadEditText)

        builder.setView(inputLayout)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val task = Task(
                id = 0, // Will be assigned by the server
                title = titleEditText.text.toString(),
                description = descriptionEditText.text.toString(),
                dueDate = fechaEditText.text.toString(),
                priority = prioridadEditText.text.toString()
            )
            addTask(task)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }
}
