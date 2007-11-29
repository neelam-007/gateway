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
      echo "This utility needs to be run as the ssgconfig user. If prompted, please enter the ssgconfig login password."
  fi
}
