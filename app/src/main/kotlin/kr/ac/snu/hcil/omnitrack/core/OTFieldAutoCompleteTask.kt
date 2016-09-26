package kr.ac.snu.hcil.omnitrack.core

/**
 * Created by Young-Ho Kim on 2016-09-26.
 */
/*
class OTFieldAutoCompleteTask(
        val itemBuilder: OTItemBuilder,
        val listener: TaskListener,
        val finishedHandler: ((success: Boolean) -> Unit)?) : AsyncTask<Void?, OTFieldAutoCompleteTask.Progress, Boolean>() {

    open class Progress(val attribute: OTAttribute<out Any>, val position: Int)
    internal class AttributeValueChangedProgress(attribute: OTAttribute<out Any>, position: Int, val value: Any): Progress(attribute, position)
    internal class AttributeStateChangedProgress(attribute: OTAttribute<out Any>, position: Int, val state: OTItemBuilder.EAttributeValueState): Progress(attribute, position)

    interface TaskListener {
        fun onValueRetrieved(attribute: OTAttribute<out Any>, value: Any)
        fun onAttributeStateChanged(attribute: OTAttribute<out Any>, position: Int, state: OTItemBuilder.EAttributeValueState)
    }

    private var remain = 0

    override fun onProgressUpdate(vararg values:Progress) {
        for (value in values) {

            if(value is AttributeValueChangedProgress) {
                listener.onValueRetrieved(value.attribute, value.value)
                listener.onAttributeStateChanged(value.attribute, value.position, OTItemBuilder.EAttributeValueState.Idle)
            }
            else if(value is AttributeStateChangedProgress)
            {
                listener.onAttributeStateChanged(value.attribute, value.position, value.state)
            }
        }
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)

        finishedHandler?.invoke(true)
    }

    override fun onPreExecute() {
        super.onPreExecute()
        remain = itemBuilder.tracker.attributes.size
        if (remain == 0) {
            finishedHandler?.invoke(true)
            cancel(true)
        }
    }

    override fun doInBackground(vararg p0: Void?): Boolean {

        for (attributeEntry in itemBuilder.tracker.attributes.unObservedList.withIndex()) {
            val attribute = attributeEntry.value

            if (attribute.valueConnection == null) {

                publishProgress(AttributeStateChangedProgress(attribute, attributeEntry.index, OTItemBuilder.EAttributeValueState.Processing))
                val isSynchronous = attribute.getAutoCompleteValueAsync {
                    result ->
                    synchronized(remain)
                    {
                        remain--
                        publishProgress(AttributeValueChangedProgress(attribute, attributeEntry.index, result))
                    }
                }

                /*
                if (!isSynchronous) {
                    onAttributeStateChangedListener?.onAttributeStateChanged(attribute, attributeEntry.index, EAttributeValueState.Processing)
                }*/

            } else {
                println("request value connection")

                publishProgress(AttributeStateChangedProgress(attribute, attributeEntry.index, OTItemBuilder.EAttributeValueState.GettingExternalValue))

                attribute.valueConnection?.requestValueAsync(itemBuilder) {
                    value: Any? ->
                    if (value != null) {
                        synchronized(remain) {
                            remain--
                            println("requested value connection value received = $value")
                            publishProgress(AttributeValueChangedProgress(attribute, attributeEntry.index, value))
                        }
                    } else {
                        attribute.getAutoCompleteValueAsync {
                            result ->
                            synchronized(remain) {
                                remain--

                                publishProgress(AttributeValueChangedProgress(attribute, attributeEntry.index, result))
                            }
                        }
                    }
                }
            }
        }

        while(remain>0)
        {
        }

        return true

    }

}*/