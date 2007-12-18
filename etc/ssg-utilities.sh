ensure_JDK() {
  if [ -z "${SSG_JAVA_HOME}" ] ; then
      echo "No JDK is configured. Please run '/ssg/bin/install.sh' to configure."
      exit 1;
  fi
  if [ ! -e "${SSG_JAVA_HOME}" ] ; then
      echo "The JDK you have specified (${SSG_JAVA_HOME}) does not exist."
      echo "Please run ${SSG_HOME}/bin/install.sh to configure."
      exit 2;
  fi
}

check_user() {
  USER=${LOGNAME}
  if [ "$USER" != "ssgconfig" ] && [ "$USER" != "root" ] ; then
      echo "This utility needs to be run as the ssgconfig user."
      echo "If prompted, please enter the ssgconfig login password."
  fi
}

do_command_as_user() {
    WHICHUSER=$1
    WHICHCOMMAND=$2
   
    echo ""
    if [ "${LOGNAME}" != root ] ; then
        echo "Please enter the password for ${WHICHUSER}"
    fi
    su ${WHICHUSER} -c "${WHICHCOMMAND}"
}
