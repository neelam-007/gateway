#!/bin/sh

# layer 7 technologies, inc
# this belongs in /ssg/bin
#

PATH=$PATH:/bin:/usr/bin

PLACE=".1.3.6.1.4.1.17304.7.1"

if [ "$1" = "-g" ]; then
    VERB="get";
elif [ "$1" = "-n" ]; then
    VERB="getnext";
elif [ "$1" = "-s" ]; then
    VERB="set";
fi

wget -q -O- http://127.0.0.1:8080/ssg/management/$VERB/$2
exit 0
