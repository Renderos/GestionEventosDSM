package com.example.gestioneventosdsm.model

data class Task(
    val id: Int,              // Identificador único de la tarea
    val title: String,          // Título de la tarea
    val description: String,    // Descripción de la tarea
    val dueDate: String,          // Fecha de vencimiento de la tarea
    val priority: String           // Prioridad de la tarea (por ejemplo, 1 para alta, 2 para media, 3 para baja)
)