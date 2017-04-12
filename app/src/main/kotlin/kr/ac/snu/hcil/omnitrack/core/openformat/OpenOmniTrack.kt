package kr.ac.snu.hcil.omnitrack.core.openformat

import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute

/**
 * Created by younghokim on 2017. 4. 12..
 */
object OpenOmniTrack {

    const val NAME = "name"
    const val COLOR = "color"
    const val ON_SHORTCUT = "on_shortcut"
    const val ATTRIBUTES = "attributes"
    const val PROPERTIES = "properties"
    const val IS_EDITABLE = "editable"
    const val TYPE = "TYPE"


    fun addTracker(user: OTUser, json: JsonObject): OTTracker? {

        val trackerName = try {
            json.get(NAME).asString!!
        } catch(ex: Exception) {
            user.generateNewTrackerName(OTApplication.app)
        }

        val color = try {
            json.get(COLOR).asInt
        } catch(ex: Exception) {
            OTApplication.app.colorPalette[0]
        }

        val onShortcut = try {
            json.get(ON_SHORTCUT).asBoolean
        } catch(ex: Exception) {
            false
        }

        val isEditable = try {
            json.get(IS_EDITABLE).asBoolean
        } catch(ex: Exception) {
            true
        }

        val newTracker = user.newTracker(trackerName, true, OTTracker.CREATION_FLAG_OPEN_OMNITRACK, isEditable)
        newTracker.color = color
        newTracker.isOnShortcut = onShortcut

        //get attributes
        if (json.has(ATTRIBUTES)) {
            val attributeArray = json.getAsJsonArray(ATTRIBUTES)
            if (attributeArray != null) {
                for (attributeJson in attributeArray) {
                    val attributeObj = attributeJson.asJsonObject
                    if (attributeObj != null) {
                        val typeString: String
                        val attributeType: Int
                        try {
                            typeString = attributeObj[TYPE].asString!!
                            attributeType = OTAttribute.getTypeIdFromString(typeString)
                        } catch(ex: Exception) {
                            continue
                        }

                        val attributeName: String = try {
                            attributeObj.get(NAME).asString!!
                        } catch(ex: Exception) {
                            newTracker.generateNewAttributeName(typeString, OTApplication.app)
                        }
                        val attribute = OTAttribute.createAttribute(newTracker, attributeName, attributeType)

                        if (attributeObj.has(PROPERTIES)) {

                        }

                    }
                }
            }
        }

        //get reminders

        return newTracker
    }
}