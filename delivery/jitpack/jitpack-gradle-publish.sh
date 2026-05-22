#!/usr/bin/env bash
# JitPack install step: publish committed AAR/sources from delivery/jitpack/ via jitpack-upload.
# Resolves Git root (Android detection may leave cwd outside the root).
# Prefers jitpack-upload/gradlew so a slim GitHub repo can ship the wrapper without the full Android tree.
set -eu
ROOT="$(git rev-parse --show-toplevel)"
JP="$ROOT/jitpack-upload"
GW=""
if [ -f "$JP/gradlew" ]; then
  GW="$JP/gradlew"
elif [ -f "$ROOT/gradlew" ]; then
  GW="$ROOT/gradlew"
else
  GW=$(find "$ROOT" -maxdepth 6 -type f -name gradlew 2>/dev/null | head -n1 || true)
fi
if [ -z "$GW" ] || [ ! -f "$GW" ]; then
  echo "No gradlew found under ${ROOT}." >&2
  echo "Commit jitpack-upload/gradlew and jitpack-upload/gradle/wrapper/ (or repo-root wrapper)." >&2
  ls -la "$ROOT" >&2 || true
  exit 1
fi
chmod +x "$GW" 2>/dev/null || true
exec "$GW" -p "$JP" --no-daemon publishToMavenLocal
