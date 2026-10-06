package com.example.mapsgldemo

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.example.mapsgldemo.databinding.ActivityDocsExamplesMenuBinding
import com.example.mapsgldemo.docExamples.AddGeoJsonLayerActivity
import com.example.mapsgldemo.docExamples.AddRasterLayerActivity
import com.example.mapsgldemo.docExamples.AddVectorLayerActivity
import com.example.mapsgldemo.docExamples.ChangeMapUnitsActivity
import com.example.mapsgldemo.docExamples.ChangeTimelineRangeActivity
import com.example.mapsgldemo.docExamples.CustomAlertStylesActivity
import com.example.mapsgldemo.docExamples.CustomEarthquakeShaderActivity
import com.example.mapsgldemo.docExamples.CustomFiresShaderActivity
import com.example.mapsgldemo.docExamples.CustomHeatIndexLegendActivity
import com.example.mapsgldemo.docExamples.CustomLightningShaderActivity
import com.example.mapsgldemo.docExamples.CustomLightningStylesActivity
import com.example.mapsgldemo.docExamples.CustomRadarColorscaleActivity
import com.example.mapsgldemo.docExamples.CustomTempsFillActivity
import com.example.mapsgldemo.docExamples.CustomWindParticlesActivity
import com.example.mapsgldemo.docExamples.DayNightOverlayActivity
import com.example.mapsgldemo.docExamples.FilterAlertsActivity
import com.example.mapsgldemo.docExamples.FrostFreezeLayerActivity
import com.example.mapsgldemo.docExamples.WindParticlesDrawRangeActivity
import com.example.mapsgldemo.helpers.startDemoReturningHere
import com.example.mapsgldemo.moreExamples.AdjustDrawRangeActivity
import com.example.mapsgldemo.moreExamples.PreloadAnimationDataActivity
import com.example.mapsgldemo.moreExamples.TimelineControlsActivity
import com.example.mapsgldemo.docExamples.WindSpeedCategoriesActivity
import com.example.mapsgldemo.docExamples.LayerMaskingActivity
import com.example.mapsgldemo.docExamples.SetMapUnitsActivity
import com.example.mapsgldemo.docExamples.QueryLayerStaticGeoJsonActivity
import com.example.mapsgldemo.docExamples.QueryLayerApiGeoJsonActivity

/**
 * Sub-menu for the MapsGL documentation examples ported to Android. Each button opens one activity
 * that mirrors a published example from https://www.xweather.com/docs/mapsgl/examples.
 *
 * Opened from [MainActivity].
 */
class DocsExamplesMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocsExamplesMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocsExamplesMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // The lead line links out to the published guides. The <a> tag has to be parsed, and a
        // movement method attached, or the span renders as styled text that does not respond to a
        // tap.
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

        wireMenuButtons()
    }

    private fun wireMenuButtons() {
        binding.menuAddGeojsonLayerButton.setOnClickListener {
            startActivity(Intent(this, AddGeoJsonLayerActivity::class.java))
        }
        binding.menuAddRasterLayerButton.setOnClickListener {
            startActivity(Intent(this, AddRasterLayerActivity::class.java))
        }
        binding.menuAddVectorLayerButton.setOnClickListener {
            startActivity(Intent(this, AddVectorLayerActivity::class.java))
        }
        binding.menuChangeMapUnitsButton.setOnClickListener {
            startActivity(Intent(this, ChangeMapUnitsActivity::class.java))
        }

        binding.menuChangeTimelineRangeButton.setOnClickListener {
            startActivity(Intent(this, ChangeTimelineRangeActivity::class.java))
        }

        binding.menuCustomAlertStylesButton.setOnClickListener {
            startActivity(Intent(this, CustomAlertStylesActivity::class.java))
        }

        binding.menuCustomEarthquakeShaderButton.setOnClickListener {
            startActivity(Intent(this, CustomEarthquakeShaderActivity::class.java))
        }

        binding.menuCustomFiresShaderButton.setOnClickListener {
            startActivity(Intent(this, CustomFiresShaderActivity::class.java))
        }

        binding.menuCustomHeatIndexLegendButton.setOnClickListener {
            startActivity(Intent(this, CustomHeatIndexLegendActivity::class.java))
        }

        binding.menuCustomLightningShaderButton.setOnClickListener {
            startActivity(Intent(this, CustomLightningShaderActivity::class.java))
        }

        binding.menuCustomLightningStylesButton.setOnClickListener {
            startActivity(Intent(this, CustomLightningStylesActivity::class.java))
        }

        binding.menuCustomRadarColorscaleButton.setOnClickListener {
            startActivity(Intent(this, CustomRadarColorscaleActivity::class.java))
        }

        binding.menuCustomTempsFillButton.setOnClickListener {
            startActivity(Intent(this, CustomTempsFillActivity::class.java))
        }

        binding.menuCustomWindParticlesButton.setOnClickListener {
            startActivity(Intent(this, CustomWindParticlesActivity::class.java))
        }

        binding.menuDayNightOverlayButton.setOnClickListener {
            startActivity(Intent(this, DayNightOverlayActivity::class.java))
        }

        binding.menuFilterAlertsButton.setOnClickListener {
            startActivity(Intent(this, FilterAlertsActivity::class.java))
        }
        binding.menuFrostFreezeLayerButton.setOnClickListener {
            startActivity(Intent(this, FrostFreezeLayerActivity::class.java))
        }
        binding.menuLayerMaskingButton.setOnClickListener {
            startActivity(Intent(this, LayerMaskingActivity::class.java))
        }
        binding.menuQueryLayerApiGeojsonButton.setOnClickListener {
            startActivity(Intent(this, QueryLayerApiGeoJsonActivity::class.java))
        }
        binding.menuQueryLayerStaticGeojsonButton.setOnClickListener {
            startActivity(Intent(this, QueryLayerStaticGeoJsonActivity::class.java))
        }
        binding.menuSetMapUnitsButton.setOnClickListener {
            startActivity(Intent(this, SetMapUnitsActivity::class.java))
        }
        // Examples whose screen lives in the More Examples menu: open it, but come back here.
        binding.menuPreloadAnimationDataButton.setOnClickListener {
            startDemoReturningHere(PreloadAnimationDataActivity::class.java)
        }
        binding.menuTempsDrawRangeButton.setOnClickListener {
            startDemoReturningHere(AdjustDrawRangeActivity::class.java)
        }
        binding.menuTimelineControlsButton.setOnClickListener {
            startDemoReturningHere(TimelineControlsActivity::class.java)
        }
        binding.menuWindParticlesDrawRangeButton.setOnClickListener {
            startActivity(Intent(this, WindParticlesDrawRangeActivity::class.java))
        }
        binding.menuWindSpeedCategoriesButton.setOnClickListener {
            startActivity(Intent(this, WindSpeedCategoriesActivity::class.java))
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
