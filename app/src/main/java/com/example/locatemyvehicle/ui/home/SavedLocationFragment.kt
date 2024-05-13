package com.example.locatemyvehicle.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.locatemyvehicle.R
import com.example.locatemyvehicle.databinding.FragmentSavedLocationBinding


class SavedLocationFragment : Fragment() {
    private lateinit var binding: FragmentSavedLocationBinding
    private val tempCoordinatesList = mutableListOf<String>()
    private var adapter: SavedLocationsAdapter? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val viewModel: SharedViewModel by activityViewModels()



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSavedLocationBinding.inflate(inflater, container, false)
        // Initiera RecyclerView
        val recyclerView = binding.recyclerViewSavedLocations
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val savedLocations = viewModel.savedLocationsList

        // Hämta SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("SavedLocations", Context.MODE_PRIVATE)

        // Kontrollera om adaptern redan är instansierad innan du initialiserar en ny
        if (adapter == null) {
            //val savedLocations = viewModel.savedLocationsList
            // Skapa adaptern och tilldela den till RecyclerView med klicklyssnare
            adapter = SavedLocationsAdapter(
                tempCoordinatesList,
                { location ->
                    // Hantera klick på sparad plats
                    // Exempel: Visa platsen på kartan i HomeFragment
                    showLocationOnMap(location)
                },
                { position ->
                    // Hantera borttagning av platsen
                    removeLocation(position)
                },
                { position ->
                    // Hantera klick på anteckningsknappen, här kan du göra vad du vill att som ska hända när knappen klickas på
                }
            )
            recyclerView.adapter = adapter
        }



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
                tempCoordinatesList.add(savedLocation)

                //binding.recyclerViewSavedLocations.adapter = adapter
                adapter?.notifyDataSetChanged()

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

    // Funktion för att ta bort en sparad plats
    private fun removeLocation(position: Int) {
        tempCoordinatesList.removeAt(position)
        adapter?.notifyDataSetChanged()
        saveLocationsToSharedPreferences() // Uppdatera SharedPreferences efter borttagning
    }


    // Funktion för att visa platsen på kartan i HomeFragment
    private fun showLocationOnMap(location: String) {
        // Skapa en bundle för att skicka platsinformationen till HomeFragment
        val bundle = Bundle().apply {
            putString("location", location)
        }
        // Navigera till HomeFragment och skicka med platsinformationen
        findNavController().navigate(R.id.action_savedlocationFragment_to_homeFragment, bundle)
    }


    // Metod för att spara platserna till SharedPreferences
    private fun saveLocationsToSharedPreferences() {
        val editor = sharedPreferences.edit()
        editor.putStringSet("savedLocations", tempCoordinatesList.toSet())
        editor.apply()
    }

    // Metod för att hämta sparade platser från SharedPreferences
    private fun loadSavedLocations() {
        val savedLocationsSet = sharedPreferences.getStringSet("savedLocations", emptySet())
        //savedLocationsList.clear() // Rensa listan för att bara behålla de sparade platserna
        tempCoordinatesList.addAll(savedLocationsSet ?: emptySet())

        adapter?.notifyDataSetChanged()
    }


    // Metod för att rensa historiken på sparade platser
    private fun clearHistory() {
        // Rensa listan med sparade platser och uppdatera adaptern
        tempCoordinatesList.clear()
        adapter?.notifyDataSetChanged()

        // Rensa sparade platser från SharedPreferences
        sharedPreferences.edit().remove("savedLocations").apply()

        // Visa en bekräftelse att historiken har rensats
        Toast.makeText(requireContext(), "History cleared", Toast.LENGTH_SHORT).show()
    }
}