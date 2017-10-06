package kr.ac.snu.hcil.omnitrack.core.database.synchronization

import kotlin.experimental.or

/**
 * Created by younghokim on 2017. 10. 1..
 */
enum class SyncDirection(val code: Short) {
    DOWNLOAD(0b01), UPLOAD(0b10), BIDIRECTIONAL(0b11);

    companion object {
        fun fromCode(code: Short): SyncDirection {
            return SyncDirection.values().find { it.code == code } ?: throw IllegalArgumentException("Unexpected code.")
        }
    }
}

operator fun SyncDirection.plus(b: SyncDirection): SyncDirection {
    return SyncDirection.fromCode(this.code or b.code)
}