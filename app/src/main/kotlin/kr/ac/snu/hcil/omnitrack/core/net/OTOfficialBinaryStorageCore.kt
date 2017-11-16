package kr.ac.snu.hcil.omnitrack.core.net

import android.content.Context
import android.net.Uri
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.UploadTaskInfo
import kr.ac.snu.hcil.omnitrack.utils.io.FileHelper
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.FileNotFoundException

/**
 * Created by younghokim on 2017-11-15.
 */
class OTOfficialBinaryStorageCore(val context: Context, val retrofit: Lazy<Retrofit>) : IBinaryStorageCore {
    /**
     * Retrofit Interface
     */
    interface OfficialBinaryStorageServerService {

        @Multipart
        @POST("api/upload/item_media")
        fun uploadItemMediaFile(@Part("trackerId") trackerId: String, @Part("itemId") itemId: String, @Part file: MultipartBody.Part): Call<ResponseBody>
    }

    private val service: OfficialBinaryStorageServerService by lazy {
        retrofit.get().create(OfficialBinaryStorageServerService::class.java)
    }

    override fun startNewUploadTaskImpl(taskInfo: UploadTaskInfo, onProgress: (session: String) -> Unit): Completable {
        return Completable.defer {
            val uri = taskInfo.localUriCompat()
            val mimeType = FileHelper.getMimeTypeOf(uri, context)
            if (mimeType != null) {
                val mediaType = MediaType.parse(mimeType)
                if (mediaType != null) {
                    val fileBody = MultipartBody.create(mediaType, File(uri.path))
                    return@defer Completable.complete()
                } else return@defer Completable.error(FileNotFoundException("MimeType extraction was failed"))
            } else return@defer Completable.error(FileNotFoundException("MimeType extraction was failed"))
        }
    }

    override fun makeFilePath(itemId: String, trackerId: String, userId: String, fileName: String): Uri {
        return Uri.parse(fileName)
    }

    override fun downloadFileTo(pathString: String, localUri: Uri): Single<Uri> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}