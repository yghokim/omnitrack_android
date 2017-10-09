package kr.ac.snu.hcil.omnitrack.core.externals.jawbone

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jawbone.upplatformsdk.api.ApiManager
import com.jawbone.upplatformsdk.utils.UpPlatformSdkConstants
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response
import rx.Observable
import java.util.*

/**
 * Created by Young-Ho Kim on 2017-01-25.
 */
abstract class AJawboneMoveMeasure : OTMeasureFactory.OTRangeQueriedMeasure {

    constructor() : super()
    constructor(serialized: String) : super(serialized)

    override fun getValueRequest(start: Long, end: Long): Observable<Nullable<out Any>> {
        if (TimeHelper.isSameDay(start, end - 10)) {
            return Observable.create<Nullable<out Any>> {
                subscriber ->
                ApiManager.getRestApiInterface().getMoveEventsList(UpPlatformSdkConstants.API_VERSION_STRING, HashMap<String, Long>().apply { this["date"] = JawboneUpService.makeFormattedDateInteger(start).toLong() }, object : Callback<Any> {
                    override fun failure(error: RetrofitError) {
                        if (!subscriber.isUnsubscribed) {
                            subscriber.onError(error)
                        }
                    }

                    override fun success(result: Any, response: Response) {

                        if (!subscriber.isUnsubscribed) {
                            subscriber.onNext(Nullable(extractValueFromResult(result)))
                            subscriber.onCompleted()
                        }
                    }
                })
            }
        } else {
            return Observable.create<Nullable<out Any>> {
                subscriber ->

                Observable.zip(TimeHelper.sliceToDate(start, end).map {
                    Observable.create<Float> {
                        subscriber ->
                        ApiManager.getRestApiInterface().getMoveEventsList(UpPlatformSdkConstants.API_VERSION_STRING, JawboneUpService.makeIntraDayRequestQueryParams(it.first, it.second, null), object : Callback<Any> {
                            override fun failure(error: RetrofitError) {
                                if (!subscriber.isUnsubscribed) {
                                    subscriber.onError(error)
                                }
                            }

                            override fun success(result: Any, response: Response) {
                                if (!subscriber.isUnsubscribed) {
                                    subscriber.onNext(extractValueFromResult(result))
                                    subscriber.onCompleted()
                                }
                            }
                        })
                    }
                }, { values ->
                    var sum = 0.0f
                    for (value in values) {
                        if (value is Float) {
                            sum += value
                        }
                    }
                    sum
                }).subscribe({
                    result ->
                    if (!subscriber.isUnsubscribed) {
                        subscriber.onNext(Nullable(result))
                        subscriber.onCompleted()
                    }
                }, {
                    error ->
                    subscriber.onError(error)
                })
            }
        }
    }

    protected fun extractValueFromResult(result: Any): Float {
        try {
            val json = Gson().toJsonTree(result).asJsonObject
            val items = json.getAsJsonObject("data").getAsJsonArray("items")
            return items
                    .map { it -> extractValueFromItem(it.asJsonObject) }
                    .sum()

        } catch(exception: Exception) {
            exception.printStackTrace()
            return 0f
        }
    }

    protected abstract fun extractValueFromItem(item: JsonObject): Float
}