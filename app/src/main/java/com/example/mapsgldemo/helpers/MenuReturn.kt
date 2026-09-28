package com.example.mapsgldemo.helpers

import android.app.Activity
import android.content.Intent

/**
 * Intent extra naming the menu a demo should return to, when that is not the menu the demo
 * normally lives under.
 *
 * Several demos are listed in more than one menu - the documentation examples also appear in
 * [com.example.mapsgldemo.MoreExamplesMenuActivity]. Each demo's back control reopens its own menu
 * with `CLEAR_TOP`, which would drop a user who came from somewhere else into a menu they never
 * opened. A launcher that is not the demo's home menu sets this, and [returnToMenu] honours it.
 */
const val EXTRA_RETURN_MENU = "com.example.mapsgldemo.RETURN_MENU"

/** Starts [demo], asking it to come back to this activity rather than its own home menu. */
fun Activity.startDemoReturningHere(demo: Class<out Activity>) {
    startActivity(Intent(this, demo).putExtra(EXTRA_RETURN_MENU, javaClass.name))
}

/**
 * Reopens the menu this demo was launched from - the one named by [EXTRA_RETURN_MENU], or
 * [defaultMenu] when the launcher did not name one - and finishes the demo.
 */
fun Activity.returnToMenu(defaultMenu: Class<out Activity>) {
    val target = intent.getStringExtra(EXTRA_RETURN_MENU)
    val menuIntent = if (target != null) {
        Intent().setClassName(this, target)
    } else {
        Intent(this, defaultMenu)
    }
    startActivity(menuIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
    finish()
}
