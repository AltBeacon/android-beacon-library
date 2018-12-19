#!/usr/bin/env bash
./gradlew clean jacocoTestReport
cp -r ./lib/build/reports/jacoco/jacocoTestReport/html/ ./build/
open ./build/index.html