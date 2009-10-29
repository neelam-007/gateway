#!/bin/bash
#
# Script for the starting/stopping the SecureSpan Process Controller in software mode

# Config
PID_FILE="sspcd"
PC_USER="gateway"
PC_SCRIPT="/opt/SecureSpan/Controller/bin/processcontroller.sh"

start() {
        if [ ! -x "${PC_SCRIPT}" ] ; then
            echo "Service script not found or not executable."
            exit 5
        fi

        echo -n $"Starting Gateway Services: "
        "${PC_SCRIPT}" "${PC_USER}" "/var/run/${PID_FILE}.pid"
        RETVAL=$?
        if [ $RETVAL -eq 0 ] ; then
          echo done.
        else
          echo failed.
        fi
        return $RETVAL
}

stop() {
        echo -n $"Shutting down Gateway Services: "
        [ -f "/var/run/${PID_FILE}.pid" ] && pid=$(<"/var/run/${PID_FILE}.pid") 
        if [ -n "${pid}" -a -d /proc/${pid} ] ; then
            kill -TERM "${pid}" 2>&1 >/dev/null
            sleep 3
            if [ -d /proc/${pid} ] ; then
                kill -KILL "${pid}"
            fi
            KILLWAIT=0
            while [ $KILLWAIT -lt 10 ]
            do
              KILLWAIT=`expr $KILLWAIT + 1`
              if [ -d /proc/${pid} ] ; then
                sleep 1
              else
                echo " done."
                RETVAL=0
                return ${RETVAL}
              fi
            done
        else
            echo "Process Controller not running."
            RETVAL=2
            return ${RETVAL}
        fi

        echo " failed."
        RETVAL=1
        return ${RETVAL}
}

status() {
        if [ -f "/var/run/${PID_FILE}.pid" ] ; then
            PID=$(<"/var/run/${PID_FILE}.pid")
            if [ ! -z "${PID}" ] && [ -d "/proc/${PID}" ] ; then
                echo "process controller pid $PID is running..."
                return 0
            else
                echo "process controller dead but pid file exists"
                return 1
            fi
        else
            echo "process controller is stopped"
            return 3
        fi
}

case "$1" in
  start)
	start
	;;
  stop)
	stop
	;;
  restart)
	stop
        sleep 5
	start
	;;
  status)
        status sspcd
        ;;
  *)
        echo $"Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac

exit $RETVAL
