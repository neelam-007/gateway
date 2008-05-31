#!/bin/sh
#
# Rebuilds the JNI header file for Win32LocalTcpPeerIdentifier class.
# Assumes that JAVA_HOME environment variable is set appropriately.
# Assumes that a compiled production UneasyRooster source tree can be found here:
#      ../../../build/ideaclasses/production/UneasyRooster
# relative to the location of this script.
#
cd `dirname $0`
mv -f com_l7tech_common_security_socket_Win32LocalTcpPeerIdentifier.h com_l7tech_common_security_socket_Win32LocalTcpPeerIdentifier_h.PREV
$JAVA_HOME/bin/javah -classpath ../../../build/ideaclasses/production/UneasyRooster com.l7tech.common.security.socket.Win32LocalTcpPeerIdentifier
