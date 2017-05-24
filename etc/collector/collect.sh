#!/bin/bash

VERSION="1.1"
DATESTRING=$(date +%s"_"T%R_%B_%d_%Y_%Z%z | sed 's/://g')
DEFAULTMODULE=
DEFAULTGROUP=all
OUTPUT_HOME=/home/ssgconfig
DATED_OUTPUT_NAME="dct_${DATESTRING}"
DEFAULTMODE="module"
JAVA_HOME=/opt/SecureSpan/JDK
TEMP_GATEWAY_USER_DUMPFOLDER=/tmp/heapdump_$(date +%s)

DEBUG=0

MODULE=$DEFAULTMODULE
GROUP=$DEFAULTGROUP
MODE=
HEAP_DUMP=
INCL_SENSITIVE_DATA=

export COLLECTOR_HOME=/opt/SecureSpan/Collector

# Pull in collector support functions
. ${COLLECTOR_HOME}/collectorlib


function calculatePaths
{
    BASE_OUTPUT_DIR="${OUTPUT_HOME}/${DATED_OUTPUT_NAME}"
    export ALL_MODULES_BASE_OUTPUT_DIR="${BASE_OUTPUT_DIR}/categorized-by-module"
}

# set variables for the values of several file system locations based on BASE_OUTPUT_DIR
calculatePaths


function usage
{
    echo "Usage: ./collect.sh [options...]"
    echo -e "\nAPI Gateway Data Collection Utility version ${VERSION}"
    echo "- For collecting valuable troubleshooting data for CA Support."
    echo "- Collects logs, configurations, and system metrics such as disk usage, thread dumps and heap dumps."
    echo "- The final output is categorized by module, has shortcuts to every gathered file and a compressed folder containing the same."
    echo -e "\nNote that since the output folder is uniquely named, each run will take up more disk space."
    echo "The user is responsible for managing the cleanup of these folders."
    echo -e "\nOptions:"
    echo -e "\n[-m <collection-module>]"
    echo "Collect data from a single module."
    echo "Valid values:";ls -p /opt/SecureSpan/Collector/modules | grep -v /
    echo -e "\n[-a] all"
    echo "Collect data from all modules"
    echo -e "\n[-D]"
    echo "Collect a heap dump. Use caution as this can significantly affect performance."
    echo -e "\n[-d <output-directory>]"
    echo "Where to put the files containing the output. By default, this is rooted in /home/ssgconfig."
    echo "  Specify another root path here if you want them somewhere else."
    echo -e "\n[-h help]"
    echo -e "\n[-s]"
    echo "Include sensitive data such as /etc/passwd."
    echo
}

# Prevent unintentional overwriting of past results.
function doesOutputDirectoryExist
{
    if [ -e "${BASE_OUTPUT_DIR}" ]
    then
        echo "WARNING: The output directory already exists at ${BASE_OUTPUT_DIR}.  If you proceed, data in that folder may be overwritten."
        read -p "Do you wish to proceed?  Choose y or n:  " yn
        case $yn in
            [Yy]* ) echo "Proceeding to gather data.";;
            [Nn]* ) echo "Exiting.  Consult the script help for how to specify a new output directory."; exit 1;;
            * ) doesOutputDirectoryExist;;
        esac
    fi
}

function checkForSpace
{
    if [ $(($(stat -f --format="%a*%S" "${OUTPUT_HOME}"))) -eq 0 ]
    then
        echo "Out of space on ${OUTPUT_HOME}. Terminating data collection."
        exit 1
    fi
}

# Process an individual module
# Parameters $1 = module's name
function doModule
{
    checkForSpace
    script=("${COLLECTOR_HOME}"/modules/$1)
    if [ -x "$script" ] && [ -f "$script" ]
    then
        $script "$MODULE" 2>&1
    else
        echo "Error there is no module named $1"
        return 1
    fi
}

# Process all modules
# Parameters $1 = include sensitive data
function doAll
{
    doAllInDirectory "$COLLECTOR_HOME"/modules
}

# Process all sensitive modules
function doSensitive
{
    if [ "$INCL_SENSITIVE_DATA" ]
    then
        doAllInDirectory "$COLLECTOR_HOME"/modules/sensitive
    else
        echo "Skipping sensitive data. Specify -s to include sensitive data."
    fi
}

# Process all modules in a directory (non-recursive)
# Parameters $1 = directory
function doAllInDirectory
{
    for script in "$1"/*
    do
        checkForSpace
        MODULE=$(basename "$script")
        if [ -x "$script" ] && [ -f "$script" ]
        then
            $script "$MODULE" 2>&1
        fi
    done
}

#Place all files in one folder for easier viewability
function createSymlinksToEveryFile
{
    ALL_OUTPUT_IN_ONE_FOLDER="${BASE_OUTPUT_DIR}"/links-to-all-files
    PREFIX_LENGTH=$(echo "${BASE_OUTPUT_DIR}" | wc -m)
    mkdir -p "${ALL_OUTPUT_IN_ONE_FOLDER}"
    if [ ! -w "${ALL_OUTPUT_IN_ONE_FOLDER}" ]
    then
        echo "Unable to create directory ${ALL_OUTPUT_IN_ONE_FOLDER}"
        exit 1
    fi
    find "${ALL_MODULES_BASE_OUTPUT_DIR}" -type f 2>/dev/null \
     | cut -c"$PREFIX_LENGTH"- \
     | xargs -I{} ln -s ..{} "${ALL_OUTPUT_IN_ONE_FOLDER}"
}

# Parameters $1 = directory where you want your output stored
function recalculatePaths
{
    if [ ! -w "$1" ]
    then
        echo "Your desired output directory $1 does not exist or is not writable. Please specify a different one or amend its permissions."
        exit 1
    fi

    OUTPUT_HOME=$1
    calculatePaths
}

# Collect a heap dump from the gateway process
function getHeapDump
{
    checkForSpace
    echo "Beginning heap dump"

    GATEWAY_DUMP_DIR="${ALL_MODULES_BASE_OUTPUT_DIR}/gateway/dumps"
    GW_PID=$(ps awwx | grep Gateway.jar | grep -v grep | awk '{print $1}')

    if [ "$GW_PID" != "" ]; then
        logAndRunCmd su -c \"mkdir -p "${TEMP_GATEWAY_USER_DUMPFOLDER}"\" -s /bin/sh gateway
        if [ $? -ne 0 ]
        then
            echo "Could not create temp directory"
            return 1
        fi

        logAndRunCmd su -c \""${JAVA_HOME}"/bin/jmap -dump:live,format=b,file="${TEMP_GATEWAY_USER_DUMPFOLDER}"/heap.hprof "${GW_PID}"\" -s /bin/sh gateway
        if [ $? -ne 0 ]
        then
            echo "Could not create heap dump"
            return 1
        fi

        logAndRunCmd mkdir -p "${GATEWAY_DUMP_DIR}"
        if [ $? -ne 0 ]
        then
            echo "Could not create gateway dump folder"
            return 1
        fi

        logAndRunCmd mv "${TEMP_GATEWAY_USER_DUMPFOLDER}"/heap.hprof "${GATEWAY_DUMP_DIR}/heapDump.hprof"
        if [ $? -ne 0 ]
        then
            echo "Could not move heap dump file"
            return 1
        fi

        logAndRunCmd rmdir "${TEMP_GATEWAY_USER_DUMPFOLDER}"
        if [ $? -ne 0 ]
        then
            echo "Could not remove temp directory"
            return 1
        fi
    else
        echo "Could not get Gateway PID"
        return 1
    fi

    echo "Finished heap dump"
}


while getopts "hm:aDd:s" opt; do

  case $opt in

      h)
      usage
      exit 0
      ;;

      D)
      HEAP_DUMP="true"
      ;;

      m)
      if [ "$MODE" ]
      then
          echo "Only one of -m or -a may be selected"
          usage
          exit 1
      fi
      MODE=module
      if [ "${OPTARG#-}" != "$OPTARG" ]
      then
          echo "Argument required for -m."
          exit 1
      fi
      export MODULE="$OPTARG"
      ;;

      a)
      if [ "$MODE" ]
      then
          echo "Only one of -m or -a may be selected"
          usage
          exit 1
      fi
      MODE=all
      GROUP="$OPTARG"
      ;;

      d)
      if [ "${OPTARG#-}" != "$OPTARG" ]
      then
          echo "Argument required for -d."
          usage
          exit 1
      fi
      recalculatePaths "$OPTARG"
      ;;

      s)
      INCL_SENSITIVE_DATA="true"
      ;;

      \?)
      usage

      exit 1

      ;;

  esac

done

DEBUG=0
if [ ${DEBUG} -eq 1 ]
then
    echo "Running Mode: $MODE"
    echo "MODULE: $MODULE"
    echo "GROUP: $GROUP"
fi

doesOutputDirectoryExist

if [ "$MODE" == "module" ]
then
    if doModule "$MODULE"
    then
        doSensitive
    fi
elif [ "$MODE" == "all" ]
then
    doAll
    doSensitive
elif ! [ "$HEAP_DUMP" ]
then
    echo "ERROR: No module was specified.  Please enter a module or execute collect.sh -h for help."
fi

if [ "$HEAP_DUMP" ]
then
    getHeapDump
fi

createSymlinksToEveryFile

#Compress all the output into one folder
if [ -e "${ALL_MODULES_BASE_OUTPUT_DIR}" ]
then
    FINAL_ZIP_NAME="${BASE_OUTPUT_DIR}/${DATED_OUTPUT_NAME}".tar.gz
    beginCompression
    tar -zcvf ${FINAL_ZIP_NAME} -C ${OUTPUT_HOME} -T <(echo -e "$DATED_OUTPUT_NAME/categorized-by-module\n$DATED_OUTPUT_NAME/links-to-all-files")
    endCompression "${FINAL_ZIP_NAME}"

else
    echo
    echo "ERROR: There is no collected output at ${ALL_MODULES_BASE_OUTPUT_DIR}.  Check console output for errors."
fi


