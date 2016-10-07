package kr.ac.snu.hcil.omnitrack.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Created by Young-Ho Kim on 16. 8. 24
 */
class ObservableMapDelegate<OWNER, TYPE : Any>(private val initialValue: TYPE, private val map: MutableMap<String, Any?>, private val changedListener: (value: TYPE) -> Unit) : ReadWriteProperty<OWNER, TYPE> {

    operator override fun getValue(thisRef: OWNER, property: KProperty<*>): TYPE {
        if (!map.containsKey(property.name)) {
            map[property.name] = initialValue
        }

        @Suppress("UNCHECKED_CAST")
        return map[property.name] as TYPE
    }

    operator override fun setValue(thisRef: OWNER, property: KProperty<*>, value: TYPE) {
        if (map[property.name] != value) {
            map[property.name] = value
            changedListener.invoke(value)
        }
    }

}