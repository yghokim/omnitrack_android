package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.transition.TransitionManager
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService

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
        //listView.addItemDecoration(HorizontalDividerItemDecoration(0, 20))

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
                        pendedActivations[requestCode].holderState = OTExternalService.ServiceState.ACTIVATED
                    } else {
                        pendedActivations[requestCode].holderState = OTExternalService.ServiceState.DEACTIVATED
                    }
                    pendedActivations.removeAt(requestCode)
                }
            } else {
                //activation failed.
                println("some permissions not granted. activation failed.")
                pendedActivations[requestCode].holderState = OTExternalService.ServiceState.DEACTIVATED
                pendedActivations.removeAt(requestCode)
            }
        }
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

            val activationButton: Switch
            val progressBar: ProgressBar

            val activationSwitchGroup: ViewGroup
            val activationIndicator: TextView

            var holderState: OTExternalService.ServiceState = OTExternalService.ServiceState.DEACTIVATED
                set(value) {
                    if (field != value) {
                        field = value
                        applyState(field)
                    }
                }


            init {
                thumbView = view.findViewById(R.id.thumb) as ImageView
                nameView = view.findViewById(R.id.name) as TextView
                descriptionView = view.findViewById(R.id.description) as TextView

                progressBar = view.findViewById(R.id.ui_progress_bar) as ProgressBar

                activationSwitchGroup = view.findViewById(R.id.ui_activation_switch_group) as ViewGroup

                activationIndicator = view.findViewById(R.id.ui_activation_indicator_text) as TextView


                activationButton = view.findViewById(R.id.ui_button_activate) as Switch

                activationButton.setOnClickListener {
                    val service = getService(adapterPosition)

                    println("service state: ${service.state}")
                    when (service.state) {
                        OTExternalService.ServiceState.ACTIVATED -> {
                            service.deactivate()
                            holderState = OTExternalService.ServiceState.DEACTIVATED
                        }
                        OTExternalService.ServiceState.ACTIVATING -> {

                        }
                        OTExternalService.ServiceState.DEACTIVATED -> {
                            holderState = OTExternalService.ServiceState.ACTIVATING

                            if (!service.permissionGranted) {
                                pendedActivations.setValueAt(adapterPosition, this@ViewHolder)
                                service.grantPermissions(this@ServiceListFragment, adapterPosition)
                            } else {
                                service.activateAsync(context) {
                                    success ->
                                    if (success) {
                                        holderState = OTExternalService.ServiceState.ACTIVATED
                                    } else {
                                        holderState = OTExternalService.ServiceState.DEACTIVATED
                                    }
                                }
                            }
                        }
                    }
                }

            }

            private fun applyState(state: OTExternalService.ServiceState) {
                TransitionManager.beginDelayedTransition(view as ViewGroup)
                when (state) {
                    OTExternalService.ServiceState.ACTIVATED -> {
                        progressBar.visibility = View.GONE
                        activationIndicator.visibility = View.GONE
                        activationButton.visibility = View.VISIBLE
                        activationButton.isChecked = true
                    }
                    OTExternalService.ServiceState.ACTIVATING -> {
                        activationButton.visibility = View.GONE
                        progressBar.visibility = View.VISIBLE
                        activationButton.isChecked = false
                        activationIndicator.visibility = View.VISIBLE
                    }
                    OTExternalService.ServiceState.DEACTIVATED -> {
                        progressBar.visibility = View.GONE
                        activationIndicator.visibility = View.GONE
                        activationButton.visibility = View.VISIBLE
                        activationButton.isChecked = false
                    }
                }
            }

            fun bind(service: OTExternalService) {
                nameView.text = context.resources.getString(service.nameResourceId)
                descriptionView.text = context.resources.getString(service.descResourceId)
                thumbView.setImageResource(service.thumbResourceId)

                holderState = service.state
            }

        }
    }
}