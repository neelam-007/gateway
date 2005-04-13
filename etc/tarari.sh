#!/bin/sh
if [ -e  /usr/local/Tarari ]; then
	export TARARIROOT=/usr/local/Tarari
	export PATH=$TARARIROOT/bin:$PATH
	export LD_LIBRARY_PATH=$TARARIROOT/lib:$LD_LIBRARY_PATH
	export KERNELSOURCE=/usr/src/linux
	export JAVA_OPTS="-Dcom.l7tech.common.xml.tarari.enable=true $JAVA_OPTS"
fi
