package com.ibm.rescunet

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment

class StopServiceDialogFragment : DialogFragment() {
    var onStopServiceListener: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage("Are you sure you want to stop the service?")
            builder.setTitle("Warning")
            builder.setPositiveButton("Yes") {
                _, _ -> onStopServiceListener?.let { it() }
            }
            builder.setNegativeButton("No") {
                _, _ -> Unit
            }
            return builder.create()
        } ?: throw NullPointerException()
    }
}