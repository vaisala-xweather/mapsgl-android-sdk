package com.example.mapsgldemo

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.example.mapsgldemo.databinding.ActivityDocsExamplesMenuBinding
import com.example.mapsgldemo.databinding.WidgetDocsExampleCardBinding
import com.example.mapsgldemo.helpers.startDemoReturningHere

/**
 * Sub-menu for the MapsGL documentation examples ported to Android. Sections follow
 * https://www.xweather.com/docs/mapsgl/examples. Each card opens one activity.
 *
 * Opened from [MainActivity].
 */
class DocsExamplesMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocsExamplesMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocsExamplesMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.docsExamplesMenuLeadTextView.apply {
            text = HtmlCompat.fromHtml(
                getString(R.string.docs_examples_lead),
                HtmlCompat.FROM_HTML_MODE_LEGACY,
            )
            movementMethod = LinkMovementMethod.getInstance()
        }

        binding.docsExamplesMenuBackToMainButton.root.setOnClickListener { returnToMainMenu() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToMainMenu()
            }
        })

        buildRows()
    }

    private fun buildRows() {
        val container = binding.docsExamplesMenuButtonContainer
        DocsExamplesCatalog.sections.forEachIndexed { index, section ->
            val header = layoutInflater.inflate(
                R.layout.widget_more_examples_section,
                container,
                false,
            ) as TextView
            header.text = section.title
            if (index == 0) header.setPadding(header.paddingLeft, 0, header.paddingRight, header.paddingBottom)
            container.addView(header)

            for (example in section.examples) {
                val card = WidgetDocsExampleCardBinding.inflate(layoutInflater, container, false)
                card.docsExampleTitle.text = example.title
                card.docsExampleSummary.text = example.summary
                val image = example.image
                if (image != null) {
                    card.docsExampleImage.setImageResource(image)
                    card.docsExampleImage.contentDescription = example.imageDescription
                    card.docsExampleImage.visibility = View.VISIBLE
                }
                card.root.setOnClickListener { startDemoReturningHere(example.activity) }
                container.addView(card.root)
            }
        }
    }

    private fun returnToMainMenu() {
        startActivity(
            android.content.Intent(this, MainActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}
