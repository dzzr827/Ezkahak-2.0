#!/usr/bin/env sh
set -eu
if ! command -v java >/dev/null 2>&1; then
  echo 'JDK 21 is required.' >&2
  exit 1
fi
if ! command -v gradle >/dev/null 2>&1; then
  echo 'Gradle is not installed. Install Gradle, then run this script again.' >&2
  exit 1
fi
java -version
gradle clean build
printf '\nBuild output:\n%s\n' "$(pwd)/build/libs/ezkahak-2.0.0.jar"
