package com.example.locatemyvehicle.ui.nearby

import android.content.Context
import android.graphics.drawable.Drawable
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MarkerCluster<T : ClusterItem>(
    private val context: Context,
    private val mapView: MapView,
    private val hotelIcon: Drawable // Exempel: Referens till hotelikonen från HotelFragment
) {
    private val markerList = mutableListOf<Marker>()
    private lateinit var locationOverlay: MyLocationNewOverlay

    init {
        mapView.overlayManager.addAll(markerList)
        mapView.invalidate()
    }

    fun addMarkers(items: List<T>) {
        for (item in items) {
            val marker = Marker(mapView)
            marker.position = item.position
            // Använd ikonen från ClusterItem-objektet
            marker.icon = item.icon
            markerList.add(marker)
        }
        mapView.invalidate()
    }

    fun clearMarkers() {
        mapView.overlayManager.removeAll(markerList)
        markerList.clear()
        mapView.invalidate()
    }

    fun setLocationOverlay(overlay: MyLocationNewOverlay) {
        locationOverlay = overlay
        mapView.overlayManager.add(locationOverlay)
        mapView.invalidate()
    }
}