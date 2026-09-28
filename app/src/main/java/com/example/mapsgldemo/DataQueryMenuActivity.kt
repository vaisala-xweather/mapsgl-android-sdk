package com.example.mapsgldemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsgldemo.databinding.ActivityDataQueryMenuBinding

/**
 * Sub-menu for data-query (`*-text`) examples. Opened from [MainActivity].
 */
class DataQueryMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataQueryMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataQueryMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.dataQueryMenuBackToMainButton.root.setOnClickListener { returnToMainMenu() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToMainMenu()
            }
        })

        binding.menuDataQueryTextButton.setOnClickListener {
            startActivity(Intent(this, DataQueryTextActivity::class.java))
        }
        binding.menuDataQueryTextSizeButton.setOnClickListener {
            startActivity(Intent(this, DataQueryTextSizeActivity::class.java))
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
