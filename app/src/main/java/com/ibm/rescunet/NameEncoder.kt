package com.ibm.rescunet

import android.location.Location
import java.lang.Exception
import java.util.*
import kotlin.math.max

const val encoding = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-"

/**
 * Decodes a single character into 0..63
 */
fun decodeChar(c: Char): Int {
    if (encoding.indexOf(c) == -1)
        throw Exception()
    else
        return encoding.indexOf(c)
}

/**
 * Encodes a non-negative integer in base 64 using <code>length</code> characters
 */
fun encodeInt(n: Int, length: Int): String {
    assert(n >= 0)
    assert(length >= 0)
    var result = ""
    var x = n
    for (i in 0 until length) {
        result = encoding[x % 64] + result
        x /= 64
    }
    return result
}

/**
 * Encodes an id between 0 and 4095
 */
fun encodeDeviceId(id: Int) = encodeInt(id, 2)

/**
 * Encodes location and time
 * location is encoded using the digits 0.00xxx in the long and lat axis
 * time is encoded in units of 10 seconds, mod 180
 * accuracy is encoded as 0 to 4
 */
fun encodeLocationAndTime(lat: Int, long: Int, time: Int): String {
    assert(long < 2000)
    assert(lat < 2000)
    assert(time < 180)
    return encodeInt((time * 2048 * 2048 + lat * 2048 + long), 5)
}

/**
 * Decodes a base 64 integer
 */
fun decodeInt(s: String): Int {
    var n = 0
    for (c in s) {
        n *= 64
        n += decodeChar(c)
    }
    return n
}

/**
 * Extract the location and time info out of a 5-character 64 bit string
 * Format: lat, long, accuracy (0-4), time
 */
fun decodeLocationAndTime(s: String): Array<Int> {
    val n = decodeInt(s)
    return arrayOf(n / 2048 % 2048, n % 2048 % 2048, n / (2048 * 2048))
}

fun decodeDeviceInfo(s: String): DeviceInfo {
    val l = decodeLocationAndTime(s.slice(2..6))
    return DeviceInfo(decodeInt(s.slice(0..1)), l[0], l[1], l[2].toLong())
}

fun encodeDeviceInfo(id: Int, lat: Int, long: Int, time: Int): String {
    return encodeDeviceId(id) + encodeLocationAndTime(lat, long, time)
}