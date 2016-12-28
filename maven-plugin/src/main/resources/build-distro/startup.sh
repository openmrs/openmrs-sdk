#!/bin/bash

# wait for mysql to initialise
/usr/local/tomcat/wait-for-it.sh --timeout=3600 mysql:3306

# start tomcat
/usr/local/tomcat/bin/catalina.sh run
