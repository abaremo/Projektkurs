package com.example.locatemyvehicle.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.locatemyvehicle.R

class SavedLocationsAdapter(private val dataSet: List<String>) :
    RecyclerView.Adapter<SavedLocationsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewLocationName: TextView = view.findViewById(R.id.textViewLocationName)
        //val btnDeleteLocation: ImageButton = view.findViewById(R.id.btnDeleteLocation)
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