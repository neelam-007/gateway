#!/bin/sh

SSGUSER="gateway"
# number of minutes of latest metrics data to aggregate
AGGREGATE_INTERVAL=15
HEAP_SIZE=512m

# Determine if gateway is listening on port 8443
ns=`netstat -tnap | grep 8443`

if [ -n "$ns" ]
then
    echo "Starting metrics sync."
    /opt/SecureSpan/JDK/bin/java -Xmx${HEAP_SIZE} -jar /opt/SecureSpan/ApiPortal/layer7-portal-metrics.jar sync /opt/SecureSpan/ApiPortal/metrics.properties ${AGGREGATE_INTERVAL}
    echo "Finished metrics sync."
else
    echo "Gateway is not listening on port 8443. Skipping metrics sync."
fi
