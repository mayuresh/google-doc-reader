#!/bin/sh
#
# Copyright © 2015-2021 the original authors.
# Gradle wrapper script.

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
APP_HOME=$(cd "$(dirname "$0")" && pwd)

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
APP_MAIN_CLASS="org.gradle.wrapper.GradleWrapperMain"

set -e
exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    $APP_MAIN_CLASS \
    "$@"
