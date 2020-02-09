#!/usr/bin/env bash

cd ..
# set java to version 11
export JAVA_HOME=$(/usr/libexec/java_home -v "11")
# maven package the project
mvn -s ~/.m2/defaultsettings.xml clean package
cd ./Docker || exit
unzip ../target/oauth-hivemq-*.zip
docker build -f Dockerfile -t michael/ace-mqtt .
rm -rf ./oauth-hivemq
