package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import dagger.Lazy
import kr.ac.snu.hcil.android.common.containers.CachedObjectPoolWithIntegerKey
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.*
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.factories.*

class AttributeViewFactoryManager(private val attributeManager: Lazy<OTAttributeManager>) : CachedObjectPoolWithIntegerKey<AttributeViewFactory<out OTAttributeHelper>>() {

    override fun createNewInstance(key: Int): AttributeViewFactory<out OTAttributeHelper> {
        return when (key) {
            OTAttributeManager.TYPE_TIME -> TimeViewFactory(attributeManager.get().get(key) as OTTimeAttributeHelper)
            OTAttributeManager.TYPE_TIMESPAN -> TimeSpanViewFactory(attributeManager.get().get(key) as OTTimeSpanAttributeHelper)
            OTAttributeManager.TYPE_RATING -> RatingViewFactory(attributeManager.get().get(key) as OTRatingAttributeHelper)
            OTAttributeManager.TYPE_AUDIO -> AudioRecordViewFactory(attributeManager.get().get(key) as OTAudioRecordAttributeHelper)
            OTAttributeManager.TYPE_CHOICE -> ChoiceViewFactory(attributeManager.get().get(key) as OTChoiceAttributeHelper)
            OTAttributeManager.TYPE_IMAGE -> ImageViewFactory(attributeManager.get().get(key) as OTImageAttributeHelper)
            OTAttributeManager.TYPE_LOCATION -> LocationViewFactory(attributeManager.get().get(key) as OTLocationAttributeHelper)
            OTAttributeManager.TYPE_LONG_TEXT -> LongTextViewFactory(attributeManager.get().get(key) as OTLongTextAttributeHelper)
            OTAttributeManager.TYPE_SHORT_TEXT -> ShortTextViewFactory(attributeManager.get().get(key) as OTShortTextAttributeHelper)
            OTAttributeManager.TYPE_NUMBER -> NumberViewFactory(attributeManager.get().get(key) as OTNumberAttributeHelper)
            else -> throw UnsupportedOperationException("This type is not supported in this version.")
        }
    }

}