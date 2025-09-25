#!/usr/bin/env sh
set -e
PRG="$0"
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done
SAVED="`pwd`"
cd "`dirname "$PRG"`/"
APP_HOME="`pwd -P`"
cd "$SAVED"
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
if [ -n "$JAVA_HOME" ] ; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi
exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
