#!/bin/bash

# Stops a orders server started with ./start-server.sh, if it's running.

# Matches the running server, if any. The bracket prevents grep from matching itself.
pid=$(ps -ef | grep "[j]ava" | grep "order-system-server.jar" | awk '{print $2}' | head -1)

if [ -z "$pid" ]; then
   echo "No order-system-server.jar process found - is it running?"
   exit 1
fi

kill -TERM "$pid"
RETVAL=$?

echo "Stopped service (pid: $pid) $RETVAL"
