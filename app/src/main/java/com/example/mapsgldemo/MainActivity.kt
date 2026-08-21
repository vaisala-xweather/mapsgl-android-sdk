package com.example.mapsgldemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.databinding.ActivityMainBinding
import com.example.mapsgldemo.maplayers.MapLayersActivity
import com.example.mapsgldemo.vector.VectorCircleMapLayersActivity
import com.example.mapsgldemo.vector.VectorFillMapLayersActivity
import com.example.mapsgldemo.vector.VectorHeatmapMapLayersActivity
import com.example.mapsgldemo.vector.VectorLineMapLayersActivity

/**
 * Launcher screen: [MapLayersActivity], [LocalActivity], and [StencilMaskMenuActivity].
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wireMenuButtons()
    }

    private fun wireMenuButtons() {
        binding.menuMapLayersButton.setOnClickListener {
            startActivity(Intent(this, MapLayersActivity::class.java))
        }
        binding.menuLocalWeatherButton.setOnClickListener {
            startActivity(Intent(this, LocalActivity::class.java))
        }
        binding.menuStencilMaskDemosButton.setOnClickListener {
            startActivity(Intent(this, StencilMaskMenuActivity::class.java))
        }
        binding.menuMapTimeFilterButton.setOnClickListener {
            startActivity(Intent(this, MapTimeFilterActivity::class.java))
        }
        binding.menuLightningSymbolCircleButton.setOnClickListener {
            startActivity(Intent(this, LightningSymbolCircleActivity::class.java))
        }
        binding.menuPlacesTextButton.setOnClickListener {
            startActivity(Intent(this, PlacesTextActivity::class.java))
        }
        binding.menuVectorFillButton.setOnClickListener {
            startActivity(Intent(this, VectorFillMapLayersActivity::class.java))
        }
        binding.menuVectorLineButton.setOnClickListener {
            startActivity(Intent(this, VectorLineMapLayersActivity::class.java))
        }
        binding.menuVectorCircleButton.setOnClickListener {
            startActivity(Intent(this, VectorCircleMapLayersActivity::class.java))
        }
        binding.menuVectorHeatmapButton.setOnClickListener {
            startActivity(Intent(this, VectorHeatmapMapLayersActivity::class.java))
        }
    }
}
