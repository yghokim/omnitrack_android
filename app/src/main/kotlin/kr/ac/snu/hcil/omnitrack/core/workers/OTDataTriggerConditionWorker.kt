package kr.ac.snu.hcil.omnitrack.core.workers

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import dagger.internal.Factory
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.di.global.Backend
import javax.inject.Inject

class OTDataTriggerConditionWorker(val context: Context, workerParams: WorkerParameters) : RxWorker(context, workerParams) {

    @field:[Inject Backend]
    lateinit var realmFactory: Factory<Realm>

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }


    override fun createWork(): Single<Result> {
        return Single.defer {
            val realm = realmFactory.get()
            return@defer Single.just(Result.success())
        }
    }

}