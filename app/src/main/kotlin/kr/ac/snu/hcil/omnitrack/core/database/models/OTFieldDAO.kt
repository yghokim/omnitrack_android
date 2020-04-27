package kr.ac.snu.hcil.omnitrack.core.database.models

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import io.reactivex.Single
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTStringStringEntryDAO
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTFieldHelper
import kr.ac.snu.hcil.omnitrack.core.fields.validators.ValidatorManager
import kr.ac.snu.hcil.omnitrack.core.flags.F
import kr.ac.snu.hcil.omnitrack.core.flags.LockFlagLevel
import kr.ac.snu.hcil.omnitrack.core.flags.LockedPropertiesHelper
import java.util.*

open class OTFieldDAO : RealmObject() {

    @Suppress("PropertyName")
    @PrimaryKey
    var _id: String? = null

    @Index
    var localId: String = ""

    @Index
    var trackerId: String? = null

    var name: String = ""

    @Index
    var isHidden: Boolean = false

    @Index
    var isInTrashcan: Boolean = false

    var position: Int = 0
    var serializedConnection: String? = null
    var type: Int = -1
    var isRequired: Boolean = false
    var properties = RealmList<OTStringStringEntryDAO>()

    var validators = RealmList<OTFieldValidatorDAO>()

    //If connection is specified, this policy is overriden.
    var fallbackValuePolicy: String = DEFAULT_VALUE_POLICY_NULL
    var fallbackPresetSerializedValue: String? = null

    var userCreatedAt: Long = System.currentTimeMillis()
    var userUpdatedAt: Long = System.currentTimeMillis()

    var serializedCreationFlags: String = "{}"
    var serializedLockedPropertyInfo: String = "{}"

    fun getParsedConnection(context: Context): OTConnection? {
        return try {
            serializedConnection?.let { (context.applicationContext as OTAndroidApp).applicationComponent.getConnectionTypeAdapter().fromJson(it) }
        } catch (ex: JsonSyntaxException) {
            ex.printStackTrace(); null
        }
    }


    @Ignore
    private var _parsedLockedPropertyInfo: JsonObject? = null

    fun getParsedLockedPropertyInfo(): JsonObject {
        if (_parsedLockedPropertyInfo == null) {
            _parsedLockedPropertyInfo = LockedPropertiesHelper.parseFlags(serializedLockedPropertyInfo)
        }
        return _parsedLockedPropertyInfo!!
    }

    fun isEditingAllowed(): Boolean {
        return LockedPropertiesHelper.flag(LockFlagLevel.Field, F.Modify, getParsedLockedPropertyInfo())
    }

    fun isEditPropertyEnabled(): Boolean {
        return LockedPropertiesHelper.flag(LockFlagLevel.Field, F.EditProperties, getParsedLockedPropertyInfo())
    }

    fun isEditNameEnabled(): Boolean {
        return LockedPropertiesHelper.flag(LockFlagLevel.Field, F.EditName, getParsedLockedPropertyInfo())
    }

    fun isEditMeasureFactoryEnabled(): Boolean {
        return LockedPropertiesHelper.flag(LockFlagLevel.Field, F.EditMeasureFactory, getParsedLockedPropertyInfo())
    }

    fun isRequiredToggleEnabled(): Boolean {
        return LockedPropertiesHelper.flag(LockFlagLevel.Field, F.ToggleRequired, getParsedLockedPropertyInfo())
    }

    fun isRemovalAllowed(): Boolean {
        return LockedPropertiesHelper.flag(LockFlagLevel.Field, F.Delete, getParsedLockedPropertyInfo())
    }

    fun isVisibilityToggleAllowed(): Boolean {
        return LockedPropertiesHelper.flag(LockFlagLevel.Field, F.ToggleVisibility, getParsedLockedPropertyInfo())
    }

    fun getHelper(context: Context): OTFieldHelper {
        return (context.applicationContext as OTAndroidApp).applicationComponent.getAttributeManager().get(type)
    }

    fun isValueValid(value: Any?, pivotTime: Long? = null): Boolean {
        val requiredSatisfied = isRequired == false || value != null

        val validatorPassed = validators.isEmpty() || validators.all { ValidatorManager.isValid(it.type, null, value, pivotTime) }

        return requiredSatisfied && validatorPassed
    }

    fun initializeUserCreated(context: Context) {
        val lockedFlags = LockedPropertiesHelper.generateDefaultFlags(LockFlagLevel.Field, true)
        serializedLockedPropertyInfo = lockedFlags.toString()
        _parsedLockedPropertyInfo = lockedFlags
        initializePropertiesWithDefaults(context)
        getHelper(context).initialize(this)
    }

    fun initializePropertiesWithDefaults(context: Context) {
        val fieldHelper = getHelper(context)
        fieldHelper.propertyKeys.forEach { key ->
            setPropertySerializedValue(key, fieldHelper.getPropertyHelper<Any>(key).getSerializedValue(fieldHelper.getPropertyInitialValue(key)!!))
        }
    }

    fun getFallbackValue(context: Context, realm: Realm): Single<Nullable<out Any>> {
        return getHelper(context).getFallbackValue(this, realm)
    }

    fun setPropertySerializedValue(key: String, serializedValue: String): Boolean {
        var changed = false
        val match = properties.find { it.key == key }
        if (match != null) {
            if (match.value != serializedValue)
                changed = true

            match.value = serializedValue
        } else {
            properties.add(OTStringStringEntryDAO().apply {
                this.id = UUID.randomUUID().toString()
                this.key = key
                this.value = serializedValue
            })
            changed = true
        }

        return changed
    }

    fun getPropertySerializedValue(key: String): String? {
        return properties.find { it.key == key }?.value
    }


    companion object {

        const val DEFAULT_VALUE_POLICY_NULL = "null"
        const val DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE = "intrinsic"
        const val DEFAULT_VALUE_POLICY_FILL_WITH_PRESET = "preset"
        const val DEFAULT_VALUE_POLICY_FILL_WITH_LAST_ITEM = "last"
    }
}

open class OTFieldValidatorDAO : RealmObject() {
    @PrimaryKey
    var id: String = ""
    var type: String = ""
    var serializedParameterArray: String? = null
}