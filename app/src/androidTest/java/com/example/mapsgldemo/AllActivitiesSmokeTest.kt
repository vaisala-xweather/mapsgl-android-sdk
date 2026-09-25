package com.example.mapsgldemo

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Launches every activity the demo declares and asserts each one reaches RESUMED without throwing.
 *
 * This is the cheapest test that would actually have caught the SDK breakages behind v1.7.0: a
 * renamed public type or a stripped one compiles fine against unobfuscated stubs and only fails
 * when the minified AAR is really on the classpath and a screen really runs. Build it against the
 * release variant to exercise the R8-minified SDK.
 *
 * The activity list comes from the PackageManager rather than a hardcoded array, so screens added
 * later are covered without touching this file. The demo's activities are not exported, which is
 * why this runs as instrumentation: `adb shell am start` is refused with a SecurityException.
 *
 * Every failure is collected and reported together — one broken keep rule usually takes several
 * screens down at once, and seeing all of them at once is what tells you it was a keep rule.
 */
@RunWith(AndroidJUnit4::class)
class AllActivitiesSmokeTest {

    /**
     * The map screens ask for location on first resume. Without this the system dialog takes focus
     * and the activity sits in PAUSED forever -- a test failure that says nothing about the app.
     */
    @get:Rule
    val locationPermission: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    /** Time for a map surface to come up and the first tiles to be requested. */
    private val settleMillis = 2_500L

    @Test
    fun everyActivityLaunchesWithoutCrashing() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val packageName = context.packageName
        val activities = context.packageManager
            .getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            .activities
            .orEmpty()
            .map { it.name }
            // Library activities (e.g. Play services' GoogleApiActivity) are not demo screens and
            // finish themselves immediately by design, so only this app's own screens are covered.
            .filter { it.startsWith("com.example.mapsgldemo") }
            .sorted()

        if (activities.isEmpty()) fail("No activities found for $packageName")

        val failures = mutableListOf<String>()
        var launched = 0

        for (name in activities) {
            val intent = Intent().setComponent(ComponentName(packageName, name))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                ActivityScenario.launch<android.app.Activity>(intent).use { scenario ->
                    scenario.moveToState(Lifecycle.State.RESUMED)
                    Thread.sleep(settleMillis)
                    val state = scenario.state
                    if (state != Lifecycle.State.RESUMED) {
                        failures += "$name — settled in $state, expected RESUMED"
                    } else {
                        launched++
                    }
                }
            } catch (t: Throwable) {
                failures += "$name — ${t.javaClass.simpleName}: ${t.message?.take(300)}"
            }
        }

        if (failures.isNotEmpty()) {
            fail(
                "${failures.size} of ${activities.size} activities failed to launch " +
                    "($launched OK):\n" + failures.joinToString("\n") { "  $it" },
            )
        }
    }
}
