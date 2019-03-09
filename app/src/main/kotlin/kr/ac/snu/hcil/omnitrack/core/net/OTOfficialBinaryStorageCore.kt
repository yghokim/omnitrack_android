package kr.ac.snu.hcil.omnitrack.core.net

import android.content.Context
import android.net.Uri
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.android.common.file.FileHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.UploadTaskInfo
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

/**
 * Created by younghokim on 2017-11-15.
 */
class OTOfficialBinaryStorageCore(val context: Context, val retrofit: Lazy<Retrofit>) : IBinaryStorageCore {

    /**
     * Retrofit Interface
     */
    interface OfficialBinaryStorageServerService {

        @Multipart
        @POST("api/upload/item_media/{trackerId}/{itemId}/{attrLocalId}/{fileIdentifier}")
        fun uploadItemMediaFile(@Path("trackerId") trackerId: String, @Path("itemId") itemId: String, @Path("attrLocalId") attributeLocalId: String, @Path("fileIdentifier") fileIdentifier: String, @Part file: MultipartBody.Part): Single<ResponseBody>

        @GET("api/files/item_media/{trackerId}/{itemId}/{attrLocalId}/{fileIdentifier}/{processingType}")
        fun downloadMediaFile(
                @Path("trackerId") trackerId: String,
                @Path("itemId") itemId: String,
                @Path("attrLocalId") attributeLocalId: String,
                @Path("fileIdentifier") fileIdentifie: String,
                @Path("processingType") processingType: String): Single<ResponseBody>

        @GET("api/files/item_media/{trackerId}/{itemId}/{attrLocalId}/{fileIdentifier}")
        fun downloadMediaFile(
                @Path("trackerId") trackerId: String,
                @Path("itemId") itemId: String,
                @Path("attrLocalId") attributeLocalId: String,
                @Path("fileIdentifier") fileIdentifier: String): Single<ResponseBody>


    }

    private val service: OfficialBinaryStorageServerService by lazy {
        retrofit.get().create(OfficialBinaryStorageServerService::class.java)
    }

    override fun startNewUploadTaskImpl(taskInfo: UploadTaskInfo, onProgress: (session: String) -> Unit): Completable {
        return Completable.defer {
            val uri = taskInfo.localUriCompat()
            val fileSize = FileHelper.getFileSizeOf(uri, context)
            if (fileSize > 0) {
                val mediaType = MediaType.parse(taskInfo.localFileMimeType)
                if (mediaType != null) {
                    val fileBody = MultipartBody.Part.createFormData("file", taskInfo.localUriCompat().lastPathSegment, RequestBody.create(mediaType, File(uri.path)))
                    val split = taskInfo.serverPath.split("/")
                    return@defer service.uploadItemMediaFile(split[0], split[1], split[2], split[3], fileBody).subscribeOn(Schedulers.io())
                            .doOnError { error ->
                                error.printStackTrace()
                            }.ignoreElement()
                } else return@defer Completable.error(FileNotFoundException("MimeType extraction was failed"))
            } else return@defer Completable.error(FileNotFoundException("file size zero"))
        }
    }

    override fun makeServerPath(userId: String, trackerId: String, itemId: String, attributeLocalId: String, fileIdentifier: String): String {
        return "$trackerId/$itemId/$attributeLocalId/$fileIdentifier"
    }

    override fun decodeTrackerIdFromServerPath(serverPath: String): String? {
        val split = serverPath.split("/")
        if (split.size >= 4) {
            return split[0]
        } else return null
    }


    override fun downloadFileTo(pathString: String, localUri: Uri): Single<Uri> {
        val split = pathString.split("/")
        return service.downloadMediaFile(split[0], split[1], split[2], split[3]).subscribeOn(Schedulers.io()).map { responseBody: ResponseBody ->

            println("downloaded file size: ${responseBody.contentLength()}")
            println("downloaded file type: ${responseBody.contentType()}")
            FileHelper.dumpStreamToOther(responseBody.byteStream(), FileOutputStream(localUri.path))

            localUri
        }.doOnError { error ->
            error.printStackTrace()
        }
    }

}