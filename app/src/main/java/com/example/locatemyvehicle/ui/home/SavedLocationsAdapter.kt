package com.example.locatemyvehicle.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.locatemyvehicle.R

class SavedLocationsAdapter(private val dataSet: List<String>, private val onItemClick: (String) -> Unit) :
    RecyclerView.Adapter<SavedLocationsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewLocationName: TextView = view.findViewById(R.id.textViewLocationName)
        //val btnDeleteLocation: ImageButton = view.findViewById(R.id.btnDeleteLocation)

        init {
            // Sätt en klicklyssnare för varje sparad plats
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val location = dataSet[position]
                    onItemClick(location)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_location, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val savedLocation = dataSet[position]
        holder.textViewLocationName.text = savedLocation



        // Sätt en klicklyssnare för att ta bort platsen när knappen trycks
        //holder.btnDeleteLocation.setOnClickListener {
            // Anropa en funktion för att ta bort platsen från listan eller annan hantering
            // Exempel: removeItem(position)
        //}
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}