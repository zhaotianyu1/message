#!/bin/bash

OUT_DIR=maap

set -e

# 编译
rm -rf ${OUT_DIR}
mkdir ${OUT_DIR} 

./gradlew clean
./gradlew assembleDebug

cp app/build/outputs/apk/debug/app-debug.apk ./${OUT_DIR}/
