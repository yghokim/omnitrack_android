package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

/**
 * Created by Young-Ho Kim on 2016-12-28.
 */
class FieldPresetSelectionBottomSheetFragment : BottomSheetDialogFragment() {

    interface Callback {
        fun onAttributePermittedToAdd(typeInfo: AttributePresetInfo)
    }

    private val behaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {

        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }

    }


    private lateinit var newAttributeGrid: RecyclerView

    private lateinit var gridAdapter: GridAdapter

    var callback: Callback? = null


    private val subscriptions = CompositeSubscription()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gridAdapter = GridAdapter()

        subscriptions.add(
                Observable.defer { Observable.just(makeAttributePresets()) }
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            presets ->
                            gridAdapter.presets = presets
                            gridAdapter.notifyDataSetChanged()
                        }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        val contentView = View.inflate(context, R.layout.fragment_field_preset_selection, null)
        dialog.setContentView(contentView)
        val lp = ((contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams)
        val behavior = lp.behavior
        if (behavior is BottomSheetBehavior) {
            behavior.setBottomSheetCallback(behaviorCallback)
        }

        newAttributeGrid = contentView.findViewById(R.id.ui_new_attribute_grid)
        newAttributeGrid.layoutManager = GridLayoutManager(context, resources.getInteger(R.integer.new_attribute_panel_horizontal_count))
        newAttributeGrid.adapter = gridAdapter
    }


    private fun makeAttributePresets(): Array<AttributePresetInfo> {
        return arrayOf(
                SimpleAttributePresetInfo(OTAttribute.TYPE_SHORT_TEXT, R.drawable.field_icon_shorttext, this.getString(R.string.type_shorttext_name), this.getString(R.string.type_shorttext_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_LONG_TEXT, R.drawable.field_icon_longtext, this.getString(R.string.type_longtext_name), this.getString(R.string.type_longtext_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_NUMBER, R.drawable.field_icon_number, this.getString(R.string.type_number_name), this.getString(R.string.type_number_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_RATING, R.drawable.field_icon_rating, this.getString(R.string.type_rating_name), this.getString(R.string.type_rating_desc)),
                //                SimpleAttributePresetInfo(OTAttribute.TYPE_TIME, R.drawable.field_icon_time, this.getString(R.string.type_timepoint_name), this.getString(R.string.type_timepoint_desc)),

                AttributePresetInfo(OTAttribute.TYPE_TIME, R.drawable.field_icon_time_hour, this.getString(R.string.type_timepoint_time_name), this.getString(R.string.type_timepoint_time_desc),
                        { tracker, columnName ->
                            val attr = OTAttribute.createAttribute(tracker, columnName, OTAttribute.TYPE_TIME) as OTTimeAttribute
                            attr.granularity = OTTimeAttribute.GRANULARITY_MINUTE
                            attr
                        }),

                AttributePresetInfo(OTAttribute.TYPE_TIME, R.drawable.field_icon_time_date, this.getString(R.string.type_timepoint_date_name), this.getString(R.string.type_timepoint_date_desc),
                        { tracker, columnName ->
                            val attr = OTAttribute.createAttribute(tracker, columnName, OTAttribute.TYPE_TIME) as OTTimeAttribute
                            attr.granularity = OTTimeAttribute.GRANULARITY_DAY
                            attr
                        }),

                AttributePresetInfo(OTAttribute.TYPE_TIMESPAN, R.drawable.field_icon_timer, this.getString(R.string.type_timespan_name), this.getString(R.string.type_timespan_desc),
                        { tracker, columnName ->
                            (OTAttribute.createAttribute(tracker, columnName, OTAttribute.TYPE_TIMESPAN) as OTTimeSpanAttribute).apply {
                                setPropertyValue(OTTimeSpanAttribute.PROPERTY_GRANULARITY, OTTimeSpanAttribute.GRANULARITY_MINUTE)
                            }
                        }),
                SimpleAttributePresetInfo(OTAttribute.TYPE_TIMESPAN, R.drawable.field_icon_time_range_date, this.getString(R.string.type_timespan_date_name), this.getString(R.string.type_timespan_date_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_LOCATION, R.drawable.field_icon_location, this.getString(R.string.type_location_name), this.getString(R.string.type_location_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_IMAGE, R.drawable.field_icon_image, this.getString(R.string.type_image_name), this.getString(R.string.type_image_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_AUDIO, R.drawable.field_icon_audio, this.getString(R.string.type_audio_record_name), this.getString(R.string.type_audio_record_desc)),

                AttributePresetInfo(OTAttribute.TYPE_CHOICE, R.drawable.field_icon_singlechoice, this.getString(R.string.type_single_choice_name), this.getString(R.string.type_single_choice_desc),
                        { tracker, columnName ->
                            val attr = OTAttribute.createAttribute(tracker, columnName, OTAttribute.TYPE_CHOICE) as OTChoiceAttribute
                            attr.allowedMultiSelection = false
                            attr
                        }),

                AttributePresetInfo(OTAttribute.TYPE_CHOICE, R.drawable.field_icon_multiplechoice, this.getString(R.string.type_multiple_choices_name), this.getString(R.string.type_multiple_choices_desc),
                        { tracker, columnName ->
                            val attr = OTAttribute.createAttribute(tracker, columnName, OTAttribute.TYPE_CHOICE) as OTChoiceAttribute
                            attr.allowedMultiSelection = true
                            attr
                        })

        )
    }


    inner class GridAdapter : RecyclerView.Adapter<GridAdapter.ViewHolder>() {

        var presets: Array<AttributePresetInfo>? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.attribute_type_grid_element, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (presets != null) {
                holder.bind(presets!![position])
            }
        }

        override fun getItemCount(): Int {
            return presets?.size ?: 0
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.name)
            val typeIcon: ImageView = view.findViewById(R.id.type_icon)

            init {

                view.setOnClickListener {
                    val typeInfo = presets?.get(adapterPosition)
                    if (typeInfo != null) {

                        OTAttribute.showPermissionCheckDialog(this@FieldPresetSelectionBottomSheetFragment.activity,
                                typeInfo.typeId, typeInfo.name,
                                {
                                    dismiss()
                                    callback?.onAttributePermittedToAdd(typeInfo)
                                },
                                {

                                })
                    }
                }
            }

            fun bind(entry: AttributePresetInfo) {
                name.text = entry.name
                typeIcon.setImageResource(entry.iconId)
            }
        }
    }
}