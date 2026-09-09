package com.example.mapsgldemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.databinding.ActivityShowcaseMenuBinding
import com.example.mapsgldemo.stencil.Demo2LandMaskTemperatureActivity

/**
 * Curated SDK walkthrough: a few screens that show the calls an integrator would actually write.
 * Opened from [MainActivity].
 */
class ShowcaseMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShowcaseMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowcaseMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.showcaseWeatherItem.showcaseItemTitle.text = "Animated weather"
        binding.showcaseWeatherItem.showcaseItemDescription.text =
            "addWeatherLayer(LayerCode.TEMPERATURES) on a looping timeline. Open the layer list for radar, wind, and alerts. Tap the map to inspect values."

        binding.showcaseForecastItem.showcaseItemTitle.text = "7-day forecast"
        binding.showcaseForecastItem.showcaseItemDescription.text =
            "High and low at a point (Xweather /forecasts). Tap the map to move the pin; tap a day to inspect that day's model."

        binding.showcaseLocalItem.showcaseItemTitle.text = "Local weather"
        binding.showcaseLocalItem.showcaseItemDescription.text =
            "A small set of layers at your location, with previous/next day. Closest to a finished app."

        binding.showcaseTropicalItem.showcaseItemTitle.text = "Tropical cyclones"
        binding.showcaseTropicalItem.showcaseItemDescription.text =
            "addWeatherLayer(LayerCode.TROPICAL_CYCLONES_ICONS). Vector icons and tracks scrub with map time."

        binding.showcaseMaskItem.showcaseItemTitle.text = "Land and water mask"
        binding.showcaseMaskItem.showcaseItemDescription.text =
            "Temperature clipped to land or water. Same pattern as LayerMasks.land() on a weather layer."

        binding.showcaseMenuBackToMainButton.root.setOnClickListener { returnToMainMenu() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToMainMenu()
            }
        })

        binding.showcaseWeatherItem.root.setOnClickListener {
            startActivity(Intent(this, ShowcaseWeatherActivity::class.java))
        }
        binding.showcaseForecastItem.root.setOnClickListener {
            startActivity(Intent(this, ShowcaseForecastActivity::class.java))
        }
        binding.showcaseLocalItem.root.setOnClickListener {
            startActivity(
                Intent(this, LocalActivity::class.java)
                    .putExtra(EXTRA_RETURN_TO_SHOWCASE, true),
            )
        }
        binding.showcaseTropicalItem.root.setOnClickListener {
            startActivity(Intent(this, ShowcaseTropicalActivity::class.java))
        }
        binding.showcaseMaskItem.root.setOnClickListener {
            startActivity(
                Intent(this, Demo2LandMaskTemperatureActivity::class.java)
                    .putExtra(EXTRA_RETURN_TO_SHOWCASE, true),
            )
        }
    }

    private fun returnToMainMenu() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    companion object {
        const val EXTRA_RETURN_TO_SHOWCASE = "return_to_showcase"
    }
}
