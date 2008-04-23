#!/bin/bash

/usr/sbin/snmptrapd -c snmptrapd.conf -C -Lsd -Lf /tmp/snmptrapd.log -p /var/run/snmptrapd.pid

