#!/bin/sh

SSGUSER="gateway"

# test database connections
su -c - ${SSGUSER} --command="/opt/SecureSpan/JDK/bin/java -jar /opt/SecureSpan/ApiPortal/layer7-portal-metrics.jar test /opt/SecureSpan/ApiPortal/metrics.properties"
