package com.example.mapsgldemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.databinding.ActivityStencilMaskMenuBinding
import com.example.mapsgldemo.stencil.Demo1PortugalSatelliteMaskActivity
import com.example.mapsgldemo.stencil.Demo2LandMaskTemperatureActivity
import com.example.mapsgldemo.stencil.Demo4PortugalMaskTemperatureActivity
import com.example.mapsgldemo.stencil.HighwaysConusMaskTemperatureActivity
import com.example.mapsgldemo.stencil.LandHighwayMaskTemperatureActivity

/**
 * Sub-menu for GLES stencil mask demo activities. Opened from [MainActivity].
 */
class StencilMaskMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStencilMaskMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStencilMaskMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.stencilMenuBackToMainButton.root.setOnClickListener { returnToMainMenu() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToMainMenu()
            }
        })

        wireMenuButtons()
    }

    private fun wireMenuButtons() {
        binding.menuHighwaysConusMaskTemperatureButton.setOnClickListener {
            startActivity(Intent(this, HighwaysConusMaskTemperatureActivity::class.java))
        }
        binding.menuDemo2LandMaskTemperatureButton.setOnClickListener {
            startActivity(Intent(this, Demo2LandMaskTemperatureActivity::class.java))
        }
        binding.menuDemo4PortugalMaskTemperatureButton.setOnClickListener {
            startActivity(Intent(this, Demo4PortugalMaskTemperatureActivity::class.java))
        }
        binding.menuLandHighwayMaskTemperatureButton.setOnClickListener {
            startActivity(Intent(this, LandHighwayMaskTemperatureActivity::class.java))
        }
        binding.menuPortugalSatelliteButton.setOnClickListener {
            startActivity(Intent(this, Demo1PortugalSatelliteMaskActivity::class.java))
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
