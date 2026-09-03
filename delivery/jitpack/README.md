# Published artifacts

This directory holds the prebuilt MapsGL SDK artifacts that JitPack serves. The SDK is built in a
separate repository; nothing here is compiled from source.

| File | Purpose |
|------|---------|
| `mapsglmaps.aar` | The SDK itself. |
| `mapsglmaps-sources.jar` | Sources, so the IDE can show KDoc on hover. |
| `jitpack-transitive-dependencies.xml` | The `<dependencies>` block merged into the published POM. |

## Depending on the SDK

Use a **single** `implementation` line:

```groovy
implementation 'com.github.vaisala-xweather:mapsgl-android-sdk:<tag>'
```

Do not also depend on `com.github.vaisala-xweather.mapsgl-android-sdk:mapsglmaps` — the same library
would land on the classpath twice and Gradle reports duplicate classes.

**Mapbox is deliberately not a transitive dependency.** Your app declares the Mapbox Maps SDK and
`https://api.mapbox.com/downloads/v2/releases/maven` itself. No Gradle `exclude` rules are required.

### IDE sources and KDoc

Declare JitPack so Gradle resolves from the POM rather than Gradle metadata:

```groovy
// settings.gradle — inside dependencyResolutionManagement { repositories { ... } }
maven {
    url 'https://jitpack.io'
    metadataSources {
        mavenPom()
        artifact()
    }
}
```

Then sync Gradle. If hovers stay empty, try **Invalidate Caches / Restart** once.

## Upgrading to 1.7.0 — remove `java-vector-tile`

As of **1.7.0** MapsGL decodes Mapbox Vector Tiles with its own reader, so the published POM no
longer references `no.ecc.vectortile:java-vector-tile`, `com.google.protobuf:protobuf-java` or
`org.locationtech.jts:jts-core`. Delete these from your app if you have them:

```groovy
dependencies {
    implementation 'no.ecc.vectortile:java-vector-tile:1.4.1'   // <- delete
}

repositories {
    maven { url 'https://maven.ecc.no/releases' }               // <- delete
}
```

Also delete anything you added to work around that dependency: `exclude group: 'com.google.protobuf',
module: 'protobuf-java'` rules, and `-dontwarn no.ecc.vectortile.**` / `-dontwarn org.locationtech.**`
ProGuard lines. Leaving `java-vector-tile` in place puts `protobuf-java` back on the classpath, where
it declares the same classes as `protobuf-javalite` and fails dexing with duplicate-class errors.

## Using the AAR directly

If your app uses `implementation files('libs/mapsglmaps.aar')` instead of JitPack, Gradle does not
resolve transitive dependencies. Add them explicitly:

```groovy
dependencies {
    implementation files('libs/mapsglmaps.aar')
    implementation 'com.mapbox.maps:android-ndk27:11.15.3'
    implementation 'org.maplibre:earcut4j:3.0.0'
}
```
