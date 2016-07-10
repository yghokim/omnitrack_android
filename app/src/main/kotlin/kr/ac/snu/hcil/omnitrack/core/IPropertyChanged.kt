package kr.ac.snu.hcil.omnitrack.core

/**
 * Created by Young-Ho on 7/11/2016.
 */
interface IPropertyChanged {

    fun onPropertyChanged(propertyName: String, to: Any): Unit
}