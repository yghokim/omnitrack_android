package kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions

import com.udojava.evalex.Expression
import dagger.Lazy
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.di.Backend
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 11. 14..
 */
abstract class RealmLazyFunction : Expression.LazyFunction {

    @Inject lateinit var dbManager: Lazy<BackendDbManager>
    @field:[Inject Backend] lateinit var realmProvider: Factory<Realm>

    constructor(app: OTApp, name: String?, numParams: Int, booleanFunction: Boolean) : super(name, numParams, booleanFunction) {
        app.applicationComponent.inject(this)
    }

    constructor(app: OTApp, name: String?, numParams: Int) : super(name, numParams) {
        app.applicationComponent.inject(this)
    }

}