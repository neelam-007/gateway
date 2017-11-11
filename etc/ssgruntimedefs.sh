# LAYER 7 TECHNOLOGIES
# Defines JAVA_HOME, etc

default_java_opts="-server "

if [ ! -f "/etc/sysconfig/network" -o  "$(grep "^NETWORKING_IPV6=yes" /etc/sysconfig/network 2>/dev/null)" ]
then
  default_java_opts="$default_java_opts -Djava.net.preferIPv4Stack=false "
else
  default_java_opts="$default_java_opts -Djava.net.preferIPv4Stack=true "
fi

default_java_opts="$default_java_opts -Djava.security.policy=${SSG_HOME}/runtime/etc/ssg.policy"
default_java_opts="$default_java_opts -Djava.security.egd=file:/dev/./urandom"
default_java_opts="$default_java_opts -Dfile.encoding=UTF-8 "
default_java_opts="$default_java_opts -Djava.awt.headless=true -XX:CompileThreshold=1500 "
default_java_opts="$default_java_opts -Dcom.l7tech.server.defaultClusterHostname=$(hostname)"

SSG_JAVA_OPTS="$SSG_JAVA_OPTS $default_java_opts";

unset default_java_opts

export SSG_JAVA_OPTS

if ! echo $LD_LIBRARY_PATH | /bin/egrep -s "(^|:)$SSG_HOME/runtime/lib($|:)" >/dev/null ; then
	LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$SSG_HOME/runtime/lib
fi

export LD_LIBRARY_PATH
