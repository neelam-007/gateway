#!/bin/sh

ssgDir="/ssg/bin"
ssgDBDir="/ssg/etc/sql"
ssgDBName="ssg"

echo "Please base on the release upgrade geometry (from release x to release y)"
echo "to figure out the upgrade ssg and database script required to be run on this RPM upgrade."
echo "If you have existed from this $0 script, you may invoke it manually, which locates at /ssg/bin/ directory."
cat /ssg/bin/upgrade.txt | more
echo "Does this upgrade require any \"ssg upgrade script\" to be run? (y/n)"
read inputReqSsgUp
if [ $inputReqSsgUp = y ]; then
  echo "Name of \"ssg upgrade script\"? (eg. upgrade_<releaseX>-<releaseY>.sh)"
  read inputNameSsgUp
  if [ -e $ssgDir/$inputNameSsgUp -a -x $ssgDir/$inputNameSsgUp ]; then
    echo "Start running script \"$ssgDir/$inputNameSsgUp\"..."
    $ssgDir/$inputNameSsgUp
  else
    echo "$ssgDir/$inputNameSsgUp does not exist and/or not executable, so $ssgDir/$inputNameSsgUp did NOT run"
  fi
fi
echo "Does this upgrade require any \"database upgrade script\" to be run? (y/n)" 
read inputReqDBUp
if [ $inputReqDBUp = y ]; then
  echo "Name of \"database upgrade script\"? (eg. upgrade_<releaseX>-<releaseY>.sql)"
  read inputNameDBUp
  if [ -e $ssgDBDir/$inputNameDBUp ]; then
    echo "What is the database vendor? (Select by entering a number which represent a database vendor, and press <Enter> key)"
    select dbVendor in "MySQL" "Postgres" "Oracle"; do
      break
    done
    echo "You have selected $dbVendor"
    case $dbVendor in 
      MySQL)
        echo "Start running $ssgDBDir/$inputNameDBUp script on $dbVendor database $ssgDBName - \"mysql $ssgDBName < $ssgDBDir/$inputNameDBUp\"..."
        mysql $ssgDBName < $ssgDBDir/$inputNameDBUp
    ;;
      Postgres)
        echo "Start running $ssgDBDir/$inputNameDBUp script on $dbVendor database $ssgDBName - \"psql $ssgDBName -f $ssgDBDir/$inputNameDBUp\"..."
        psql $ssgDBName -f $ssgDBDir/$inputNameDBUp
    ;;
      Oracle)
        echo "Database connection username?"
        read dbuser
        echo "Above user's password?"
        read dbpass
        echo "Start running $ssgDBDir/$inputNameDBUp script on $dbVendor database $ssgDBName - \"sqlplus $dbuser/$dbpass @$ssgDBDir/$inputNameDBUp\"..."
        sqlplus $dbuser/$dbpass @$ssgDBDir/$inputNameDBUp
    ;;
      *)
        echo "Bad vendor selection, database upgrade script is NOT run"
    ;;
    esac
  else
    echo "$ssgDBDir/$inputNameDBUp does not exist, so $ssgDBDir/$inputNameDBUp did NOT run"
  fi
fi
