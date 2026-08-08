#!/usr/bin/env sh
set -eu

MVN_VERSION="3.9.9"
SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
CACHE_ROOT="$SCRIPT_DIR/.mvn/wrapper/cache"
MVN_CACHE_DIR="$CACHE_ROOT/$MVN_VERSION"
MVN_HOME="$MVN_CACHE_DIR/apache-maven-$MVN_VERSION"
ARCHIVE="$MVN_CACHE_DIR/apache-maven-$MVN_VERSION-bin.tar.gz"
DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MVN_VERSION/apache-maven-$MVN_VERSION-bin.tar.gz"
PROJECT_DIR="$PWD"
if [ ! -f "$PROJECT_DIR/pom.xml" ] && [ -f "$SCRIPT_DIR/wardrobe-backend/pom.xml" ]; then
  PROJECT_DIR="$SCRIPT_DIR/wardrobe-backend"
fi

if [ ! -x "$MVN_HOME/bin/mvn" ]; then
  mkdir -p "$MVN_CACHE_DIR"
  if [ -f "$ARCHIVE" ] && [ ! -s "$ARCHIVE" ]; then
    rm -f "$ARCHIVE"
  fi
  if [ ! -f "$ARCHIVE" ]; then
    curl -fsSL "$DOWNLOAD_URL" -o "$ARCHIVE"
  fi
  rm -rf "$MVN_HOME"
  tar -xzf "$ARCHIVE" -C "$MVN_CACHE_DIR"
fi

cd "$PROJECT_DIR"
exec "$MVN_HOME/bin/mvn" "$@"
