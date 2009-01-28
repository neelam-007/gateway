
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
        EJ_javaver=`${SSG_JAVA_HOME}/bin/java -version 2>&1 | awk -F\" '/version/ {print $2}' | awk -F\. '{print $1"."$2}'`;

        if [ "${EJ_javaver}" != "${1}" ]; then
            echo "Java ${1} is required, but ${EJ_javaver} was found."
            exit 2
        fi

        unset EJ_javaver
    fi
}
