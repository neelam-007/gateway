#!/bin/bash
#############################################################################
# Uses liquibase to perform db actions
#############################################################################
#

# check java home
if [ -z "${SSG_JAVA_HOME}" ] ; then
    echo "Java is not configured for gateway."
    exit 13
fi
if [ ! -x "${SSG_JAVA_HOME}/bin/java" ] ; then
    echo "Java not found: ${SSG_JAVA_HOME}"
    exit 13
fi

# Check the arguments
args=$(getopt -l "help" -l "type:" -l "url:" -l "changeLogFile:" -l "username:" -l "password:" -l "command:" -o "t:u:p::c:h" -- "$@")
eval set -- "$args"

while [ $# -ge 1 ]; do
    case "$1" in
        --)
            # No more options left.
            shift
            break
           ;;
        -h|--help)
            echo "Usage: $0 --type=[mysql|derby] --url='jdbc:mysql://localhost:3306/ssg' --changeLogFile='filepath' -u 'username' -p 'userpass'"
            exit 0
            ;;
        -t|--type)
            type="$2"
            shift
            ;;
        --url)
            url="$2"
            shift
            ;;
        --changeLogFile)
            changeLogFile="$2"
            shift
            ;;
        -u|--username)
            username="$2"
            shift
            ;;
        -p|--password)
            password="$2"
            shift
            ;;
        -c|--command)
            command="$2"
            shift
            ;;
        *)
            command="$2"
            shift

    esac
    shift
done

if [[ -z "$type" ]] 
  then 
    type="mysql"
fi
if [[ -z "$url" ]]
  then 
    url="jdbc:mysql://localhost:3306/ssg"
fi
if [[ -z "$command" ]]
  then 
    command="update"
fi
if [[ -z "$changeLogFile" ]]
  then 
    echo "Missing changeLogFile. Supply a changeLogFile using the --changeLogFile option"
    exit 0
fi
if [[ -z "$username" ]]
  then 
    echo "Missing username. Supply a user name using the -u option"
    exit 0
fi    
if [[ -z "$password" ]]
  then 
    read -s -p "Enter Password: " password
    echo ""
fi    
    
# Detect install directory
CURRENT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
INSTALL_DIR="${CURRENT_DIR}/../.."

LIQUIBASE_JAR="${INSTALL_DIR}/runtime/lib/liquibase-3.2.2.jar"
MYSQL_JAR="${INSTALL_DIR}/runtime/lib/mysql-connector-java-5.1.20.jar"
DERBY_JAR="${INSTALL_DIR}/runtime/lib/derby-10.7.1.1.jar"

case "$type" in 
    "mysql")
        DB_JAR=${MYSQL_JAR}
        DB_DRIVER="com.mysql.jdbc.Driver"
        ;;
    "derby")
        DB_JAR=${DERBY_JAR}
        DB_DRIVER="org.apache.derby.jdbc.EmbeddedDriver"       
        ;;
    *)
        echo "Unknown database type. Given '${type}', expected either 'mysql' or 'derby'"
        exit 0
esac

changeLogFileName=$(basename ${changeLogFile})
changeLogFileDir=$(dirname ${changeLogFile})

pushd $changeLogFileDir > /dev/null
liquibaseOut=$( ${SSG_JAVA_HOME}/bin/java -jar ${LIQUIBASE_JAR} --classpath=${DB_JAR} --driver=${DB_DRIVER} --changeLogFile=${changeLogFileName} --url=${url} --username=${username} --password=${password} ${command} 2>&1 )
popd > /dev/null

echo "${liquibaseOut}"
exit $?
