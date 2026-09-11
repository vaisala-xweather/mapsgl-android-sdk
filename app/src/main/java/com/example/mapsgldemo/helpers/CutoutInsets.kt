package com.example.mapsgldemo.helpers

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * A view to hold clear of the display cutout, and which of its edges to push.
 *
 * Name only the edges the view is actually anchored to. A caption constrained below the back
 * button inherits the button's top offset and needs [top] left off, and writing a margin the
 * layout did not ask for is how a view that something else positions gets moved out from under it.
 */
data class InsetEdges(
    val view: View,
    val top: Boolean = false,
    val start: Boolean = false,
    val end: Boolean = false,
)

/**
 * Lets a demo's map draw into the display cutout, and keeps [chrome] out of it.
 *
 * The map fills the root of every one of these screens, so letting the window extend past the
 * system bars is all it takes to hand it the strip the cutout sits in - on the test device 26dp of
 * full-width map that was otherwise a blank band for a hole 20dp wide. Everything that is not the
 * map then has to be moved back out of that strip, which is what [chrome] is for.
 *
 * Call it from `onCreate` straight after `setContentView`:
 *
 * ```kotlin
 * drawBehindCutout(
 *     binding.root,
 *     InsetEdges(binding.addGeojsonBackButton, top = true, start = true),
 *     InsetEdges(binding.addGeojsonCaption, start = true, end = true),
 * )
 * ```
 *
 * The offsets come from the window insets rather than a measured constant, for two reasons. The
 * strip is a different size on every device, and it does not stay on the top edge: rotate, and the
 * cutout moves to a side, `top` becomes 0 and `left` or `right` does not. These activities declare
 * `configChanges="orientation|screenSize"` so they turn without being recreated, and the listener
 * re-runs on each new set of insets.
 *
 * [WindowInsetsCompat.Type.systemBars] is unioned in because the cutout is not the only thing that
 * can eat an edge - the navigation bar does too, and on a screen whose theme leaves the status bar
 * showing, so does that.
 */
fun Activity.drawBehindCutout(root: View, vararg chrome: InsetEdges) {
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // Margins each view was laid out with, so insets add to them instead of replacing them. Held
    // per call site: the listener re-runs on every rotation, and reading the margins again after
    // the first pass would compound them.
    val baseMargins = HashMap<View, Rect>()

    ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
        val inset = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
        )
        for (target in chrome) {
            val base = baseMargins.getOrPut(target.view) {
                val lp = target.view.layoutParams as ViewGroup.MarginLayoutParams
                Rect(lp.marginStart, lp.topMargin, lp.marginEnd, lp.bottomMargin)
            }
            target.view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (target.top) topMargin = base.top + inset.top
                if (target.start) marginStart = base.left + inset.left
                if (target.end) marginEnd = base.right + inset.right
            }
        }
        // Returned unconsumed: a screen's own listeners - the timeline's navigation-bar padding,
        // say - are downstream of this one and never fire if the insets stop here.
        windowInsets
    }
}
