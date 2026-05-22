# Manual JitPack artifacts (business requirement)

**On JitPack**, the root `jitpack.yml` runs **`delivery/jitpack/jitpack-gradle-publish.sh`**, which invokes the repo-root **`gradlew`** against **`jitpack-upload/`** (`publishToMavenLocal`). The script resolves the Git root first so Android detection does not break `./gradlew` with “No such file or directory”. The standalone project `jitpack-upload/` applies `maven-publish` and publishes the files in **this folder** to `~/.m2/repository` with the correct coordinates. Plain `mvn install:install-file` is not sufficient (“No build artifacts found”).

For **local** installs without Gradle, you can still run `bash delivery/jitpack/install-to-m2.sh` (same coordinates and `~/.m2` layout).

## What to commit here (exact names)

| File | Required | Notes |
|------|------------|--------|
| `jitpack-gradle-publish.sh` | yes | Used by root `jitpack.yml`; must stay **Unix LF** (see root `.gitattributes`). |
| `jitpack-upload/gradlew` + `jitpack-upload/gradle/wrapper/*` | yes | Self-contained wrapper so JitPack works even when the GitHub repo has **no** repo-root `gradlew` (slim artifact-only repo). Sync with root `gradle/wrapper` when you upgrade Gradle. The publish script falls back to repo-root `gradlew` if present. |
| `maven-coordinates.properties` | yes | `groupId`, `artifactId`; `version` is for **local** installs only (JitPack uses **git** for version). |
| `{artifactId}.aar` | yes | Example: `mapsglmaps.aar` — copy/rename from Gradle output (see below). |
| `{artifactId}-sources.jar` | yes | Example: `mapsglmaps-sources.jar` — KDoc/sources for IDE hovers. |
| `{artifactId}-javadoc.jar` | no | If present, it is installed with classifier `javadoc`. |

**Naming rule:** the stem must match `artifactId` in `maven-coordinates.properties` (e.g. `mapsglmaps` → `mapsglmaps.aar`, `mapsglmaps-sources.jar`).

## Where Gradle writes the binaries (to copy from)

From repo root, after a release build:

1. **AAR** (typical path):  
   `mapsglmaps/build/outputs/aar/mapsglmaps-release.aar`  
   → copy here as **`mapsglmaps.aar`** (name must match `artifactId` + `.aar`).

2. **Sources JAR:**  
   `.\gradlew :mapsglmaps:sourceReleaseJar`  
   then copy from e.g. `mapsglmaps/build/libs/` the `*-sources.jar` → rename to **`mapsglmaps-sources.jar`**.

3. **Javadoc JAR (optional):**  
   If you build `dokkaJavadocJar`, copy the `*-javadoc.jar` → **`mapsglmaps-javadoc.jar`**.

## Release checklist

1. **`groupId`** in the properties file uses the **multi-module** form `com.github.<User>.<Repo>` (e.g. `com.github.jasonsuto.test240815` for [jasonsuto/test240815](https://github.com/jasonsuto/test240815/)). The JitPack Gradle publish step **derives** the [classic JitPack GAV](https://docs.jitpack.io/building/) `com.github.<User>:<Repo>` and publishes the **real AAR + `-sources.jar` (+ optional javadoc)** there directly (not a POM-only aggregator). That way `implementation 'com.github.jasonsuto:test240815:Tag'` is a **direct** library dependency and Android Studio can attach sources for **KDoc / hovers**.
2. **`artifactId`** is the Gradle module name (`mapsglmaps`) — your committed files stay named `mapsglmaps.aar` / `mapsglmaps-sources.jar`.
3. **Consumers:** use **one** `implementation` line — **`implementation 'com.github.jasonsuto:test240815:Tag'`** (same pattern as [JitPack docs](https://docs.jitpack.io/building/)). Do **not** also add `com.github.jasonsuto.test240815:mapsglmaps` in the same project, or Gradle can put the same library on the classpath twice (**duplicate class** errors). The Gradle publish job for JitPack only publishes the classic coordinate; **`install-to-m2.sh`** still installs **both** GAVs under `~/.m2` for local testing only.
   After upgrading the dependency, sync Gradle; if hovers stay empty, try **Invalidate Caches / Restart** once. If the **AAR is R8-shrunk** (short obfuscated names) but the **sources JAR is normal Kotlin**, the IDE often cannot tie KDoc to bytecode — prefer a **non-minified AAR** (or `consumerProguardFiles` only) for the artifact you publish to JitPack.

### IDE docs / KDoc hovers (JitPack rewrites Gradle metadata)

**What goes wrong:** Gradle can publish a correct **`mapsglmaps-*.module`** (sources URL ends in **`-sources.jar`**). On **jitpack.io**, the **same URL** can still return **rewritten** JSON: `component` becomes **`com.github.jasonsuto` / `test240815`** and the sources file becomes **`mapsglmaps-*.jar`** instead of **`-sources.jar`**. The IDE then never treats the documentation artifact as sources.

**What we do in this repo:** the JitPack Gradle build **does not publish any `*.module` file** (only **POM + `.aar` + `-sources.jar`**), and the POM does **not** include Gradle’s `published-with-gradle-metadata` marker, so clients behave like plain Maven.

**What app authors should do:** in the **consumer** project, declare JitPack so Gradle does **not** prefer Gradle metadata for that host (belt-and-suspenders if JitPack ever injects a synthetic `.module` again):

```kotlin
// settings.gradle.kts — inside dependencyResolutionManagement { repositories { ... } }
maven {
    url = uri("https://jitpack.io")
    metadataSources {
        mavenPom()
        artifact()
    }
}
```

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

Then **Sync Gradle** (and bump to a **new** library tag after the publisher change above). Use a **single** `implementation` line: **`com.github.jasonsuto:test240815:Tag`**.

4. **`version=`** in the properties file is used for **local** `install-to-m2.sh` runs. **On JitPack**, `JITPACK=true` causes the script to **ignore** that value and use **`git describe`** so the Maven version matches the **tag or commit** JitPack is building (otherwise artifacts land under the wrong folder and JitPack cannot find them).
5. Replace the binary files under `delivery/jitpack/` with the new build outputs (exact filenames above).
6. Commit, tag, push — JitPack runs **`jitpack-upload`** (`publishToMavenLocal`), not the shell script.

## Large binaries

If policy allows, consider **Git LFS** for `.aar` / `.jar` files so the main repo stays lean.

## Shell script line endings (Windows)

`install-to-m2.sh` **must be committed with Unix LF** only. CRLF causes JitPack errors like `set: pipefail: invalid option name` (the `\r` corrupts the `set` line). Root **`.gitattributes`** forces `eol=lf` for `delivery/jitpack/*.sh`. After changing the script, normalize once:

```bash
git add --renormalize delivery/jitpack/install-to-m2.sh
```

Or re-save the file in the editor as **LF** / disable CRLF for `*.sh`.

## Local test

**Same path as JitPack** (from repo root; uses `version` in `maven-coordinates.properties` unless you set `JITPACK=true` to mimic JitPack’s `git describe` version):

```bash
bash delivery/jitpack/jitpack-gradle-publish.sh
# or from repo root only: ./gradlew -p jitpack-upload publishToMavenLocal
```

**Maven-only** (shell script; Linux/macOS or Git Bash):

```bash
bash delivery/jitpack/install-to-m2.sh
ls ~/.m2/repository/com/github/jasonsuto/test240815/mapsglmaps/
```
