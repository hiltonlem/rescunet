package com.ibm.rescunet

import android.util.Log
import java.io.DataOutputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

class UploadToApiThread(val data: String, val onFinish: (String) -> Unit) : Runnable {
    override fun run() {
        var error = ""
        val connection = URL("http://rescunetbackend.mybluemix.net/json").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            connection.doOutput = true

            DataOutputStream(connection.outputStream).apply {
                writeBytes(data)
                flush()
                close()
            }

            // connection.connect()

            val r = connection.responseCode
            if (r >= 300)
                error = "HTTP error $r"
        } catch (e: Exception) {
            error = e.message?:"Failed to upload to server"
        } finally {
            onFinish(error)
            try {
                connection.disconnect()
            } catch (e: Exception) {}
        }
    }
}