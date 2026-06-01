# Run from publish repo root:
#   powershell -File delivery/jitpack/copy-from-gradle.ps1 -SdkRoot C:\path\to\mapsgl-android-sdk
#
# Copies release AAR, sources jar, and jitpack-transitive-dependencies.xml into delivery/jitpack/.
# This repo has no mapsglmaps module — SdkRoot must point at the full MapsGL SDK checkout.
param(
    [string]$SdkRoot = $env:MAPSGL_SDK_ROOT
)

$ErrorActionPreference = "Stop"
$dest = $PSScriptRoot
$publishRepo = Resolve-Path (Join-Path $dest "..\..")

if (-not $SdkRoot) {
    throw "Set -SdkRoot to the MapsGL Android SDK repo root (contains mapsglmaps/), or set MAPSGL_SDK_ROOT."
}
$SdkRoot = (Resolve-Path $SdkRoot).Path
$mod = Join-Path $SdkRoot "mapsglmaps"
if (-not (Test-Path $mod)) { throw "mapsglmaps module not found under SdkRoot: $mod" }

$aarDir = Join-Path $mod "build\outputs\aar"
$aar = Get-ChildItem $aarDir -Filter "*.aar" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $aar) { throw "No AAR in $aarDir — run in SdkRoot: .\gradlew :mapsglmaps:bundleReleaseAar" }
Copy-Item $aar.FullName (Join-Path $dest "mapsglmaps.aar") -Force
Write-Host "Copied $($aar.Name) -> mapsglmaps.aar"

$libs = Join-Path $mod "build\libs"
$src = Get-ChildItem $libs -Filter "*-sources.jar" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $src) { throw "No *-sources.jar in $libs — run in SdkRoot: .\gradlew :mapsglmaps:sourceReleaseJar" }
Copy-Item $src.FullName (Join-Path $dest "mapsglmaps-sources.jar") -Force
Write-Host "Copied $($src.Name) -> mapsglmaps-sources.jar"

$jdoc = Get-ChildItem $libs -Filter "*-javadoc.jar" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($jdoc) {
    Copy-Item $jdoc.FullName (Join-Path $dest "mapsglmaps-javadoc.jar") -Force
    Write-Host "Copied $($jdoc.Name) -> mapsglmaps-javadoc.jar"
} else {
    Write-Host "No *-javadoc.jar in $libs (optional). Run dokkaJavadocJar if you need javadoc on JitPack."
}

Push-Location $SdkRoot
Write-Host "Exporting JitPack transitive POM dependencies from SdkRoot..."
& .\gradlew :mapsglmaps:exportJitpackTransitiveDependencies --quiet
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "exportJitpackTransitiveDependencies failed" }
Pop-Location

$sdkDeps = Join-Path $SdkRoot "delivery\jitpack\jitpack-transitive-dependencies.xml"
if (-not (Test-Path $sdkDeps)) { throw "Missing $sdkDeps after export" }
Copy-Item $sdkDeps (Join-Path $dest "jitpack-transitive-dependencies.xml") -Force
Write-Host "Copied jitpack-transitive-dependencies.xml"

Write-Host "Done ($publishRepo). Update maven-coordinates.properties version if needed, then commit delivery/jitpack/*.aar *.jar jitpack-transitive-dependencies.xml"
