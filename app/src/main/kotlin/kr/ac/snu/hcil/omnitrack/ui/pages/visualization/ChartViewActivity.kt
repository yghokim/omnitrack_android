package kr.ac.snu.hcil.omnitrack.ui.pages.visualization

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.ac.snu.hcil.android.common.view.inflateContent
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.Granularity
import kr.ac.snu.hcil.omnitrack.core.visualization.INativeChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.IWebBasedChartModel
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.choice.SelectionView
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.IChartView
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.NativeChartView
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.WebBasedChartView
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper

class ChartViewActivity : MultiButtonActionBarActivity(R.layout.activity_chart_view), View.OnClickListener {

    companion object {
        fun makeIntent(trackerId: String, context: Context): Intent {
            val intent = Intent(context, ChartViewActivity::class.java)
            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            return intent
        }

        const val VIEW_TYPE_NATIVE = 0
        const val VIEW_TYPE_WEB = 1
    }

    private lateinit var currentScopeView: TextView
    private lateinit var currentYearView: TextView

    private lateinit var leftNavigationButton: View
    private lateinit var rightNavigationButton: View

    private lateinit var scopeSelectionView: SelectionView

    private lateinit var viewModel: TrackerChartViewListViewModel

    private lateinit var listView: RecyclerView

    private var adapter = ChartViewListAdapter()

    private val currentChartViewModelList = ArrayList<ChartModel<*>>()

    private val supportedGranularity = arrayOf(Granularity.WEEK_REL, Granularity.WEEK_2_REL, Granularity.WEEK_4_REL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(Mode.Back)

        leftNavigationButton = findViewById(R.id.ui_navigate_left)
        rightNavigationButton = findViewById(R.id.ui_navigate_right)
        leftNavigationButton.setOnClickListener(this)
        rightNavigationButton.setOnClickListener(this)

        currentScopeView = findViewById(R.id.ui_current_time)
        currentYearView = findViewById(R.id.ui_current_year)

        scopeSelectionView = findViewById(R.id.ui_scope_selection)
        scopeSelectionView.setValues(supportedGranularity.map { resources.getString(it.nameId) }.toTypedArray())
        scopeSelectionView.onSelectedIndexChanged += { _, index ->
            viewModel.granularity = supportedGranularity[index]
        }

        listView = findViewById(R.id.ui_recyclerview_with_fallback)
        listView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        listView.addItemDecoration(HorizontalImageDividerItemDecoration(R.drawable.horizontal_separator_pattern, this))

        listView.adapter = adapter

        viewModel = ViewModelProviders.of(this).get(TrackerChartViewListViewModel::class.java)

        creationSubscriptions.add(
                viewModel.currentGranularitySubject.subscribe { granularity ->
                    val point = viewModel.point
                    updateScopeUI(point, granularity)
                }
        )

        creationSubscriptions.add(
                viewModel.currentPointSubject.subscribe { point ->
                    val granularity = viewModel.granularity
                    updateScopeUI(point, granularity)
                }
        )

        creationSubscriptions.add(
                viewModel.chartViewModels.subscribe { newList ->
                    println("new chart data list: $newList")
                    val diffResult = DiffUtil.calculateDiff(
                            TrackerChartViewListViewModel.ChartViewModelListDiffUtilCallback(currentChartViewModelList, newList)
                    )

                    currentChartViewModelList.clear()
                    currentChartViewModelList.addAll(newList)
                    diffResult.dispatchUpdatesTo(adapter)
                }
        )

        creationSubscriptions.add(
                viewModel.trackerNameSubject.subscribe {
                    title = String.format(resources.getString(R.string.title_activity_chart_view, it))
                }
        )


        val trackerId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)
        if (trackerId != null) {
            viewModel.init(trackerId)
            viewModel.setScope(System.currentTimeMillis(), supportedGranularity[scopeSelectionView.selectedIndex])
        }
    }

    private fun updateScopeUI(point: Long, granularity: Granularity) {

        supportedGranularity.indexOf(granularity).let {
            if (it > -1) {
                scopeSelectionView.selectedIndex = it
            }
        }

        val ts = TimeSpan()
        granularity.convertToRange(point, ts)

        if (granularity == Granularity.YEAR) {
            currentYearView.visibility = View.GONE
        } else {
            currentYearView.visibility = View.VISIBLE
            currentYearView.text = TimeHelper.getYear(ts.from).toString()
        }

        currentScopeView.text = granularity.getFormattedCurrentScope(point, this)
    }

    override fun onToolbarLeftButtonClicked() {
        finish()
    }

    override fun onToolbarRightButtonClicked() {

    }

    override fun onClick(view: View) {
        if (view === leftNavigationButton) {
            var point = viewModel.point
            val granularity = viewModel.granularity
            point -= granularity.getIntervalMillis(false, point)
            viewModel.point = point
        } else if (view === rightNavigationButton) {
            var point = viewModel.point
            val granularity = viewModel.granularity
            point += granularity.getIntervalMillis(true, point)
            viewModel.point = point
        }
    }

    inner class ChartViewListAdapter : RecyclerView.Adapter<ChartViewListAdapter.ChartViewHolder>() {
        private var chartViewId: Int = View.generateViewId()

        override fun getItemCount(): Int {
            return currentChartViewModelList.size
        }

        override fun getItemViewType(position: Int): Int {
            val model = currentChartViewModelList[position]
            return when (model) {
                is INativeChartModel -> VIEW_TYPE_NATIVE
                is IWebBasedChartModel -> VIEW_TYPE_WEB
                else -> throw IllegalArgumentException("model should implement either INativeChartModel or IWebBasedChartModel.")
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartViewHolder {
            val view = parent.inflateContent(R.layout.chart_view_list_element, false) as ViewGroup
            return ChartViewHolder(view,
                    when (viewType) {
                        VIEW_TYPE_NATIVE -> NativeChartView(parent.context)
                        VIEW_TYPE_WEB -> WebBasedChartView(parent.context)
                        else -> NativeChartView(parent.context)
                    }
            )
        }

        override fun onBindViewHolder(holder: ChartViewHolder, position: Int) {
            val model = currentChartViewModelList[position]
            holder.bindChart(model)
        }

        inner class ChartViewHolder(view: ViewGroup, val chartView: IChartView) : RecyclerView.ViewHolder(view) {

            private val nameView: TextView = view.findViewById(R.id.ui_chart_name)

            init {
                if (chartView is View) {
                    val formerChartView = view.findViewById<View>(chartViewId)
                    if (formerChartView != null && formerChartView !== chartView) {
                        view.removeViewInLayout(formerChartView)
                    }

                    chartView.id = chartViewId

                    chartView.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT)

                    view.addView(chartView)
                }
            }

            fun bindChart(model: ChartModel<*>) {
                nameView.text = model.name
                model.reload()
                chartView.model = model
            }
        }
    }
}
