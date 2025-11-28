package com.example.gestioneventosdsm.model

import com.example.gestioneventosdsm.network.ApiService

class TaskRepository(private val apiService: ApiService) {

    // Método para obtener la lista de tareas desde la API REST
    suspend fun getTasks(): List<Task> {
        return apiService.getTasks()
    }

    // Método para obtener una tarea por su ID
    suspend fun getTask(taskId: Int): Task {
        return apiService.getTask(taskId)
    }

    // Método para agregar una nueva tarea a través de la API REST
    suspend fun addTask(task: Task): Task {
        return apiService.addTask(task)
    }

    // Método para eliminar una tarea a través de la API REST
    suspend fun deleteTask(taskId: Int) {
        apiService.deleteTask(taskId)
    }

    // Método para actualizar una tarea a través de la API REST
    suspend fun updateTask(task: Task): Task {
        return apiService.updateTask(task.id, task)
    }
}