package kr.ac.snu.hcil.omnitrack.ui.pages.visualization

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.visualization.Granularity
import kr.ac.snu.hcil.omnitrack.ui.activities.OTTrackerAttachedActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.choice.SelectionView
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper

class ChartViewActivity : OTTrackerAttachedActivity(R.layout.activity_chart_view), View.OnClickListener {

    companion object{
            fun makeIntent(trackerId: String, context: Context): Intent {
                val intent = Intent(context, ChartViewActivity::class.java)
                intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
                return intent
            }
    }

    private lateinit var timeNavigator: View
    private lateinit var currentScopeView: TextView
    private lateinit var currentYearView: TextView


    private lateinit var leftNavigationButton: View
    private lateinit var rightNavigationButton: View

    private lateinit var scopeSelectionView: SelectionView

    private val currentScope: Granularity
        get() = supportedGranularity[scopeSelectionView.selectedIndex]

    private var currentPoint: Long = System.currentTimeMillis()

    private lateinit var listView: RecyclerView

    private var adapter: TrackerChartListAdapter = TrackerChartListAdapter(null)

    private var supportedGranularity = arrayOf(Granularity.WEEK_REL, Granularity.WEEK_2_REL, Granularity.WEEK_4_REL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(Mode.Back)


        timeNavigator = findViewById(R.id.ui_time_navigation)
        leftNavigationButton = findViewById(R.id.ui_navigate_left)
        rightNavigationButton = findViewById(R.id.ui_navigate_right)
        leftNavigationButton.setOnClickListener(this)
        rightNavigationButton.setOnClickListener(this)

        currentScopeView = findViewById(R.id.ui_current_time) as TextView
        currentYearView = findViewById(R.id.ui_current_year) as TextView


        scopeSelectionView = findViewById(R.id.ui_scope_selection) as SelectionView
        scopeSelectionView.setValues(supportedGranularity.map { resources.getString(it.nameId) }.toTypedArray())
        scopeSelectionView.onSelectedIndexChanged += {
            sender, index ->
            currentPoint = System.currentTimeMillis()
            onTimeQueryChanged()
        }
        listView = findViewById(R.id.ui_list) as RecyclerView
        listView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        listView.addItemDecoration(HorizontalImageDividerItemDecoration(R.drawable.horizontal_separator_pattern, this))

        listView.adapter = adapter

    }

    override fun onToolbarLeftButtonClicked() {
        finish()
    }

    override fun onToolbarRightButtonClicked() {

    }

    override fun onTrackerLoaded(tracker: OTTracker) {
        super.onTrackerLoaded(tracker)

        adapter.tracker = tracker

        title = String.format(resources.getString(R.string.title_activity_chart_view, tracker.name))

        currentPoint = System.currentTimeMillis()

        onTimeQueryChanged()
    }

    private fun onTimeQueryChanged() {
        /*
        (model as? ITimelineChart)?.setTimeScope(currentPoint, currentScope)
        model?.reload()
        chartView.chartDrawer?.refresh()
        chartView.invalidate()
*/
        adapter.setScopedQueryRange(currentPoint, currentScope)

        val ts = TimeSpan()
        currentScope.convertToRange(currentPoint, ts)

        if(currentScope == Granularity.YEAR)
        {
            currentYearView.visibility = View.GONE
        }
        else{
            currentYearView.visibility = View.VISIBLE
            currentYearView.text = TimeHelper.getYear(ts.from).toString()
        }

        currentScopeView.text = currentScope.getFormattedCurrentScope(currentPoint, this)
    }


    override fun onClick(view: View) {
        if (view === leftNavigationButton) {
            currentPoint -= currentScope.getIntervalMillis(false, currentPoint)
            onTimeQueryChanged()

        } else if (view === rightNavigationButton) {

            currentPoint += currentScope.getIntervalMillis(true, currentPoint)
            onTimeQueryChanged()
        }
    }
}
