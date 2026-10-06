package com.example.mapsgldemo

import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.databinding.ActivityQa180MenuBinding
import com.example.mapsgldemo.databinding.WidgetShowcaseMenuItemBinding
import com.example.mapsgldemo.helpers.returnToMenu
import com.example.mapsgldemo.helpers.startDemoReturningHere

/**
 * "Extended Tests": [Qa180TestsCatalog] as a menu - the 1.8.0 test notes, one row per step, each a
 * title over what to check. Opened from [MoreExamplesMenuActivity], and returns there.
 *
 * Built the way [MoreExamplesMenuActivity] is - an overline per section, rows from the catalog -
 * and every row is started with [startDemoReturningHere], because each screen also lives under
 * another menu and would otherwise return there.
 */
class Qa180MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQa180MenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQa180MenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.qa180MenuBackToMainButton.root.setOnClickListener { returnToMenu(MoreExamplesMenuActivity::class.java) }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToMenu(MoreExamplesMenuActivity::class.java)
            }
        })

        buildRows()
    }

    private fun buildRows() {
        val container = binding.qa180MenuButtonContainer
        Qa180TestsCatalog.categories.forEachIndexed { index, category ->
            val header = sectionHeader(category.title)
            // The first overline sits straight under the header divider, like the other menus.
            if (index == 0) header.setPadding(header.paddingLeft, 0, header.paddingRight, header.paddingBottom)
            container.addView(header)

            for (example in category.examples) {
                val row = WidgetShowcaseMenuItemBinding.inflate(layoutInflater, container, false)
                row.showcaseItemTitle.text = example.title
                row.showcaseItemDescription.text = example.summary
                row.root.setOnClickListener { startDemoReturningHere(example.activity) }
                container.addView(row.root)
            }
        }
        for ((title, text) in Qa180TestsCatalog.notes) {
            container.addView(sectionHeader(title))
            container.addView(TextView(this, null, 0, R.style.Xw_RowDescription).apply { this.text = text })
        }
    }

    private fun sectionHeader(title: String): TextView =
        (layoutInflater.inflate(R.layout.widget_more_examples_section, binding.qa180MenuButtonContainer, false) as TextView)
            .apply { text = title }
}
