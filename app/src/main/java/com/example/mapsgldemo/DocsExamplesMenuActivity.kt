package com.example.mapsgldemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
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
    }

    private fun returnToMainMenu() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}
