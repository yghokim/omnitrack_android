package kr.ac.snu.hcil.omnitrack.core.database.synchronization

/**
 * Created by younghokim on 2017. 9. 28..
 */
data class SyncResultEntry(val id: String, val synchronizedAt: Long) {
    override fun toString(): String {
        return "SyncResultEntry{ id: ${id}, synchronizedAt: ${synchronizedAt} }"
    }
}