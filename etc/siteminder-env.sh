# LAYER 7 TECHNOLOGIES
# SET SITEMINDER ENVIRONMENT

CAROOT=/opt/CA
export CAROOT

if [ -z "${LD_LIBRARY_PATH}" ] ; then
    LD_LIBRARY_PATH=$CAROOT/sdk/bin64
elif ! echo $LD_LIBRARY_PATH | /bin/egrep -q "(^|:)$CAROOT/sdk/bin64($|:)" ; then
    LD_LIBRARY_PATH=$CAROOT/sdk/bin64:$LD_LIBRARY_PATH
fi

CAPKIHOME=$CAROOT/CAPKI
export LD_LIBRARY_PATH CAPKIHOME