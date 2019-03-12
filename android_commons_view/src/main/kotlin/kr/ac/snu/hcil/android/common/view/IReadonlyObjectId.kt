package kr.ac.snu.hcil.android.common.view

import androidx.recyclerview.widget.DiffUtil

/**
 * Created by younghokim on 2017. 10. 20..
 */
interface IReadonlyObjectId {
    val objectId: String?

    open class DiffUtilCallback(val oldList: List<IReadonlyObjectId>, val newList: List<IReadonlyObjectId>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].objectId == newList[newItemPosition].objectId
        }

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return areItemsTheSame(oldItemPosition, newItemPosition)
        }

    }
}