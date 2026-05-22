#!/usr/bin/env bash
set -eu
# Installs committed AAR + sources (+ optional javadoc) into ~/.m2 for JitPack to pick up.
# https://docs.jitpack.io/building/
# File must use Unix LF line endings (see root .gitattributes). CRLF breaks bash on Linux/JitPack.

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PROP="$ROOT/delivery/jitpack/maven-coordinates.properties"

if [[ ! -f "$PROP" ]]; then
  echo "Missing $PROP"
  exit 1
fi

GROUP_ID=""
ARTIFACT_ID=""
VERSION=""

while IFS= read -r line || [[ -n "$line" ]]; do
  line="${line//$'\r'/}"
  [[ "$line" =~ ^[[:space:]]*# ]] && continue
  [[ -z "${line// }" ]] && continue
  key="${line%%=*}"
  value="${line#*=}"
  key="${key// /}"
  case "$key" in
    groupId) GROUP_ID="$value" ;;
    artifactId) ARTIFACT_ID="$value" ;;
    version) VERSION="$value" ;;
  esac
done < "$PROP"

if [[ -z "$GROUP_ID" || -z "$ARTIFACT_ID" ]]; then
  echo "maven-coordinates.properties must set groupId and artifactId"
  exit 1
fi

# JitPack discovers artifacts under ~/.m2 using the same GAV as the dependency
# (tag / commit id as version, and com.github.USER.REPO for multi-module).
# Env JITPACK is documented at https://docs.jitpack.io/building/
if [[ "${JITPACK:-}" == "true" ]]; then
  cd "$ROOT"
  if git describe --tags --exact-match >/dev/null 2>&1; then
    VERSION=$(git describe --tags --exact-match)
  else
    VERSION=$(git describe --tags --always 2>/dev/null || true)
  fi
  if [[ -z "${VERSION:-}" ]]; then
    echo "JITPACK=true but could not determine version from git; set 'version=' in maven-coordinates.properties"
    exit 1
  fi
  echo "JitPack build: using Maven version '${VERSION}' from git"
fi

if [[ -z "${VERSION:-}" ]]; then
  echo "maven-coordinates.properties must set version (for non-JitPack local installs)"
  exit 1
fi

DIR="$ROOT/delivery/jitpack"
AAR="$DIR/${ARTIFACT_ID}.aar"
SRC="$DIR/${ARTIFACT_ID}-sources.jar"
DOC="$DIR/${ARTIFACT_ID}-javadoc.jar"

if [[ ! -f "$AAR" ]]; then
  echo "Missing required file: $AAR"
  exit 1
fi
if [[ ! -f "$SRC" ]]; then
  echo "Missing required file: $SRC"
  exit 1
fi

install_one() {
  local file="$1"
  local gid="$2"
  local aid="$3"
  local ver="$4"
  local pkg="$5"
  local genpom="${6:-false}"
  local classifier="${7:-}"

  local args=(
    -q install:install-file
    "-Dfile=$file"
    "-DgroupId=$gid"
    "-DartifactId=$aid"
    "-Dversion=$ver"
    "-Dpackaging=$pkg"
  )
  if [[ "$genpom" == "true" ]]; then
    args+=(-DgeneratePom=true)
  fi
  if [[ -n "$classifier" ]]; then
    args+=("-Dclassifier=$classifier")
  fi
  mvn "${args[@]}"
}

# 1) Multi-module / explicit coordinates (e.g. com.github.USER.REPO:mapsglmaps:tag)
install_one "$AAR" "$GROUP_ID" "$ARTIFACT_ID" "$VERSION" aar true ""
install_one "$SRC" "$GROUP_ID" "$ARTIFACT_ID" "$VERSION" jar false "sources"
if [[ -f "$DOC" ]]; then
  install_one "$DOC" "$GROUP_ID" "$ARTIFACT_ID" "$VERSION" jar false "javadoc"
fi
echo "Installed ${GROUP_ID}:${ARTIFACT_ID}:${VERSION} (aar + sources)"

# 2) JitPack default coordinates from https://docs.jitpack.io/building/ :
#    implementation 'com.github.User:RepoName:Version'
#    → groupId com.github.User, artifactId RepoName (same AAR/sources files).
if [[ "$GROUP_ID" == com.github.*.* ]]; then
  rest="${GROUP_ID#com.github.}"
  if [[ "$rest" == *.* ]]; then
    jp_user="${rest%%.*}"
    jp_repo="${rest#*.}"
    simple_group="com.github.${jp_user}"
    echo "Also installing JitPack default GAV ${simple_group}:${jp_repo}:${VERSION}"
    install_one "$AAR" "$simple_group" "$jp_repo" "$VERSION" aar true ""
    install_one "$SRC" "$simple_group" "$jp_repo" "$VERSION" jar false "sources"
    if [[ -f "$DOC" ]]; then
      install_one "$DOC" "$simple_group" "$jp_repo" "$VERSION" jar false "javadoc"
    fi
  fi
fi
