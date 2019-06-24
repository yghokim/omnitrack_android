package kr.ac.snu.hcil.omnitrack.core.fields

import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO

/**
 * Created by younghokim on 16. 8. 13..
 */


open class FieldPresetInfo(val typeId: Int, val iconId: Int, val name: String, val description: String?, internal val processor: ((OTFieldDAO, Realm) -> OTFieldDAO))

class SimpleFieldPresetInfo(typeId: Int, iconId: Int, name: String, description: String?) : FieldPresetInfo(typeId, iconId, name, description,
        { dao, realm ->
            dao
        })
