#!/bin/bash
umask 0002

cd `dirname $0`
pushd .. > /dev/null
SSG_HOME=`pwd`
popd > /dev/null

. ${SSG_HOME}/etc/profile

if [ "$1" = "jpda" ] ; then
  if [ -z "$JPDA_TRANSPORT" ]; then
    JPDA_TRANSPORT="dt_socket"
  fi
  if [ -z "$JPDA_ADDRESS" ]; then
    JPDA_ADDRESS="8000"
  fi
  if [ -z "$JPDA_OPTS" ]; then
    JPDA_OPTS="-Xdebug -Xrunjdwp:transport=$JPDA_TRANSPORT,address=$JPDA_ADDRESS,server=y,suspend=n"
  fi
  JAVA_OPTS="$JAVA_OPTS $JPDA_OPTS"
  shift
fi

if [ -z "${SSG_HOME}" ] ; then
  echo SSG_HOME not set
  exit 17
fi

cd ${SSG_HOME}

if [ "$1" = "start" ] ; then
   shift
   if [ ! -z "$GATEWAY_SHUTDOWN" ]; then
       rm -f $GATEWAY_SHUTDOWN
   fi

    #enable logging of stdout/stderr using JDK logging as well as the standard SSG logging facilities
    ${SSG_JAVA_HOME}/bin/java -Djava.ext.dirs="${SSG_JAVA_HOME}/jre/lib/ext:${SSG_HOME}/lib/ext" -Djava.util.logging.config.class=com.l7tech.server.log.JdkLogConfig $JAVA_OPTS -jar Gateway.jar "$@" &

    if [ ! -z "$GATEWAY_PID" ]; then
        echo $! > $GATEWAY_PID
    fi

elif [ "$1" = "run" ] ; then
   shift
   if [ ! -z "$GATEWAY_SHUTDOWN" ]; then
       rm -f $GATEWAY_SHUTDOWN
   fi

    ${SSG_JAVA_HOME}/bin/java -Djava.ext.dirs="${SSG_JAVA_HOME}/jre/lib/ext:${SSG_HOME}/lib/ext" $JAVA_OPTS -jar Gateway.jar "$@"

    if [ ! -z "$GATEWAY_PID" ]; then
        echo $! > $GATEWAY_PID
    fi

elif [ "$1" = "stop" ] ; then
  shift

  FORCE=0
  if [ "$1" = "-force" ]; then
    shift
    FORCE=1
  fi

  if [ ! -z "$GATEWAY_SHUTDOWN" ]; then
    touch $GATEWAY_SHUTDOWN
    sleep 2
  else
    if [ $FORCE -eq 0 ]; then
      echo Shutdown failed -- must either provide GATEWAY_SHUTDOWN or use -force
      exit 21
    fi
  fi

  if [ $FORCE -eq 1 ]; then
    if [ ! -z "$GATEWAY_PID" ]; then
       echo "Killing: `cat $GATEWAY_PID`"
       kill -9 `cat $GATEWAY_PID`
    else
       echo "Kill failed: \$GATEWAY_PID not set"
    fi
  fi
fi


