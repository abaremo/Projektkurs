package com.example.locatemyvehicle.ui.nearby

import android.graphics.drawable.Drawable
import org.osmdroid.util.GeoPoint

interface ClusterItem {
    val position: GeoPoint
    val icon: Drawable
}