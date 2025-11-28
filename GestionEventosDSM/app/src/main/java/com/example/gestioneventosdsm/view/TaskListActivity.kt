package com.example.gestioneventosdsm.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.app.DatePickerDialog
import java.util.Calendar
import android.app.TimePickerDialog
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.facebook.login.LoginManager // Import para cerrar sesión de Facebook
import com.google.android.gms.auth.api.signin.GoogleSignIn // Import para cerrar sesión de Google
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.ActionBarDrawerToggle
import android.widget.TextView

class TaskListActivity : AppCompatActivity() {

    // --- Views ---
    private lateinit var eventRecyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter
    private lateinit var addTaskButton: FloatingActionButton
    private lateinit var toolbar: Toolbar
    private lateinit var auth: FirebaseAuth

    private enum class EventFilter{
        FUTUROS, PASADOS, TODOS
    }
    private var currentFilter = EventFilter.FUTUROS

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView


    // --- Controller & Data ---
    private val taskController = TaskController(TaskRepository(ApiService.create()))
    private var selectedTask: Task? = null // To store the long-clicked task

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_list)

        auth = FirebaseAuth.getInstance()

        // --- Initialize Views ---
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        addTaskButton = findViewById(R.id.addTaskButton)
        eventRecyclerView = findViewById(R.id.eventRecyclerView) // FIX: Correct ID

        // --- Set Toolbar ---
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Eventos Próximos"

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, // (Necesitas añadir estos strings en strings.xml)
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_eventos_futuros -> {
                    currentFilter = EventFilter.FUTUROS
                    supportActionBar?.title = "Eventos Futuros"
                    loadTasks()
                }
                R.id.nav_eventos_pasados -> {
                    currentFilter = EventFilter.PASADOS
                    supportActionBar?.title = "Eventos Pasados"
                    loadTasks()
                }
                R.id.nav_todos_los_eventos -> {
                    currentFilter = EventFilter.TODOS
                    supportActionBar?.title = "Todos los Eventos"
                    loadTasks()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        val headerView = navigationView.getHeaderView(0)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.nav_header_user_email)
        userEmailTextView.text = auth.currentUser?.email ?: "No identificado"

        // Marcar el primer ítem como seleccionado por defecto
        navigationView.setCheckedItem(R.id.nav_eventos_futuros)

        // --- Setup RecyclerView y Listeners ---
        setupRecyclerView()
        loadTasks() // Carga inicial
        addTaskButton.setOnClickListener { showAddTaskDialog() }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Usa el nuevo archivo de menú que creamos
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                // Llama a la función para cerrar sesión
                signOut()
                true // Indica que hemos manejado el clic
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun signOut() {
        // Muestra un diálogo de confirmación
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                // Cierra sesión en Firebase
                auth.signOut()

                // Cierra sesión en Facebook
                LoginManager.getInstance().logOut()

                // Cierra sesión en Google
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                val googleSignInClient = GoogleSignIn.getClient(this, gso)
                googleSignInClient.signOut().addOnCompleteListener {
                    // Una vez completado, redirige a LoginActivity
                    val intent = Intent(this, LoginActivity::class.java)
                    // Limpia el historial de actividades para que el usuario no pueda volver atrás
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
    private fun setupRecyclerView() {
        // Initialize the adapter with an empty list and click handlers
        eventAdapter = EventAdapter(
            events = emptyList(),
            onItemClick = { task ->
                // Logging
                    Log.d("TaskListActivity", "Item clicked. Preparing to open details for task:")
                    Log.d("TaskListActivity", "ID: ${task.id} (Type: ${task.id::class.simpleName})")
                    Log.d("TaskListActivity", "Title: ${task.title}")
                    Log.d("TaskListActivity", "Description: ${task.description}")
                    Log.d("TaskListActivity", "Due Date: ${task.dueDate}")
                    Log.d("TaskListActivity", "Priority: ${task.priority}")
                // You can navigate to a detail screen here if you want
                val intent = Intent(this, TaskDetailActivity::class.java).apply {
                    // 2. Pass all the task data to the detail activity.
                    putExtra("taskId", task.id)
                    putExtra("title", task.title)
                    putExtra("description", task.description)
                    putExtra("dueDate", task.dueDate)
                    putExtra("priority", task.priority)
                }
                // 3. Start the activity.
                startActivity(intent)
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

    @RequiresApi(Build.VERSION_CODES.O)
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

// In TaskListActivity.kt


    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTasks() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Fetch the original list of tasks.
                val allTasks = taskController.getTasks()

                // 2. Get today's date as a string in "YYYY-MM-DD" format.
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

                val filteredTasks = when (currentFilter) {
                    EventFilter.FUTUROS -> allTasks.filter { it.dueDate >= today }
                    EventFilter.PASADOS -> allTasks.filter { it.dueDate < today }
                    EventFilter.TODOS -> allTasks
                }

                // 4. Sort the filtered list of future tasks by their due date.
                val sortedFutureTasks = filteredTasks.sortedBy { it.dueDate }

                // 5. Switch to the main thread to update the UI.
                withContext(Dispatchers.Main) {
                    // 6. Pass the final sorted and filtered list to the adapter.
                    eventAdapter.updateData(sortedFutureTasks)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // It's helpful to log the error for debugging.
                    Log.e("TaskListActivity", "Error loading or filtering tasks", e)
                    Toast.makeText(this@TaskListActivity, "Error loading tasks: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
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
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDeleteConfirmationDialog(task: Task) {
        AlertDialog.Builder(this)
            .setMessage("¿Estás seguro de que quieres eliminar esta tarea?")
            .setCancelable(false)
            .setPositiveButton("Sí") { _, _ -> deleteTask(task) }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAddTaskDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Agregar Nuevo Evento")

        val inputLayout = LinearLayout(this)
        inputLayout.orientation = LinearLayout.VERTICAL

        inputLayout.setPadding(50, 40, 50, 24)


        val titleEditText = EditText(this).apply { hint = "Título del Evento" }
        inputLayout.addView(titleEditText)

        val descriptionEditText = EditText(this).apply { hint = "Descripción" }
        inputLayout.addView(descriptionEditText)

        val imageUrlEditText = EditText(this).apply { hint = "URL de la Imagen (Opcional)" }
        inputLayout.addView(imageUrlEditText)

        val fechaEditText = EditText(this).apply {
            hint = "Fecha"
            isFocusable = false
            isClickable = true
        }

        fechaEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = "$selectedYear-${selectedMonth + 1}-$selectedDay"
                    fechaEditText.setText(selectedDate)
                },
                year, month, day
            )
            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
            datePickerDialog.show()
        }
        inputLayout.addView(fechaEditText)

        val eventHourEditText = EditText(this).apply {
            hint = "Hora (HH:mm)"
            isFocusable = false
            isClickable = true
        }
        eventHourEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                    eventHourEditText.setText(formattedTime)
                },
                hour,
                minute,
                true // 24-hour format
            )
            timePickerDialog.show()
        }
        inputLayout.addView(eventHourEditText)

//        val prioridadEditText = EditText(this).apply { hint = "Prioridad" }
//        inputLayout.addView(prioridadEditText)
        val priorityOptions = arrayOf("publico", "privado")
        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            priorityOptions
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val prioridadEditText = Spinner(this).apply {
            adapter = priorityAdapter
            (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = 8
        }

        inputLayout.addView(prioridadEditText)

        builder.setView(inputLayout)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val selectedPriority = prioridadEditText.selectedItem.toString()
            val task = Task(
                id = 0, // Will be assigned by the server
                title = titleEditText.text.toString(),
                description = descriptionEditText.text.toString(),
                dueDate = fechaEditText.text.toString(),
                eventHour = eventHourEditText.text.toString(),
                priority = selectedPriority,
                imageUrl = imageUrlEditText.text.toString()
            )
            addTask(task)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }
}
