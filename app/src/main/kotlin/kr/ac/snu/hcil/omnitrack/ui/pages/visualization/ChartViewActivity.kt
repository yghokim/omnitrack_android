package kr.ac.snu.hcil.omnitrack.ui.pages.visualization

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemEditingActivity

class ChartViewActivity : MultiButtonActionBarActivity(R.layout.activity_chart_view) {

    companion object{
            fun makeIntent(trackerId: String, context: Context): Intent {
                val intent = Intent(context, ChartViewActivity::class.java)
                intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
                return intent
            }
    }

    private lateinit var tracker: OTTracker
    private var isTrackerLoaded = false

    private lateinit var listView: RecyclerView

    private var adapter: TrackerChartListAdapter = TrackerChartListAdapter(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(Mode.Back)

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

    override fun onStart() {
        super.onStart()

        val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
        val tracker = OTApplication.app.currentUser[trackerId]
        if(tracker!=null)
        {
            this.tracker = tracker
            isTrackerLoaded = true
            adapter.tracker = this.tracker

            setTitle(String.format(resources.getString(R.string.title_activity_chart_view, tracker.name)))
        }
    }

    override fun onResume() {
        super.onResume()

    }
}
