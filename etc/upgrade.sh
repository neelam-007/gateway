#/bin/sh

if [ ! $# = 1 ]; then
  echo "ERROR: Usage - $0 [getFromRelease|doUpgrade]"
  exit 0
fi

option="$1"
ssgDir="/ssg/bin"
ssgDBDir="/ssg/etc/sql"

releaseFromFile="/tmp/releaseFrom.txt"

hibernateFile="/ssg/etc/conf/hibernate.properties"

if [ "$option" = "getFromRelease" ]; then
  # record RPM distribution, assumption - returns ssg-x.y.z-n pattern
  rpm -qa | grep -i ^ssg- > $releaseFromFile
fi

if [ "$option" = "doUpgrade" ]; then
  # extract x.y.z from ssg-x.y.z-n pattern from $releaseFromFile (taken before rpm distribution)
  releaseFromVersion=`cat $releaseFromFile | sed -e 's/ssg-//' | sed -e 's/-.*//'`
  # extract x.y.z from ssg-x.y.z-n pattern from ssg version taken after rpm distribution 
  releaseToVersion=`rpm -qa | grep ssg | sed -e 's/ssg-//' | sed -e 's/-.*//'`
  echo "INFO: Detected upgrade is from <$releaseFromVersion> to <$releaseToVersion>"
  # assumption - version specific upgrade file of naming convention upgrade_<releaseFromVersion>-<releaseToVersion>.sh
  upgradeSSGFile=$ssgDir/upgrade_$releaseFromVersion-$releaseToVersion.sh
  # attempt to upgrade ssg
  if [ -e $upgradeSSGFile -a -x $upgradeSSGFile -a ! -d $upgradeSSGFile ]; then
    echo "INFO: Stating sgg upgrade script $upgradeSSGFile"
    $upgradeSSGFile
  else 
    echo "WARNING: SSG upgrade script $upgradeSSGFile NOT found/run, or may not be necessary"
  fi
  # check for database vendor of mysql and database name of ssg from $hibernateFile
  dbVendor=`grep -i ^hibernate.connection.url.*jdbc:mysql.*\/ssg\? $hibernateFile`
  if [ ! "$dbVendor" = "" ]; then
    echo "INFO: SSG is running with MySQL of database name ssg - <$dbVendor> from <$hibernateFile>"
    # assumption - version specific upgrade file of naming convention upgrade_<releaseFromVersion>-<releaseToVersion>.sql
    upgradeDBFile=$ssgDBDir/upgrade_$releaseFromVersion-$releaseToVersion.sql
    # attempt to upgrade database if it's running MySQL
    if [ -e $upgradeDBFile -a ! -d $upgradeDBFile ]; then 
      echo "INFO: Invoking database upgrade with command \"mysql ssg < $upgradeDBFile\""
      mysql ssg < $upgradeDBFile
    else
      echo "WARNING: SSG database upgrade script $upgradeDBFile NOT found/run, or may not be necessary" 
    fi
  else
    echo "MANUAL TASK: If the database vendor is not MySQL and/or the SSG database name is not \"ssg\", database requires update manually if database upgrade applicable"
  fi
fi
