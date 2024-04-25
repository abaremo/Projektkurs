package com.example.locatemyvehicle.ui.home

import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    fun removeSavedLocation(position: Int) {

    }

    val savedLocationsList = mutableListOf<String>()
}