package kr.ac.snu.hcil.omnitrack.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.codecrafters.tableview.TableView
import de.codecrafters.tableview.toolkit.SimpleTableDataAdapter
import de.codecrafters.tableview.toolkit.SimpleTableHeaderAdapter
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import java.util.*

class ItemBrowserActivity : AppCompatActivity() {

    private var tracker: OTTracker? = null

    private val items = ArrayList<OTItem>()

    private lateinit var tableView: TableView<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_browser)

        tableView = findViewById(R.id.ui_table) as TableView<Array<String>>
    }

    override fun onStart() {
        super.onStart()

        items.clear()
        if (intent.getStringExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER) != null) {
            tracker = OmniTrackApplication.app.currentUser.trackers.filter { it.objectId == intent.getStringExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER) }.first()

            OmniTrackApplication.app.dbHelper.getItems(tracker!!, items)

            tableView.dataAdapter = SimpleTableDataAdapter(this, items.map { it.extractFormattedStringArray(tracker!!) })

            val headers = tracker!!.attributes.unObservedList.map { it.name }.toTypedArray()
            tableView.headerAdapter = SimpleTableHeaderAdapter(this, *headers)
            tableView.setColumnWeight(0, 2)
        }
    }

}
