package com.example.mapsgldemo

import android.app.Activity
import com.example.mapsgldemo.maplayers.MapLayersActivity
import com.example.mapsgldemo.moreExamples.LegendAndInspectorActivity
import com.example.mapsgldemo.moreExamples.ValuesAtPlacesActivity

/** One row in [MoreExamplesMenuActivity]: what the example is called, what it shows, and where it is. */
class Example(
    val title: String,
    val summary: String,
    val activity: Class<out Activity>,
)

/** A titled group of [examples], drawn as an overline and the rows under it. */
class ExampleCategory(
    val title: String,
    val examples: List<Example>,
)

/**
 * Every example in [MoreExamplesMenuActivity], in the same groups and order as
 * [DocsExamplesCatalog] and https://www.xweather.com/docs/mapsgl/examples.
 *
 * The weather-layer explorer and the legend control are Android screens with no card on that page,
 * so they sit in Explore and in Legends.
 */
object MoreExamplesCatalog {

    val categories: List<ExampleCategory> = buildList {
        add(
            ExampleCategory(
                "Explore",
                listOf(
                    Example(
                        "Weather layer explorer",
                        "Browse every weather layer, with timeline playback, legends, and data inspection.",
                        MapLayersActivity::class.java,
                    ),
                ),
            ),
        )
        DocsExamplesCatalog.sections.forEach { section ->
            val rows = section.examples.map { Example(it.title, it.summary, it.activity) }.toMutableList()
            if (section.title == "Legends, Units & Readouts") {
                rows.add(
                    1,
                    Example(
                        "Legend and data inspector",
                        "Add the SDK's legend and tap-to-inspect controls.",
                        LegendAndInspectorActivity::class.java,
                    ),
                )
                rows.add(
                    Example(
                        "Built-in city temperature labels",
                        "The temperatures-text layer, sampling the shared cities tileset.",
                        ValuesAtPlacesActivity::class.java,
                    ),
                )
            }
            add(ExampleCategory(section.title, rows))
        }
    }
}
