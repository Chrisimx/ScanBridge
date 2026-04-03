#!/bin/bash
# Script adb+
# Usage
# You can run any command adb provide on all your current devices
# ./adb+ <command> is the equivalent of ./adb -s <serial number> <command>
#
# Examples
# ./adb+ version
# ./adb+ install apidemo.apk
# ./adb+ uninstall com.example.android.apis

adb devices | while read line
do
    if [ ! "$line" = "" ] && [ `echo $line | awk '{print $2}'` = "device" ]
    then
        device=`echo $line | awk '{print $1}'`
        echo "$device $@ ..."
        adb -s $device $@ || echo "already uninstalled"

    fi
done