package kr.ac.snu.hcil.omnitrack.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Created by younghokim on 16. 8. 24..
 */
class ObservableMapDelegate<OWNER, TYPE : Any>(val initialValue: TYPE, val map: MutableMap<String, Any?>, val changedListener: (value: TYPE) -> Unit) : ReadWriteProperty<OWNER, TYPE> {
    override fun getValue(thisRef: OWNER, property: KProperty<*>): TYPE {
        if (map[property.name] == null) {
            map[property.name] = initialValue
        }

        return map[property.name] as TYPE
    }

    override fun setValue(thisRef: OWNER, property: KProperty<*>, value: TYPE) {
        if (map[property.name] != value) {
            map[property.name] = value
            changedListener.invoke(value)
        }
    }

}