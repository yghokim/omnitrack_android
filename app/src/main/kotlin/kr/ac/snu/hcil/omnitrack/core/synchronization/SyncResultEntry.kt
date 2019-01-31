package kr.ac.snu.hcil.omnitrack.core.synchronization

import androidx.annotation.Keep

/**
 * Created by younghokim on 2017. 9. 28..
 */
@Keep
data class SyncResultEntry(val id: String, val synchronizedAt: Long) {
    override fun toString(): String {
        return "SyncResultEntry{ id: $id, synchronizedAt: $synchronizedAt }"
    }
}