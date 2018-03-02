#!/bin/sh -e

exec strace -f \
  java -javaagent:/opt/woken/aspectjweaver.jar \
          -Djava.library.path=/lib \
          -Dconfig.file=/opt/woken/config/application.conf \
          -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector \
          -jar /opt/woken/woken.jar
