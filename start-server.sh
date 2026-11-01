#!/bin/bash

# Starts orders server in the background (headless).
#
# Usage:
#   ./start.sh --profiles dev,verbose      start with the dev and verbose profile

profiles=""
app_jarfile=order-system-server.jar
app_logfile=.log/order-system-server-stdout.log

if [ ! -f "$app_jarfile" ]; then
    app_jarfile=order-system-server/target/order-system-server.jar
fi

if [ ! -f "$app_jarfile" ]; then
    echo "No $app_jarfile found - run './mvnw clean install' first"
    exit 1
fi

# Matches the running server, if any. The bracket prevents grep from matching itself.
find_pid() {
  ps -ef | grep "[j]ava" | grep "order-system-server.jar" | awk '{print $2}' | head -1
}

pid=$(find_pid)
if [ -n "$pid" ]; then
   echo "Existing process found ($pid) - is it already running?"
   exit 1
fi

start() {
  mkdir -p .log

  args=()
  if [ -n "$profiles" ]; then
    args+=(--profiles "$profiles")
  fi
  args+=("$@")

  nohup java -jar "$app_jarfile" "${args[@]}" > "$app_logfile" 2>&1 &

  sleep 2

  pid=$(find_pid)

  if [ -z "$pid" ]; then
     echo "No order-system-server.jar process found - check $app_logfile"
     exit 1
  else
     echo "Start successful (pid: $pid) - check $app_logfile"
     exit 0
  fi
}

start "$@"
