package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.core.OTUser

/**
 * Created by younghokim on 16. 8. 13..
 */


open class AttributePresetInfo(val typeId: Int, val iconId: Int, val name: String, val description: String?, val creater: ((user: OTUser, columnName: String) -> OTAttribute<out Any>))

class SimpleAttributePresetInfo(typeId: Int, iconId: Int, name: String, description: String?) : AttributePresetInfo(typeId, iconId, name, description,
        {
            user, columnName ->
            OTAttribute.createAttribute(user, columnName, typeId)
        })
