package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.AttributePresetInfo
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.SimpleAttributePresetInfo
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTChoiceAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTTimeAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTTimeSpanAttributeHelper
import kr.ac.snu.hcil.omnitrack.ui.components.common.DismissingBottomSheetDialogFragment
import javax.inject.Inject

/**
 * Created by Young-Ho Kim on 2016-12-28.
 */
class FieldPresetSelectionBottomSheetFragment : DismissingBottomSheetDialogFragment(R.layout.fragment_field_preset_selection) {

    interface Callback {
        fun onAttributePermittedToAdd(typeInfo: AttributePresetInfo)
    }

    @Inject
    protected lateinit var attributeManager: OTAttributeManager

    private lateinit var newAttributeGrid: RecyclerView

    private lateinit var gridAdapter: GridAdapter

    var callback: Callback? = null


    private val subscriptions = CompositeDisposable()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (requireActivity().application as OTAndroidApp).applicationComponent.inject(this)
    }

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

    override fun setupDialogAndContentView(dialog: Dialog, contentView: View) {
        newAttributeGrid = contentView.findViewById(R.id.ui_new_attribute_grid)
        newAttributeGrid.layoutManager = GridLayoutManager(context, resources.getInteger(R.integer.new_attribute_panel_horizontal_count))
        newAttributeGrid.adapter = gridAdapter
    }


    private fun makeAttributePresets(): Array<AttributePresetInfo> {
        return arrayOf(
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_SHORT_TEXT, R.drawable.field_icon_shorttext, this.getString(R.string.type_shorttext_name), this.getString(R.string.type_shorttext_desc)),
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_LONG_TEXT, R.drawable.field_icon_longtext, this.getString(R.string.type_longtext_name), this.getString(R.string.type_longtext_desc)),
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_NUMBER, R.drawable.field_icon_number, this.getString(R.string.type_number_name), this.getString(R.string.type_number_desc)),
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_RATING, R.drawable.field_icon_rating, this.getString(R.string.type_rating_name), this.getString(R.string.type_rating_desc)),
                //                SimpleAttributePresetInfo(OTAttributeManager.TYPE_TIME, R.drawable.field_icon_time, this.getString(R.string.type_timepoint_name), this.getString(R.string.type_timepoint_desc)),

                AttributePresetInfo(OTAttributeManager.TYPE_TIME, R.drawable.field_icon_time_hour, this.getString(R.string.type_timepoint_time_name), this.getString(R.string.type_timepoint_time_desc),
                        { dao, realm ->
                            attributeManager.getAttributeHelper(OTAttributeManager.TYPE_TIME)
                                    .setPropertyValue(OTTimeAttributeHelper.GRANULARITY, OTTimeAttributeHelper.GRANULARITY_MINUTE, dao, realm)
                            dao
                        }),

                AttributePresetInfo(OTAttributeManager.TYPE_TIME, R.drawable.field_icon_time_date, this.getString(R.string.type_timepoint_date_name), this.getString(R.string.type_timepoint_date_desc),
                        { dao, realm ->
                            attributeManager.getAttributeHelper(OTAttributeManager.TYPE_TIME)
                                    .setPropertyValue(OTTimeAttributeHelper.GRANULARITY, OTTimeAttributeHelper.GRANULARITY_DAY, dao, realm)
                            dao
                        }),

                AttributePresetInfo(OTAttributeManager.TYPE_TIMESPAN, R.drawable.field_icon_timer, this.getString(R.string.type_timespan_name), this.getString(R.string.type_timespan_desc),
                        { dao, realm ->
                            attributeManager.getAttributeHelper(OTAttributeManager.TYPE_TIMESPAN)
                                    .setPropertyValue(OTTimeSpanAttributeHelper.PROPERTY_GRANULARITY, OTTimeSpanAttributeHelper.GRANULARITY_MINUTE, dao, realm)
                            dao
                        }),
                AttributePresetInfo(OTAttributeManager.TYPE_TIMESPAN, R.drawable.field_icon_time_range_date, this.getString(R.string.type_timespan_date_name), this.getString(R.string.type_timespan_date_desc),
                        { dao, realm ->
                            attributeManager.getAttributeHelper(OTAttributeManager.TYPE_TIMESPAN)
                                    .setPropertyValue(OTTimeSpanAttributeHelper.PROPERTY_GRANULARITY, OTTimeSpanAttributeHelper.GRANULARITY_DAY, dao, realm)
                            dao
                        }),
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_LOCATION, R.drawable.field_icon_location, this.getString(R.string.type_location_name), this.getString(R.string.type_location_desc)),
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_IMAGE, R.drawable.field_icon_image, this.getString(R.string.type_image_name), this.getString(R.string.type_image_desc)),
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_AUDIO, R.drawable.field_icon_audio, this.getString(R.string.type_audio_record_name), this.getString(R.string.type_audio_record_desc)),

                AttributePresetInfo(OTAttributeManager.TYPE_CHOICE, R.drawable.field_icon_singlechoice, this.getString(R.string.type_single_choice_name), this.getString(R.string.type_single_choice_desc),
                        { dao, realm ->
                            attributeManager.getAttributeHelper(OTAttributeManager.TYPE_CHOICE)
                                    .setPropertyValue(OTChoiceAttributeHelper.PROPERTY_MULTISELECTION, false, dao, realm)
                            dao
                        }),

                AttributePresetInfo(OTAttributeManager.TYPE_CHOICE, R.drawable.field_icon_multiplechoice, this.getString(R.string.type_multiple_choices_name), this.getString(R.string.type_multiple_choices_desc),
                        { dao, realm ->
                            attributeManager.getAttributeHelper(OTAttributeManager.TYPE_CHOICE)
                                    .setPropertyValue(OTChoiceAttributeHelper.PROPERTY_MULTISELECTION, true, dao, realm)
                            dao
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

                        attributeManager.showPermissionCheckDialog(this@FieldPresetSelectionBottomSheetFragment,
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