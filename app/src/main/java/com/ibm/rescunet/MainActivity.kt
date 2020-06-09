package com.ibm.rescunet

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.*
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.random.Random

class MainActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        const val ACTION_MESSAGE = "com.ibm.rescunet.MESSAGE"
    }
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var mainService: MainService
    private var bound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bound = true
            mainService = (service as MainService.MyBinder).service
            mainService.start()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
        }
    }
    private val br = BR()

    // device sensor manager
    private var mSensorManager: SensorManager? = null
    internal lateinit var compassHeading: TextView

    //region UI initialization
    private fun setUpObservers() {
//        viewModel.headerText.observe(this, Observer { textView2.text = it ?: "" })
    }

    private fun confirmStop() {
        val fragment = StopServiceDialogFragment()
        fragment.onStopServiceListener = {
            mainService.removeGroup()
            mainService.setDeviceName("Stopped - rescunet")
//            System.exit(0)
            unbindService(connection)
            mainService.stopSelf()
            stopService(intent)
            finish()
            Log.d("ACTIVITY", "Stop")
        }
        fragment.show(supportFragmentManager, "stopService")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        val intent = Intent(this, MainService::class.java)
        if (!::mainService.isInitialized) {
            startService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.my_toolbar))

        setUpObservers()

        view_device_plot.updateListener = object : DevicePlotView.DeviceUpdateListener {
            override fun onDeviceNameUpdate(list: List<DeviceInfo>) {
                view_device_list.devices.apply {
                    clear()
                    addAll(list)
                }
                view_device_list.adapter.notifyDataSetChanged()
            }

            override fun onMyGPSUpdate(lat: Int, long: Int) {
                view_device_list.updateMyGPS(lat, long)
            }
        }

        view_peerList.setOnItemClickListener { parent, view, position, id ->
            mainService.wifiReceiver.connect(position)
        }

        my_toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_stop -> {
                    confirmStop()
                    true
                }
                R.id.action_help -> {
                    startActivity(Intent(this, GettingStartedActivity::class.java))
                    true
                }
                R.id.action_upload -> {
                    if (mainService.deviceList.isNotEmpty() && mainService.deviceList[0].timeStamp > 0) {
                        val devices = mainService.deviceList.map {
                            "{\"id\": \"${if (it.id==0) mainService.myID else it.id}\", \"lat\": \"${String.format("%.5f", it.lat / 100000.0)}\", \"long\": \"${String.format("%.5f", it.long / 100000.0)}\"}"
                        }
                        val dataString = "{\"myId\": \"${mainService.myID}\", \"devices\": [${devices.joinToString(",")}]}"
                        Log.d("TAG", dataString)
                        Thread(UploadToApiThread(dataString) {
                            runOnUiThread {
                                if (it == "") {
                                    Toast.makeText(this, "Successfully uploaded", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Failed to upload: $it", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }).start()
                    } else {
                        Toast.makeText(this, "Please wait for a GPS connection", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> {
                    super.onOptionsItemSelected(item)
                }
            }
        }

        viewModel.isGroupOwnerState.value = true

        LocalBroadcastManager.getInstance(this).registerReceiver(br, IntentFilter().apply { addAction(ACTION_MESSAGE); addAction("PLOT_PEERS") })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
        } else {
            onRequestPermissionsResult(0, arrayOf(), intArrayOf())
        }

        // TextView that will tell the user what degree is he heading
        compassHeading = findViewById(R.id.compassHeading)

        // initialize your android device sensor capabilities
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(br)
    }

    inner class BR : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_MESSAGE) Log.d("ACTIVITY", intent?.getStringExtra("message"))
            else {
                Log.d("ACTIVITY", mainService.deviceList.filter { it.id > 0 }.toString())
                view_device_plot.devices = mainService.deviceList
                view_device_plot.renderPoints(false)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    //endregion

    /** register the BroadcastReceiver with the intent values to be matched  */
    public override fun onResume() {
        super.onResume()

        // for the system's orientation sensor registered listeners
        mSensorManager!!.registerListener(
            this, mSensorManager!!.getDefaultSensor(Sensor.TYPE_ORIENTATION),
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    public override fun onPause() {
        super.onPause()
        // unregisterReceiver(this.wifiReceiver)

        // to stop the listener and save battery
        mSensorManager!!.unregisterListener(this)
    }

    override fun onBackPressed() {
        view_device_plot.undoClick()
    }

    override fun onSensorChanged(event: SensorEvent) {

        // get the angle around the z-axis rotated
        val degree = Math.round(event.values[0]).toFloat()

        compassHeading.text = "Heading: " + java.lang.Float.toString(degree) + " degrees"

        view_device_plot.heading = degree
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // not in use
    }
}

