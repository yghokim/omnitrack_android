package kr.ac.snu.hcil.omnitrack.ui.pages.diagnostics

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.LoggingDbHelper
import java.util.*

/**
 * Created by younghokim on 2016. 10. 13..
 */
class SystemLogViewHolder(view: View) : RecyclerView.ViewHolder(view) {


    private val idView: TextView
    private val logView: TextView
    private val tagView: TextView
    private val timestampView: TextView

    init {
        idView = view.findViewById(R.id.ui_id) as TextView
        logView = view.findViewById(R.id.ui_log) as TextView
        tagView = view.findViewById(R.id.ui_tag) as TextView
        timestampView = view.findViewById(R.id.ui_timestamp) as TextView
    }

    fun bind(log: LoggingDbHelper.OTLog) {
        idView.text = log.id.toString()
        logView.text = log.log
        tagView.text = log.tag
        timestampView.text = LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(log.timestamp))
    }
}