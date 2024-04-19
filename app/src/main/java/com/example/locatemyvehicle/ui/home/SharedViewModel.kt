package com.example.locatemyvehicle.ui.home

import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val savedLocationsList = mutableListOf<String>()
}