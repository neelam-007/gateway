#!/bin/sh

option="$1"

# verify number of parameter
if [ ! $# = 1 ]; then
  echo "ERROR: Missing/Too many parameter"
  echo "ERROR: Usage - $0 [getFromRelease|getToRelease|doUpgrade]"
  echo "ERROR: <$0 $option> exits"
  exit 1
fi

# verify parameter validity
if [ "$option" = "getFromRelease" -o "$option" = "getToRelease" -o "$option" = "doUpgrade" ]; then
  echo "INFO: Running script <$0 $option>"
else
  echo "ERROR: Invalid parameters <$option>"
  echo "ERROR: Usage - $0 [getFromRelease|getToRelease|doUpgrade]"
  echo "ERROR: <$0 $option> exits"
  exit 1
fi

ssgDir="/ssg/bin"
ssgDBDir="/ssg/etc/sql"

releaseFromFile="/tmp/releaseFrom.txt"
releaseToFile="/tmp/releaseTo.txt"

hibernateFile="/ssg/etc/conf/hibernate.properties"

if [ "$option" = "getFromRelease" -o "$option" = "getToRelease" ]; then
  # purge file, as doUpgrade base on its existence to verify if previous/current version number found and upgrade can be proceed
  if [ "$option" = "getFromRelease" ]; then 
    rm -f $releaseFromFile
  elif [ "$option" = "getToRelease" ]; then
    rm -f $releaseToFile
  fi
  # mapFile for storing buildNumber-versionNumber map
  mapFile="/ssg/bin/buildVersion.txt"
  if [ ! -e $mapFile -o -d $mapFile ]; then
    echo "WARNING: Not applicable if there's no SSG version specific upgrade srcipt to be run - Missing <$mapFile> - unable to derive the installed SSG version number"
    exit 1
  fi

  # verify existence for /ssg/dist/ROOT-b<buildNumber>.war
  if [ ! -e /ssg/dist/ROOT-b*.war -o -d /ssg/dist/ROOT-b*.war ]; then
    echo "ERROR: Missing WAR distribution </ssg/dist/ROOT-b*.war> - unable to derive the installed SSG build number"
    exit 1
  fi

  # derive build number from /ssg/dist/ROOT-b<buildNumber>.war
  # expected naming convention of the war file would be ROOT-b<buildNumber>.war so <buildNumber> can be extracted
  buildNumber=`/bin/ls /ssg/dist/ROOT-b*.war | sed -e 's/^.*ROOT-b//' | sed -e 's/\.war//'`
  # verify $mapFile contains only unique record for the $buildNumber
  grepCount=`grep -c ^.*$buildNumber.*= $mapFile | sed -e 's/^.*=//'`
  echo $grepCount
  if [ ! "$grepCount" = 1 ]; then 
    echo "WARNING: Not applicable if there's no SSG version specific upgrade srcipt to be 
run - <$mapFile> contains more than one record (total of <$grepCount>) for buildNumber <$buildNumber>"
    echo `grep ^.*$buildNumber.*= $mapFile`
    exit 1
  fi
  # get version number base on build number from $mapFile
  versionNumber=`grep ^.*$buildNumber.*= $mapFile | sed -e 's/^.*=//'`
  if [ "$option" = "getFromRelease" ]; then
    echo "INFO: Save fromRelease version number <$versionNumber> to <$releaseFromFile>"
    echo $versionNumber > $releaseFromFile
  elif [ "$option" = "getToRelease" ]; then
    echo "INFO: Save toRelesae version number <$versionNumber> to <$releaseToFile>"
    echo $versionNumber > $releaseToFile
  fi
fi

if [ "$option" = "doUpgrade" ]; then
  # SSG version specific upgrade
  echo "INFO: Performing Upgrade"
  # get $releaseToFile
  $0 getToRelease
  # verify the existence of $releaseFromFile and $releaseToFile, if they do not exist means no records from $mapFile 
  if [ ! -e $releaseFromFile -o -d $releaseFromFile -o ! -e $releaseToFile -o -d $releaseToFile ]; then
    echo "WARNING: <$releaseFromFile>/<$releaseToFile> NOT found/run therefore SSG version specific upgrade script can not be deduced - SSG version specific upgrade not applicable"
    # inform user to upgrade database if necessary
    echo "MANUAL TASK: Please update database by running sql script (located at $ssgDBDir/) manually if applicable"
    exit 0
  fi
  releaseFromVersion=`cat $releaseFromFile`
  releaseToVersion=`cat $releaseToFile`
  echo "INFO: Detected upgrade is from <$releaseFromVersion> to <$releaseToVersion>"
  # assumption - version specific upgrade file of naming convention upgrade_<releaseFromVersion>-<releaseToVersion>.sh
  upgradeSSGFile=$ssgDir/upgrade_$releaseFromVersion-$releaseToVersion.sh
  # attempt to upgrade ssg
  if [ -e $upgradeSSGFile -a -x $upgradeSSGFile -a ! -d $upgradeSSGFile ]; then
    echo "INFO: Stating sgg upgrade script <$upgradeSSGFile>"
    $upgradeSSGFile
  else 
    echo "WARNING: SSG upgrade script <$upgradeSSGFile> NOT found/run - SSG version specific upgrade not applicable"
    # inform user to upgrade database if necessary
    echo "MANUAL TASK: Please update database by running sql script (located at $ssgDBDir/) manually if applicable"
  fi
fi
