package com.ibm.rescunet

import android.arch.lifecycle.*


class MainActivityViewModel : ViewModel() {
    val isGroupOwnerState by lazy {
        MutableLiveData<Boolean>()
    }
    val deviceID by lazy {
        MutableLiveData<Int>()
    }

    val headerText = MediatorLiveData<String>()

    init {
        headerText.addSource(isGroupOwnerState) { state ->
            if (state == true)
                headerText.value = "GO ${deviceID.value}"
            else
                headerText.value = "Client ${deviceID.value}"
        }

        headerText.addSource(deviceID) { id ->
            if (isGroupOwnerState.value == true)
                headerText.value = "GO $id"
            else
                headerText.value = "Client $id"
        }
    }
}