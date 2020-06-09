package com.ibm.rescunet

import android.os.Parcel
import android.os.Parcelable
import java.util.*
import kotlin.math.round

class DeviceInfo(val id: Int, lat: Int, long: Int, timeStamp: Long) : Parcelable {
    val lat get() = history.first.first
    val long get() = history.first.second
    val timeStamp get() = history.first.third
    /**
     * Time since the last history item, in MILLISECONDS
     */
    fun getTimeSinceLastSeen() = Date().time - history.first.third
    fun getEncodedString() = encodeDeviceInfo(id, (lat + 360_00000) % 2000, (long + 360_00000) % 2000, (history.first.third / 10000 % 180).toInt())
    val history: LinkedList<Triple<Int, Int, Long>> = LinkedList(listOf(Triple(lat, long, timeStamp)))

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readLong()
    )

    constructor(id: Int, lat: Int, long: Int, timeStamp: Int, relativeLat: Int, relativeLong: Int) :
            this(id, handleEncodedData(lat, relativeLat, 2000), handleEncodedData(long, relativeLong, 2000), handleEncodedTime(timeStamp * 10000L, Date().time, 1800000))

    override fun toString(): String {
        return "$id: $history"
    }

    fun pushToHistory(encodedLat: Int, encodedLong: Int, encodedTime: Int) {
        val pushedItem =
            Triple(
                handleEncodedData(encodedLat, lat, 2000),
                handleEncodedData(encodedLong, long, 2000),
                handleEncodedTime(encodedTime.toLong() * 10000, Date().time, 1800000L)
            )
        if (!history.contains(pushedItem)) {
            history.push(pushedItem)
            history.sortBy { -it.third }
        }
    }

    fun pushToHistoryAbsolute(encodedLat: Int, encodedLong: Int, encodedTime: Long) {
        val pushedItem =
            Triple(encodedLat, encodedLong, encodedTime
            )
        if (!history.contains(pushedItem)) {
            history.push(pushedItem)
            history.sortBy { -it.third }
        }
    }
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(lat)
        parcel.writeInt(long)
        parcel.writeLong(history.first.third)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DeviceInfo> {
        override fun createFromParcel(parcel: Parcel): DeviceInfo {
            return DeviceInfo(parcel)
        }

        override fun newArray(size: Int): Array<DeviceInfo?> {
            return arrayOfNulls(size)
        }

        private fun handleEncodedData(encoded: Int, original: Int, limit: Int): Int {
            return round((original-encoded).toDouble() / limit.toDouble()).toInt() * limit + encoded
        }

        private fun handleEncodedTime(encoded: Long, original: Long, limit: Long): Long {
            return if ((original + limit / 5) % limit > encoded)
                (original + limit/5) / limit * limit + encoded
            else
                (original + limit/5) / limit * (limit - 1)+ encoded
        }
    }
}