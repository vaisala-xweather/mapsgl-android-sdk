package com.example.mapsgldemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.databinding.ActivityMainBinding
import com.example.mapsgldemo.maplayers.MapLayersActivity

/**
 * Launcher screen: [ShowcaseMenuActivity], [MapLayersActivity], [JsParityLayersActivity],
 * [DocsExamplesMenuActivity], [LocalActivity], [StencilMaskMenuActivity], and
 * [MoreExamplesMenuActivity].
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
        binding.menuShowcaseButton.setOnClickListener {
            startActivity(Intent(this, ShowcaseMenuActivity::class.java))
        }
        binding.menuMapLayersButton.setOnClickListener {
            startActivity(Intent(this, MapLayersActivity::class.java))
        }
        binding.menuLocalWeatherButton.setOnClickListener {
            startActivity(Intent(this, LocalActivity::class.java))
        }
        binding.menuStencilMaskDemosButton.setOnClickListener {
            startActivity(Intent(this, StencilMaskMenuActivity::class.java))
        }

        binding.menuJsParityLayersButton.setOnClickListener {
            startActivity(Intent(this, JsParityLayersActivity::class.java))
        }

        binding.menuDocsExamplesButton.setOnClickListener {
            startActivity(Intent(this, DocsExamplesMenuActivity::class.java))
        }
        binding.menuMoreExamplesButton.setOnClickListener {
            startActivity(Intent(this, MoreExamplesMenuActivity::class.java))
        }
    }
}
