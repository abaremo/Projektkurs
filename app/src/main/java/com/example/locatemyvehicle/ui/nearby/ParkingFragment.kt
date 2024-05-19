package com.example.locatemyvehicle.ui.nearby

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import com.example.locatemyvehicle.R
import com.example.locatemyvehicle.databinding.FragmentParkingBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.wms.BuildConfig
import java.io.IOException

class ParkingFragment : Fragment() {
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var binding: FragmentParkingBinding
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val startPoint = GeoPoint(62.0, 16.0)
    val fragment = this


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentParkingBinding.inflate(inflater, container, false)
        return binding.root


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurationMap()
        initMap()
        setZoomMultiTouch(true) //FUNKAR EJ
        getLocation(true)

        val jsonObject = loadJSONFromAsset(requireContext(), "parking.json")
        val featuresH = jsonObject.getJSONArray("features")
        for (i in 0 until featuresH.length()) {
            val oneOb = featuresH.getJSONObject(i)
            val geometry = oneOb.getJSONObject("geometry")
            val latlon = geometry.getJSONArray("coordinates")
            //get to coordinates in json and set the first coordinate to lon and
            //the second to lat
            val lat = latlon.getDouble(1)
            val lon = latlon.getDouble(0)
            val point = GeoPoint(lat, lon)
            //val name = ("Vindkraftverk")
            val markerH = Marker(binding.mapOSM)
            //setMarker(point, name, "vindkraft")
            markerH.position = point
            markerH.icon = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_parking_onmap
            )
            markerH.title = "Parking spot"
            markerH.setOnMarkerClickListener { marker, mapOSM ->
                Toast.makeText(requireContext(), marker.title, Toast.LENGTH_SHORT).show()
                showTransportSelectionDialog(marker.position)
                return@setOnMarkerClickListener true
            }
            binding.mapOSM.overlays.add(markerH)
            binding.mapOSM.invalidate()
        }
    }






    //return JSONObject of input .json file. It holds all rows of the geojson.
    private fun loadJSONFromAsset(context: Context, fileName: String):
            JSONObject {
        val jsonString: String
        try {
            val inputStream = context.assets.open(fileName)
            jsonString = inputStream.bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle error
            return JSONObject() // Return empty JSONObject in case of error
        }
        return JSONObject(jsonString)

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
                Toast.makeText(fragment.requireContext(), "Platsbehörighet nekades.", Toast.LENGTH_SHORT).show()
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
        //locationOverlay.enableFollowLocation()
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
                } else {
// Om användarens plats inte är tillgänglig, hantera det här
                    Toast.makeText(fragment.requireContext(), "Användarens plats är inte tillgänglig.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    /// build road
    private fun buildRoad(endPoint: GeoPoint, meanOfTransport: String ) {
        binding.mapOSM.overlays.removeAll { it is Polyline }
        CoroutineScope(Dispatchers.IO).launch {
            val roadManager = OSRMRoadManager(
                requireContext(),
                System.getProperty("http.agent")
            )
// Hur reser du i rutten, cykel, gå, bil
            //roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT)

            when (meanOfTransport) {
                "foot" -> roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT)
                "bike" -> roadManager.setMean(OSRMRoadManager.MEAN_BY_BIKE)
                "car" -> roadManager.setMean(OSRMRoadManager.MEAN_BY_CAR)
                // Lägg till fler transportsätt här om det behövs
            }
            val waypoints =
                arrayListOf<GeoPoint>(
                    locationOverlay?.myLocation ?: startPoint,
                    endPoint
                )
            try {
                val road = roadManager.getRoad(waypoints)
                val roadOverlay = RoadManager.buildRoadOverlay(road)
                roadOverlay.color = Color.BLACK
                withContext(Dispatchers.Main) {
                    binding.mapOSM.overlays.add(0, roadOverlay)
                    binding.mapOSM.invalidate()

                    val formatlength = "%.2f".format(road.mLength)
                    val formattime = "%.2f".format(road.mDuration / 60)
                    Toast.makeText(
                        requireContext(), "${formatlength} km, " +
                                "${formattime} min", Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("RoadBuildingError", "Error building road: ${e.message}")
// Hantera fel här
            }
        }
    }

    private fun showTransportSelectionDialog(endPoint: GeoPoint) {
        val transports = arrayOf("Foot", "Bike", "Car") // Lägg till fler alternativ om det behövs

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Choose Transportation")
            .setItems(transports) { _, which ->
                // Anropa buildRoad med det valda transportsättet
                when (which) {
                    0 -> buildRoad(endPoint, "foot")
                    1 -> buildRoad(endPoint, "bike")
                    2 -> buildRoad(endPoint, "car")
                    // Lägg till fler alternativ här om det behövs
                }
            }

        val dialog = builder.create()
        dialog.show()
    }
}
