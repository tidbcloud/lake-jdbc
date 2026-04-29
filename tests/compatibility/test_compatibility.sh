#!/bin/bash

set -ex

LAKE_JDBC_RELEASE_REPO="${LAKE_JDBC_RELEASE_REPO:-tidbcloud/lake-jdbc}"

download_release_asset() {
    gh release download "$1" -R "$LAKE_JDBC_RELEASE_REPO" -p "$2" --clobber
}

ARROW_JAVA_TOOL_OPTIONS="--add-opens=java.base/java.nio=ALL-UNNAMED -Dio.netty.tryReflectionSetAccessible=true"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }${ARROW_JAVA_TOOL_OPTIONS}"

curl -sSLfo ./testng.jar https://repo.maven.apache.org/maven2/org/testng/testng/7.11.0/testng-7.11.0.jar
curl -sSLfo ./semver4j.jar https://repo1.maven.org/maven2/com/vdurmont/semver4j/3.1.0/semver4j-3.1.0.jar
curl -sSLfo ./jcommander.jar https://repo1.maven.org/maven2/org/jcommander/jcommander/1.83/jcommander-1.83.jar
curl -sSLfo ./jts-core.jar https://repo1.maven.org/maven2/org/locationtech/jts/jts-core/1.19.0/jts-core-1.19.0.jar
curl -sSLfo ./slf4j-api.jar https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.jar

original_dir=$(pwd)
cd ../..
# got 1 if not in java project
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
cd "$original_dir"

TEST_SIDE=${TEST_SIDE:-server}
TEST_VER=${LAKE_JDBC_TEST_VERSION:-$CURRENT_VERSION}
JDBC_VER=${LAKE_JDBC_VERSION:-$CURRENT_VERSION}

JDBC_TEST_JAR="lake-jdbc-${TEST_VER}-tests.jar"
if [ "$JDBC_VER" = "current" ]; then
    JDBC_JAR="lake-jdbc-${CURRENT_VERSION}.jar"
else
    JDBC_JAR="lake-jdbc-${JDBC_VER}.jar"
fi

if [ "$TEST_SIDE" = "server" ]; then
    download_release_asset "v${TEST_VER}" "${JDBC_TEST_JAR}"
else
    cp "../../lake-jdbc/target/${JDBC_TEST_JAR}" .
fi

if [ -z "${LAKE_JDBC_VERSION:-}" ] || [ "$JDBC_VER" = "current" ]; then
    # test the jar built in the current workflow run
    cp "../../lake-jdbc/target/${JDBC_JAR}" .
else
    download_release_asset "v${JDBC_VER}" "${JDBC_JAR}"
fi

if [ "$JDBC_VER" = "current" ]; then
    export LAKE_JDBC_VERSION=$CURRENT_VERSION
else
    export LAKE_JDBC_VERSION=$JDBC_VER
fi
java -Dlogback.logger.root=INFO -cp "testng.jar:slf4j-api.jar:${JDBC_JAR}:${JDBC_TEST_JAR}:jcommander.jar:semver4j.jar" org.testng.TestNG testng.xml
