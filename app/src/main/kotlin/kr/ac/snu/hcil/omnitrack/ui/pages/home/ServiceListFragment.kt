package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.transition.TransitionManager
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.net.NetworkHelper

/**
 * Created by Young-Ho on 7/29/2016.
 */
class ServiceListFragment : OTFragment() {

    private lateinit var listView: RecyclerView

    private lateinit var adapter: Adapter

    private val pendedActivations = SparseArray<Adapter.ViewHolder>()

    private val internetRequiredAlertBuilder: MaterialDialog.Builder by lazy {
        DialogHelper.makeSimpleAlertBuilder(context, context.getString(R.string.msg_external_service_activation_requires_internet))
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

            val activationButton: AppCompatButton
            val progressBar: ProgressBar

            val measureFactoryListView: RecyclerView

            val measureFactoryAdapter = MeasureFactoryAdapter()

            private val activateColor: ColorStateList by lazy {
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPointed))
            }

            private val deactivateColor: ColorStateList by lazy {
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorRed_Light))
            }

            private val onActivatingColor: ColorStateList by lazy {
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.material_grey_100))
            }


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

                measureFactoryListView = view.findViewById(R.id.ui_supported_measure_list) as RecyclerView
                measureFactoryListView.adapter = measureFactoryAdapter
                measureFactoryListView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                measureFactoryListView.addItemDecoration(HorizontalDividerItemDecoration(ContextCompat.getColor(context, R.color.separator_Light), (0.6f * resources.displayMetrics.density + .5f).toInt()))

                activationButton = view.findViewById(R.id.ui_button_activate) as AppCompatButton
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
                            if (service.isInternetRequiredForActivation && !NetworkHelper.isConnectedToInternet()) {
                                internetRequiredAlertBuilder.show()
                            } else {
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
            }

            private fun applyState(state: OTExternalService.ServiceState) {
                TransitionManager.beginDelayedTransition(view as ViewGroup)
                when (state) {
                    OTExternalService.ServiceState.ACTIVATED -> {
                        progressBar.visibility = View.GONE
                        activationButton.setText(R.string.msg_deactivate)
                        activationButton.supportBackgroundTintList = deactivateColor
                        activationButton.isEnabled = true
                    }
                    OTExternalService.ServiceState.ACTIVATING -> {
                        progressBar.visibility = View.VISIBLE
                        activationButton.supportBackgroundTintList = onActivatingColor
                        activationButton.setText(R.string.msg_service_activating)
                        activationButton.isEnabled = false
                    }
                    OTExternalService.ServiceState.DEACTIVATED -> {
                        progressBar.visibility = View.GONE
                        activationButton.setText(R.string.msg_activate)
                        activationButton.supportBackgroundTintList = activateColor
                        activationButton.isEnabled = true
                    }
                }
            }

            fun bind(service: OTExternalService) {
                nameView.text = context.resources.getString(service.nameResourceId)
                descriptionView.text = context.resources.getString(service.descResourceId)
                thumbView.setImageResource(service.thumbResourceId)

                measureFactoryAdapter.service = service

                holderState = service.state
            }
        }
    }
}