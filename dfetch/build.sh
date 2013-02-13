#!/bin/sh

basedir=$(dirname $0)

echo javac $basedir/dfetch.java
javac $basedir/dfetch.java

echo dx --dex --output="$basedir/dfetch.jar" "$basedir/dfetch.class"
dx --dex --output="$basedir/dfetch.jar" "$basedir/dfetch.class"

echo rm "$basedir/dfetch.class"
rm "$basedir/dfetch.class"


echo 'Output in $basedir/dfetch.jar'
