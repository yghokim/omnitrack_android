package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.database.DataSetObserver
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.dipRound
import org.jetbrains.anko.dip
import kotlin.properties.Delegates


/**
 * Created by Young-Ho Kim on 2017-01-17.
 */
class ExtendedSpinner : LinearLayout, View.OnClickListener {

    interface OnItemSelectedListener {
        fun onItemSelected(spinner: ExtendedSpinner, position: Int)
    }

    private var arrowView: AppCompatImageView? = null
    private var showArrow: Boolean = true

    private var itemView: View? = null

    private val unSelectedIndicatorItemView: AppCompatTextView

    private var adapterItemMaxWidth: Int = 0
    private var adapterItemMaxDropdownWidth: Int = 0

    var onItemSelectedListener: OnItemSelectedListener? = null

    private var suspendPositionChangedEvent: Boolean = false
    private val mTempRect = Rect()
    private var mWidthMeasureSpec: Int = MeasureSpec.UNSPECIFIED

    private val popup: SpinnerPopup

    var selectedItemPosition: Int by Delegates.observable(-1) {
        prop, old, new ->
        if (old != new && !suspendPositionChangedEvent)
            onSelectedPositionChanged(new)
    }

    val selectedItem: Any?
        get() {
            if (selectedItemPosition == -1) {
                return null
            } else {
                return adapter?.getItem(selectedItemPosition)
            }
        }

    var adapter: SpinnerAdapter? by Delegates.observable(null as SpinnerAdapter?) {
        prop, old, new ->
        if (old != new) {
            onAdapterChanged(new)
        }
    }

    init {
        arrowView = AppCompatImageView(context).apply {
            this.scaleType = ImageView.ScaleType.FIT_XY
            this.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                this.gravity = Gravity.CENTER_VERTICAL
                this.marginStart = dip(4)
            }
        }

        unSelectedIndicatorItemView = AppCompatTextView(context).apply {
            this.setText(R.string.msg_spinner_tap_to_select)
        }

        InterfaceHelper.setTextAppearance(unSelectedIndicatorItemView, R.style.TextAppearance_AppCompat_Light_Widget_PopupMenu_Small)

        addView(unSelectedIndicatorItemView)

        addView(arrowView)

        setOnClickListener(this)
    }

    constructor(context: Context) : super(context) {
        arrowView?.setImageResource(R.drawable.down)
        popup = DropdownPopup(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
        popup = DropdownPopup(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr)
        popup = DropdownPopup(context, attrs, defStyleAttr, 0)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) {

        val a = context.obtainStyledAttributes(attrs, R.styleable.ExtendedSpinner, defStyleAttr, 0)

        try {
            arrowView?.setImageResource(a.getResourceId(R.styleable.ExtendedSpinner_arrowResource, R.drawable.down))
            arrowView?.setColorFilter(a.getColor(R.styleable.ExtendedSpinner_arrowTint, ContextCompat.getColor(context, R.color.buttonIconColorDark)))

        } finally {
            a.recycle()
        }
    }

    fun <T> setItems(vararg items: T) {
        adapter = ArrayAdapter(context, R.layout.simple_text_element, R.id.textView, items).apply {
            this.setDropDownViewResource(R.layout.simple_list_element_text_dropdown)
        }

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (popup.isShowing()) {
            popup.dismiss()
        }
    }


    override fun onClick(p0: View?) {
        if (p0 == this) {
            if (!popup.isShowing()) {
                popup.show(textDirection, textAlignment)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val mode = MeasureSpec.getMode(widthMeasureSpec)
        if (mWidthMeasureSpec != mode) {
            mWidthMeasureSpec = mode
            calculateViewWidth()
        }
    }

    private fun onAdapterChanged(newAdapter: SpinnerAdapter?) {
        popup.setAdapter(DropDownAdapter(newAdapter))
        if (newAdapter != null) {
            if (newAdapter.count > 0) {
                calculateViewWidth()
                selectedItemPosition = 0
            } else {
                this.removeViewAt(0)
            }
        } else {
            this.removeViewAt(0)
        }
    }

    private fun calcMaximumItemWidth(adapter: SpinnerAdapter): Int {
        var width = 0
        var itemView: View? = null
        var itemType = 0
        val widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        // Make sure the number of items we'll measure is capped. If it's a huge data set
        // with wildly varying sizes, oh well.
        var start = Math.max(0, selectedItemPosition)
        val end = Math.min(adapter.count, start + 15)
        val count = end - start
        start = Math.max(0, start - (15 - count))
        for (i in start..end - 1) {
            val positionType = adapter.getItemViewType(i)
            if (positionType != itemType) {
                itemType = positionType
                itemView = null
            }
            itemView = adapter.getView(i, itemView, this)
            if (itemView.layoutParams == null) {
                itemView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            itemView.measure(widthMeasureSpec, heightMeasureSpec)
            width = Math.max(width, itemView.measuredWidth)
        }
        return width
    }

    private fun calculateViewWidth() {
        val adapter = adapter
        if (adapter != null) {
            adapterItemMaxWidth = calcMaximumItemWidth(adapter)
            if (layoutParams.width == LayoutParams.WRAP_CONTENT) {
                itemView?.minimumWidth = adapterItemMaxWidth
            } else {
                itemView?.layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT).apply { this.weight = 1f; this.gravity = Gravity.CENTER_VERTICAL }
                itemView?.minimumWidth = 0
            }
        }
    }

    private fun onSelectedPositionChanged(newPosition: Int) {

        if (newPosition == -1) {
            var lp = unSelectedIndicatorItemView.layoutParams
            if (!(lp is LinearLayout.LayoutParams)) {
                lp = LinearLayout.LayoutParams(lp)
            }

            val margin = dipRound(6)
            lp.topMargin = margin
            lp.marginStart = margin
            lp.bottomMargin = margin

            lp.gravity = Gravity.CENTER_VERTICAL

            if (layoutParams.width == LayoutParams.WRAP_CONTENT) {
                lp.width = LayoutParams.WRAP_CONTENT
                lp.weight = 0f
            } else {
                lp.weight = 1f
                lp.width = 0
            }
            unSelectedIndicatorItemView.layoutParams = lp
            unSelectedIndicatorItemView.visibility = VISIBLE
            unSelectedIndicatorItemView.setTextColor(ContextCompat.getColor(context, R.color.colorRed))

            itemView?.visibility = GONE
        } else {
            unSelectedIndicatorItemView.visibility = GONE
            itemView?.visibility = VISIBLE

            itemView = adapter?.getView(newPosition, itemView, this)
            if (this.indexOfChild(itemView) == -1) {
                var lp = itemView?.layoutParams
                if (lp is LinearLayout.LayoutParams) {
                    lp.gravity = Gravity.CENTER_VERTICAL
                } else {
                    lp = LinearLayout.LayoutParams(lp)
                    lp.gravity = Gravity.CENTER_VERTICAL
                    itemView?.layoutParams = lp
                }
                this.addView(itemView, 0)
            }
        }

        onItemSelectedListener?.onItemSelected(this, newPosition)
    }

    fun measureContentWidth(adapter: SpinnerAdapter?, background: Drawable?): Int {

        if (adapter == null) {
            return 0
        }

        var width = calcMaximumItemWidth(adapter)

        // Add background padding to measured width
        if (background != null) {
            background.getPadding(mTempRect)
            width += mTempRect.left + mTempRect.right
        }

        return width
    }

    /**
     *
     * Wrapper class for an Adapter. Transforms the embedded Adapter instance
     * into a ListAdapter.
     */
    private class DropDownAdapter
    /**
     *
     * Creates a new ListAdapter wrapper for the specified adapter.

     * @param adapter the Adapter to transform into a ListAdapter
     */
    (private val mAdapter: SpinnerAdapter?) : ListAdapter, SpinnerAdapter {
        private var mListAdapter: ListAdapter? = null

        init {
            if (mAdapter is ListAdapter) {
                this.mListAdapter = mAdapter
            }
        }

        override fun getCount(): Int {
            return mAdapter?.count ?: 0
        }

        override fun getItem(position: Int): Any? {
            return mAdapter?.getItem(position)
        }

        override fun getItemId(position: Int): Long {
            return mAdapter?.getItemId(position) ?: -1
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            return getDropDownView(position, convertView, parent)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            return mAdapter?.getDropDownView(position, convertView, parent)
        }

        override fun hasStableIds(): Boolean {
            return mAdapter != null && mAdapter.hasStableIds()
        }

        override fun registerDataSetObserver(observer: DataSetObserver) {
            mAdapter?.registerDataSetObserver(observer)
        }

        override fun unregisterDataSetObserver(observer: DataSetObserver) {
            mAdapter?.unregisterDataSetObserver(observer)
        }

        /**
         * If the wrapped SpinnerAdapter is also a ListAdapter, delegate this call.
         * Otherwise, return true.
         */
        override fun areAllItemsEnabled(): Boolean {
            val adapter = mListAdapter
            if (adapter != null) {
                return adapter.areAllItemsEnabled()
            } else {
                return true
            }
        }

        /**
         * If the wrapped SpinnerAdapter is also a ListAdapter, delegate this call.
         * Otherwise, return true.
         */
        override fun isEnabled(position: Int): Boolean {
            val adapter = mListAdapter
            if (adapter != null) {
                return adapter.isEnabled(position)
            } else {
                return true
            }
        }

        override fun getItemViewType(position: Int): Int {
            return 0
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun isEmpty(): Boolean {
            return count == 0
        }
    }

    private interface SpinnerPopup {
        fun setAdapter(adapter: ListAdapter)

        /**
         * Show the popup
         */
        fun show(textDirection: Int, textAlignment: Int)

        /**
         * Dismiss the popup
         */
        fun dismiss()

        /**
         * @return true if the popup is showing, false otherwise.
         */
        fun isShowing(): Boolean

        /**
         * Set hint text to be displayed to the user. This should provide
         * a description of the choice being made.
         * @param hintText Hint text to set.
         */
        fun setPromptText(hintText: CharSequence)

        fun getHintText(): CharSequence?

        fun setBackgroundDrawable(bg: Drawable?)
        fun getBackground(): Drawable?
        fun getVerticalOffset(): Int
        fun getHorizontalOffset(): Int
    }

    private inner class DropdownPopup(
            context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : ListPopupWindow(context, attrs, defStyleAttr, defStyleRes), SpinnerPopup {

        private var mHintText: CharSequence? = null

        override fun getHintText(): CharSequence? {
            return mHintText
        }

        private var mAdapter: ListAdapter? = null

        init {
            anchorView = this@ExtendedSpinner
            isModal = true
            promptPosition = POSITION_PROMPT_ABOVE
            setOnItemClickListener { parent, v, position, id ->
                this@ExtendedSpinner.selectedItemPosition = position
                dismiss()
            }

            this.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.white_panel_with_shadow))
        }

        override fun setAdapter(adapter: ListAdapter) {
            super.setAdapter(adapter)
            mAdapter = adapter
        }

        override fun setPromptText(hintText: CharSequence) {
            // Hint text is ignored for dropdowns, but maintain it here.
            this.mHintText = hintText
        }

        internal fun computeContentWidth() {
            val background = background
            var hOffset = 0
            if (background != null) {
                background.getPadding(mTempRect)
                hOffset = -mTempRect.left
            } else {
                mTempRect.right = 0
                mTempRect.left = mTempRect.right
            }

            val spinnerPaddingLeft = this@ExtendedSpinner.paddingLeft
            val spinnerPaddingRight = this@ExtendedSpinner.paddingRight
            val spinnerWidth = this@ExtendedSpinner.width

            /*
            if (mDropDownWidth === WRAP_CONTENT) {
                var contentWidth = measureContentWidth(
                        mAdapter as SpinnerAdapter?, getBackground())
                val contentWidthLimit = mContext.getResources()
                        .getDisplayMetrics().widthPixels - mTempRect.left - mTempRect.right
                if (contentWidth > contentWidthLimit) {
                    contentWidth = contentWidthLimit
                }
                setContentWidth(Math.max(
                        contentWidth, spinnerWidth - spinnerPaddingLeft - spinnerPaddingRight))
            } else if (mDropDownWidth === MATCH_PARENT) {
                setContentWidth(spinnerWidth - spinnerPaddingLeft - spinnerPaddingRight)
            } else {*/
            //setContentWidth(adapterItemMaxWidth)
            //}

            var contentWidth = measureContentWidth(
                    mAdapter as SpinnerAdapter?, getBackground())
            val contentWidthLimit = context.resources.displayMetrics.widthPixels - mTempRect.left - mTempRect.right
            if (contentWidth > contentWidthLimit) {
                contentWidth = contentWidthLimit
            }
            setContentWidth(Math.max(
                    contentWidth, spinnerWidth - spinnerPaddingLeft - spinnerPaddingRight))

            //hOffset += spinnerPaddingLeft

            horizontalOffset = hOffset
        }

        override fun show(textDirection: Int, textAlignment: Int) {
            val wasShowing = isShowing

            computeContentWidth()

            inputMethodMode = ListPopupWindow.INPUT_METHOD_NOT_NEEDED
            super.show()
            val listView = listView
            listView.choiceMode = ListView.CHOICE_MODE_SINGLE
            listView.textDirection = textDirection
            listView.textAlignment = textAlignment
            setSelection(this@ExtendedSpinner.selectedItemPosition)

            if (wasShowing) {
                // Skip setting up the layout/dismiss listener below. If we were previously
                // showing it will still stick around.
                return
            }

            // Make sure we hide if our anchor goes away.
            // TODO: This might be appropriate to push all the way down to PopupWindow,
            // but it may have other side effects to investigate first. (Text editing handles, etc.)
            val vto = viewTreeObserver
            if (vto != null) {
                val layoutListener = OnGlobalLayoutListener {
                    //if (!this@ExtendedSpinner.isVisibleToUser()) {
                    //    dismiss()
                    //} else {
                    computeContentWidth()

                    // Use super.show here to update; we don't want to move the selected
                    // position or adjust other things that would be reset otherwise.
                    super@DropdownPopup.show()
                    //}
                }
                vto.addOnGlobalLayoutListener(layoutListener)
                setOnDismissListener {
                    val vto = viewTreeObserver
                    vto.removeOnGlobalLayoutListener(layoutListener)
                }
            }
        }
    }
}