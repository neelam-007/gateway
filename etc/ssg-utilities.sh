
#
# Check the Java path and optionally version
#
ensure_JDK() {
    if [ -z "${SSG_JAVA_HOME}" ] ; then
        echo "No JDK is configured. Please run ${SSG_HOME}/runtime/bin/setup.sh to configure."
        exit 1;
    fi

    if [ ! -e "${SSG_JAVA_HOME}" ] ; then
        echo "The JDK you have specified (${SSG_JAVA_HOME}) does not exist."
        echo "Please run ${SSG_HOME}/runtime/bin/setup.sh to configure."
        exit 2;
    fi

    if [ ! -z "${1}" ] ; then
        EJ_javaver=$("${JAVA_HOME}/bin/java" -version 2>&1 | awk -F\" '/version/ {print $2}' | awk -F\. '{print $1"."$2}');

        EJ_want_major=$(echo "${1}" | awk -F'.' '{print $1}')
        EJ_want_minor=$(echo "${1}" | awk -F'.' '{print $2}')
        EJ_javaver_major=$(echo "${EJ_javaver}" | awk -F'.' '{print $1}')
        EJ_javaver_minor=$(echo "${EJ_javaver}" | awk -F'.' '{print $2}')

        if [ "${EJ_want_major}" -gt "${EJ_javaver_major}" ] ; then
            echo "Java ${1} is required, but ${EJ_javaver} was found."
            exit 1
        fi

        if [ "${EJ_want_major}" -eq "${EJ_javaver_major}" ] && [ "${EJ_want_minor}" -gt "${EJ_javaver_minor}" ]; then
            echo "Java ${1} is required, but ${EJ_javaver} was found."
            exit 1
        fi

        unset EJ_javaver EJ_want_major EJ_want_minor EJ_javaver_major EJ_javaver_minor
    fi
}
