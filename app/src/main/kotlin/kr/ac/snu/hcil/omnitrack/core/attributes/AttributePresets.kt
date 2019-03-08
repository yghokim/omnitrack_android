package kr.ac.snu.hcil.omnitrack.core.attributes

import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO

/**
 * Created by younghokim on 16. 8. 13..
 */


open class AttributePresetInfo(val typeId: Int, val iconId: Int, val name: String, val description: String?, internal val processor: ((OTAttributeDAO, Realm) -> OTAttributeDAO))

class SimpleAttributePresetInfo(typeId: Int, iconId: Int, name: String, description: String?) : AttributePresetInfo(typeId, iconId, name, description,
        { dao, realm ->
            dao
        })
