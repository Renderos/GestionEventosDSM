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
import com.example.gestioneventosdsm.util.sendMail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.app.DatePickerDialog
import androidx.appcompat.widget.Toolbar
import java.util.Calendar
import com.bumptech.glide.Glide
import android.widget.ImageView
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import android.app.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import android.view.Menu
import android.view.MenuItem
import com.facebook.login.LoginManager // Import para cerrar sesión de Facebook
import com.google.android.gms.auth.api.signin.GoogleSignIn // Import para cerrar sesión de Google
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class TaskDetailActivity : AppCompatActivity() {

    private val taskController = TaskController(TaskRepository(ApiService.create()))
    private var currentTask: Task? = null
    private lateinit var eventImageView: ImageView
    private lateinit var selectImageButton: Button
    private var selectedImageUri: Uri? = null

    private lateinit var toolbar: Toolbar
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var dueDateEditText: EditText
    private lateinit var eventHourEditText: EditText
    private lateinit var prioritySpinner: Spinner
    private lateinit var priorityAdapter: ArrayAdapter<String>

    private lateinit var confirmAttendanceButton: Button
    private lateinit var auth: FirebaseAuth


    private val TAG = "TaskDetailActivity"

    // --- 1. Launcher for Getting Content (the image picker) ---
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // The user has successfully selected an image
            Log.d(TAG, "Image selected: $it")

            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Log.d(TAG, "Permiso persistente para la URI obtenido con éxito.")
            } catch (e: SecurityException) {
                Log.e(TAG, "Fallo al obtener permiso persistente para la URI.", e)
            }
            selectedImageUri = it
            // Display the selected image immediately in the ImageView
            Glide.with(this)
                .load(it)
                .placeholder(R.color.material_grey_300)
                .into(eventImageView)
        }
    }

    // --- 2. Launcher for Requesting Permission ---
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Launch the image picker.
                Log.d(TAG, "Storage permission granted.")
                pickImageLauncher.launch("image/*")
            } else {
                // Permission was denied. Show a toast.
                Log.w(TAG, "Storage permission denied.")
                Toast.makeText(this, "Permiso de almacenamiento denegado.", Toast.LENGTH_SHORT).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        supportActionBar?.title = "Detalles del Evento"
        auth = FirebaseAuth.getInstance()

        try {
            toolbar = findViewById(R.id.toolbarDetail)
            eventImageView = findViewById(R.id.eventImageView)
            selectImageButton = findViewById(R.id.selectImageButton)
            titleEditText = findViewById(R.id.titleEditText)
            descriptionEditText = findViewById(R.id.descriptionEditText)
            dueDateEditText = findViewById(R.id.dueDateEditText)
            eventHourEditText = findViewById(R.id.eventHourEditText)
            prioritySpinner = findViewById(R.id.prioritySpinner)
            confirmAttendanceButton = findViewById(R.id.confirmAttendanceButton)
            val saveButton = findViewById<Button>(R.id.saveButton)

            selectImageButton.setOnClickListener {
                checkPermissionAndPickImage()
            }
            confirmAttendanceButton.setOnClickListener {
                confirmAttendanceAndSendEmail()
            }

            setSupportActionBar(toolbar)
            supportActionBar?.title = "Detalles del evento"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)


            setupDatePicker()
            setupTimePicker()
            setupPrioritySpinner()


            // --- Get the taskId from the Intent ---
            val taskId = intent.getIntExtra("taskId", -1)
            Log.d(TAG, "Retrieved taskId: $taskId")

            // --- Validate the taskId ---
            if (taskId == -1) {
                Log.e(TAG, "Error: Invalid taskId received (-1). Closing activity.")
                Toast.makeText(this, "Error: No se pudo cargar el evento.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // --- Load task data ---
            loadTaskDetails(taskId)

            // --- Set up the save button listener ---
            saveButton.setOnClickListener {
                updateTaskDetails()
            }

        } catch (e: Exception) {
            Log.e(TAG, "FATAL CRASH in onCreate: ${e.message}", e)
            Toast.makeText(this, "Ocurrió un error al abrir los detalles.", Toast.LENGTH_LONG).show()
            finish()
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Standard way to handle back navigation
        return true
    }

    private fun setupDatePicker() {
        // Make the EditText non-editable by keyboard
        dueDateEditText.isFocusable = false
        dueDateEditText.isClickable = true

        // Set the click listener to show the DatePickerDialog
        dueDateEditText.setOnClickListener {
            // Use the current date from the EditText if available, otherwise use today's date
            val calendar = Calendar.getInstance()
            try {
                // Try to parse the date from the EditText
                val dateParts = dueDateEditText.text.toString().split("-")
                if (dateParts.size == 3) {
                    calendar.set(dateParts[0].toInt(), dateParts[1].toInt() - 1, dateParts[2].toInt())
                }
            } catch (e: Exception) {
                // If parsing fails, calendar will just use the current date
                Log.w(TAG, "Could not parse date from EditText, using current date for picker.", e)
            }

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Format the date to "YYYY-MM-DD" and set it in the EditText
                    val formattedDate = String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                    dueDateEditText.setText(formattedDate)
                },
                year,
                month,
                day
            )
            // Prevent picking a date in the past
            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
            datePickerDialog.show()
        }
    }

    private fun setupTimePicker() {
        // Make the EditText non-editable by keyboard
        eventHourEditText.isFocusable = false
        eventHourEditText.isClickable = true

        // Set the click listener to show the TimePickerDialog
        eventHourEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    // This block is called when the user picks a time and clicks "OK".
                    // Format the time to "HH:mm"
                    val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                    eventHourEditText.setText(formattedTime)
                },
                currentHour,
                currentMinute,
                true // Use 24-hour format
            )
            timePickerDialog.show()
        }
    }

    private fun setupPrioritySpinner() {
        val priorityOptions = arrayOf("público", "privado")
        priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            priorityOptions
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        prioritySpinner.adapter = priorityAdapter
    }


    private fun loadTaskDetails(taskId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = taskController.getTask(taskId)
                currentTask = task
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
        // This function now populates all four EditTexts
        titleEditText.setText(task.title)
        descriptionEditText.setText(task.description)
        dueDateEditText.setText(task.dueDate)
        eventHourEditText.setText(task.eventHour)

        val imageSource = task.imageUrl
        if (!imageSource.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageSource)
                .placeholder(R.drawable.ic_image_not_found)
                .error(R.drawable.ic_image_not_found)
                .into(eventImageView)
        }

        val priorityPosition = priorityAdapter.getPosition(task.priority)
        if (priorityPosition >= 0) {
            prioritySpinner.setSelection(priorityPosition)
        } else {
            Log.e(TAG, "Priority not found in adapter: ${task.priority}")
        }

        //)
        Log.d(TAG, "Views populated successfully.")
    }
    private fun checkPermissionAndPickImage() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED -> {
                // The permission is already granted, launch the image picker directly.
                pickImageLauncher.launch("image/*")
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) -> {
                // Explain to the user why you need the permission (optional but good practice)
                AlertDialog.Builder(this)
                    .setTitle("Permiso Necesario")
                    .setMessage("Esta aplicación necesita acceso a tus imágenes para poder adjuntarlas a un evento.")
                    .setPositiveButton("OK") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                    .create()
                    .show()
            }
            else -> {
                // Directly ask for the permission.
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
    }
    private fun confirmAttendanceAndSendEmail() {
        val user = auth.currentUser
        val event = currentTask

        // --- 2. Validate that we have a user and an event ---
        if (user == null) {
            Toast.makeText(this, "Error: No se ha encontrado un usuario.", Toast.LENGTH_SHORT).show()
            return
        }
        if (event == null) {
            Toast.makeText(this, "Error: No se han podido cargar los detalles del evento.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- 3. Get the user's email ---
        val userEmail = user.email
        if (userEmail.isNullOrEmpty()) {
            Toast.makeText(this, "Error: No se ha encontrado el email del usuario.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- 4. Simulate saving the attendance and show feedback ---
        // In a real app, you would make a network call here to your backend.
        // For example: taskController.confirmAttendance(event.id, user.uid)
        Toast.makeText(this, "¡Gracias por confirmar tu asistencia!", Toast.LENGTH_LONG).show()

        // --- 5. Create and launch the email Intent ---
        val emailSubject = "Confirmación de Asistencia: ${event.title}"
        val emailBody = """
            Hola ${user.displayName ?: ""},

            Has confirmado tu asistencia para el siguiente evento:

            Evento: ${event.title}
            Fecha: ${event.dueDate}
            Hora: ${event.eventHour ?: "No especificada"}

            ¡Te esperamos!

            Atentamente,
            El equipo de GestioEventosDSM
        """.trimIndent()

//        val intent = Intent(Intent.ACTION_SENDTO).apply {
//            data = Uri.parse("mailto:") // Only email apps should handle this
//            putExtra(Intent.EXTRA_EMAIL, arrayOf(userEmail))
//            putExtra(Intent.EXTRA_SUBJECT, emailSubject)
//            putExtra(Intent.EXTRA_TEXT, emailBody)
//        }

        // Not secure for demonstration only
            sendMail().enviarEmail(userEmail, emailSubject, emailBody)


        // Use a chooser to let the user pick their email app
        try {
            startActivity(Intent.createChooser(intent, "Enviar email de confirmación..."))
        } catch (ex: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "No se encontró ninguna aplicación de email.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTaskDetails() {
        val taskToUpdate = currentTask
        if (taskToUpdate == null) {
            Toast.makeText(this, "No se puede actualizar, la tarea no se cargó.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedPriority = prioritySpinner.selectedItem.toString()
        val newImageUrl = selectedImageUri?.toString() ?: taskToUpdate.imageUrl

        // Create a new Task object with all the updated values from the EditTexts
        val updatedTask = taskToUpdate.copy(
            title = titleEditText.text.toString(),
            description = descriptionEditText.text.toString(),
            dueDate = dueDateEditText.text.toString(),
            eventHour = eventHourEditText.text.toString(),
            priority = selectedPriority,
            imageUrl = newImageUrl
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                taskController.updateTask(updatedTask)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TaskDetailActivity, "Tarea Actualizada", Toast.LENGTH_SHORT).show()
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
