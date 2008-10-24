if [ -e  /usr/local/Tarari ]; then
	TARARIROOT=/usr/local/Tarari
	SSGTARARI=true

    if ! echo $PATH | /bin/egrep -q "(^|:)$TARARIROOT/bin($|:)" ; then
        PATH=$TARARIROOT/bin:$PATH
    fi

	if [ -z "${LD_LIBRARY_PATH}" ] ; then
	    LD_LIBRARY_PATH=$TARARIROOT/lib
	elif ! echo $LD_LIBRARY_PATH | /bin/egrep -q "(^|:)$TARARIROOT/lib($|:)" ; then
        LD_LIBRARY_PATH=$TARARIROOT/lib:$LD_LIBRARY_PATH
    fi                                 

	export TARARIROOT
    export SSGTARARI
    export LD_LIBRARY_PATH
    export PATH
    export XCX_JOB_MODE=sqb,1000
fi