package kr.ac.snu.hcil.omnitrack.ui.pages.export

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import kotlinx.android.synthetic.main.simple_layout_with_recycler_view.view.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.utils.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.utils.events.IEventListener

abstract class EntitySelectionFragment<EntityType : IReadonlyObjectId, ChildType : IReadonlyObjectId> : OTFragment() {

    protected lateinit var viewModel: PackageExportViewModel
        private set

    private val selectedIdList = ArrayList<String>()

    private val entityList = ArrayList<EntityType>()

    private val adapter = EntityListAdapter()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        this.viewModel = ViewModelProviders.of(act).get(PackageExportViewModel::class.java)
        creationSubscriptions.add(
                getEntityListObservable(this.viewModel).subscribe { list ->
                    entityList.clear()
                    entityList.addAll(list)
                    adapter.notifyDataSetChanged()
                }
        )

        creationSubscriptions.add(
                getSelectedIdsObservable(this.viewModel).subscribe { list ->
                    selectedIdList.clear()
                    selectedIdList.addAll(list)
                    adapter.notifyDataSetChanged()
                }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.simple_layout_with_recycler_view, container, false)

        rootView.ui_recyclerview_with_fallback.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        rootView.ui_recyclerview_with_fallback.adapter = adapter

        return rootView
    }

    protected abstract fun getEntityListObservable(viewModel: PackageExportViewModel): Observable<List<EntityType>>
    protected abstract fun getSelectedIdsObservable(viewModel: PackageExportViewModel): Observable<List<String>>
    protected abstract fun makeEntityViewHolder(parent: ViewGroup): ACheckableTrackingEntityViewHolder<EntityType, ChildType>
    protected abstract fun setEntityChecked(entity: EntityType, viewModel: PackageExportViewModel, checked: Boolean, id: String)

    inner class EntityListAdapter : RecyclerView.Adapter<ACheckableTrackingEntityViewHolder<EntityType, ChildType>>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ACheckableTrackingEntityViewHolder<EntityType, ChildType> {
            return makeEntityViewHolder(parent)
        }

        override fun getItemCount(): Int {
            return entityList.size
        }

        override fun onBindViewHolder(holder: ACheckableTrackingEntityViewHolder<EntityType, ChildType>, position: Int) {
            val entity = entityList[position]
            holder.entity = entity
            holder.isSelected = selectedIdList.contains(entity.objectId)
            holder.selectionChangedHandler = object : IEventListener<Boolean> {
                override fun onEvent(sender: Any, args: Boolean) {
                    setEntityChecked(entity, viewModel, args, entity.objectId ?: "")
                }
            }
        }
    }
}