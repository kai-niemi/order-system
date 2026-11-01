#!/bin/bash

profiles=""
app_jarfile=order-system-server/target/order-system-server.jar

if [ ! -f "$app_jarfile" ]; then
    ./mvnw clean install
fi

args=()
if [ -n "$profiles" ]; then
  args+=(--profiles "$profiles")
fi
args+=("$@")

java -jar "$app_jarfile" "${args[@]}"
