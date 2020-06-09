package com.ibm.rescunet

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_credits_page.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [GettingStartedPage.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [GettingStartedPage.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class GettingStartedPage : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val t = arguments?.getInt("page")
        var view: View? = null
        if (t in 0..5 && t != null)
            view = inflater.inflate(
                listOf(
                    R.layout.fragment_credits_page,
                    R.layout.fragment_explain_ssid,
                    R.layout.fragment_explan_hop,
                    R.layout.fragment_do_not_exit,
                    R.layout.fragment_do_not_force_stop,
                    R.layout.fragment_getting_started_page
                )[t], container, false
            )
        if (t == 0) {
            view?.findViewById<Button>(R.id.a2_view_license_button)?.setOnClickListener {
                AlertDialog.Builder(context).create().apply {
                    setMessage(getString(R.string.apache_2_license))
                    setButton(AlertDialog.BUTTON_NEUTRAL, "DONE") { _, _ -> }
                }.show()
            }
        }
        // Inflate the layout for this fragment
        return view
    }
}
