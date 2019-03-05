package kr.ac.snu.hcil.omnitrack.core.database.models.research

import androidx.annotation.Keep
import androidx.recyclerview.widget.DiffUtil
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required

/**
 * Created by younghokim on 2018. 1. 3..
 */
open class OTExperimentDAO : RealmObject() {
    @PrimaryKey
    var id: String = ""

    @Required
    var name: String = ""

    var joinedAt: Long = System.currentTimeMillis()

    var droppedAt: Long? = null

    fun getInfo(): ExperimentInfo {
        return ExperimentInfo(id, name, joinedAt, droppedAt)
    }
}

@Keep
data class ExperimentInfo(val id: String, val name: String, val joinedAt: Long, val droppedAt: Long? = null) {

    override fun toString(): String {
        return name
    }

    class DiffUtilCallback(val oldList: List<ExperimentInfo>, val newList: List<ExperimentInfo>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            return oldItem.joinedAt == newItem.joinedAt &&
                    oldItem.name == newItem.name &&
                    oldItem.id == newItem.id &&
                    oldItem.droppedAt == newItem.droppedAt
        }

    }
}