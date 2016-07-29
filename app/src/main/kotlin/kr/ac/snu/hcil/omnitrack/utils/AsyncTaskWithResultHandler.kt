package kr.ac.snu.hcil.omnitrack.utils

import android.os.AsyncTask

/**
 * Created by Young-Ho Kim on 2016-07-29.
 */
abstract class AsyncTaskWithResultHandler(val handler: ((Boolean) -> Unit)?) : AsyncTask<Void?, Void?, Boolean>() {
    override fun onPostExecute(result: Boolean) {
        super.onPostExecute(result)
        handler?.invoke(result)
    }

}