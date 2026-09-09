package com.example.mapsgldemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.customize.CustomShaderActivity
import com.example.mapsgldemo.customize.DataDrivenColorActivity
import com.example.mapsgldemo.customize.FillSortOrderActivity
import com.example.mapsgldemo.customize.HeatmapTuningActivity
import com.example.mapsgldemo.customize.IconOrientationActivity
import com.example.mapsgldemo.databinding.ActivityLayerCustomizationMenuBinding

/**
 * Sub-menu for the layer-customization demos. Opened from [MoreExamplesMenuActivity].
 *
 * Where the other menus answer "which weather layers can I add?", these answer "what can I do to
 * a layer once I have it?" - each screen changes one thing and says so on the map.
 *
 * Every demo subclasses
 * [com.example.mapsgldemo.customize.CustomizationDemoActivity], which owns the map and timeline
 * boilerplate, so the demo file itself is only the customization worth copying.
 */
class LayerCustomizationMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLayerCustomizationMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLayerCustomizationMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.customizationMenuBackToMainButton.root.setOnClickListener { returnToMainMenu() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = returnToMainMenu()
        })

        wireMenuButtons()
    }

    private fun wireMenuButtons() {
        binding.menuDataDrivenColorButton.setOnClickListener {
            startActivity(Intent(this, DataDrivenColorActivity::class.java))
        }
        binding.menuCustomShaderButton.setOnClickListener {
            startActivity(Intent(this, CustomShaderActivity::class.java))
        }
        binding.menuFillSortOrderButton.setOnClickListener {
            startActivity(Intent(this, FillSortOrderActivity::class.java))
        }
        binding.menuHeatmapTuningButton.setOnClickListener {
            startActivity(Intent(this, HeatmapTuningActivity::class.java))
        }
        binding.menuIconOrientationButton.setOnClickListener {
            startActivity(Intent(this, IconOrientationActivity::class.java))
        }
    }

    private fun returnToMainMenu() {
        startActivity(
            Intent(this, MoreExamplesMenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}
