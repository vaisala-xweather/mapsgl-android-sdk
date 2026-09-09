package com.example.mapsgldemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.databinding.ActivityMoreExamplesMenuBinding

/**
 * Sub-menu for the remaining demos that no longer fit on the launcher screen:
 * [MapTimeFilterActivity], [LightningSymbolCircleActivity], [PlacesTextActivity],
 * [AnimatedVectorLayersMenuActivity], and [LayerCustomizationMenuActivity].
 * Opened from [MainActivity].
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

        wireMenuButtons()
    }

    private fun wireMenuButtons() {
        binding.menuMapTimeFilterButton.setOnClickListener {
            startActivity(Intent(this, MapTimeFilterActivity::class.java))
        }
        binding.menuLightningSymbolCircleButton.setOnClickListener {
            startActivity(Intent(this, LightningSymbolCircleActivity::class.java))
        }
        binding.menuPlacesTextButton.setOnClickListener {
            startActivity(Intent(this, PlacesTextActivity::class.java))
        }
        binding.menuAnimatedVectorLayersButton.setOnClickListener {
            startActivity(Intent(this, AnimatedVectorLayersMenuActivity::class.java))
        }
        binding.menuLayerCustomizationsButton.setOnClickListener {
            startActivity(Intent(this, LayerCustomizationMenuActivity::class.java))
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
