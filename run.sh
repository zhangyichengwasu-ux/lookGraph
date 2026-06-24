#!/bin/bash
export JAVA_HOME=$(/usr/libexec/java_home -v 24)
cd /Users/zhangyicheng/Documents/GitHub/lookGraph
$JAVA_HOME/bin/java -jar target/*.jar
