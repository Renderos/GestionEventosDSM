package com.example.gestioneventosdsm.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.gestioneventosdsm.R
import com.example.gestioneventosdsm.model.Task

class TaskListAdapter(context: Context, taskList: List<Task>) :
    ArrayAdapter<Task>(context, 0, taskList) {

    private class ViewHolder {
        lateinit var idTextView: TextView
        lateinit var titleTextView: TextView
        lateinit var descriptionTextView: TextView
        lateinit var dueDateTextView: TextView
        lateinit var priorityTextView: TextView
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val holder: ViewHolder

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_event_card, parent, false)
            holder = ViewHolder()
            //holder.idTextView = convertView.findViewById(R.id.idTextView)
            holder.titleTextView = convertView.findViewById(R.id.titleTextView)
            holder.descriptionTextView = convertView.findViewById(R.id.descriptionTextView)
            holder.dueDateTextView = convertView.findViewById(R.id.dueDateTextView)
            holder.priorityTextView = convertView.findViewById(R.id.priorityTextView)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        val task = getItem(position)
        holder.idTextView.text = task?.id.toString()
        holder.titleTextView.text = task?.title
        holder.descriptionTextView.text = task?.description
        holder.dueDateTextView.text = task?.dueDate
        holder.priorityTextView.text = task?.priority

        return convertView!!
    }
}
