package com.example.mapsgldemo


import android.app.Activity

import android.content.Intent

import android.content.SharedPreferences

import android.os.Bundle

import android.widget.CheckBox

import androidx.appcompat.app.AppCompatActivity

import com.example.mapsgldemo.databinding.ActivityMainBinding
import com.example.mapsgldemo.maplayers.MapLayersActivity


/**

 * Launcher screen: [MapLayersActivity], [LocalActivity], [LegendDataInspectorMenuActivity],

 * [VectorLayersMenuActivity], [SampleLayersMenuActivity], [StencilMaskMenuActivity],

 * and [GriddedLayersMenuActivity].

 *

 * Optional checkboxes (one at a time) remember which activity to open on the **next cold start

 * from the app icon** ([Intent.CATEGORY_LAUNCHER]). Sub-menu checkboxes use the same prefs keys.

 */

class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding

    private lateinit var launchPrefs: SharedPreferences

    private var syncingCheckboxesFromPrefs = false


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)



        launchPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)



        if (savedInstanceState == null && shouldForwardToSavedLauncherTarget(intent)) {

            val targetClass = launchPrefs.getString(KEY_AUTO_LAUNCH_CLASS, null)

            if (!targetClass.isNullOrBlank()) {

                if (tryStartActivityForClassName(targetClass)) {

                    finish()

                    return

                }

                launchPrefs.edit().remove(KEY_AUTO_LAUNCH_CLASS).apply()

            }

        }



        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)



        syncCheckboxesFromPrefs()

        wireAutoLaunchCheckboxes()

        wireMenuButtons()

    }


    private fun wireMenuButtons() {

        binding.menuMapLayersButton.setOnClickListener {

            startActivity(Intent(this, MapLayersActivity::class.java))

        }

        binding.menuLocalWeatherButton.setOnClickListener {

            startActivity(Intent(this, LocalActivity::class.java))

        }

        binding.menuLegendDataInspectorButton.setOnClickListener {

            startActivity(Intent(this, LegendDataInspectorMenuActivity::class.java))

        }

        binding.menuVectorLayersButton.setOnClickListener {

            startActivity(Intent(this, VectorLayersMenuActivity::class.java))

        }

        binding.menuSampleLayersButton.setOnClickListener {

            startActivity(Intent(this, SampleLayersMenuActivity::class.java))

        }

        binding.menuStencilMaskDemosButton.setOnClickListener {

            startActivity(Intent(this, StencilMaskMenuActivity::class.java))

        }

        binding.menuGriddedLayersButton.setOnClickListener {

            startActivity(Intent(this, GriddedLayersMenuActivity::class.java))

        }

        binding.menuBlankMapButton.setOnClickListener {

            startActivity(Intent(this, BlankMapActivity::class.java))

        }

    }


    private fun wireAutoLaunchCheckboxes() {

        val pairs = listOf(

            binding.checkBoxMapLayersAuto to MapLayersActivity::class.java.name,

            binding.checkBoxLocalWeatherAuto to LocalActivity::class.java.name,

            )

        val allBoxes = pairs.map { it.first }

        for ((box, className) in pairs) {

            box.setOnCheckedChangeListener { _, isChecked ->

                if (syncingCheckboxesFromPrefs) return@setOnCheckedChangeListener

                if (isChecked) {

                    launchPrefs.edit().putString(KEY_AUTO_LAUNCH_CLASS, className).apply()

                    uncheckOthers(allBoxes, box)

                } else if (launchPrefs.getString(KEY_AUTO_LAUNCH_CLASS, null) == className) {

                    launchPrefs.edit().remove(KEY_AUTO_LAUNCH_CLASS).apply()

                }

            }

        }

    }


    private fun uncheckOthers(boxes: List<CheckBox>, keep: CheckBox) {

        syncingCheckboxesFromPrefs = true

        for (b in boxes) {

            if (b != keep) b.isChecked = false

        }

        syncingCheckboxesFromPrefs = false

    }


    private fun syncCheckboxesFromPrefs() {

        syncingCheckboxesFromPrefs = true

        binding.checkBoxMapLayersAuto.isChecked = false

        binding.checkBoxLocalWeatherAuto.isChecked = false

        when (launchPrefs.getString(KEY_AUTO_LAUNCH_CLASS, null)) {

            MapLayersActivity::class.java.name -> binding.checkBoxMapLayersAuto.isChecked = true

            LocalActivity::class.java.name -> binding.checkBoxLocalWeatherAuto.isChecked = true

            else -> Unit

        }

        syncingCheckboxesFromPrefs = false

    }


    private fun shouldForwardToSavedLauncherTarget(intent: Intent?): Boolean {

        if (intent == null) return false

        if (!intent.hasCategory(Intent.CATEGORY_LAUNCHER)) return false

        if (intent.action != Intent.ACTION_MAIN) return false

        val cls = launchPrefs.getString(KEY_AUTO_LAUNCH_CLASS, null)

        return !cls.isNullOrBlank()

    }


    @Suppress("UNCHECKED_CAST")

    private fun tryStartActivityForClassName(className: String): Boolean {

        return try {

            val clazz = Class.forName(className) as Class<out Activity>

            if (!Activity::class.java.isAssignableFrom(clazz)) return false

            startActivity(Intent(this, clazz))

            true

        } catch (_: Throwable) {

            false

        }

    }


    companion object {

        const val PREFS_NAME = "main_launch_prefs"

        const val KEY_AUTO_LAUNCH_CLASS = "auto_launch_activity_class"

    }

}

