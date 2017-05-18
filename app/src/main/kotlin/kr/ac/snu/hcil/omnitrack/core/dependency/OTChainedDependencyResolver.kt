package kr.ac.snu.hcil.omnitrack.core.dependency

/**
 * Created by younghokim on 2017. 5. 17..
 */
/*
class OTChainedDependencyResolver(vararg dependencies: OTSystemDependencyResolver): OTSystemDependencyResolver() {

    private val dependencyList = ArrayList<OTSystemDependencyResolver>()
    init{
        dependencyList.addAll(dependencies)
    }

    override fun checkDependencySatisfied(context: Context, selfResolve: Boolean): Single<Boolean> {
        return Single.concat<Boolean>(*(dependencyList.map{dep-> dep.checkDependencySatisfied(context, selfResolve)}))
    }

    override fun tryResolve(context: Context): Single<Boolean> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}*/