package kr.ac.snu.hcil.omnitrack.activities.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.ui.HorizontalDividerItemDecoration

/**
 * Created by Young-Ho on 7/29/2016.
 */
class ServiceListFragment : Fragment() {

    private lateinit var listView: RecyclerView

    private lateinit var adapter: Adapter

    private val pendedActivations = SparseArray<Adapter.ViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_home_services, container, false)

        listView = rootView.findViewById(R.id.ui_list) as RecyclerView

        listView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listView.addItemDecoration(HorizontalDividerItemDecoration(0, 20))

        adapter = Adapter()

        listView.adapter = adapter

        return rootView
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        println("permission result: $requestCode ${pendedActivations.size()}")
        if (pendedActivations[requestCode] != null) {
            if (grantResults.indexOf(PackageManager.PERMISSION_DENIED) == -1) {
                OTExternalService.availableServices[requestCode].activateAsync(context) {
                    success ->
                    if (success) {
                        pendedActivations[requestCode].applyState(OTExternalService.ServiceState.ACTIVATED)
                    } else {
                        pendedActivations[requestCode].applyState(OTExternalService.ServiceState.DEACTIVATED)
                    }
                    pendedActivations.removeAt(requestCode)
                }
            } else {
                //activation failed.
                println("some permissions not granted. activation failed.")
                pendedActivations[requestCode].applyState(OTExternalService.ServiceState.DEACTIVATED)
                pendedActivations.removeAt(requestCode)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {


        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getService(position))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.external_service_list_element, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return OTExternalService.availableServices.size
        }

        private fun getService(position: Int): OTExternalService {
            return OTExternalService.availableServices[position]
        }


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            val thumbView: ImageView
            val nameView: TextView
            val descriptionView: TextView

            val activationButton: Button

            init {
                thumbView = view.findViewById(R.id.thumb) as ImageView
                nameView = view.findViewById(R.id.name) as TextView
                descriptionView = view.findViewById(R.id.description) as TextView

                activationButton = view.findViewById(R.id.ui_button_activate) as Button

                activationButton.setOnClickListener {
                    val service = getService(adapterPosition)
                    when (service.state) {
                        OTExternalService.ServiceState.ACTIVATED -> {
                            service.deactivate()
                            applyState(OTExternalService.ServiceState.DEACTIVATED)
                        }
                        OTExternalService.ServiceState.ACTIVATING -> {

                        }
                        OTExternalService.ServiceState.DEACTIVATED -> {
                            applyState(OTExternalService.ServiceState.ACTIVATING)

                            if (!service.permissionGranted) {
                                pendedActivations.setValueAt(adapterPosition, this@ViewHolder)
                                service.grantPermissions(this@ServiceListFragment, adapterPosition)
                            } else {
                                service.activateAsync(context) {
                                    success ->
                                    if (success) {
                                        applyState(OTExternalService.ServiceState.ACTIVATED)
                                    } else {
                                        applyState(OTExternalService.ServiceState.DEACTIVATED)
                                    }
                                }
                            }
                        }
                    }
                }

            }

            fun applyState(state: OTExternalService.ServiceState) {
                when (state) {
                    OTExternalService.ServiceState.ACTIVATED -> {
                        activationButton.text = "Deactivate"
                        activationButton.setTextColor(Color.WHITE)
                        activationButton.setBackgroundResource(R.drawable.button_background_red)
                        activationButton.isEnabled = true
                    }
                    OTExternalService.ServiceState.ACTIVATING -> {

                        activationButton.text = "Activating..."
                        activationButton.setTextColor(Color.parseColor("#ff252525"))
                        activationButton.setBackgroundResource(R.drawable.transparent_button_background)
                        activationButton.isEnabled = false

                    }
                    OTExternalService.ServiceState.DEACTIVATED -> {

                        activationButton.text = "Activate"
                        activationButton.setTextColor(Color.WHITE)
                        activationButton.setBackgroundResource(R.drawable.button_background_green)
                        activationButton.isEnabled = true
                    }
                }
            }

            fun bind(service: OTExternalService) {
                nameView.text = context.resources.getString(service.nameResourceId)
                descriptionView.text = context.resources.getString(service.descResourceId)
                thumbView.setImageResource(service.thumbResourceId)

                applyState(service.state)
            }

        }
    }
}