# Run from repo root:  powershell -File delivery/jitpack/copy-from-gradle.ps1
# Copies release AAR and sources jar into delivery/jitpack/ with the fixed names JitPack expects.
$ErrorActionPreference = "Stop"
$dest = $PSScriptRoot
$mod = Join-Path (Resolve-Path (Join-Path $dest "..\..")) "mapsglmaps"

if (-not (Test-Path $mod)) { throw "mapsglmaps module not found: $mod" }

$aarDir = Join-Path $mod "build\outputs\aar"
$aar = Get-ChildItem $aarDir -Filter "*.aar" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $aar) { throw "No AAR in $aarDir — run: .\gradlew :mapsglmaps:bundleReleaseAar" }
Copy-Item $aar.FullName (Join-Path $dest "mapsglmaps.aar") -Force
Write-Host "Copied $($aar.Name) -> mapsglmaps.aar"

$libs = Join-Path $mod "build\libs"
$src = Get-ChildItem $libs -Filter "*-sources.jar" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $src) { throw "No *-sources.jar in $libs — run: .\gradlew :mapsglmaps:sourceReleaseJar" }
Copy-Item $src.FullName (Join-Path $dest "mapsglmaps-sources.jar") -Force
Write-Host "Copied $($src.Name) -> mapsglmaps-sources.jar"

$jdoc = Get-ChildItem $libs -Filter "*-javadoc.jar" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($jdoc) {
    Copy-Item $jdoc.FullName (Join-Path $dest "mapsglmaps-javadoc.jar") -Force
    Write-Host "Copied $($jdoc.Name) -> mapsglmaps-javadoc.jar"
} else {
    Write-Host "No *-javadoc.jar in $libs (optional). Run dokkaJavadocJar if you need javadoc on JitPack."
}

Write-Host "Done. Update version in maven-coordinates.properties if needed, then commit delivery/jitpack/*.aar *.jar"
