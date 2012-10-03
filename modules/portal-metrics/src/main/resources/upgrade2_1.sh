#!/bin/sh

SSGUSER="gateway"

# execute upgrade from 2.0 to 2.1
su -c - ${SSGUSER} --command="/opt/SecureSpan/JDK/bin/java -jar /opt/SecureSpan/ApiPortal/layer7-portal-metrics.jar upgrade /opt/SecureSpan/ApiPortal/metrics.properties"
