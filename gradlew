#!/usr/bin/env sh
CLASSPATH=$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar
exec ${JAVA_HOME:+$JAVA_HOME/bin/}java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
