package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.TrackerPickerDialogBuilder
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.TrackerDetailActivity
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import kr.ac.snu.hcil.omnitrack.utils.inflateContent

/**
 * Created by younghokim on 2016. 10. 4..
 */
class MeasureFactoryAdapter : RecyclerView.Adapter<MeasureFactoryAdapter.MeasureFactoryViewHolder>() {

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

        val nameView: TextView
        val descriptionView: TextView

        val connectButton: AppCompatImageButton

        init {
            nameView = view.findViewById(R.id.name) as TextView
            descriptionView = view.findViewById(R.id.description) as TextView
            connectButton = view.findViewById(R.id.ui_connect_button) as AppCompatImageButton

            connectButton.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (view === connectButton) {
                val dialog = TrackerPickerDialogBuilder(this).createDialog(itemView.getActivity()!!, null) {
                    tracker ->
                    if (tracker != null) {
                        DialogHelper.makeYesNoDialogBuilder(itemView.context, "OmniTrack", "Add a field to this tracker?", {
                            val exampleAttribute = this.measureFactory.makeNewExampleAttribute()
                            tracker.attributes.add(exampleAttribute)
                            //open tracker window
                            val intent = TrackerDetailActivity.makeIntent(tracker.objectId, exampleAttribute, itemView.context)
                            itemView.context.startActivity(intent)
                        }, null).show()
                    }
                }
                dialog.show()
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
            private val messageView: TextView

            init {
                messageView = view.findViewById(R.id.message) as TextView
            }

            override fun bind(tracker: OTTracker) {
                super.bind(tracker)
                val numConnectedAttributes = tracker.attributes.filter { it.isMeasureFactoryConnected(measureFactory) }.size
                if (numConnectedAttributes > 0) {
                    messageView.visibility = View.VISIBLE
                    messageView.text = String.format(itemView.context.resources.getString(R.string.msg_sentence_field_already_connected_to_measure),
                            itemView.context.resources.getQuantityString(R.plurals.field, numConnectedAttributes))

                } else {
                    messageView.visibility = View.GONE
                }
            }
        }
    }

}