package com.example.locatemyvehicle.ui.nearby

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.locatemyvehicle.R

class NearbyFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_nearby, container, false)

        // Referenser till knapparna
        val fuelButton: ImageButton = view.findViewById(R.id.fuelButton)
        val parkingButton: ImageButton = view.findViewById(R.id.parkingButton)
        val hotelButton: ImageButton = view.findViewById(R.id.hotelButton)
        val restaurantButton: ImageButton = view.findViewById(R.id.restaurantButton)
        val bicycleButton: ImageButton = view.findViewById(R.id.bicycleButton)
        val chargingButton: ImageButton = view.findViewById(R.id.chargingButton)


        fuelButton.setOnClickListener {
            findNavController().navigate(R.id.action_nearbyFragment_to_fuelFragment)
        }


        parkingButton.setOnClickListener {
            findNavController().navigate(R.id.action_nearbyFragment_to_parkingFragment)
        }


        hotelButton.setOnClickListener {
             findNavController().navigate(R.id.action_nearbyFragment_to_hotelFragment)
         }


        restaurantButton.setOnClickListener {
             findNavController().navigate(R.id.action_nearbyFragment_to_restaurantFragment)
         }


        bicycleButton.setOnClickListener {
            findNavController().navigate(R.id.action_nearbyFragment_to_bicycleparkingFragment)
        }


        chargingButton.setOnClickListener {
            findNavController().navigate(R.id.action_nearbyFragment_to_chargingstationFragment)
        }

        return view
    }
}

