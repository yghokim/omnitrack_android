package kr.ac.snu.hcil.omnitrack.core.database.synchronization

import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Created by younghokim on 2017. 10. 1..
 */
enum class SyncDirection(val code: Short) {
    DOWNLOAD(0b01), UPLOAD(0b10), BIDIRECTIONAL(0b11);

    fun contains(direction: SyncDirection): Boolean {
        return this.code and direction.code == direction.code
    }

    companion object {
        fun fromCode(code: Short): SyncDirection {
            return SyncDirection.values().find { it.code == code } ?: throw IllegalArgumentException("Unexpected code.")
        }

        fun union(directions: Collection<SyncDirection>): SyncDirection{
            var union: SyncDirection? = null
            for(direction in directions)
            {
                if(union != null) {
                    union += direction
                }
                else union = direction

                if(union == BIDIRECTIONAL) break
            }

            return union!!
        }
    }
}

operator fun SyncDirection.plus(b: SyncDirection): SyncDirection {
    return SyncDirection.fromCode(this.code or b.code)
}