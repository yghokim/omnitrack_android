package kr.ac.snu.hcil.omnitrack.ui.pages.export

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.tracker_for_package_list_element.view.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.events.IEventListener

abstract class ACheckableTrackingEntityViewHolder<EntityType, ChildType>(parent: ViewGroup, val useChildrenList: Boolean) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.tracker_for_package_list_element, parent, false)), View.OnClickListener {

    var entity: EntityType? = null
        set(value) {
            if (field != value) {
                childrenAdapter = null
                field = value
                if (value != null) {
                    onBindEntity(value)
                    if (useChildrenList) {
                        val c = getEntityChildren(value)
                        if (c.isEmpty()) {
                            itemView.ui_group_children.visibility = View.GONE
                            itemView.ui_children_list.swapAdapter(null, false)
                        } else {
                            itemView.ui_group_children.visibility = View.VISIBLE
                            childrenAdapter = getChildrenAdapter(value)
                            itemView.ui_children_list.swapAdapter(childrenAdapter!!, false)
                        }
                    }
                }
            }
        }

    protected abstract val childrenHeaderNameRes: Int

    protected abstract fun onBindEntity(entity: EntityType)

    protected abstract fun getEntityChildren(entity: EntityType): Array<ChildType>
    protected abstract fun getChildrenAdapter(entity: EntityType): RecyclerView.Adapter<out RecyclerView.ViewHolder>

    var selectionChangedHandler: IEventListener<Boolean>? = null

    protected var childrenAdapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>? = null
        private set

    var isSelected: Boolean
        get() {
            return itemView.ui_name_checkbox.isChecked
        }
        set(value) {
            itemView.ui_name_checkbox.isChecked = value
        }

    var title: CharSequence
        get() {
            return itemView.ui_name_checkbox.text
        }
        set(value) {
            itemView.ui_name_checkbox.text = value
        }

    init {
        itemView.ui_children_header.setText(childrenHeaderNameRes)

        if (useChildrenList) {
            itemView.ui_group_children.visibility = View.VISIBLE
            itemView.ui_children_list.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)
        } else {
            itemView.ui_group_children.visibility = View.GONE
        }

        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v === itemView) {
            //checkbox
            this.isSelected = !this.isSelected
            selectionChangedHandler?.onEvent(this, this.isSelected)
        }
    }

}