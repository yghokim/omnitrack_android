package kr.ac.snu.hcil.omnitrack.ui.pages.services

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import dagger.Lazy
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.AttributePresetInfo
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.SimpleAttributePresetInfo
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTChoiceAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTTimeAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTTimeSpanAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.AWizardPage
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import org.jetbrains.anko.padding
import java.util.*
import javax.inject.Inject

/**
 * Created by junhoe on 2017. 11. 1..
 */
class AttributeSelectionPage(override val parent : ServiceWizardView) : AWizardPage(parent) {

    override val canGoBack: Boolean = true
    override val canGoNext: Boolean = false
    override val getTitleResourceId: Int = R.string.msg_service_wizard_title_field_selection

    @Inject
    lateinit var attributeManager: OTAttributeManager

    @Inject
    lateinit var dbManager: Lazy<BackendDbManager>

    @field:[Inject Backend]
    lateinit var realmProvider: Factory<Realm>

    private var attributes: MutableList<OTAttributeDAO> = ArrayList()
    private var attributeListView: RecyclerView? = null

    lateinit var trackerId: String
    private lateinit var currentMeasureFactory: OTMeasureFactory

    var attributeDAO: OTAttributeDAO? = null

    init {
        val component = (parent.context.applicationContext as OTAndroidApp).currentConfiguredContext.configuredAppComponent
        component.inject(this)

    }

    override fun onLeave() {
    }

    override fun onEnter() {
        trackerId = parent.trackerDao.objectId!!
        currentMeasureFactory = parent.currentMeasureFactory
        val dao = dbManager.get().getTrackerQueryWithId(trackerId, realmProvider.get()).findFirstAsync()
        dao.asFlowable<OTTrackerDAO>().filter { it.isValid && it.isLoaded }.subscribe { snapshot ->
            val validAttributes = snapshot.attributes.filter { !it.isHidden && !it.isInTrashcan }
            attributes.clear()
            attributes.addAll(validAttributes)
            attributes.removeAll { !currentMeasureFactory.isAttachableTo(it) }
            attributeListView?.adapter?.notifyDataSetChanged()
        }
    }

    private fun refreshAttributeList() {
        attributes.clear()
        onEnter()
    }

    override fun makeViewInstance(context: Context): View {
        attributeListView = AttributeListWizardPanel(context)
        return attributeListView!!
    }

    inner class AttributeListWizardPanel : RecyclerView {

        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

        init {
            padding = context.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = AttributeListAdapter()
        }
    }

    inner class AttributeListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                0 -> AttributeAttachViewHolder(parent)
                1 -> AttributeListViewHolder(parent)
                else -> throw IllegalArgumentException()
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (position >= 1) {
                (holder as AttributeListViewHolder).bind(attributes[position - 1])
            }
        }

        override fun getItemCount(): Int = attributes.size + 1

        override fun getItemViewType(position: Int): Int = if (position == 0) 0 else 1

    }

    private inner class AttributeAttachViewHolder(parent: ViewGroup?) :
            RecyclerView.ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.layout_attached_attribute_list_add, parent, false)), View.OnClickListener {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            val currentTypeId = parent.currentMeasureFactory.getAttributeType()
            val preset = getAttributePresets().filter { preset -> currentTypeId == preset.typeId }[0]
            newAttributeNameDialog.input(null, preset.name, false) {
                _, input ->
                val realm = realmProvider.get()
                addNewAttribute(input.toString(), currentTypeId, realm, preset.processor)
                refreshAttributeList()
            }.show()
        }
    }

    private inner class AttributeListViewHolder(parent: ViewGroup?) :
            RecyclerView.ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.simple_icon_and_text, parent, false)), View.OnClickListener {

        init {
            itemView.setOnClickListener(this)
        }

        val icon: AppCompatImageView = itemView.findViewById(R.id.ui_attribute_type)
        val textView: TextView = itemView.findViewById(R.id.text)
        lateinit var attributeDao: OTAttributeDAO

        override fun onClick(view: View?) {
            this@AttributeSelectionPage.attributeDAO = attributeDao
            requestGoNextPage(ServiceWizardView.PAGE_QUERY_RANGE_SELECTION)
        }

        fun bind(attributeDao: OTAttributeDAO) {
            val helper = attributeManager.getAttributeHelper(attributeDao.type)
            icon.setImageResource(helper.getTypeSmallIconResourceId(attributeDao))
            this.attributeDao = attributeDao
            textView.text = attributeDao.name
        }
    }

    private val newAttributeNameDialog: MaterialDialog.Builder by lazy {
        MaterialDialog.Builder(parent.context!!)
                .title(R.string.msg_new_field_name)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .setSyncWithKeyboard(true)
                .inputRangeRes(1, 20, R.color.colorRed)
                .cancelable(true)
                .negativeText(R.string.msg_cancel)
    }

    private fun getAttributePresets(): Array<AttributePresetInfo> {
        val context = parent.context
        return arrayOf(
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_SHORT_TEXT, R.drawable.field_icon_shorttext, context.getString(R.string.type_shorttext_name), context.getString(R.string.type_shorttext_desc)),
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_LONG_TEXT, R.drawable.field_icon_longtext, context.getString(R.string.type_longtext_name), context.getString(R.string.type_longtext_desc)),
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_NUMBER, R.drawable.field_icon_number, context.getString(R.string.type_number_name), context.getString(R.string.type_number_desc)),
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_RATING, R.drawable.field_icon_rating, context.getString(R.string.type_rating_name), context.getString(R.string.type_rating_desc)),

                AttributePresetInfo(OTAttributeManager.TYPE_TIME, R.drawable.field_icon_time_hour, context.getString(R.string.type_timepoint_time_name), context.getString(R.string.type_timepoint_time_desc),
                        { dao, realm ->
                            attributeManager.getAttributeHelper(OTAttributeManager.TYPE_TIME)
                                    .setPropertyValue(OTTimeAttributeHelper.GRANULARITY, OTTimeAttributeHelper.GRANULARITY_MINUTE, dao, realm)
                            dao
                        }),

                AttributePresetInfo(OTAttributeManager.TYPE_TIME, R.drawable.field_icon_time_date, context.getString(R.string.type_timepoint_date_name), context.getString(R.string.type_timepoint_date_desc),
                        { dao, realm ->
                            attributeManager.getAttributeHelper(OTAttributeManager.TYPE_TIME)
                                    .setPropertyValue(OTTimeAttributeHelper.GRANULARITY, OTTimeAttributeHelper.GRANULARITY_DAY, dao, realm)
                            dao
                        }),

                AttributePresetInfo(OTAttributeManager.TYPE_TIMESPAN, R.drawable.field_icon_timer, context.getString(R.string.type_timespan_name), context.getString(R.string.type_timespan_desc),
                        { dao, realm ->
                            attributeManager.getAttributeHelper(OTAttributeManager.TYPE_TIMESPAN)
                                    .setPropertyValue(OTTimeSpanAttributeHelper.PROPERTY_GRANULARITY, OTTimeSpanAttributeHelper.GRANULARITY_MINUTE, dao, realm)
                            dao
                        }),
                AttributePresetInfo(OTAttributeManager.TYPE_TIMESPAN, R.drawable.field_icon_time_range_date, context.getString(R.string.type_timespan_date_name), context.getString(R.string.type_timespan_date_desc),
                        { dao, realm ->
                            attributeManager.getAttributeHelper(OTAttributeManager.TYPE_TIMESPAN)
                                    .setPropertyValue(OTTimeSpanAttributeHelper.PROPERTY_GRANULARITY, OTTimeSpanAttributeHelper.GRANULARITY_DAY, dao, realm)
                            dao
                        }),
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_LOCATION, R.drawable.field_icon_location, context.getString(R.string.type_location_name), context.getString(R.string.type_location_desc)),
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_IMAGE, R.drawable.field_icon_image, context.getString(R.string.type_image_name), context.getString(R.string.type_image_desc)),
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_AUDIO, R.drawable.field_icon_audio, context.getString(R.string.type_audio_record_name), context.getString(R.string.type_audio_record_desc)),

                AttributePresetInfo(OTAttributeManager.TYPE_CHOICE, R.drawable.field_icon_singlechoice, context.getString(R.string.type_single_choice_name), context.getString(R.string.type_single_choice_desc),
                        { dao, realm ->
                            attributeManager.getAttributeHelper(OTAttributeManager.TYPE_CHOICE)
                                    .setPropertyValue(OTChoiceAttributeHelper.PROPERTY_MULTISELECTION, false, dao, realm)
                            dao
                        }),

                AttributePresetInfo(OTAttributeManager.TYPE_CHOICE, R.drawable.field_icon_multiplechoice, context.getString(R.string.type_multiple_choices_name), context.getString(R.string.type_multiple_choices_desc),
                        { dao, realm ->
                            attributeManager.getAttributeHelper(OTAttributeManager.TYPE_CHOICE)
                                    .setPropertyValue(OTChoiceAttributeHelper.PROPERTY_MULTISELECTION, true, dao, realm)
                            dao
                        })

        )
    }

    fun addNewAttribute(name: String, type: Int, realm: Realm, processor: ((OTAttributeDAO, Realm) -> OTAttributeDAO)? = null) {
        val configuredContext = (parent.context.applicationContext as OTApp).currentConfiguredContext
        val trackerDao = parent.trackerDao
        val newDao = OTAttributeDAO()
        newDao.objectId = UUID.randomUUID().toString()
        newDao.name = name
        newDao.type = type
        newDao.trackerId = trackerId
        newDao.initialize(configuredContext)
        processor?.invoke(newDao, realm)
        newDao.localId = attributeManager.makeNewAttributeLocalId(newDao.userCreatedAt)
        newDao.trackerId = trackerDao.objectId
        realm.executeTransactionIfNotIn {
            trackerDao.attributes.add(newDao)
        }
    }
}