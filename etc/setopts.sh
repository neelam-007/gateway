#reads the options listed in jvmoptions and appends them to the SSG_JAVA_OPTS environment variable


APPLIANCE="$SSG_HOME/appliance"
JVM_OPTIONS="$SSG_HOME/etc/profile.d/jvmoptions"

if [ ! -e ${APPLIANCE} ] && [ -s ${JVM_OPTIONS} ] ; then
        SSG_JAVA_OPTS="$SSG_JAVA_OPTS `tr '\n' ' ' < "${JVM_OPTIONS}"`"
        export SSG_JAVA_OPTS
fi

