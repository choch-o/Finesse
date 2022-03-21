package com.chocho.finest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object App {    // Singleton
    var isRecordingActive: Boolean = false
    var isRecording: Boolean?
        get() = isRecordingLiveData.value
        set(value) {
            isRecordingLiveData.postValue(value)
        }
    val isRecordingLiveData = MutableLiveData<Boolean>()
}