package com.ibm.rescunet

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun encoding_isCorrect() {
        assertEquals(100, decodeInt(encodeDeviceId(100)))
        assertArrayEquals(
            arrayOf(495, 239, 58),
            decodeLocationAndTime(encodeLocationAndTime(495, 239, 58))
        )
        assertEquals(
            DeviceInfo(498, 1495, 1939, 58).toString(),
            decodeDeviceInfo(encodeDeviceInfo(498, 1495, 1939, 58)).toString()
        )
    }
}
