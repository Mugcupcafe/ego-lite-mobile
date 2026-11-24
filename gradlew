#!/bin/sh
set -e

APP_HOME=$(cd -P "$(dirname "$0")" && pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi

if [ ! -f "$CLASSPATH" ]; then
  echo "ERROR: $CLASSPATH not found. Run 'gradle wrapper --gradle-version 8.7' first." >&2
  exit 1
fi

exec "$JAVACMD" ${DEFAULT_JVM_OPTS:-"-Xmx64m" "-Xms64m"} \
  "-Dorg.gradle.appname=gradlew" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
