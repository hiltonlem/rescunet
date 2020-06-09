package com.ibm.rescunet

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Network
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.util.Log
import android.widget.Toast

private const val TAG: String = "WifiHandler"

class WifiHandler(private val manager: WifiP2pManager, private val channel: WifiP2pManager.Channel, private val service: MainService)
    : BroadcastReceiver(), WifiP2pManager.ActionListener, WifiP2pManager.PeerListListener {

    // private lateinit var channel: WifiP2pManager.Channel
    private var isWifiGroupCreated : Boolean = false
    val peers = mutableListOf<WifiP2pDevice>()
    var waitForConnection = false
        set(value) {
            Log.d(TAG, if (value) "open for connection" else "not open for connection")
            field = value
        }

    override fun onReceive(context: Context?, intent: Intent) {

        when(intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Determine if Wifi P2P mode is enabled or not, alert
                // the Activity.
                // setIsWifiP2pEnabled(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
                sendNetworkStatus("wifi state changed")
            }
            WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                sendNetworkStatus("wifi discovery changed")
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // Request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()
                // service.onPeersChanged()
                // sendNetworkStatus("Peers changed, requesting")
                // manager?.requestPeers(channel, this)
                val deviceList = intent.getParcelableExtra<WifiP2pDeviceList>(WifiP2pManager.EXTRA_P2P_DEVICE_LIST)
                service.sendDebugMessage("PEERS_CHANGED intent received ${deviceList.deviceList.map { it -> it.deviceName + " " + it.deviceAddress }}")
                service.onPeersChanged(deviceList)

                // TODO("the wifi p2p peers changed action is aggressive. will need to throttle the requestPeers or discovery")
                // TODO("the wifi p2p peer discovery appears to pause on sleep/lock. will need to run in background")
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                if (waitForConnection) {
                    val groupInfo = intent.getParcelableExtra<WifiP2pGroup>(WifiP2pManager.EXTRA_WIFI_P2P_GROUP)
                    if (groupInfo.clientList.isNotEmpty()) {
                        service.onConnectionEstablished()
                    }
                }

                return
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                sendNetworkStatus("wifi this device changed")
                sendNetworkStatus(
                    (intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE) as WifiP2pDevice).deviceName
                )
                if (waitForConnection) {
                    val groupInfo = intent.getParcelableExtra<WifiP2pGroup?>(WifiP2pManager.EXTRA_WIFI_P2P_GROUP)
                    sendNetworkStatus("${groupInfo?.networkName}")
                    if (groupInfo?.clientList?.isNotEmpty() == true) {
                        service.onConnectionEstablished()
                    }
                }

                // sendNetworkStatus(intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE).deviceName)
                /*
                (activity.supportFragmentManager.findFragmentById(R.id.frag_list) as DeviceListFragment)
                    .apply {
                        updateThisDevice(
                            intent.getParcelableExtra(
                                WifiP2pManager.EXTRA_WIFI_P2P_DEVICE) as WifiP2pDevice
                        )
                    }*/
            }
        }
    }


    //region GO methods

    //endregion

    //region Client methods

    //endregion


    // wifi Action Listener
    override fun onSuccess() {
        // Toast.makeText(activity, "Discovery Initiated",
        //  Toast.LENGTH_SHORT).show();    }
    }

    override fun onFailure(reason: Int) {
        sendNetworkStatus("Discovery Failed with error code $reason")
    }

    fun connect(position: Int) {
        // Picking the first device found on the network.
        val device = peers[position]

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                sendNetworkStatus("Successfully connected")
            }

            override fun onFailure(reason: Int) {
                sendNetworkStatus("Connection failed. retry")
            }
        })
    }

    // PeerList Listener

    override fun onPeersAvailable(peerList: WifiP2pDeviceList) {
        val refreshedPeers = peerList.deviceList
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers.filter { it.deviceName.endsWith("rescunet") })
        }

        if (peers.isEmpty()) {
            sendNetworkStatus("No peers found")
        } else {
            sendNetworkStatus(peers.size.toString() + " peers found")
            peers.forEach {
                sendNetworkStatus("Found device ${it.deviceName}")
            }

            // start trying to connect to any peer
            // this.connect()
        }
    }

    private fun sendNetworkStatus(message: String) {
        service.sendDebugMessage(message)
    }

}