package kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields

import dagger.Lazy
import kr.ac.snu.hcil.android.common.containers.CachedObjectPoolWithIntegerKey
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.*
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.factories.*

class OTFieldViewFactoryManager(private val fieldManager: Lazy<OTFieldManager>) : CachedObjectPoolWithIntegerKey<OTFieldViewFactory<out OTFieldHelper>>() {

    override fun createNewInstance(key: Int): OTFieldViewFactory<out OTFieldHelper> {
        return when (key) {
            OTFieldManager.TYPE_TIME -> TimeViewFactory(fieldManager.get().get(key) as OTTimeFieldHelper)
            OTFieldManager.TYPE_TIMESPAN -> TimeSpanViewFactory(fieldManager.get().get(key) as OTTimeSpanFieldHelper)
            OTFieldManager.TYPE_RATING -> RatingViewFactory(fieldManager.get().get(key) as OTRatingFieldHelper)
            OTFieldManager.TYPE_AUDIO -> AudioRecordViewFactory(fieldManager.get().get(key) as OTAudioRecordFieldHelper)
            OTFieldManager.TYPE_CHOICE -> ChoiceViewFactory(fieldManager.get().get(key) as OTChoiceFieldHelper)
            OTFieldManager.TYPE_IMAGE -> ImageViewFactory(fieldManager.get().get(key) as OTImageFieldHelper)
            OTFieldManager.TYPE_LOCATION -> LocationViewFactory(fieldManager.get().get(key) as OTLocationFieldHelper)
            OTFieldManager.TYPE_LONG_TEXT -> LongTextViewFactory(fieldManager.get().get(key) as OTLongTextFieldHelper)
            OTFieldManager.TYPE_SHORT_TEXT -> ShortTextViewFactory(fieldManager.get().get(key) as OTShortTextFieldHelper)
            OTFieldManager.TYPE_NUMBER -> NumberViewFactory(fieldManager.get().get(key) as OTNumberFieldHelper)
            else -> throw UnsupportedOperationException("This type is not supported in this version.")
        }
    }

}