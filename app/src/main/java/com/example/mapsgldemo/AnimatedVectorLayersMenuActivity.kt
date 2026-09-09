package com.example.mapsgldemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.databinding.ActivityAnimatedVectorLayersMenuBinding
import com.example.mapsgldemo.vector.VectorCircleMapLayersActivity
import com.example.mapsgldemo.vector.VectorFillMapLayersActivity
import com.example.mapsgldemo.vector.VectorHeatmapMapLayersActivity
import com.example.mapsgldemo.vector.VectorLineMapLayersActivity

/**
 * Sub-menu for GLES animated vector layer demos. Opened from [MoreExamplesMenuActivity].
 */
class AnimatedVectorLayersMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnimatedVectorLayersMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnimatedVectorLayersMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.animatedVectorMenuBackToMainButton.root.setOnClickListener { returnToMainMenu() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToMainMenu()
            }
        })

        wireMenuButtons()
    }

    private fun wireMenuButtons() {
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

    private fun returnToMainMenu() {
        startActivity(
            Intent(this, MoreExamplesMenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}
