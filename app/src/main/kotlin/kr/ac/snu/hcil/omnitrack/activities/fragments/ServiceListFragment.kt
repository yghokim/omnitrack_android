package kr.ac.snu.hcil.omnitrack.activities.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.microsoft.band.MicrosoftBandService

/**
 * Created by Young-Ho on 7/29/2016.
 */
class ServiceListFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_home_services, container, false)

        val connectButton = rootView.findViewById(R.id.connect)

        connectButton?.setOnClickListener {
            MicrosoftBandService.connectAsync {
                connected ->
                if (MicrosoftBandService.measureFactories[0].permissionGranted) {

                    println("request heart rate")
                    MicrosoftBandService.measureFactories[0].requestValueAsync {
                        result ->
                        println("current haert rate: $result")
                    }
                } else {
                    MicrosoftBandService.measureFactories[0].grantPermissions(activity) {
                        accepted ->
                        if (accepted) {

                            println("request heart rate")
                            MicrosoftBandService.measureFactories[0].requestValueAsync {
                                result ->
                                println("current haert rate: $result")
                            }
                        }
                    }
                }
            }
        }


        return rootView
    }
}