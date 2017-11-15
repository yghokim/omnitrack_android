package kr.ac.snu.hcil.omnitrack.core.net

import android.net.Uri
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.UploadTaskInfo
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Created by younghokim on 2017-11-15.
 */
class OTOfficialBinaryStorageCore(retrofit: Lazy<Retrofit>) : IBinaryStorageCore {
    /**
     * Retrofit Interface
     */
    interface OfficialBinaryStorageServerService {

        @Multipart
        @POST("api/upload/item_media")
        fun uploadItemMediaFile(@Part("trackerId") trackerId: String, @Part("itemId") itemId: String, @Part("description") description: RequestBody, @Part file: MultipartBody.Part): Call<ResponseBody>
    }

    private val service: OfficialBinaryStorageServerService by lazy {
        retrofit.get().create(OfficialBinaryStorageServerService::class.java)
    }

    override fun startNewUploadTaskImpl(taskInfo: UploadTaskInfo, onProgress: (session: String) -> Unit): Completable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun makeFilePath(itemId: String, trackerId: String, userId: String, fileName: String): Uri {
        return Uri.parse(fileName)
    }

    override fun downloadFileTo(pathString: String, localUri: Uri): Single<Uri> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}