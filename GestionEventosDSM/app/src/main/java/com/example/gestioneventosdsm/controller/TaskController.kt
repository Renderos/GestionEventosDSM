package com.example.gestioneventosdsm.controller

import com.example.gestioneventosdsm.model.Task
import com.example.gestioneventosdsm.model.TaskRepository

class TaskController(private val taskRepository: TaskRepository) {

    // Método para obtener la lista de tareas
    suspend fun getTasks(): List<Task> {
        return taskRepository.getTasks()
    }

    // Método para obtener una tarea por su ID
    suspend fun getTask(taskId: Int): Task {
        return taskRepository.getTask(taskId)
    }

    // Método para agregar una nueva tarea
    suspend fun addTask(task: Task): Task {
        return taskRepository.addTask(task)
    }

    // Método para eliminar una tarea
    suspend fun deleteTask(taskId: Int) {
        taskRepository.deleteTask(taskId)
    }

    // Método para actualizar una tarea
    suspend fun updateTask(task: Task): Task {
        return taskRepository.updateTask(task)
    }
}
