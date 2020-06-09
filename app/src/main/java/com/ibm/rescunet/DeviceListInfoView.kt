package com.ibm.rescunet

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.ibm.rescunet.Formatter.formatCoord
import com.ibm.rescunet.Formatter.formatTimeSinceSeen
import kotlinx.android.synthetic.main.list_view_device_information.view.*
import kotlinx.android.synthetic.main.view_device_info_list.view.*

class DeviceListInfoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    val adapter = DeviceInfoAdapter(context as Activity, mutableListOf())
    val devices: MutableList<DeviceInfo> get() = adapter.devices

    init {
        View.inflate(context, R.layout.view_device_info_list, this)
        list_device.adapter = adapter
    }

    class DeviceInfoAdapter(val context: Activity, val devices: MutableList<DeviceInfo>) :
        ArrayAdapter<DeviceInfo>(context, R.layout.list_view_device_information, devices) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return context.layoutInflater.inflate(R.layout.list_view_device_information, null, true).apply {
                text_id.text = if (devices[position].id == 0) "Me" else devices[position].id.toString()
                text_id.setTextColor(
                    Color.rgb(
                        45 + devices[position].id % 16 * 14,
                        45 + devices[position].id / 16 % 16 * 14,
                        45 + devices[position].id / 16 / 16 * 14
                    )
                )
                text_info.text = formatTimeSinceSeen(devices[position].getTimeSinceLastSeen())
                text_gps_lat.text = formatCoord(devices[position].lat, "N", "S")
                text_gps_long.text = formatCoord(devices[position].long, "E", "W")
            }
        }
    }

    fun updateMyGPS(lat: Int, long: Int) {
        text_my_gps.text = "${formatCoord(lat, "N", "S")}, ${formatCoord(long, "E", "W")}"
    }
}