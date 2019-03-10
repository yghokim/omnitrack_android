package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.RecyclerView
import kr.ac.snu.hcil.android.common.view.inflateContent
import kr.ac.snu.hcil.android.common.view.wizard.WizardView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.TrackerPickerDialogBuilder
import kr.ac.snu.hcil.omnitrack.ui.pages.services.ServiceWizardView

/**
 * Created by younghokim on 2016. 10. 4..
 */
class MeasureFactoryAdapter(private val context: Context) : RecyclerView.Adapter<MeasureFactoryAdapter.MeasureFactoryViewHolder>() {

    var service: OTExternalService? = null
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun getItemCount(): Int {
        return service?.measureFactories?.size ?: 0
    }

    override fun onBindViewHolder(holder: MeasureFactoryViewHolder, position: Int) {
        if (service != null) {
            holder.bind(service!!.measureFactories[position])
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeasureFactoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.external_service_supported_measure_list_element, parent, false)
        return MeasureFactoryViewHolder(view)
    }

    inner class MeasureFactoryViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener, TrackerPickerDialogBuilder.ViewHolderFactory {

        private lateinit var measureFactory: OTMeasureFactory

        val nameView: TextView = view.findViewById(R.id.name)
        val descriptionView: TextView = view.findViewById(R.id.description)

        val connectButton: AppCompatImageButton = view.findViewById(R.id.ui_connect_button)

        init {
            connectButton.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (view === connectButton) {

                val wizardView = ServiceWizardView(this@MeasureFactoryAdapter.context, measureFactory)

                val wizardDialog = AlertDialog.Builder(this@MeasureFactoryAdapter.context)
                        .setView(wizardView)
                        .create()

                wizardView.setWizardListener(object : WizardView.IWizardListener {
                    override fun onComplete(wizard: WizardView) {
                        println("new connection refreshed.")
                        wizardDialog.dismiss()
                    }

                    override fun onCanceled(wizard: WizardView) {
                        wizardDialog.dismiss()
                    }
                })
                wizardDialog.show()
            }
        }

        fun bind(measureFactory: OTMeasureFactory) {
            this.measureFactory = measureFactory
            nameView.setText(measureFactory.nameResourceId)
            descriptionView.setText(measureFactory.descResourceId)
        }

        override fun createViewHolder(parent: ViewGroup, viewType: Int): TrackerPickerDialogBuilder.TrackerViewHolder {
            val view = parent.inflateContent(R.layout.tracker_selection_element_with_message, false)
            return TrackerPickerElementViewHolder(view)
        }

        private inner class TrackerPickerElementViewHolder(view: View) : TrackerPickerDialogBuilder.TrackerViewHolder(view) {
            private val messageView: TextView = view.findViewById(R.id.message)

            override fun bind(trackerInfo: OTTrackerDAO.SimpleTrackerInfo) {

            }
            /*
            override fun bind(tracker: OTTracker) {
               //TODO migrate this logic
                super.bind(tracker.)
                val numConnectedAttributes = tracker.attributes.filter { it.isMeasureFactoryConnected(measureFactory) }.size
                if (numConnectedAttributes > 0) {
                    messageView.visibility = View.VISIBLE
                    messageView.text = String.format(itemView.context.resources.getString(R.string.msg_sentence_field_already_connected_to_measure),
                            itemView.context.resources.getQuantityString(R.plurals.field, numConnectedAttributes))

                } else {
                    messageView.visibility = View.GONE
                }
            }*/
        }
    }
}