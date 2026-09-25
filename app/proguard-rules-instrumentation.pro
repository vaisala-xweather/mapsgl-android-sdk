# Keep rules for the instrumentable, minified build type only (see `minifiedTest` in build.gradle).
# NOT applied to the real `release` build, which stays exactly as shipped.
#
# Why this file has to exist at all:
#
# AndroidJUnitRunner.onCreate() calls androidx.tracing.Trace. The *app* never calls it, so R8
# deletes it from the app APK -- `androidx.tracing.Trace` appears in neither the release mapping
# nor the dex, while its siblings (TraceApi29Impl) survive. R8 then minifies the instrumentation
# APK with the app APK on its classpath, treats androidx.tracing as already provided by the app,
# and does not package its own copy. The class ends up in neither APK and the runner dies with
# NoClassDefFoundError before a single test starts.
#
# Keeping it on the *app* side is the fix. Adding the keep (or the dependency) to the test APK
# does not work, because the test APK is not where R8 decided to drop it.
-keep class androidx.tracing.Trace { *; }
