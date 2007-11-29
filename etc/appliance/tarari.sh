if [ -e  /usr/local/Tarari ]; then
	TARARIROOT=/usr/local/Tarari
	export TARARIROOT

	if ! echo $PATH | /bin/egrep -q "(^|:)$TARARIROOT/bin($|:)" ; then
        PATH=$TARARIROOT/bin:$PATH
    fi

	if ! echo $LD_LIBRARY_PATH | /bin/egrep -q "(^|:)$TARARIROOT/lib($|:)" ; then
        LD_LIBRARY_PATH=$TARARIROOT/lib:$LD_LIBRARY_PATH
    fi                                 

    export LD_LIBRARY_PATH
    export PATH
fi