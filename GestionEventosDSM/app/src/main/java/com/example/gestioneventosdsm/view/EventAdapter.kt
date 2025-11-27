package com.example.gestioneventosdsm.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gestioneventosdsm.R
import com.example.gestioneventosdsm.model.Task
import com.bumptech.glide.Glide

class EventAdapter(
    private var events: List<Task>,
    private val onItemClick: (Task) -> Unit, // Listener for short clicks
    private val onItemLongClick: (Task, View) -> Boolean // Listener for long clicks
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    // This is the ViewHolder that holds the views for a single card
    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.eventTitleTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.eventDateTextView)
        val locationTextView: TextView = itemView.findViewById(R.id.eventLocationTextView) // Assuming 'description' is the location
        private val imageView: ImageView = itemView.findViewById(R.id.eventCardImageView)

        fun bind(task: Task, onItemClick: (Task) -> Unit, onItemLongClick: (Task, View) -> Boolean) {
            titleTextView.text = task.title
            dateTextView.text = "Fecha: ${task.dueDate}"
            locationTextView.text = "Descripción: ${task.description}"
            // Load image using Glide or Picasso
            if (!task.imageUrl.isNullOrEmpty()) {
                imageView.visibility = View.VISIBLE // Show the ImageView
                Glide.with(itemView.context)
                    .load(task.imageUrl)
                    .placeholder(R.color.material_grey_300) // Show a placeholder while loading
                    .error(R.drawable.ic_image_not_found) // Show an error image if it fails
                    .into(imageView)
            } else {
                imageView.visibility = View.GONE // Hide the ImageView if there's no URL
            }

            itemView.setOnClickListener { onItemClick(task) }
            itemView.setOnLongClickListener { onItemLongClick(task, itemView) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        // Inflate the card layout
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_card, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        // Bind the data to the ViewHolder
        holder.bind(events[position], onItemClick, onItemLongClick)
    }

    override fun getItemCount(): Int {
        return events.size
    }

    // A method to update the data in the adapter
    fun updateData(newEvents: List<Task>) {
        this.events = newEvents
        notifyDataSetChanged() // Reload the list
    }
}
