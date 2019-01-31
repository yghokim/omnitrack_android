package kr.ac.snu.hcil.omnitrack.ui.pages.diagnostics

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.LoggingDbHelper
import java.util.*

/**
 * Created by younghokim on 2016. 10. 13..
 */
class SystemLogViewHolder(view: View) : RecyclerView.ViewHolder(view) {


    private val idView: TextView = view.findViewById(R.id.ui_id)
    private val logView: TextView = view.findViewById(R.id.ui_log)
    private val tagView: TextView = view.findViewById(R.id.ui_tag)
    private val timestampView: TextView = view.findViewById(R.id.ui_timestamp)

    fun bind(log: LoggingDbHelper.OTLog) {
        idView.text = log.id.toString()
        logView.text = log.log
        tagView.text = log.tag
        timestampView.text = LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(log.timestamp))
    }
}