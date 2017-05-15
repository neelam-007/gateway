#!/bin/sh

GATEWAY_DUMP_DIR=${ALL_MODULES_BaseOutputDirectory}/gateway/dumps
COLLECTOR_HOME=/opt/SecureSpan/Collector
JAVA_HOME=/opt/SecureSpan/JDK
TEMP_GATEWAY_USER_DUMPFOLDER=/tmp/heapdump_$(date +%s)

. ${COLLECTOR_HOME}/collectorlib

echo "Beginning heap dump"

GW_PID=$(ps awwx | grep Gateway.jar | grep -v grep | awk '{print $1}')

logAndRunCmd su -c \"mkdir -p ${TEMP_GATEWAY_USER_DUMPFOLDER}\" -s /bin/sh gateway
logAndRunCmd su -c \"${JAVA_HOME}/bin/jmap -dump:live,format=b,file=${TEMP_GATEWAY_USER_DUMPFOLDER}/heap.hprof ${GW_PID}\" -s /bin/sh gateway
logAndRunCmd mkdir -p ${GATEWAY_DUMP_DIR}
logAndRunCmd mv ${TEMP_GATEWAY_USER_DUMPFOLDER}/heap.hprof ${GATEWAY_DUMP_DIR}/heapDump.hprof
logAndRunCmd rm -rf ${TEMP_GATEWAY_USER_DUMPFOLDER}

echo "Finished heap dump"