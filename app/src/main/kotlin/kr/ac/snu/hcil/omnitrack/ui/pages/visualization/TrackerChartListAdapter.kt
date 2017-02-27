package kr.ac.snu.hcil.omnitrack.ui.pages.visualization

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.Granularity
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.ChartView
import kr.ac.snu.hcil.omnitrack.utils.inflateContent

/**
 * Created by Young-Ho on 9/7/2016.
 */
class TrackerChartListAdapter(_tracker: OTTracker?): RecyclerView.Adapter<TrackerChartListAdapter.ChartViewHolder>() {

    var tracker: OTTracker? = _tracker
        set(value)
        {
            if(field !== value)
            {
                field = value

                models = value?.getRecommendedChartModels()

                notifyDataSetChanged()
            }
        }

    var models: Array<ChartModel<*>>? = null

    fun setScopedQueryRange(pivot: Long, scope: Granularity)
    {
        if(models != null) {
            for (model in models!!) {
                model.setTimeScope(pivot, scope)
                model.reload()
            }
        }
    }

    fun dispose() {
        models?.forEach { it.recycle() }
    }

    override fun getItemCount(): Int {
        return models?.size ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartViewHolder {
        val view = parent.inflateContent(R.layout.chart_view_list_element, false)
        return ChartViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChartViewHolder, position: Int) {
        val model = models?.get(position)
        if(model!=null)
            holder.bindChart(model)
    }

    inner class ChartViewHolder(view: View): RecyclerView.ViewHolder(view){

        private val nameView: TextView
        private val chartView: ChartView

        init{
            nameView = view.findViewById(R.id.ui_chart_name) as TextView
            chartView = view.findViewById(R.id.ui_chart_view) as ChartView
        }

        fun bindChart(model: ChartModel<*>)
        {
            nameView.text = model.name
            model.reload()
            chartView.model = model
        }
    }
}