package pl.pw.geoapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object StatusLiveData {
    private val _isGpsEnabled = MutableLiveData(false)
    private val _isBluetoothEnabled = MutableLiveData(false)

    val isGpsEnabled: LiveData<Boolean> get() = _isGpsEnabled
    val isBluetoothEnabled: LiveData<Boolean> get() = _isBluetoothEnabled

    fun updateGpsStatus(enabled: Boolean) {
        _isGpsEnabled.postValue(enabled)
    }

    fun updateBluetoothStatus(enabled: Boolean) {
        _isBluetoothEnabled.postValue(enabled)
    }
}
