package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components

import android.graphics.Canvas
import java.util.*

/**
 * Created by younghokim on 16. 9. 8..
 */
class DataEncodedDrawingList<T>() : ADataEncodedDrawer<T>() {

    private var elements = ArrayList<ADataEncodedDrawer<T>>()
    private var _updateElements = ArrayList<Pair<IndexedValue<T>, ADataEncodedDrawer<T>>>()
    private var _enterData = ArrayList<IndexedValue<T>>()
    private var _exitElements = ArrayList<ADataEncodedDrawer<T>>()

    private var identifier: ((a: T, b: T) -> Boolean)? = null


    override fun onDraw(canvas: Canvas) {
        for (element in elements) {
            element.onDraw(canvas)
        }
    }

    fun setIdentifier(identifierFunc: ((a: T, b: T) -> Boolean)?): DataEncodedDrawingList<T> {
        identifier = identifierFunc

        return this
    }

    fun setData(data: List<T>): DataEncodedDrawingList<T> {

        val original = elements.map { it.datum }.filter { it == null }

        _updateElements.clear()
        _enterData.clear()
        _exitElements.clear()

        for (existingElementEntry in elements.withIndex()) {
            //element exists, data not -> exit
            //element exists, data exists -> update
            var found = false
            for (datum in data.withIndex()) {
                if (
                isPairedData(datum.value, datum.index, existingElementEntry.value.datum!!, existingElementEntry.index)
                ) {
                    //update selection
                    found = true
                    _updateElements.add(Pair(datum, existingElementEntry.value))
                    break
                }
            }

            if (!found) {
                //exit
                _exitElements.add(existingElementEntry.value)
            }
        }

        for (datum in data.withIndex()) {
            var found = false
            for (existingElementEntry in elements.withIndex()) {
                if (
                isPairedData(datum.value, datum.index, existingElementEntry.value.datum!!, existingElementEntry.index)
                ) {
                    //update selection
                    found = true
                    break
                }
            }

            if (!found) {
                _enterData.add(datum)
            }
        }

        println("enter: ${_enterData}")
        println("update: ${_updateElements}")
        println("exit: ${_exitElements}")

        return this
    }

    private fun isPairedData(a: T, aIndex: Int, b: T, bIndex: Int): Boolean {
        return if (identifier != null) {
            identifier?.invoke(a, b) ?: false
        } else {
            aIndex == bIndex
        }
    }

    fun updateElement(updater: (newItem: IndexedValue<T>, element: ADataEncodedDrawer<T>) -> Unit) {
        for (element in _updateElements) {
            updater.invoke(element.first, element.second)
        }
    }

    fun appendEnterSelection(generator: (IndexedValue<T>) -> ADataEncodedDrawer<T>): DataEncodedDrawingList<T> {
        for (enter in _enterData) {
            val newObj = generator(enter)
            newObj.datum = enter.value
            elements.add(enter.index, newObj)
        }

        return this
    }

    fun getExitElements(): Collection<ADataEncodedDrawer<T>> {
        return _exitElements
    }

    fun removeElements(toRemove: Collection<ADataEncodedDrawer<T>>) {
        elements.removeAll(toRemove)
    }

    fun onResizedCanvas(sizeUpdater: (IndexedValue<T>, ADataEncodedDrawer<T>) -> Unit) {
        for (element in elements.withIndex()) {
            sizeUpdater.invoke(IndexedValue(element.index, element.value.datum!!), element.value)
        }
    }

}