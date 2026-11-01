#!/bin/bash

app_jarfile=order-system-client/target/order-system-client.jar

if [ ! -f "$app_jarfile" ]; then
    ./mvnw clean install
fi

java -jar $app_jarfile $*
