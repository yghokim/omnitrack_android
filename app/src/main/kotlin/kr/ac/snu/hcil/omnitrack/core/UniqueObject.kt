package kr.ac.snu.hcil.omnitrack.core

import java.util.UUID;

/**
 * Created by Young-Ho on 7/11/2016.
 */
abstract class UniqueObject(id: String?, name: String) : IPropertyChanged {
    val ObjectId: String by lazy {
        id ?: UUID.randomUUID().toString()
    }


    var name: String = name
        set(value) {
            if (value != field) {
                field = value
                onPropertyChanged("name", value)
            }
        }

    constructor() : this(null, "Nomame")

}
