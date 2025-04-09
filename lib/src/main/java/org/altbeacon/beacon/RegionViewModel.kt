package org.altbeacon.beacon

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RegionViewModel: ViewModel() {
    val regionState: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }
    val rangedBeacons: MutableLiveData<Collection<Beacon>> by lazy {
        MutableLiveData<Collection<Beacon>>()
    }
}