package com.example.locatemyvehicle.ui.home


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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

class HomeFragment : Fragment() {
    //private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var binding: FragmentHomeBinding
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val startPoint = GeoPoint(62.0, 16.0)
    val fragment = this
    private lateinit var mapEventsOverlay: MapEventsOverlay
    private lateinit var lastMarker: Marker
    //private var savedParkingPosition: GeoPoint? = null
    private val tempCoordinateList = mutableListOf<GeoPoint>()
    private var shouldSaveLocation = false
    private lateinit var savedLocationsAdapter: SavedLocationsAdapter
    private val viewModel: SharedViewModel by activityViewModels()
    private var savedLocation: GeoPoint? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        tempCoordinateList.clear()

        // Kontrollera om det finns platsinformation i bundle
        arguments?.getString("location")?.let { location ->
            // Visa platsen på kartan med den mottagna platsinformationen
            showLocationOnMap(location)
        }
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

        // Använd savedLocationsList från viewmodelen här
        val savedLocations = viewModel.savedLocationsList

        savedLocationsAdapter = SavedLocationsAdapter(
            savedLocations,
            onItemClick = { location ->
                // Hantera klick på sparad plats
                // Exempel: Visa platsen på kartan i HomeFragment
                showLocationOnMap(location)
            },
            onRemoveClick = { position ->
                // Hantera borttagning av sparad plats
                // Exempel: Ta bort platsen från listan och uppdatera adaptern
                removeSavedLocation(position)
            })

        // Tilldela adaptern till RecyclerView
        binding.recyclerViewSavedLocations.adapter = savedLocationsAdapter


        // Sätt klicklyssnare för hela verktygsfältet
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.btnSaveLocation -> {
                    if (::lastMarker.isInitialized) {
// Hämta den senaste markörens koordinater och lägg till i listan
                        val lastMarkerPosition = lastMarker.position
                        tempCoordinateList.add(lastMarkerPosition)
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

                R.id.btnRoad -> {
                    // Klicklyssnare för knappen "Road"
                    if (savedLocation != null) {
                        // Om sista markören är initierad, bygg vägen
                        buildRoad(savedLocation!!)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "No location saved",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    true // Returnera true för att indikera att knappen har hanterats
                }

                R.id.btnSaved -> {
                    val bundle = Bundle().apply {
                        putStringArray(
                            "savedLocations",
                            tempCoordinateList.map { "${it.latitude}, ${it.longitude}" }
                                .toTypedArray()
                        )
                    }
                    findNavController().navigate(R.id.savedlocationFragment, bundle)
                    shouldSaveLocation = false
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
                if (p != null && shouldSaveLocation) {
                    binding.mapOSM.overlays.removeAll { it is Marker } //gör att det inte blir markörer överallt
                    addMarker(p)
                    return true

                }
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        })

        binding.mapOSM.overlays.add(0, mapEventsOverlay)

        val fabButton: FloatingActionButton = view.findViewById(R.id.addMarkers)
        fabButton.setOnClickListener {
            shouldSaveLocation = true
        }

        binding.btnToggleMap.setOnClickListener {
            toggleMapType(view)
        }

        binding.btnNav.setOnClickListener {
            if (savedLocation != null) {
                // Hämta latitud och longitud med endast två decimaler
                val startLat = String.format("%.2f", locationOverlay.myLocation.latitude)
                val startLong = String.format("%.2f", locationOverlay.myLocation.longitude)
                val destLat = String.format("%.2f", savedLocation!!.latitude)
                val destLong = String.format("%.2f", savedLocation!!.longitude)

                // Uppdatera textvyerna med formaterade koordinater
                binding.tvStart.text = "Start: Lat: $startLat, Long: $startLong"
                binding.tvDestination.text = "Destination: Lat: $destLat, Long: $destLong"

                // Bygg vägen och navigera
                buildRoad(savedLocation!!)
            } else {
                Toast.makeText(requireContext(), "No location saved", Toast.LENGTH_SHORT).show()
            }
        }

    }


    private fun removeSavedLocation(position: Int) {
        viewModel.removeSavedLocation(position)
        savedLocationsAdapter.notifyDataSetChanged()
    }

    // Funktion för att visa platsen på kartan i HomeFragment
    private fun showLocationOnMap(location: String) {
        // Dela upp strängen vid bindestrecket
        val parts = location.split(" - ")
        if (parts.size == 2) {
            val placeName = parts[0] // Namnet på platsen
            val coordinates = parts[1].split(", ") // Dela upp koordinaterna
            if (coordinates.size == 2) {
                try {
                    val latitude = coordinates[0].toDouble() // Latitude-koordinat
                    val longitude = coordinates[1].toDouble() // Longitude-koordinat
                    val savedLocation = GeoPoint(latitude, longitude)

                    // Lägg till markören för den sparade platsen på kartan
                    addSavedLocationIcon(savedLocation)

                    // Flytta kartan till den sparade platsen och zooma in
                    binding.mapOSM.controller.setCenter(savedLocation)
                    binding.mapOSM.controller.setZoom(17.0)
                } catch (e: NumberFormatException) {
                    // Hantera felaktiga koordinater
                    Log.e("NumberFormatException", "Invalid coordinates format: $location")
                }
            } else {
                // Om koordinaterna inte är i rätt format
                Log.e("InvalidCoordinatesFormat", "Invalid coordinates format: $location")
            }
        } else {
            // Om strängen inte är i rätt format
            Log.e("InvalidLocationFormat", "Invalid location format: $location")
        }
    }

    // Funktion för att lägga till ikonen för den sparade platsen på kartan
    private fun addSavedLocationIcon(savedLocation: GeoPoint) {
        this.savedLocation = savedLocation
        val marker = Marker(binding.mapOSM)
        marker.position = savedLocation
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_saved_location_icon)
        binding.mapOSM.overlays.add(marker)
        binding.mapOSM.invalidate()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(
            R.menu.main,
            menu
        ) // Ersätt "your_menu_file_name" med rätt filnamn för din meny
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun addMarker(geoPoint: GeoPoint) {
        if (shouldSaveLocation) {
            //tempCoordinateList.add(geoPoint)
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

        //buildRoad(marker.position)
    }


    private fun configurationMap() {
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        Configuration.getInstance().osmdroidBasePath = requireActivity().filesDir
    }

    fun toggleMapType(view: View) {
        if (binding.mapOSM.tileProvider.tileSource == TileSourceFactory.MAPNIK) {
            // Byt till mörk karta
            binding.mapOSM.setTileSource(TileSourceFactory.USGS_SAT)
        } else {
            // Byt till vanlig karta
            binding.mapOSM.setTileSource(TileSourceFactory.MAPNIK)
        }
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
                Toast.makeText(
                    fragment.requireContext(),
                    "Location permission denied.",
                    Toast.LENGTH_SHORT
                ).show()
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
            val waypoints = arrayListOf<GeoPoint>(
                locationOverlay?.myLocation ?: startPoint,
                    endPoint
                )
            try {
                val road = roadManager.getRoad(waypoints)
                val roadOverlay = RoadManager.buildRoadOverlay(road)


                //Funktion för navigering
                val bearing = calculateBearing(startPoint, endPoint)
                val path = roadOverlay?.actualPoints
                val everyTenthPoint = mutableListOf<GeoPoint>()

                for ((index, point) in path?.withIndex()!!) {

                    if (index % 10 == 0) {
// Check if the index is divisible by 10
                    if (index
                        % 10
                        == 0) {
// // Kontrollera om indexet är delbart med 10

                        everyTenthPoint.add(point)
// lägg till punkter i listan
                    }

                }

                withContext(Dispatchers.Main) {
                    binding.mapOSM.overlays.add(0, roadOverlay)
                    binding.mapOSM.invalidate()

                    val formatlength = "%.2f".format(road.mLength)
                    val formattime = "%.2f".format(road.mDuration / 60)
                    Toast.makeText(
                        requireContext(), "${formatlength} km, " +
                                "${formattime} min", Toast.LENGTH_SHORT
                    ).show()
                    navigate(startPoint, endPoint, 10)
                }
            } catch (e: Exception) {
                Log.e("RoadBuildingError", "Error building road: ${e.message}")
            }
        }
    }

    fun calculateBearing(startPoint: GeoPoint, endPoint: GeoPoint): Double {

        val lat1 = Math.toRadians(startPoint.latitude)

        val lon1 = Math.toRadians(startPoint.longitude)

        val lat2 = Math.toRadians(endPoint.latitude)

        val lon2 = Math.toRadians(endPoint.longitude)

        val dLon = lon2-lon1

        val y = Math.sin(dLon) * Math.cos(lat2)

        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)

        var bearing = Math.atan2(y, x)

        bearing = Math.toDegrees(bearing)

        bearing = (bearing + 360) % 360

        return bearing

    }

    fun interpolatePointsAlongPolyline(
        startPoint:
        GeoPoint, endPoint:
        GeoPoint, numPoints:
        Int
    ):
            List<GeoPoint> {
        val marker = Marker(binding.mapOSM)


        val points = mutableListOf<GeoPoint>()
        val totalDistance = startPoint.distanceToAsDouble(endPoint)
        val stepDistance = totalDistance / (numPoints + 1)
        val bearing = startPoint.bearingTo(endPoint)
        var currentDistance = stepDistance
        repeat(numPoints)
        {
            val interpolatedPoint = startPoint.destinationPoint(currentDistance, bearing)

            points.add(interpolatedPoint)

            currentDistance += stepDistance

            Toast.makeText(requireContext(),
                "Distance: $currentDistance km",
                Toast.LENGTH_SHORT).show()

        }

        points.forEach()
        { point ->
            val marker = Marker(binding.mapOSM)
            marker.position = point

            // Anpassa markören

            binding.mapOSM.overlays.add(marker)

        }

        return points

    }

    private fun navigate(startPoint: GeoPoint, endPoint: GeoPoint, numPoints: Int) {
        val interpolatedPoints = interpolatePointsAlongPolyline(startPoint, endPoint, numPoints)
        val bearings = mutableListOf<Double>()
        for (i in 0 until interpolatedPoints.size - 1) {
            val currentPoint = interpolatedPoints[i]
            val nextPoint = interpolatedPoints[i + 1]
            val bearing = calculateBearing(currentPoint, nextPoint)
            bearings.add(bearing)
        }

    }

    // Funktion för att uppdatera gångvägbeskrivningen
    private fun updateGuidance(startPoint: GeoPoint, endPoint: GeoPoint, interpolatedPoints: List<GeoPoint>, bearings: List<Double>) {
        val currentLocation = locationOverlay?.myLocation ?: startPoint
        val nearestPointIndex = findNearestPointIndex(currentLocation, interpolatedPoints)
        val targetBearing = bearings[nearestPointIndex]


        }


    private fun findNearestPointIndex(location: GeoPoint, points: List<GeoPoint>): Int {
        var nearestIndex = 0
        var shortestDistance = Double.MAX_VALUE
        for ((index, point) in points.withIndex()) {
            val distance = location.distanceToAsDouble(point)
            if (distance < shortestDistance) {
                shortestDistance = distance
                nearestIndex = index
            }
        }
        return nearestIndex
    }
}


