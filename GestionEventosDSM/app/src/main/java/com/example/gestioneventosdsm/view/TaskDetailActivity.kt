package com.example.gestioneventosdsm.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gestioneventosdsm.R
import com.example.gestioneventosdsm.controller.TaskController
import com.example.gestioneventosdsm.model.Task
import com.example.gestioneventosdsm.model.TaskRepository
import com.example.gestioneventosdsm.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskDetailActivity : AppCompatActivity() {

    private val taskController = TaskController(TaskRepository(ApiService.create()))
    private var currentTask: Task? = null // Make it nullable to handle errors

    // --- Declare View Variables ---
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    // Add other EditTexts for fields you want to be editable

    private val TAG = "TaskDetailActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail) // Ensure this layout is correct

        supportActionBar?.title = "Detalles del Evento"

        try {
            // --- 1. Initialize all views correctly ---
            // These views should be EditTexts in your XML to allow updating.
            titleEditText = findViewById(R.id.titleEditText)
            descriptionEditText = findViewById(R.id.descriptionEditText)
            // Example: If you want to edit the due date and priority, they must also be EditTexts
            // val dueDateEditText = findViewById<EditText>(R.id.dueDateEditText)
            // val priorityEditText = findViewById<EditText>(R.id.priorityEditText)
            val saveButton = findViewById<Button>(R.id.saveButton)

            // --- 2. Get the taskId from the Intent ---
            val taskId = intent.getIntExtra("taskId", -1)
            Log.d(TAG, "Retrieved taskId: $taskId")

            // --- 3. Validate the taskId ---
            if (taskId == -1) {
                Log.e(TAG, "Error: Invalid taskId received (-1). Closing activity.")
                Toast.makeText(this, "Error: No se pudo cargar el evento.", Toast.LENGTH_LONG).show()
                finish() // Close the activity
                return
            }

            // --- 4. Load task data from the network/database ---
            loadTaskDetails(taskId)

            // --- 5. Set up the save button listener ---
            saveButton.setOnClickListener {
                updateTaskDetails()
            }

        } catch (e: Exception) {
            // This will catch any error during initialization (e.g., a view ID not found)
            Log.e(TAG, "FATAL CRASH in onCreate: ${e.message}", e)
            Toast.makeText(this, "Ocurrió un error al abrir los detalles.", Toast.LENGTH_LONG).show()
            finish() // Close the activity safely
        }
    }

    private fun loadTaskDetails(taskId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = taskController.getTask(taskId)
                currentTask = task // Store the fetched task
                withContext(Dispatchers.Main) {
                    displayTaskDetails(task)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Failed to load task details: ${e.message}", e)
                    Toast.makeText(this@TaskDetailActivity, "Error al cargar los detalles.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun displayTaskDetails(task: Task) {
        // This function populates the views with the data
        titleEditText.setText(task.title)
        descriptionEditText.setText(task.description)
        // If you had EditTexts for due date and priority, you would set them here:
        // dueDateEditText.setText(task.dueDate)
        // priorityEditText.setText(task.priority)
        Log.d(TAG, "Views populated successfully.")
    }

    private fun updateTaskDetails() {
        val taskToUpdate = currentTask
        if (taskToUpdate == null) {
            Toast.makeText(this, "No se puede actualizar, la tarea no se cargó.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a new Task object with the updated values from the EditTexts
        val updatedTask = taskToUpdate.copy(
            title = titleEditText.text.toString(),
            description = descriptionEditText.text.toString()
            // Add other fields if they are editable
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                taskController.updateTask(updatedTask)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TaskDetailActivity, "Tarea Actualizada", Toast.LENGTH_SHORT).show()
                    // Go back to the list activity after a successful update
                    val intent = Intent(this@TaskDetailActivity, TaskListActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Failed to update task: ${e.message}", e)
                    Toast.makeText(this@TaskDetailActivity, "Error al actualizar la tarea.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
