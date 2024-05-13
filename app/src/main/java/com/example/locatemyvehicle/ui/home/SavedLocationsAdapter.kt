package com.example.locatemyvehicle.ui.home

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.locatemyvehicle.R

class SavedLocationsAdapter(private val dataSet: List<String>,
                            private val onItemClick: (String) -> Unit,
                            private val onRemoveClick: (Int) -> Unit,
                            private val onNoteClick: (Int) -> Unit) :

    RecyclerView.Adapter<SavedLocationsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewLocationName: TextView = view.findViewById(R.id.textViewLocationName)
        val btnDeleteLocation: ImageButton = view.findViewById(R.id.btnDeleteLocation)
        val noteLayout: LinearLayout = view.findViewById(R.id.noteLayout)
        val btnNote: ImageButton = view.findViewById(R.id.btnNote)
        val btnSaveNote: ImageButton = view.findViewById(R.id.btnSaveNote)
        val btnCloseNote: ImageButton = view.findViewById(R.id.btnCloseNote)
        val etNote: EditText = view.findViewById(R.id.etNote)
        var savedNotes: MutableList<String> = MutableList(dataSet.size) { "" }

        var isNoteMode: Boolean = false


        init {
            // Sätt en klicklyssnare för varje sparad plats
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val location = dataSet[position]
                    onItemClick(location)
                }
            }

            // Sätt en klicklyssnare för att ta bort platsen när knappen trycks
            btnDeleteLocation.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Skicka tillbaka positionen till fragmentet/aktiviteten för att hantera borttagningen
                    onRemoveClick(position)
                }
            }

            // Sätt klicklyssnare för att visa anteckningslayouten
            btnNote.setOnClickListener {
                isNoteMode = true // Användaren är i anteckningsläget
                updateUI()
                Log.d("SavedLocationsAdapter", "Anteckningsknapp klickad")
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNoteClick(position)
                    Log.d("SavedLocationsAdapter", "Visa anteckningslayout")
                    noteLayout.visibility = View.VISIBLE
                }
            }



            // Spara anteckningen när användaren klickar på "Spara"-knappen
            btnSaveNote.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val noteText = etNote.text.toString()
                    // Spara anteckningen på lämpligt sätt, till exempel i en lista
                    savedNotes[position] = noteText
                    // Dölj anteckningsrutan när anteckningen är sparad
                    noteLayout.visibility = View.GONE
                    isNoteMode = false // Användaren lämnar anteckningsläget
                    updateUI() // Uppdatera UI för att visa ikoner för att skapa anteckning och ta bort platsen
                }
            }

            // Dölj anteckningsrutan när användaren är klar och klickar på "Stäng"-knappen
            btnCloseNote.setOnClickListener {
                noteLayout.visibility = View.GONE
                isNoteMode = false // Användaren lämnar anteckningsläget
                updateUI() // Uppdatera UI för att visa ikoner för att skapa anteckning och ta bort platsen
            }

        }

        private fun updateUI() {
            if (isNoteMode) {
                // Dölj ikoner för att skapa anteckning och ta bort platsen
                btnNote.visibility = View.GONE
                btnDeleteLocation.visibility = View.GONE
            } else {
                // Visa ikoner för att skapa anteckning och ta bort platsen
                btnNote.visibility = View.VISIBLE
                btnDeleteLocation.visibility = View.VISIBLE
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

    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}