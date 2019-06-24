package kr.ac.snu.hcil.omnitrack.ui.pages.field.wizard.pages

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.ac.snu.hcil.android.common.view.container.decoration.HorizontalDividerItemDecoration
import kr.ac.snu.hcil.android.common.view.wizard.AWizardPage
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.system.OTMeasureFactoryManager
import kr.ac.snu.hcil.omnitrack.ui.pages.field.wizard.ConnectionWizardView
import org.jetbrains.anko.dip
import javax.inject.Inject

/**
 * Created by Young-Ho Kim on 2016-08-30.
 */
class SourceSelectionPage(override val parent: ConnectionWizardView, val field: OTFieldDAO) : AWizardPage(parent) {

    @Inject
    protected lateinit var measureFactoryManager: OTMeasureFactoryManager

    override val getTitleResourceId: Int = R.string.msg_connection_wizard_title_source_selection

    override val canGoBack: Boolean = false

    override val canGoNext: Boolean = true
    private val sources: List<SourceInformation>

    var selectedInformation: SourceInformation? = null
        private set

    init {
        (parent.context.applicationContext as OTAndroidApp).applicationComponent.inject(this)

        sources = measureFactoryManager.getAttachableMeasureFactories(field).map {
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
        if (information.getSource().getFactory<OTMeasureFactory>().isRangedQueryAvailable) {
            requestGoNextPage(ConnectionWizardView.PAGE_INDEX_CONFIGURATION)
        } else {
            parent.complete()
        }
    }

    inner class PageView : RecyclerView {

        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

        init {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            addItemDecoration(HorizontalDividerItemDecoration(ContextCompat.getColor(context, R.color.separator_Light),
                    dip(1.5f),
                    resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                    resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)))

            adapter = SourceAdapter()
        }
    }

    abstract class SourceInformation(val category: String, val titleResId: Int, val descriptionResId: Int) {
        abstract fun getSource(): OTMeasureFactory.OTMeasure
    }

    class MeasureFactoryInformation(val factory: OTMeasureFactory) : SourceInformation(factory.getCategoryName(), factory.nameResourceId, factory.descResourceId) {
        override fun getSource(): OTMeasureFactory.OTMeasure {
            return factory.makeMeasure()
        }

    }

    inner class SourceAdapter : RecyclerView.Adapter<SourceViewHolder>() {

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

        private val titleView: TextView = view.findViewById(R.id.title)
        private val descriptionView: TextView = view.findViewById(R.id.description)
        private val categoryView: TextView = view.findViewById(R.id.category)

        init {
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
            categoryView.text = information.category
            titleView.setText(information.titleResId)
            descriptionView.setText(information.descriptionResId)
        }
    }

}