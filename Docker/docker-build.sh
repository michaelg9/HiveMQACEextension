#!/usr/bin/env bash

cd ..
export JAVA_HOME=$(/usr/libexec/java_home -v "11")
#mvn clean package
cd ./Docker || exit
unzip ../target/oauth-hivemq-*.zip
docker build -f Dockerfile -t michael/ace-mqtt .
