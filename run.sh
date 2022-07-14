#! /bin/bash

#java -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector \
java -Dlog4j2.configurationFile=./config/log4j.properties \
     -Dlog4j2.shutdownHookEnabled=false \
     -jar ./nfs-passthrough.jar $@
