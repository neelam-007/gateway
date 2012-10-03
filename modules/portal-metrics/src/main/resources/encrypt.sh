#!/bin/sh

SSGUSER="gateway"

# encrypt password (input from user)
su -c - ${SSGUSER} --command="/opt/SecureSpan/JDK/bin/java -jar /opt/SecureSpan/ApiPortal/layer7-portal-metrics.jar encrypt /opt/SecureSpan/ApiPortal/metrics.properties ${1}"
