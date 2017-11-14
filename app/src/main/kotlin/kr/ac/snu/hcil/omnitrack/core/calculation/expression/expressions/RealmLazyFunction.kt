package kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions

import com.udojava.evalex.Expression
import dagger.Lazy
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import javax.inject.Inject
import javax.inject.Provider

/**
 * Created by younghokim on 2017. 11. 14..
 */
abstract class RealmLazyFunction : Expression.LazyFunction {

    @Inject lateinit var dbManager: Lazy<RealmDatabaseManager>
    @Inject lateinit var realmProvider: Provider<Realm>

    constructor(app: OTApp, name: String?, numParams: Int, booleanFunction: Boolean) : super(name, numParams, booleanFunction) {
        app.applicationComponent.inject(this)
    }

    constructor(app: OTApp, name: String?, numParams: Int) : super(name, numParams) {
        app.applicationComponent.inject(this)
    }

}