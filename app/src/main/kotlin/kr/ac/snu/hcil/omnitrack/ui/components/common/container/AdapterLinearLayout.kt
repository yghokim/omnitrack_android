package kr.ac.snu.hcil.omnitrack.ui.components.common.container

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.database.DataSetObserver
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.transition.TransitionManager
import com.jmedeisis.draglinearlayout.DragLinearLayout
import kotlinx.android.synthetic.main.attribute_list_element.view.*
import kr.ac.snu.hcil.omnitrack.R
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by younghokim on 2017. 4. 9..
 */
class AdapterLinearLayout : DragLinearLayout {


    open class AViewHolder(val itemView: View) {
        var viewType: Int = -1
            internal set
        var adapterPosition: Int = -1
            internal set

        init {
            itemView.tag = this
        }
    }

    abstract class ViewHolderAdapter<T : AViewHolder> : android.widget.Adapter, ListUpdateCallback {

        interface Listener {
            fun onItemAdded(position: Int)
            fun onItemRemove(position: Int)
            fun onItemChanged(position: Int)
            fun onItemMoved(from: Int, to: Int)
            fun onDataSetChanged()
        }

        //private val observers = HashSet<DataSetObserver>()

        private val listeners = HashSet<Listener>()

        fun createViewHolder(parent: ViewGroup, viewType: Int): T {
            val vh = onCreateViewHolder(parent, viewType)
            vh.viewType = viewType
            return vh
        }

        fun bindViewHolder(viewHolder: T, position: Int) {
            viewHolder.adapterPosition = position
            onBindViewHolder(viewHolder, position)
        }

        //=====================================================================
        override fun onChanged(position: Int, count: Int, payload: Any?) {
            for (i in position until position + count) {
                notifyItemChanged(i)
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            notifyItemMoved(fromPosition, toPosition)
        }

        override fun onInserted(position: Int, count: Int) {
            for (i in position until position + count) {
                notifyItemInserted(i)
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            for (i in position until position + count) {
                notifyItemRemoved(i)
            }
        }
        //=====================================================================

        abstract fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T


        abstract fun onBindViewHolder(viewHolder: T, position: Int)


        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            if (convertView != null) {
                val viewHolder = convertView.tag as? T
                if (viewHolder != null) {
                    bindViewHolder(viewHolder, position)
                    return convertView
                }
            }

            val viewHolder = createViewHolder(parent, getItemViewType(position))
            bindViewHolder(viewHolder, position)
            return viewHolder.itemView
        }


        fun notifyDataSetChanged() {
            for (listener in listeners) {
                listener.onDataSetChanged()
            }
        }

        fun notifyItemInserted(position: Int) {
            for (listener in listeners) {
                listener.onItemAdded(position)
            }
        }

        fun notifyItemRemoved(position: Int) {
            for (listener in listeners) {
                listener.onItemRemove(position)
            }
        }

        fun notifyItemMoved(fromPosition: Int, toPosition: Int) {
            for (listener in listeners) {
                listener.onItemMoved(fromPosition, toPosition)
            }
        }

        fun notifyItemChanged(position: Int) {
            for (listener in listeners) {
                listener.onItemChanged(position)
            }
        }

        override fun registerDataSetObserver(observer: DataSetObserver) {
            //observers.add(observer)
        }

        override fun unregisterDataSetObserver(observer: DataSetObserver) {
            //observers.remove(observer)
        }

        fun registerListener(listener: Listener) {
            listeners.add(listener)
        }

        fun unregisterListener(listener: Listener) {
            listeners.remove(listener)
        }

    }

    var adapter: ViewHolderAdapter<AViewHolder>? = null
        set(value) {
            if (field !== value) {
                field?.unregisterListener(dataSetListener)

                value?.registerListener(dataSetListener)

                field = value
                refreshEmptyView()
                refreshAllViews()
            }
        }

    private val dataSetListener = object : ViewHolderAdapter.Listener {
        override fun onItemChanged(position: Int) {
            adapter?.let {
                adapter ->
                val viewType = adapter.getItemViewType(position)
                val viewHolder = getChildAt(position)?.tag as? AViewHolder

                if (viewHolder != null) {
                    if (viewHolder.viewType == viewType) {
                        adapter.bindViewHolder(viewHolder, position)
                    } else {
                        pushNewViewHolder(viewHolder)
                        removeDragViewInLayout(viewHolder.itemView)
                        val view = makeView(position)
                        view.id = View.generateViewId()
                        addViewInLayout(view, position, view.layoutParams)
                        setViewDraggable(view, view.ui_drag_handle)
                        requestLayout()
                    }
                }
            }
        }

        override fun onItemMoved(from: Int, to: Int) {

        }

        override fun onItemAdded(position: Int) {

            println("item added")

            adapter?.let {
                adapter ->

                for (i in position until childCount) {
                    val vh = getChildAt(i)?.tag as? AViewHolder
                    if (vh != null) {
                        vh.adapterPosition++
                    }
                }

                val view = makeView(position)
                view.id = View.generateViewId()
                TransitionManager.beginDelayedTransition(this@AdapterLinearLayout)
                addDragView(view, view.findViewById(R.id.ui_drag_handle), position)

            }

            refreshEmptyView()
        }

        override fun onItemRemove(position: Int) {
            adapter?.let {
                adapter ->

                for (i in position + 1 until childCount) {
                    val vh = getChildAt(i)?.tag as? AViewHolder
                    if (vh != null) {
                        vh.adapterPosition--
                    }
                }

                val viewHolder = getChildAt(position)?.tag as? AViewHolder
                if (viewHolder != null) {
                    pushNewViewHolder(viewHolder)
                    removalAnimatedChild = viewHolder.itemView
                    removalAnimator.start()
                    //removeView(viewHolder.itemView)
                    println("remove")
                }

            }
            refreshEmptyView()
        }

        override fun onDataSetChanged() {
            println("all view refreshed")
            refreshAllViews()
            refreshEmptyView()
        }
    }

    var emptyView: View? = null

    var viewPool = HashMap<Int, ArrayDeque<AViewHolder>>()

    private var removalAnimatedChild: View? = null
    private var removalAnimatedChildFullHeight: Float = 0f

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    //constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    //constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
    }

    private fun refreshEmptyView() {
        if (adapter != null) {
            if (adapter?.count == 0) {
                emptyView?.visibility = View.VISIBLE
                this@AdapterLinearLayout.visibility = View.GONE
            } else {

                emptyView?.visibility = View.GONE
                this@AdapterLinearLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun pushNewViewHolder(viewHolder: AViewHolder) {
        if (!viewPool.containsKey(viewHolder.viewType)) {
            viewPool.set(viewHolder.viewType, ArrayDeque())
        }
        viewPool[viewHolder.viewType]?.push(viewHolder)
    }

    private fun makeView(position: Int): View {

        adapter?.let {
            adapter ->
            val viewType = adapter.getItemViewType(position)
            val currentView = getChildAt(position)
            if (currentView != null) {
                val currentTag = currentView.tag
                if (currentTag is AViewHolder) {
                    if (currentTag.viewType == viewType) {
                        return adapter.getView(position, currentView, this)
                    } else {
                        val newViewHolder = viewPool[viewType]?.removeFirst()
                        if (newViewHolder != null) {
                            return adapter.getView(position, newViewHolder.itemView, this)
                        } else {
                            return adapter.getView(position, null, this)
                        }
                    }
                } else {
                    return adapter.getView(position, null, this)
                }
            } else return adapter.getView(position, getChildAt(position), this)
        } ?: throw Exception("adapter is not assigned.")
    }

    fun refreshAllViews() {

        //shrink
        for (i in 0 until this.childCount) {
            val removed = getChildAt(i)

            val tag = removed.tag
            if (tag is AViewHolder) {
                pushNewViewHolder(tag)
            }
        }

        removeAllViewsInLayout()

        adapter?.let {
            adapter ->
            for (i in 0 until adapter.count) {
                val view = makeView(i)

                if (this.indexOfChild(view) == -1) {
                    addViewInLayout(view, i, view.layoutParams)
                    setViewDraggable(view, view.ui_drag_handle)
                }
                //addView(view)
                //addView(viewHolder.itemView)

                requestLayout()

                println("refresh finished. child count: $childCount, adapter count: ${adapter.count}")
            }
        }
    }

    override fun onSwap(firstView: View, firstPosition: Int, secondView: View, secondPosition: Int) {
        val firstViewHolder = firstView.tag
        val secondViewHolder = secondView.tag

        if (firstViewHolder is AViewHolder && secondViewHolder is AViewHolder) {
            firstViewHolder.adapterPosition = secondPosition
            secondViewHolder.adapterPosition = firstPosition
        }
        super.onSwap(firstView, firstPosition, secondView, secondPosition)
    }

    fun setViewIntervalDistance(distance: Int) {
        val dividerDrawable = ColorDrawable(Color.TRANSPARENT)
        dividerDrawable.setBounds(0, 0, 100, distance)
        setDividerDrawable(dividerDrawable)
        showDividers = SHOW_DIVIDER_MIDDLE
    }

    override fun onSaveInstanceState(): Parcelable {
        val parcelable = super.onSaveInstanceState()
        return SavedState(parcelable)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
        }
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>) {
        dispatchFreezeSelfOnly(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>) {
        dispatchThawSelfOnly(container)
    }

    class SavedState : BaseSavedState {


        companion object {
            @JvmField val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }

        constructor(source: Parcel?) : super(source)

        @TargetApi(24)
        constructor(source: Parcel?, loader: ClassLoader?) : super(source, loader)


        constructor(superState: Parcelable?) : super(superState)

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (removalAnimatedChild != null) {
            removeView(removalAnimatedChild)
            removalAnimatedChild = null
        }
    }


    private val removalAnimator: Animator by lazy {
        val removedObjectDisappearAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
        }
        removedObjectDisappearAnimator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                val maxTransition = -(removalAnimatedChild?.measuredWidth ?: 0)
                val ratio = animation.animatedValue as Float
                removalAnimatedChild?.translationX = maxTransition * ratio
            }

        })

        val removedObjectHeightShrinkAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 250
            interpolator = AccelerateInterpolator()
        }
        removedObjectHeightShrinkAnimator.addUpdateListener {
            animation ->
            removalAnimatedChild?.layoutParams?.height = (removalAnimatedChildFullHeight * (animation.animatedValue as Float) + 0.5f).toInt()
            removalAnimatedChild?.requestLayout()
        }


        val animator = AnimatorSet()
        animator.playSequentially(removedObjectDisappearAnimator, removedObjectHeightShrinkAnimator)

        var originalHeight: Int = LayoutParams.WRAP_CONTENT

        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                removeChild()
            }

            override fun onAnimationCancel(animation: Animator) {
                removeChild()
            }

            override fun onAnimationStart(animation: Animator) {
                originalHeight = removalAnimatedChild?.layoutParams?.height ?: LayoutParams.WRAP_CONTENT
                removalAnimatedChildFullHeight = removalAnimatedChild?.height?.toFloat() ?: 0f
            }

            fun removeChild() {
                removalAnimatedChild?.let {
                    it.translationX = 0f
                    it.layoutParams.height = originalHeight
                    this@AdapterLinearLayout.removeView(it)
                }
                removalAnimatedChild = null
            }

        })

        animator
    }


}