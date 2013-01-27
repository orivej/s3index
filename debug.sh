#!/bin/sh

if [ -z `which sbt` ]; then
   echo "Please install SBT or add it to the PATH environment variable"
   exit 1
fi

export SBT_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9999"
sbt run
