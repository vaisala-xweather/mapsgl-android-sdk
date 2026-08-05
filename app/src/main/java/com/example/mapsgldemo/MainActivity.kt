package com.example.mapsgldemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.databinding.ActivityMainBinding
import com.example.mapsgldemo.maplayers.MapLayersActivity

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
    }
}
