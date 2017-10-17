package kr.ac.snu.hcil.omnitrack.core.database.local

import io.realm.RealmList
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Created by younghokim on 2017. 10. 17..
 */
class SerializedRealmListDictionaryDelegate<OWNER, TYPE : Any>(private val initialValue: TYPE, private val realmList: RealmList<OTStringStringEntryDAO>, private val changedListener: ((value: TYPE) -> Unit)? = null) : ReadWriteProperty<OWNER, TYPE> {

    @Suppress("UNCHECKED_CAST")
    operator override fun getValue(thisRef: OWNER, property: KProperty<*>): TYPE {
        var entry = realmList.find { it.key == property.name }
        if (entry == null) {
            entry = OTStringStringEntryDAO().apply {
                id = UUID.randomUUID().toString()
                key = property.name
                value = TypeStringSerializationHelper.serialize(initialValue)
            }
            realmList.add(entry)
        }

        return entry.value?.let { TypeStringSerializationHelper.deserialize(it) } as TYPE
    }

    operator override fun setValue(thisRef: OWNER, property: KProperty<*>, value: TYPE) {
        var entry = realmList.find { it.key == property.name }
        if (entry == null) {
            entry = OTStringStringEntryDAO().apply {
                id = UUID.randomUUID().toString()
                key = property.name
                this.value = TypeStringSerializationHelper.serialize(value)
            }
            realmList.add(entry)
            changedListener?.invoke(value)
        } else {
            val serialized = TypeStringSerializationHelper.serialize(value)
            if (entry.value != serialized) {
                entry.value = serialized
                changedListener?.invoke(value)
            }
        }
    }

}