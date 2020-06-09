package com.ibm.rescunet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import java.net.ServerSocket
import java.util.*
import kotlin.math.round
import kotlin.random.Random

class MainService : Service() {
    companion object {
        const val TAG = "rescunet.MainService"
    }

    val myID = Random.nextInt(1, 4096)
    private val handler = Handler()
    private val binder = MyBinder()
    var deviceList = listOf(DeviceInfo(0, 0, 0, 0)) // TODO: GPS
    lateinit var manager: WifiP2pManager
    lateinit var channel: WifiP2pManager.Channel
    lateinit var wifiReceiver: WifiHandler
    private var inCreateGroupMode = false
    var isGroupOwner = true

    val locationListener = object : LocationListener {
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }

        override fun onProviderEnabled(provider: String?) {
        }

        override fun onProviderDisabled(provider: String?) {
        }

        override fun onLocationChanged(location: Location?) {
            location?.takeIf { it.hasAccuracy() && it.accuracy < 100f }?.apply {
                updateDeviceList(
                    listOf(
                        DeviceInfo(
                            0,
                            round(latitude * 100000).toInt(),
                            round(longitude * 100000).toInt(),
                            Date().time
                        )
                    )
                )
                sendDebugMessage("$latitude, $longitude")
            }
        }
    }

    var x = 0

    override fun onCreate() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)

        this.wifiReceiver = WifiHandler(manager, channel, this)
        registerReceiver(this.wifiReceiver, intentFilter)
    }

    override fun onBind(intent: Intent): IBinder? {
        sendDebugMessage("onBind called")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sendDebugMessage("onStartCommand called")
        setDeviceName("INVALID - rescunet")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("rescunet", "rescunet", NotificationManager.IMPORTANCE_DEFAULT)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, "rescunet")
            .setContentTitle("Rescunet is running")
            .setContentText("...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(100, notification)

        val locationManager =  getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
        sendDebugMessage("Requesting location")}
        catch (e: SecurityException) { throw e }

        return START_STICKY
    }

    override fun onDestroy() {
        val locationManager =  getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(locationListener)
        unregisterReceiver(wifiReceiver)
    }

    fun setDeviceName(s: String) {
        try {
            val setDeviceName = manager.javaClass.getMethod(
                "setDeviceName",
                WifiP2pManager.Channel::class.java, String::class.java, WifiP2pManager.ActionListener::class.java
            )
            setDeviceName.invoke(
                manager,
                channel,
                s,
                object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                    }

                    override fun onFailure(reason: Int) {

                    }
                })
        } catch (e: Exception) {
//            sendNetworkStatus("Failed to set device name")
        }
    }

    fun start() {
        isGroupOwner = true
        createGroup()
        Thread {
            while (true) {
                Thread.sleep(100)
                ++x
            }
        }.start()
    }

    //region Group Owner functions

    fun createGroup() {
//        setDeviceName(encodeDeviceInfo(487, 0, x, 39) + "0000000" + "0000000" + "0000000" + "rsn")
        manager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                wifiReceiver.waitForConnection = true
                sendDebugMessage("Created group successfully")
                inCreateGroupMode = true
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Failed to create group: $reason")
                removeGroup()
                createGroup()
            }
        })
        Thread {
            // stay as GO for 8 seconds
            Thread.sleep(20000)
            if (inCreateGroupMode) {
                handler.post {
                    inCreateGroupMode = false
                    removeGroup()
                    isGroupOwner = false
                    discoverPeers()
                }
            }
        }.start()
        // TODO("Start server")
    }

    fun removeGroup() {
        inCreateGroupMode = false
        wifiReceiver.waitForConnection = false
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                sendDebugMessage("Removed group successfully")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Failed to remove group")
            }
        })
    }

    fun onConnectionEstablished() {
        inCreateGroupMode = false
        wifiReceiver.waitForConnection = false
        sendDebugMessage("onConnectionEstablished")
        if (isGroupOwner) {
            // TODO("Perform data exchange, remove group")
            // TODO if enough time has passed, call function to start client
            Thread.sleep(3000)
            if (true) {
                sendDebugMessage("Exchanging data as server")
                removeGroup()
                isGroupOwner = false
                discoverPeers()
            } else {
                removeGroup()
                createGroup()
            }
        } else {
            Thread.sleep(3000)
            if (true) {
                sendDebugMessage("Exchanging data as client")
                isGroupOwner = true
                manager.cancelConnect(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        sendDebugMessage("disconnecting")
                    }

                    override fun onFailure(reason: Int) {
                        Log.e(TAG, "failed to disconnect: error $reason")
                    }
                })
                createGroup()
            }
        }
    }

    //endregion

    //region Client functions

    fun discoverPeers() {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                sendDebugMessage("Started peer discovery")
            }

            override fun onFailure(reason: Int) {
                sendDebugMessage("Failed to discover peers")
                onP2pFailure()
            }
        })
        Thread {
            // todo what if a connection comes in right now?
            Thread.sleep(10000)
            handler.post {
                connectToGroupOwner()
            }
        }.start()
    }

    var peersSeen = mutableListOf<WifiP2pDevice>()

    fun onPeersChanged(peerlist: WifiP2pDeviceList) {
        // update the peer list
        val list = peerlist.deviceList.filter { it.deviceName.endsWith("rsn") && it.deviceName.length == 31 }
        list.forEach { device ->
            sendDebugMessage("Found device ${device.deviceName}")
            val devices = arrayOf(0..6, 7..13, 14..20, 21..27).map { decodeDeviceInfo(device.deviceName.slice(it)) }
                .filter { it.id > 0 && it.id != myID }
            sendDebugMessage("Received devices: $devices")
            if (deviceList[0].lat != 0 || deviceList[0].long != 0) updateDeviceList(devices)
        }
    }

    private fun updateDeviceList(devices: List<DeviceInfo>) {
        sendDebugMessage(devices.toString())
        sendDebugMessage(deviceList.toString())
        val newList = deviceList.toMutableList()
        devices.forEach { updatedDevice ->
            val filteredList = deviceList.filter { it.id == updatedDevice.id }
            if (filteredList.isEmpty()) {
                newList.add(
                    DeviceInfo(
                        updatedDevice.id,
                        updatedDevice.lat,
                        updatedDevice.long,
                        updatedDevice.timeStamp.toInt(),
                        deviceList[0].lat,
                        deviceList[0].long
                    )
                )
            } else {
                val item = filteredList[0]
                if (item.id == 0) item.pushToHistoryAbsolute(updatedDevice.lat, updatedDevice.long, updatedDevice.timeStamp)
                else item.pushToHistory(updatedDevice.lat, updatedDevice.long, updatedDevice.timeStamp.toInt())
            }
        }
        sendUpdatePeersMessage()
        deviceList = newList

        var newDeviceName = encodeDeviceId(myID) + deviceList[0].getEncodedString().slice(2..6)
        val sortedList =  deviceList.filter { it.id > 0 }.sortedBy { it.getTimeSinceLastSeen() }
        sortedList.map { it.getEncodedString() }.forEach{newDeviceName += it}
        newDeviceName = newDeviceName.padEnd(28, '0').slice(0..27) + "rsn"

        setDeviceName(newDeviceName)
    }

    fun connectToGroupOwner() {
        wifiReceiver.waitForConnection = true
        if (peersSeen.isEmpty()) {
            sendDebugMessage("No peers found")
            start()
        } else {
            // for now, connect to any device
            val device = peersSeen.first()
            val config = WifiP2pConfig().apply {
                deviceAddress = device.deviceAddress
                wps.setup = WpsInfo.PBC
            }
            manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    sendDebugMessage("Connecting to device ${device.deviceName}")
                }

                override fun onFailure(reason: Int) {
                    Log.e(TAG, "Connection failed: error $reason")
                    start()
                }
            })
        }
    }
    //endregion

    fun onP2pFailure() {
        // TODO reset p2p
    }

    fun sendDebugMessage(s: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_MESSAGE
            putExtra("message", s)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendUpdatePeersMessage() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "PLOT_PEERS"
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    inner class MyBinder : Binder() {
        val service get() = this@MainService
    }
}
