package com.example.mapsgldemo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

@SuppressLint("ClickableViewAccessibility")
class LayerFilter(
    context: Context,
    private val allViewsInLayout: MutableList<View>,
    private val itemsContainerLayout: LinearLayout
) {

    val editText: EditText = EditText(context)

    init {

        val density = context.resources.displayMetrics.density

        editText.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            200
        )

        editText.post {
            val parentView = editText.parent as? View
            if (parentView != null) {
                val parentContentWidth = parentView.width

                val currentLayoutParams = editText.layoutParams

                if (1000 > parentContentWidth) {
                    if (currentLayoutParams.width != parentContentWidth) {
                        currentLayoutParams.width = parentContentWidth
                        editText.layoutParams = currentLayoutParams
                    }
                }
                // Otherwise, the initial targetWidthPx is fine as it's less than or equal to parentContentWidth
            }
        }

        editText.maxLines = 1
        editText.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        editText.setBackgroundResource(R.drawable.unselected_background)
        editText.hint = "Search"
        editText.setHintTextColor(Color.rgb(.5f, .5f, .5f))


        // --- Search Icon (Start) ---
        val searchIconDrawable = ContextCompat.getDrawable(context, R.drawable.search_image)
        val iconSizePx = (24 * density).toInt() // Assuming 24dp for both icons
        searchIconDrawable?.setBounds(0, 0, iconSizePx, iconSizePx)

        // Initially, the clear icon is not visible
        val clearIconDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.search_cancel_image)
        clearIconDrawable?.setBounds(0, 0, iconSizePx, iconSizePx)

        // Function to update drawables based on text presence
        fun updateDrawables() {
            val currentText = editText.text.toString()
            if (currentText.isNotEmpty()) {
                // Show search icon on left, clear icon on right
                editText.setCompoundDrawablesRelative(searchIconDrawable, null, clearIconDrawable, null)
            } else {
                editText.setCompoundDrawablesRelative(searchIconDrawable, null, null, null)
            }
        }

        // Initial setup of drawables
        updateDrawables()

        val iconTextPaddingPx = (0 * density).toInt()
        editText.compoundDrawablePadding = iconTextPaddingPx

        val verticalPaddingPx = (12 * density).toInt() // Example: 12dp for top and bottom padding
        // Calculate padding to keep text from overlapping the clear icon too much
        // You might need to adjust start/end padding based on icon sizes and desired spacing
        val defaultStartPadding = (12 * density).toInt()
        val defaultEndPadding = (32 * density).toInt() // Move "x" button to left in case of camera cutout
        editText.setPadding(
            defaultStartPadding,
            verticalPaddingPx, // Apply consistent vertical padding
            defaultEndPadding + iconSizePx + iconTextPaddingPx, // Space for the clear icon
            verticalPaddingPx  // Apply consistent vertical padding
        )

        // --- TextWatcher to show/hide clear icon and call onTextChanged ---
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentText = s?.toString() ?: ""

                if (currentText.trim().isEmpty()) {
                    addAllViews(itemsContainerLayout)
                } else {
                    filterViews(currentText)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                updateDrawables() // Update visibility of clear icon
            }
        })

        editText.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Check if the touch event is within the bounds of the end drawable (clear icon)
                // The clear icon is an "end" drawable.
                // editText.compoundDrawablesRelative[2] is the end drawable.
                val endDrawable = editText.compoundDrawablesRelative[2]
                if (endDrawable != null) {
                    // Check if the touch was on the drawable
                    // For LTR layouts, drawable is on the right.
                    // For RTL, it would be on the left if it were a "start" drawable,
                    // but since it's "end", it's always on the logical end.
                    val drawableEndBounds = editText.width - editText.paddingEnd
                    val drawableStartBounds = drawableEndBounds - endDrawable.intrinsicWidth

                    // A more robust way considering RTL:
                    var clearIconClicked = false
                    if (editText.layoutDirection == EditText.LAYOUT_DIRECTION_RTL) {
                        // In RTL, the "end" drawable is on the left side of the text area
                        if (event.rawX <= editText.left + editText.paddingStart + endDrawable.intrinsicWidth) {
                            clearIconClicked = true
                        }
                    } else {
                        // In LTR, the "end" drawable is on the right side of the text area
                        if (event.rawX >= editText.right - editText.paddingEnd - endDrawable.intrinsicWidth) {
                            clearIconClicked = true
                        }
                    }

                    if (clearIconClicked) {
                        editText.text.clear() // Clear the text
                        // updateDrawables() will be called by afterTextChanged
                        view.performClick() // Optional: Handle accessibility, though clearing text is the primary action
                        return@setOnTouchListener true // Consume the event
                    }
                }
            }
            false // Do not consume other touch events
        }
    }

    fun addAllViews(itemsContainerLayout: LinearLayout) {
        itemsContainerLayout.removeAllViews()

        for (customView in allViewsInLayout) {
            val viewToAdd = if (customView is LayerButtonView) {
                customView.outerView
            } else {
                customView
            }

            val parent = viewToAdd.parent as? ViewGroup
            parent?.removeView(viewToAdd)
            itemsContainerLayout.addView(viewToAdd)
        }
    }

    fun filterViews(filterText: String) {
        itemsContainerLayout.removeAllViews()
        val filterTextLower = filterText.lowercase()

        for (customView in allViewsInLayout) {
            if (customView is LayerButtonView) { // Layer button
                if (customView.text.lowercase().contains(filterTextLower) || customView.code.lowercase()
                        .contains(filterTextLower)
                ) {
                    itemsContainerLayout.addView(customView.outerView)
                }
            }
        }
    }
}