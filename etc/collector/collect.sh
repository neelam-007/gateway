#!/bin/sh

VERSION="1.1"
DATESTRING=$(date +%s"_"T%R_%B_%d_%Y_%Z%z | sed 's/://g')
DEFAULTMODULE=
DEFAULTLEVEL=1
DEFAULTGROUP=all
DEFAULT_BASE_PARENT_OUTPUT_DIR=/tmp/datacollector
DEFAULT_BASE_DATED_OUTPUT_DIR="${DEFAULT_BASE_PARENT_OUTPUT_DIR}"_"${DATESTRING}"
DEFAULTMODE="module"

DEBUG=0

BASE_OUTPUT_DIR="${DEFAULT_BASE_DATED_OUTPUT_DIR}"
MODULE=$DEFAULTMODULE
LEVEL=$DEFAULTLEVEL
GROUP=$DEFAULTGROUP
MODE=
HEAPDUMP_ENABLED=

export ALL_MODULES_BaseOutputDirectory="${DEFAULT_BASE_DATED_OUTPUT_DIR}/categorized-by-module"
export COLLECTOR_HOME=/opt/SecureSpan/Collector

# Pull in collector support functions
. ${COLLECTOR_HOME}/collectorlib

function usage () 
{
    echo "Usage:"
    echo -e "\nAPI Gateway Data Collection Utility version ${VERSION}"
    echo "- For collecting valuable troubleshooting data for CA Support."
    echo "- Collects logs, configurations, and system metrics such as disk usage, thread dumps and heap dumps."
    echo "- The final output is categorized by module, has shortcuts to every gathered file and a compressed folder containing the same."
    echo -e "\nNote that since the output folder is uniquely named, each run will take up more disk space."
    echo "The user is responsible for managing the cleanup of these folders."
    echo -e "\nCommands:"
    echo -e "\n[-m <collection-module>]"
    echo "Collect data from a single module. Valid values:";ls /opt/SecureSpan/Collector/modules
    echo -e "\n[-a] all"
    echo "Collect data from all modules"
    echo -e "\n[-l detail-level]"
    echo "1 = Basic, 2 = Medium, 3 = High.  Default is "${DEFAULTLEVEL}"."
    echo " Examples of data collected from the detail levels:"
    echo "   1 = SSG logs, node.properties, my.cnf"
    echo "   2 = /etc/sudoers, /etc/passwd, /etc/group"
    echo "   3 = *Currently nothing at this level.  Note that heap dumps have a separate flag to enable."
    echo -e "\n[-D]"
    echo "Heap Dump. Use caution when taking heap dumps as this can significantly affect performance."
    echo -e "\n[-o output-directory]"
    echo "Default is: ${DEFAULT_BASE_PARENT_OUTPUT_DIR}_<datestamp>"
    echo -e "\n[-h help]"
    echo
}

# Prevent unintentional overwriting of past results.
function doesOutputDirectoryExist ()
{
    if [ -e ${BASE_OUTPUT_DIR} ]
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

# Process an individual module
# Paramters $1 = modules name,
#           $2 = detail level
function doModule ()
{
    script=(${COLLECTOR_HOME}/modules/$1)
    if [ -e $script ] 
    then
      $script $MODULE $2 2>&1
    else 
      echo "Error there is no module named $1"
    fi
}

# Process an individual module
# Paramters $1 = detail level
function doAll ()
{
    for script in $COLLECTOR_HOME/modules/*
    do
       MODULE=`basename $script`
       if [ -x $script ]
       then
       $script $MODULE $1 2>&1
       fi
    done
}

#Place all files in one folder for easier viewability
function createSymlinksToEveryFile ()
{
    ALL_OUTPUT_IN_ONE_FOLDER="${BASE_OUTPUT_DIR}"/links-to-all-files
    mkdir -p "${ALL_OUTPUT_IN_ONE_FOLDER}"
    find "${ALL_MODULES_BaseOutputDirectory}" -type f 2>/dev/null \
     | xargs -I{} ln -s {} "${ALL_OUTPUT_IN_ONE_FOLDER}"
}


while getopts "hm:al:o:D" opt; do

  case $opt in

      h)
      usage
      exit 0
      ;;

      D)
      export HEAPDUMP_ENABLED="true"
      echo "Heap-dump enabled"
      ;;

      m)
      if [ $MODE ]
      then
          echo "Only one of -m or -a may be selected"
          usage
          exit 1
      fi
      MODE=module
      if [ ${OPTARG#-} != $OPTARG ]
      then
          echo "Argument required for -m."
          exit 1
      fi
      export MODULE=$OPTARG
      ;;

      a)
      if [ $MODE ]
      then
          echo "Only one of -m or -g may be selected"
          usage
          exit 1
      fi
      MODE=all
      GROUP=$OPTARG
      ;;

      l)
      if [ ${OPTARG#-} != $OPTARG ]
      then
          echo "Argument required for -l."
          usage
          exit 1
      fi
      LEVEL=$OPTARG

      ;;

      o)
      if [ ${OPTARG#-} != $OPTARG ]
      then
          echo "Argument required for -o."
          exit 1
      fi
      export ALL_MODULES_BaseOutputDirectory=$OPTARG
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
    echo "LEVEL: $LEVEL"
fi

doesOutputDirectoryExist

if [ "$MODE" == "module" ]
then
    doModule $MODULE $LEVEL

elif [ "$MODE" == "all" ]
then
    doAll $LEVEL
else
    echo "ERROR: No module was specified.  Please enter a module."
fi

createSymlinksToEveryFile

#Compress all the output into one folder
if [ -e ${ALL_MODULES_BaseOutputDirectory} ]
then
    FINAL_ZIP_NAME=${BASE_OUTPUT_DIR}/SSG_DataCollection_${DATESTRING}.tar.gz
    beginCompression
    tar -zcvf ${FINAL_ZIP_NAME} --exclude='*.tar.gz' ${BASE_OUTPUT_DIR}
    endCompression "${FINAL_ZIP_NAME}"

else
    echo
    echo "ERROR: There is no collected output at ${ALL_MODULES_BaseOutputDirectory}.  Check console output for errors."
fi


