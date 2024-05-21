package com.example.locatemyvehicle.ui.nearby

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.example.locatemyvehicle.R
import org.osmdroid.util.GeoPoint

data class HotelClusterItem(
    override val position: GeoPoint,
    val hotelName: String,
    val context: Context // Lägg till en context-variabel
) : ClusterItem {
    override val icon: Drawable
        get() = ContextCompat.getDrawable(context, R.drawable.ic_hotel_onmap)!!
}