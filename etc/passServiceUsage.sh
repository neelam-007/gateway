#!/bin/sh

# layer 7 technologies, inc
# this belongs in /ssg/etc/bin
#
# todo, replace hard coded results with call to database
 
PATH=$path:/bin:/usr/bin
 
PLACE=".1.3.6.1.4.1.17304.7.1"
REQ="$2"
 
if [ "$1" = "-n" ]; then
  case "$REQ" in
    $PLACE)      RET=$PLACE.1.1 ;;
    $PLACE.1.1)  RET=$PLACE.1.2 ;;
    $PLACE.1.2)  RET=$PLACE.1.3 ;;
    $PLACE.1.3)  RET=$PLACE.1.4 ;;
    $PLACE.1.4)  RET=$PLACE.1.5 ;;
    $PLACE.1.5)  RET=$PLACE.2.1 ;;
    $PLACE.2.1)  RET=$PLACE.2.2 ;;
    $PLACE.2.2)  RET=$PLACE.2.3 ;;
    $PLACE.2.3)  RET=$PLACE.2.4 ;;
    $PLACE.2.4)  RET=$PLACE.2.5 ;;
    $PLACE.2.5)  RET=$PLACE.3.1 ;;
    $PLACE.3.1)  RET=$PLACE.3.2 ;;
    $PLACE.3.2)  RET=$PLACE.3.3 ;;
    $PLACE.3.3)  RET=$PLACE.3.4 ;;
    $PLACE.3.4)  RET=$PLACE.3.5 ;;
    *)           exit 0 ;;
  esac
else
  case "$REQ" in
    $PLACE)    exit 0 ;;
    *)         RET=$REQ ;;
  esac
fi

#
# Todo, read data from database instead of hard coding results
#
echo "$RET"
case "$RET" in
  $PLACE.1.1) echo "integer"; echo "432154"; exit 0 ;;
  $PLACE.1.2) echo "string"; echo "ACMEWarehouse"; exit 0 ;;
  $PLACE.1.3) echo "integer"; echo "42"; exit 0 ;;
  $PLACE.1.4) echo "integer"; echo "30"; exit 0 ;;
  $PLACE.1.5) echo "integer"; echo "42"; exit 0 ;;
  $PLACE.2.1) echo "integer"; echo "432155"; exit 0 ;;
  $PLACE.2.2) echo "string"; echo "ACMEGeoTracker"; exit 0 ;;
  $PLACE.2.3) echo "integer"; echo "77"; exit 0 ;;
  $PLACE.2.4) echo "integer"; echo "77"; exit 0 ;;
  $PLACE.2.5) echo "integer"; echo "75"; exit 0 ;;
  $PLACE.3.1) echo "integer"; echo "438190"; exit 0 ;;
  $PLACE.3.2) echo "string"; echo "ACMERemoteLJN"; exit 0 ;;
  $PLACE.3.3) echo "integer"; echo "5"; exit 0 ;;
  $PLACE.3.4) echo "integer"; echo "0"; exit 0 ;;
  $PLACE.3.5) echo "integer"; echo "0"; exit 0 ;;
  *) echo "string"; echo "ack... $RET $REQ"; exit 0 ;;
esac
