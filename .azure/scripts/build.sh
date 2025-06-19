#!/usr/bin/env bash
set -e

echo "Build reason: ${BUILD_REASON}"
echo "Source branch: ${BRANCH}"

# The first segment of the version number is '1' for releases < 9; then '9', '10', '11', ...
JAVA_MAJOR_VERSION=$(java -version 2>&1 | sed -E -n 's/.* version "([0-9]*).*$/\1/p')
if [ "${JAVA_MAJOR_VERSION}" -eq "11" ] ; then
  # some parts of the workflow should be done only one on the main build which is currently Java 11
  export MAIN_BUILD="TRUE"
  echo "Running main build"
fi

# Build with Maven
# shellcheck disable=SC2086
mvn $MVN_ARGS install
# shellcheck disable=SC2086
mvn $MVN_ARGS spotbugs:check

# Push to Nexus
if [ "$BUILD_REASON" == "PullRequest" ] ; then
    echo "Building Pull Request - nothing to push"
elif [ "$BRANCH" != "refs/heads/main" ]; then
    echo "Not in main branch - nothing to push"
else
   if [ "${MAIN_BUILD}" = "TRUE" ] ; then
       echo "In main branch or in release tag - pushing to nexus"
       ./.azure/scripts/push-to-central.sh
   fi
fi
