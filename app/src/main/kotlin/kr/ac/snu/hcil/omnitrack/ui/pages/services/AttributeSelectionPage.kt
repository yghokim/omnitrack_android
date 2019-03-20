package kr.ac.snu.hcil.omnitrack.ui.pages.services

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.bindView
import com.afollestad.materialdialogs.MaterialDialog
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import kr.ac.snu.hcil.android.common.view.wizard.AWizardPage
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.AttributePresetInfo
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.SimpleAttributePresetInfo
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTChoiceAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTTimeAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTTimeSpanAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.di.global.Backend
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

    private val subscriptions = CompositeDisposable()

    init {
        (parent.context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    override fun onLeave() {
        subscriptions.clear()
    }

    override fun onEnter() {
        trackerId = parent.trackerDao.objectId!!
        currentMeasureFactory = parent.currentMeasureFactory
        val dao = dbManager.get().getTrackerQueryWithId(trackerId, realmProvider.get()).findFirstAsync()
        subscriptions.add(
            dao.asFlowable<OTTrackerDAO>().filter { it.isValid && it.isLoaded }.subscribe { snapshot ->
                val validAttributes = snapshot.attributes.filter { !it.isHidden && !it.isInTrashcan && !it.isVisibilityLocked() }
                attributes.clear()
                attributes.addAll(validAttributes)
                //attributes.removeAll { !currentMeasureFactory.isAttachableTo(it) || it.isEditingLocked() }
                attributeListView?.adapter?.notifyDataSetChanged()
            }
        )
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
            RecyclerView.ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.simple_icon_description_optional_description, parent, false)), View.OnClickListener {

        var isAvailable: Boolean = true

        val iconView: ImageView by bindView(R.id.icon)
        val nameView: TextView by bindView(R.id.name)
        val descriptionView: TextView by bindView(R.id.description)

        init {
            itemView.setOnClickListener(this)
        }

        lateinit var attributeDao: OTAttributeDAO

        override fun onClick(view: View?) {
            if(isAvailable) {
                this@AttributeSelectionPage.attributeDAO = attributeDao
                requestGoNextPage(ServiceWizardView.PAGE_QUERY_RANGE_SELECTION)
            }
        }

        fun bind(attributeDao: OTAttributeDAO) {
            val helper = attributeManager.get(attributeDao.type)
            iconView.setImageResource(helper.getTypeSmallIconResourceId(attributeDao))
            nameView.text = attributeDao.name
            this.attributeDao = attributeDao

            if(!parent.currentMeasureFactory.isAttachableTo(attributeDao)){
                descriptionView.visibility = View.VISIBLE
                descriptionView.setText(R.string.msg_service_wizard_no_attachable_field)
                isAvailable = false
            } else if(attributeDao.isEditingLocked()){
                descriptionView.visibility = View.VISIBLE
                descriptionView.setText(R.string.msg_service_wizard_non_modifiable_field)
                isAvailable = false
            }
            else {
                val connection = attributeDao.getParsedConnection(parent.context)
                if (connection != null) {
                    descriptionView.visibility = View.VISIBLE
                    descriptionView.text = connection.source?.getFactory<OTMeasureFactory>()?.getCategoryName()?.let {
                        String.format(parent.resources.getString(R.string.msg_service_wizard_format_already_attached_measure), it)
                    }
                            ?: parent.resources.getString(R.string.msg_service_wizard_already_attached_measure)

                    isAvailable = false
                } else {
                    descriptionView.visibility = View.GONE
                    isAvailable = true
                }
            }

            if(isAvailable){
                itemView.isEnabled = true
                itemView.alpha = 1f
            }else{
                itemView.isEnabled = false
                itemView.alpha = 0.3f
            }
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
                            attributeManager.get(OTAttributeManager.TYPE_TIME)
                                    .setPropertyValue(OTTimeAttributeHelper.GRANULARITY, OTTimeAttributeHelper.GRANULARITY_MINUTE, dao, realm)
                            dao
                        }),

                AttributePresetInfo(OTAttributeManager.TYPE_TIME, R.drawable.field_icon_time_date, context.getString(R.string.type_timepoint_date_name), context.getString(R.string.type_timepoint_date_desc),
                        { dao, realm ->
                            attributeManager.get(OTAttributeManager.TYPE_TIME)
                                    .setPropertyValue(OTTimeAttributeHelper.GRANULARITY, OTTimeAttributeHelper.GRANULARITY_DAY, dao, realm)
                            dao
                        }),

                AttributePresetInfo(OTAttributeManager.TYPE_TIMESPAN, R.drawable.field_icon_timer, context.getString(R.string.type_timespan_name), context.getString(R.string.type_timespan_desc),
                        { dao, realm ->
                            attributeManager.get(OTAttributeManager.TYPE_TIMESPAN)
                                    .setPropertyValue(OTTimeSpanAttributeHelper.PROPERTY_GRANULARITY, OTTimeSpanAttributeHelper.GRANULARITY_MINUTE, dao, realm)
                            dao
                        }),
                AttributePresetInfo(OTAttributeManager.TYPE_TIMESPAN, R.drawable.field_icon_time_range_date, context.getString(R.string.type_timespan_date_name), context.getString(R.string.type_timespan_date_desc),
                        { dao, realm ->
                            attributeManager.get(OTAttributeManager.TYPE_TIMESPAN)
                                    .setPropertyValue(OTTimeSpanAttributeHelper.PROPERTY_GRANULARITY, OTTimeSpanAttributeHelper.GRANULARITY_DAY, dao, realm)
                            dao
                        }),
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_LOCATION, R.drawable.field_icon_location, context.getString(R.string.type_location_name), context.getString(R.string.type_location_desc)),
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_IMAGE, R.drawable.field_icon_image, context.getString(R.string.type_image_name), context.getString(R.string.type_image_desc)),
                SimpleAttributePresetInfo(OTAttributeManager.TYPE_AUDIO, R.drawable.field_icon_audio, context.getString(R.string.type_audio_record_name), context.getString(R.string.type_audio_record_desc)),

                AttributePresetInfo(OTAttributeManager.TYPE_CHOICE, R.drawable.field_icon_singlechoice, context.getString(R.string.type_single_choice_name), context.getString(R.string.type_single_choice_desc),
                        { dao, realm ->
                            attributeManager.get(OTAttributeManager.TYPE_CHOICE)
                                    .setPropertyValue(OTChoiceAttributeHelper.PROPERTY_MULTISELECTION, false, dao, realm)
                            dao
                        }),

                AttributePresetInfo(OTAttributeManager.TYPE_CHOICE, R.drawable.field_icon_multiplechoice, context.getString(R.string.type_multiple_choices_name), context.getString(R.string.type_multiple_choices_desc),
                        { dao, realm ->
                            attributeManager.get(OTAttributeManager.TYPE_CHOICE)
                                    .setPropertyValue(OTChoiceAttributeHelper.PROPERTY_MULTISELECTION, true, dao, realm)
                            dao
                        })

        )
    }

    fun addNewAttribute(name: String, type: Int, realm: Realm, processor: ((OTAttributeDAO, Realm) -> OTAttributeDAO)? = null) {
        val trackerDao = parent.trackerDao
        val newDao = OTAttributeDAO()
        newDao.objectId = UUID.randomUUID().toString()
        newDao.name = name
        newDao.type = type
        newDao.trackerId = trackerId
        newDao.initialize(parent.context)
        processor?.invoke(newDao, realm)
        newDao.localId = attributeManager.makeNewAttributeLocalId(newDao.userCreatedAt)
        newDao.trackerId = trackerDao.objectId
        realm.executeTransactionIfNotIn {
            trackerDao.attributes.add(newDao)
        }
    }
}