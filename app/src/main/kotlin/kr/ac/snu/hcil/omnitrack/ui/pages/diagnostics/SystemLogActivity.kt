package kr.ac.snu.hcil.omnitrack.ui.pages.diagnostics

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.ac.snu.hcil.android.common.view.inflateContent
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.LoggingDbHelper
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import java.util.*

class SystemLogActivity : MultiButtonActionBarActivity(R.layout.activity_system_log) {

    private lateinit var systemLogListView: RecyclerView

    private lateinit var adapter: SystemLogAdapter

    private val systemLogs = ArrayList<LoggingDbHelper.OTLog>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(Mode.Back)

        systemLogListView = findViewById(R.id.ui_system_log_list)
        systemLogListView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, true)

        adapter = SystemLogAdapter()

        systemLogListView.adapter = adapter
    }

    override fun onToolbarLeftButtonClicked() {
        finish()
    }

    override fun onToolbarRightButtonClicked() {
    }

    fun refreshSystemLogs() {
        systemLogListView.isEnabled = false
        systemLogListView.alpha = 0.2f
        Thread {
            OTApp.logger.readSystemLogs(systemLogs, false)
            Handler(Looper.getMainLooper()).post {
                adapter.notifyDataSetChanged()
                systemLogListView.isEnabled = true
                systemLogListView.alpha = 1.0f
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        refreshSystemLogs()
    }

    private inner class SystemLogAdapter : RecyclerView.Adapter<SystemLogViewHolder>() {

        override fun getItemCount(): Int {
            return systemLogs.size
        }

        override fun onBindViewHolder(holder: SystemLogViewHolder, position: Int) {
            holder.bind(systemLogs[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SystemLogViewHolder {
            val view = parent.inflateContent(R.layout.system_log_list_element, false)
            return SystemLogViewHolder(view)
        }

    }
}
