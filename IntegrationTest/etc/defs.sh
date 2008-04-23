#!/bin/bash

export NEW_BUILD_DIR="new_build"
export CURRENT_BUILD_DIR="builds"
export AUTOTEST_DIR="AutoTest"
export JAVA_HOME="/ssg/jdk"
export ANT_HOME="$PWD/tools/ant"

export SUPPORT_FILES_DIR="/root/integration_test"

export DB_HOST="localhost"
export DB_USER="gateway"
export DB_PASS="7layer"
export DB_NAME="ssg_autotest"
export MYSQL_ROOT_USER="root"
export MYSQL_ROOT_PASSWORD=""

export INIT_SCRIPT_TIMEOUT="10"
export LOG_FILE=integration_test.log

export SNMPTRAPD_DIR="snmptrapd"
export SNMPTRAPD_PID="/var/run/snmptrapd.pid"

export TARARI_RPM="ssg-tarari-4.4.3.31s-1.i386.rpm"
export SITEMINDER_RPM="ssg-sm6-4.4-1.i386.rpm"
export SYMANTEC_RPM="ssg-symantec-4.4-1.noarch.rpm"
export JSAM_RPM="ssg-jsam-4.4-1.noarch.rpm"

export SSG_VERSION=""
if [ "$SSG_VERSION" = "4.3" ]; then
  export DB_DUMP_TGZ_FILE="dataDump_4.3.gzip"
  export DB_DUMP_FILE="dataDump_4.3.sql"
elif [ "$SSG_VERSION" = "4.3.2" ]; then
  export DB_DUMP_TGZ_FILE="dataDump_4.3.2.tar.gz"
  export DB_DUMP_FILE="dataDump_4.3.2.sql"
elif [ "$SSG_VERSION" = "4.4" ]; then
  export DB_DUMP_TGZ_FILE="dataDump_4.4.tar.gz"
  export DB_DUMP_FILE="dataDump_4.4.sql"
elif [ "$SSG_VERSION" = "HEAD" ]; then
  export DB_DUMP_TGZ_FILE="dataDump_4.4.tar.gz"
  export DB_DUMP_FILE="dataDump_4.4.sql"
else
  export DB_DUMP_TGZ_FILE="dataDump_4.4.tar.gz"
  export DB_DUMP_FILE="dataDump_4.4.sql"
fi
