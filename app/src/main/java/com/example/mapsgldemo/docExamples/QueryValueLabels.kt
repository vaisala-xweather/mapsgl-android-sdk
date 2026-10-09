package com.example.mapsgldemo.docExamples

import androidx.compose.ui.graphics.Color
import com.xweather.mapsgl.style.Expression
import com.xweather.mapsgl.style.StyleValue
import com.xweather.mapsgl.style.SymbolLayerPaint
import com.xweather.mapsgl.style.TextPaint
import com.xweather.mapsgl.types.Anchor
import com.xweather.mapsgl.types.AnchorOffset

/**
 * Value over place name, the label stack a data-query layer publishes.
 *
 * The coordinator writes the formatted sample to `value` and the point's [label] property to
 * `name`. The number follows the map's units, so a later `setUnits` relabels these without a
 * custom formatter.
 */
internal fun queryValueLabels(): SymbolLayerPaint = SymbolLayerPaint(
    pitchWithMap = false,
    rotateWithMap = false,
    text = listOf(
        TextPaint(
            allowOverlap = StyleValue.Constant(true),
            value = StyleValue.Expression(Expression.get("value")),
            size = StyleValue.Constant(18.0),
            weight = StyleValue.Constant("bold"),
            color = StyleValue.Constant(Color.Black),
            outlineColor = StyleValue.Constant(Color.White),
        ),
        TextPaint(
            allowOverlap = StyleValue.Constant(true),
            value = StyleValue.Expression(Expression.get("name")),
            size = StyleValue.Constant(12.0),
            color = StyleValue.Constant(Color.Black),
            outlineColor = StyleValue.Constant(Color.White),
            anchor = StyleValue.Constant(Anchor.TOP),
            offset = StyleValue.Constant(AnchorOffset(0.0, 1.1)),
        ),
    ),
)
