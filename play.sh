#!/bin/bash

if [ -z `which sbt` ]; then
   echo "Please install SBT or add it to the PATH environment variable"
   exit 1
fi

sbt "project s3index-web" run 
