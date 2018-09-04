package kr.ac.snu.hcil.omnitrack.core.net

import android.support.annotation.Keep
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dagger.Lazy
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncResultEntry

/**
 * Created by younghokim on 2017. 9. 27..
 */
interface ISynchronizationServerSideAPI {

    @Keep
    data class ServerUserInfo(val _id: String, val name: String?, val email: String, val picture: String?, val nameUpdatedAt: Long?,
                              val dataStore: JsonObject?)

    class ExperimentConsentInfo(var receiveConsentInApp: Boolean = false, var consent: String? = null, var demographicFormSchema: String? = null) {
        class ConsentInfoTypeAdapter(val gson: Lazy<Gson>) : TypeAdapter<ExperimentConsentInfo>() {
            override fun write(out: JsonWriter, value: ExperimentConsentInfo) {
                out.name("receiveConsentInApp").value(value.receiveConsentInApp)
                out.name("consent").value(value.consent)
                out.name("demographicFormSchema").jsonValue(value.demographicFormSchema)
            }

            override fun read(reader: JsonReader): ExperimentConsentInfo {
                val result = ExperimentConsentInfo()
                reader.beginObject()

                while (reader.hasNext()) {
                    val name = reader.nextName()
                    if (reader.peek() == JsonToken.NULL) {
                        reader.skipValue()
                    } else {
                        when (name) {
                            "consent" -> {
                                result.consent = reader.nextString()
                            }
                            "receiveConsentInApp" -> {
                                result.receiveConsentInApp = reader.nextBoolean()
                            }
                            "demographicFormSchema" -> {
                                result.demographicFormSchema = gson.get().fromJson<JsonObject>(reader, JsonObject::class.java).toString()
                            }
                            else -> reader.skipValue()
                        }
                    }
                }

                reader.endObject()
                return result
            }

        }
    }

    @Keep
    data class AuthenticationResult(val inserted: Boolean, val deviceLocalKey: String, val userInfo: ServerUserInfo?)

    @Keep
    data class DeviceInfoResult(var result: String, var deviceLocalKey: String?)

    @Keep
    data class InformationUpdateResult(var success: Boolean, var finalValue: String, val payloads: Map<String, String>? = null)

    @Keep
    data class DirtyRowBatchParameter(val type: ESyncDataType, val rows: Array<String>)

    //New authentication APIs======================================
    fun checkExperimentParticipationStatus(experimentId: String): Single<Boolean>

    fun authenticate(deviceInfo: OTDeviceInfo, invitationCode: String?, demographicData: JsonObject?): Single<AuthenticationResult>
    fun getExperimentConsentInfo(experimentId: String): Single<ExperimentConsentInfo>
    fun verifyInvitationCode(invitationCode: String, experimentId: String): Single<Boolean>
    //=============================================================

    fun putDeviceInfo(info: OTDeviceInfo): Single<DeviceInfoResult>
    fun removeDeviceInfo(userId: String, deviceId: String): Single<Boolean>

    fun putUserName(name: String, timestamp: Long): Single<InformationUpdateResult>

    fun getTrackingPackageJson(trackerIds: Array<String>, triggerIds: Array<String>): Single<String>
    fun uploadTemporaryTrackingPackage(packageString: String): Single<String>

    //=================================================================================================================================
    //server returns server-side changes after designated timestamp.
    fun getRowsSynchronizedAfter(vararg batch: Pair<ESyncDataType, Long>): Single<Map<ESyncDataType, Array<JsonObject>>>

    //send dirty data to server
    fun postDirtyRows(vararg batch: DirtyRowBatchParameter): Single<Map<ESyncDataType, Array<SyncResultEntry>>>
    //==================================================================================================================================
}