package com.example.mapsgldemo

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.databinding.ActivityMoreExamplesMenuBinding
import com.example.mapsgldemo.databinding.WidgetShowcaseMenuItemBinding
import com.example.mapsgldemo.helpers.startDemoReturningHere

/**
 * Sub-menu listing [MoreExamplesCatalog]: an overline per category and one row per example, each
 * row a title over a one-line summary. Opened from [MainActivity].
 *
 * The rows are built from the catalog rather than written out in XML, so adding an example is one
 * entry there. Each example is started with [startDemoReturningHere], because several of them also
 * live under [DocsExamplesMenuActivity] and would otherwise return there.
 */
class MoreExamplesMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMoreExamplesMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoreExamplesMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.moreExamplesMenuBackToMainButton.root.setOnClickListener { returnToMainMenu() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToMainMenu()
            }
        })

        buildRows()
    }

    private fun buildRows() {
        val container = binding.moreExamplesMenuButtonContainer
        MoreExamplesCatalog.categories.forEachIndexed { index, category ->
            val header = layoutInflater.inflate(R.layout.widget_more_examples_section, container, false) as TextView
            header.text = category.title
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
    }

    private fun returnToMainMenu() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}
