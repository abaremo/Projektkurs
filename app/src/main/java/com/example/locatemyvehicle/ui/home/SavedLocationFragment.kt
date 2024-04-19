package com.example.locatemyvehicle.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.locatemyvehicle.R
import com.example.locatemyvehicle.databinding.FragmentHomeBinding
import com.example.locatemyvehicle.databinding.FragmentSavedLocationBinding
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class SavedLocationFragment : Fragment() {



    private lateinit var binding: FragmentSavedLocationBinding
    private val savedLocationsList = mutableListOf<String>()
    private lateinit var adapter: SavedLocationsAdapter
    private lateinit var sharedPreferences: SharedPreferences



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSavedLocationBinding.inflate(inflater, container, false)

        // Initiera RecyclerView
        val recyclerView = binding.recyclerViewSavedLocations
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Skapa adaptern och tilldela den till RecyclerView
        adapter = SavedLocationsAdapter(savedLocationsList) // Anpassa parametern efter din adapterklass
        recyclerView.adapter = adapter

        // Hämta SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("SavedLocations", Context.MODE_PRIVATE)

        // Hämta sparade platser vid fragmentets skapande
        loadSavedLocations()

        // Hämta knappen för att rensa historiken
        val btnClear = binding.btnClear

        // Sätt en klicklyssnare för knappen
        btnClear.setOnClickListener {
            clearHistory()
        }

        binding.btnSaveLocationName.setOnClickListener {
            val locationName = binding.etLocationName.text.toString()
            val savedCoordinates = binding.tvSavedLocations.text.toString()

            if (locationName.isNotEmpty() && savedCoordinates.isNotEmpty()) {
                // Skapa en sträng som kombinerar platsnamn och koordinater
                val savedLocation = "$locationName - $savedCoordinates"

                // Lägg till den sparade platsen i tvSavedLocations
                binding.tvSavedLocations.append("\n$savedLocation")

                // Lägg till den sparade platsen i listan och uppdatera adaptern
                savedLocationsList.add(savedLocation)

                //binding.recyclerViewSavedLocations.adapter = adapter
                adapter.notifyDataSetChanged()

                // Spara den nya listan med platser till SharedPreferences
                saveLocationsToSharedPreferences()

                // Visa endast de senast sparade koordinaterna i tvSavedLocations
                binding.tvSavedLocations.text = savedCoordinates

                // Rensa platsnamnet för nästa inmatning
                binding.etLocationName.text.clear()

                Toast.makeText(requireContext(), "Location saved: $savedLocation", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Please enter a location name and save coordinates", Toast.LENGTH_SHORT).show()
            }
        }

        if (savedInstanceState == null) {
            val savedLocations = arguments?.getStringArray("savedLocations") ?: emptyArray()
            val uniqueLocations = savedLocations.toSet().toList()
            val coordinatesString = uniqueLocations.joinToString(separator = "\n")
            binding.tvSavedLocations.text = coordinatesString
        }

        return binding.root
    }

    // Metod för att spara platserna till SharedPreferences
    private fun saveLocationsToSharedPreferences() {
        val editor = sharedPreferences.edit()
        editor.putStringSet("savedLocations", savedLocationsList.toSet())
        editor.apply()
    }

    // Metod för att hämta sparade platser från SharedPreferences
    private fun loadSavedLocations() {
        val savedLocationsSet = sharedPreferences.getStringSet("savedLocations", emptySet())
        //savedLocationsList.clear() // Rensa listan för att bara behålla de sparade platserna
        savedLocationsList.addAll(savedLocationsSet ?: emptySet())

        adapter.notifyDataSetChanged()
    }


    // Metod för att rensa historiken på sparade platser
    private fun clearHistory() {
        // Rensa listan med sparade platser och uppdatera adaptern
        savedLocationsList.clear()
        adapter.notifyDataSetChanged()

        // Rensa sparade platser från SharedPreferences
        sharedPreferences.edit().remove("savedLocations").apply()

        // Visa en bekräftelse att historiken har rensats
        Toast.makeText(requireContext(), "History cleared", Toast.LENGTH_SHORT).show()
    }
}