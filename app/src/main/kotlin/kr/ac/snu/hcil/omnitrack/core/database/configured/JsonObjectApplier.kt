package kr.ac.snu.hcil.omnitrack.core.database.configured

import com.google.gson.JsonObject
import io.realm.RealmObject

/**
 * Created by younghokim on 2017-11-06.
 */
interface JsonObjectApplier<T : RealmObject> {
    fun decodeToDao(json: JsonObject): T
    fun applyToManagedDao(json: JsonObject, applyTo: T): Boolean
}