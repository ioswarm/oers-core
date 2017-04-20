#!/bin/bash

#JAVA_OPTS="-XX:+UseBiasedLocking -XX:BiasedLockingStartupDelay=0"

if [ "x" == "x$OERS_HOME" ]; then
  OERS_HOME=/opt/oers
fi

export LOCAL_IP=$(ifconfig eth0 | awk '/inet addr:/ {print $2}'|sed 's/addr://')

CLASSPATH=${OERS_HOME}/conf:${OERS_HOME}/lib/*

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi


exec "$JAVACMD" -Djava.library.path="${OERS_HOME}/lib" -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4jLogDelegateFactory -Dlog4j.configuration=log4j.properties -classpath "$CLASSPATH" ioswarm.OERS "$@"
