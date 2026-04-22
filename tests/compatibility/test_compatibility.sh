#!/bin/bash

set -ex

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$PROJECT_ROOT"
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

JDBC_VER=${DATABEND_JDBC_VERSION:-current}

# Resolve driver jar + tests jar.
# - "current": use artifacts produced by the in-run `mvn package` step.
# - anything else (e.g. "0.1.0"): download from tidbcloud/lake-jdbc GitHub Release.
#   Works for both private and public repos as long as $GITHUB_TOKEN is set.
if [ "$JDBC_VER" = "current" ]; then
    DRIVER_JAR="$PROJECT_ROOT/lake-jdbc/target/lake-jdbc-${CURRENT_VERSION}.jar"
    TESTS_JAR="$PROJECT_ROOT/lake-jdbc/target/lake-jdbc-${CURRENT_VERSION}-tests.jar"
else
    DRIVER_JAR="$SCRIPT_DIR/lake-jdbc-${JDBC_VER}.jar"
    TESTS_JAR="$SCRIPT_DIR/lake-jdbc-${JDBC_VER}-tests.jar"
    # Use `gh release download` — works for both private and public repos
    # when GITHUB_TOKEN / GH_TOKEN is set in the environment.
    gh release download "v${JDBC_VER}" \
        --repo tidbcloud/lake-jdbc \
        --pattern "lake-jdbc-${JDBC_VER}.jar" \
        --pattern "lake-jdbc-${JDBC_VER}-tests.jar" \
        --dir "$SCRIPT_DIR" \
        --clobber
fi

# Build the dependency classpath from the current pom.
# Includes test-scope deps (testng, jts-core) and runtime deps (okhttp, jackson, slf4j, ...).
# The shaded driver jar relocates jackson/guava/slf4j/commons-lang3 internally,
# but test classes reference the un-shaded packages — these come from here.
cd "$PROJECT_ROOT"
mvn -pl lake-jdbc -q \
    dependency:build-classpath \
    -Dmdep.outputFile=/tmp/lake-jdbc-cp.txt \
    -DincludeScope=test
DEPS_CP=$(cat /tmp/lake-jdbc-cp.txt)

cd "$SCRIPT_DIR"
CLASSPATH="${DRIVER_JAR}:${TESTS_JAR}:${DEPS_CP}"

java -Dlogback.logger.root=INFO -cp "$CLASSPATH" org.testng.TestNG testng.xml
