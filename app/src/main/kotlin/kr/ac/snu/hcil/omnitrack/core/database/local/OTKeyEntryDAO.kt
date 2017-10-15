package kr.ac.snu.hcil.omnitrack.core.database.local

import android.support.v7.util.DiffUtil
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Created by Young-Ho on 10/8/2017.
 */

open class OTStringStringEntryDAO : RealmObject() {
    @PrimaryKey
    var id: String = ""
    var key: String = ""
    var value: String? = null

    override fun toString(): String {
        return "{StringStringEntry | id : $id, key : $key, value : $value}"
    }
}

open class OTIntegerStringEntryDAO : RealmObject() {
    @PrimaryKey
    var id: String = ""
    var key: Int = -1
    var value: String? = null
}

class StringStringEntryListDiffCallback(a: List<OTStringStringEntryDAO>, b: List<OTStringStringEntryDAO>) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        TODO("not implemented")
    }

    override fun getOldListSize(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getNewListSize(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}