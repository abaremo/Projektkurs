package com.example.locatemyvehicle.ui.home

import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.locatemyvehicle.R
import com.example.locatemyvehicle.databinding.FragmentHomeBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.wms.BuildConfig
import java.util.Locale

class HomeFragment : Fragment() {
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var binding: FragmentHomeBinding
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val startPoint = GeoPoint(62.0, 16.0)
    val fragment = this
    private lateinit var mapEventsOverlay: MapEventsOverlay
    private lateinit var lastMarker: Marker
    private var savedParkingPosition: GeoPoint? = null
    private val savedLocations = mutableListOf<GeoPoint>()
    private var shouldSaveLocation = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        savedLocations.clear()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurationMap()
        initMap()
        setZoomMultiTouch(true) //FUNKAR EJ
        //Click function for "get back" button
        getLocation(true)
        setHasOptionsMenu(true)

        // Sätt klicklyssnare för hela verktygsfältet
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.btnSaveLocation -> {
                    if (::lastMarker.isInitialized) {
// Hämta den senaste markörens koordinater och lägg till i listan
                        val lastMarkerPosition = lastMarker.position
                        savedLocations.add(lastMarkerPosition)
                        Toast.makeText(requireContext(), "Location saved", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "No location saved",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    true
                }


                R.id.btnSaved -> {
                        val bundle = Bundle().apply {
                            putStringArray(
                                "savedLocations",
                                savedLocations.map { "${it.latitude}, ${it.longitude}" }
                                    .toTypedArray()
                            )
                        }
                        findNavController().navigate(R.id.savedlocationFragment, bundle)

                    true
                }




                R.id.btnGetBack -> {
// Lägg till hantering för knappen "Get back" här
                    binding.mapOSM.controller.animateTo(startPoint)
                    binding.mapOSM.controller.setZoom(6.5)

                    true
                }



                R.id.btnPosition -> {
// Lägg till hantering för knappen "Position" här
                    binding.mapOSM.controller.animateTo(locationOverlay.myLocation)
                    binding.mapOSM.controller.setZoom(17)
                    true
                }

                else -> false
            }
        }


        mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if ( p != null && shouldSaveLocation) {
                    binding.mapOSM.overlays.removeAll{it is Marker}
                    addMarker(p)
                    return true

                }
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        } )

        binding.mapOSM.overlays.add(0, mapEventsOverlay)


        val fabButton: FloatingActionButton = view.findViewById(R.id.addMarkers) // Ersätt YOUR_FAB_ID med den faktiska id:en för din FloatingActionButton
        fabButton.setOnClickListener {
            shouldSaveLocation = true
        }
        //måste lägga till en else för att avsluta funktionen vid klick på annan knapp


    }



    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu) // Ersätt "your_menu_file_name" med rätt filnamn för din meny
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun addMarker(geoPoint: GeoPoint) {
        if (shouldSaveLocation) {
            //savedLocations.add(geoPoint)
            //Toast.makeText(requireContext(), "Location saved", Toast.LENGTH_SHORT).show()
            //shouldSaveLocation = false // false om man bara kan lägga till en markör
        }
        val marker = Marker(binding.mapOSM)
        marker.position = geoPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_menu_map)
        binding.mapOSM.overlays.add(marker)
        lastMarker = marker
        binding.mapOSM.invalidate()

        buildRoad(marker.position)
    }



    private fun configurationMap() {
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        Configuration.getInstance().osmdroidBasePath = requireActivity().filesDir
    }

    private fun initMap() {
        binding.mapOSM.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapOSM.controller.setZoom(6.5)
        binding.mapOSM.controller.setCenter(startPoint)
    }

    private fun setZoomMultiTouch(b: Boolean) {
        binding.mapOSM.setMultiTouchControls(b)

        binding.mapOSM.overlays.add(RotationGestureOverlay(binding.mapOSM))
    }

    private fun getLocation(zoom: Boolean = false) {
// Begära behörighet för åtkomst till fina platstjänster
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
// Om tillståndet beviljades, skapa och konfigurera MyLocationNewOverlay
                createLocationOverlay(zoom)
            } else {
// Om tillståndet inte beviljades, hantera det här
                Toast.makeText(fragment.requireContext(), "Location permission denied.", Toast.LENGTH_SHORT).show()
            }
        }

// Begära platsbehörighet
        requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun createLocationOverlay(zoom: Boolean = false) {
        locationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(fragment.requireContext()),
            binding.mapOSM
        )
        locationOverlay.enableMyLocation()
        val imageDraw =
            ContextCompat.getDrawable(fragment.requireContext(), R.drawable.ic_parkinglocation)!!
                .toBitmap()
        locationOverlay.setPersonIcon(imageDraw)
        locationOverlay.setDirectionIcon(imageDraw)
        binding.mapOSM.overlays.add(locationOverlay)

// Om zoom är true, flytta till användarens plats och zooma in
        if (zoom) {
            locationOverlay.run {
                val myLocation = this.myLocation
                if (myLocation != null) {
                    binding.mapOSM.controller.animateTo(myLocation)
                    binding.mapOSM.controller.setZoom(17)
                }
            }
        }
    }

    /// build road
    private fun buildRoad(endPoint: GeoPoint) {
        binding.mapOSM.overlays.removeAll { it is Polyline }
        CoroutineScope(Dispatchers.IO).launch {
            val roadManager = OSRMRoadManager(
                requireContext(),
                System.getProperty("http.agent")
            )
// Hur reser du i rutten, cykel, gå, bil
            roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT)
            val waypoints =
                arrayListOf<GeoPoint>(
                    locationOverlay?.myLocation ?: startPoint,
                    endPoint
                )
            try {
                val road = roadManager.getRoad(waypoints)
                val roadOverlay = RoadManager.buildRoadOverlay(road)
                withContext(Dispatchers.Main) {
                    binding.mapOSM.overlays.add(0, roadOverlay)
                    binding.mapOSM.invalidate()

                    val formatlength = "%.2f".format(road.mLength)
                    val formattime = "%.2f".format(road.mDuration / 60)
                    Toast.makeText(
                        requireContext(), "${formatlength} km, " +
                                "${formattime} min", Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("RoadBuildingError", "Error building road: ${e.message}")
// Hantera fel här
            }
        }
    }


}

