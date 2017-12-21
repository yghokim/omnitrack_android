package kr.ac.snu.hcil.omnitrack.ui.pages.services

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dagger.Lazy
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.AWizardPage
import org.jetbrains.anko.padding
import javax.inject.Inject

/**
 * Created by junhoe on 2017. 11. 1..
 */
class FieldSelectionPage (override val parent : ServiceWizardView) : AWizardPage(parent) {

    override val canGoBack: Boolean = true
    override val canGoNext: Boolean = false
    override val getTitleResourceId: Int = R.string.msg_service_wizard_title_field_selection

    private var fields: MutableList<OTAttributeDAO> = ArrayList()
    private var wizardPanel: RecyclerView? = null

    lateinit var trackerId: String
    lateinit var currentMeasureFactory: OTMeasureFactory

    @Inject
    protected lateinit var realm: Realm

    @Inject
    protected lateinit var dbManager: Lazy<RealmDatabaseManager>

    init {
        val component = (parent.context.applicationContext as OTApp).applicationComponent
        component.inject(this)
    }

    override fun onLeave() {
    }

    override fun onEnter() {
        trackerId = parent.trackerId!!
        currentMeasureFactory = parent.currentMeasureFactory

        fields = ArrayList(0)
        val dao = dbManager.get().getTrackerQueryWithId(trackerId, realm).findFirstAsync()

        dao.asFlowable<OTTrackerDAO>().filter { it.isValid && it.isLoaded }.subscribe { snapshot ->
            fields.addAll(snapshot.attributes)
            fields.removeAll { !currentMeasureFactory.isAttachableTo(it) }
        }
        wizardPanel!!.adapter.notifyDataSetChanged()
    }

    override fun makeViewInstance(context: Context): View {
        wizardPanel = FieldListWizardPanel(context)
        return wizardPanel!!
    }

    inner class FieldListWizardPanel: RecyclerView {

        constructor(context: Context?) : super(context)
        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

        init {
            padding = context.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = FieldListAdapter()
        }
    }

    inner class FieldListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                0 -> FieldAttachViewHolder(parent)
                1 -> FieldListViewHolder(parent)
                else -> throw IllegalArgumentException()
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
            if (position >= 1) {
                (holder as FieldListViewHolder).bind(fields[position - 1])
            }
        }

        override fun getItemCount(): Int = fields.size + 1

        override fun getItemViewType(position: Int): Int = if (position == 0) 0 else 1

    }

    private inner class FieldAttachViewHolder(parent: ViewGroup?) :
            RecyclerView.ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.layout_attached_tracker_list_add, parent, false)), View.OnClickListener {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {

        }
    }

    private inner class FieldListViewHolder(parent: ViewGroup?) :
            RecyclerView.ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.simple_icon_and_text, parent, false)), View.OnClickListener {

        init {
            itemView.setOnClickListener(this)
        }
        val icon: AppCompatImageView = itemView.findViewById(R.id.ui_attribute_type)
        val textView: TextView = itemView.findViewById(R.id.text)

        override fun onClick(view: View?) {

        }

        fun bind(attributeDAO: OTAttributeDAO) {
            val helper = OTAttributeManager.getAttributeHelper(attributeDAO.type)
            icon.setImageResource(helper.getTypeSmallIconResourceId(attributeDAO))
            textView.text = attributeDAO.name
        }
    }

}