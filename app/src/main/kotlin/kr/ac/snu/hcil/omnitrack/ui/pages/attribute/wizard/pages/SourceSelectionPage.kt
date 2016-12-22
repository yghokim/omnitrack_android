package kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.pages

import android.content.Context
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.AWizardPage
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.ConnectionWizardView

/**
 * Created by Young-Ho Kim on 2016-08-30.
 */
class SourceSelectionPage(override val parent: ConnectionWizardView, val attribute: OTAttribute<out Any>) : AWizardPage(parent) {

    override val getTitleResourceId: Int = R.string.msg_connection_wizard_title_source_selection

    override val canGoBack: Boolean = false

    override val canGoNext: Boolean = true
    private val sources: List<SourceInformation>

    var selectedInformation: SourceInformation? = null
        private set

    init {

        sources = OTExternalService.getFilteredMeasureFactories {
            it.isAttachableTo(attribute)
        }.map {
            MeasureFactoryInformation(it)
        }
    }

    override fun onLeave() {

    }

    override fun onEnter() {

    }

    override fun makeViewInstance(context: Context): View {
        return PageView(context)
    }

    private fun onSourceSelected(information: SourceInformation) {
        selectedInformation = information
        requestGoNextPage(ConnectionWizardView.PAGE_INDEX_CONFIGURATION)
    }

    inner class PageView : RecyclerView {

        constructor(context: Context?) : super(context)
        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

        init {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(HorizontalDividerItemDecoration(ContextCompat.getColor(context, R.color.separator_Light),
                    (1.5f * resources.displayMetrics.density).toInt(),
                    resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                    resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)))

            adapter = SourceAdapter()
        }
    }

    abstract class SourceInformation(val categoryResId: Int, val titleResId: Int, val descriptionResId: Int) {
        abstract fun getSource(): OTMeasureFactory.OTMeasure
    }

    class MeasureFactoryInformation(val factory: OTMeasureFactory) : SourceInformation(factory.service.nameResourceId, factory.nameResourceId, factory.descResourceId) {
        override fun getSource(): OTMeasureFactory.OTMeasure {
            return factory.makeMeasure()
        }

    }

    inner class SourceAdapter() : RecyclerView.Adapter<SourceViewHolder>() {

        init {

            //TODO include table query measure informations

        }

        override fun onBindViewHolder(holder: SourceViewHolder, position: Int) {
            holder.bind(sources[position])
        }

        override fun getItemCount(): Int {
            return sources.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SourceViewHolder {

            val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.connection_source_list_element, parent, false)
            return SourceViewHolder(view)
        }

    }

    inner class SourceViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        private val titleView: TextView
        private val descriptionView: TextView
        private val categoryView: TextView

        init {
            categoryView = view.findViewById(R.id.category) as TextView
            titleView = view.findViewById(R.id.title) as TextView
            descriptionView = view.findViewById(R.id.description) as TextView

            view.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (view === itemView) {
                val handler = Handler()
                handler.postDelayed(object : Runnable {
                    override fun run() {
                        onSourceSelected(this@SourceSelectionPage.sources[adapterPosition])
                    }
                }, 500)
            }
        }

        fun bind(information: SourceInformation) {
            categoryView.setText(information.categoryResId)
            titleView.setText(information.titleResId)
            descriptionView.setText(information.descriptionResId)
        }
    }

}