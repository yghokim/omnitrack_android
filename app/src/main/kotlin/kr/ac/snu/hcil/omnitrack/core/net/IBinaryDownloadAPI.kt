package kr.ac.snu.hcil.omnitrack.core.net

import android.net.Uri
import io.reactivex.Single

/**
 * Created by younghokim on 2017. 9. 26..
 */
interface IBinaryDownloadAPI
{
    fun downloadFileTo(pathString: String, localUri: Uri): Single<Uri>
}