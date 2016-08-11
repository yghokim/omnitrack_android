package kr.ac.snu.hcil.omnitrack.core.externals.google.fit

import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho on 8/11/2016.
 */

class GoogleFitStepsFactory : GoogleFitService.GoogleFitMeasureFactory<Int>() {

    override val nameResourceId: Int = R.string.measure_googlefit_steps_name

    override val descResourceId: Int = R.string.measure_googlefit_steps_desc

    override val usedAPI: Api<out Api.ApiOptions.NotRequiredOptions> = Fitness.HISTORY_API
    override val usedScope: Scope = Fitness.SCOPE_ACTIVITY_READ

    override fun awaitRequestValue(): Int {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun requestValueAsync(handler: (Int) -> Unit) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}