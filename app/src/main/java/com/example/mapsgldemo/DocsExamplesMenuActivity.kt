package com.example.mapsgldemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.databinding.ActivityDocsExamplesMenuBinding
import com.example.mapsgldemo.docExamples.AddGeoJsonLayerActivity

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
    }

    private fun returnToMainMenu() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}
