#!/bin/sh

export COLLECTOR_HOME=/opt/SecureSpan/Collector

DEFAULTMODULE=
DEFAULTLEVEL=0
DEFAULTGROUP=all
DEFAULTFILENAME=ssg.data
DEFAULTDIR=$COLLECTOR_HOME

DEBUG=0

MODULE=$DEFAULTMODULE
LEVEL=$DEFAULTLEVEL
GROUP=$DEFAULTGROUP
DIR=$DEFAULTDIR
FILENAME=$DEFAULTFILENAME


function usage () 
{
    echo 
    echo "$0: [-m collection-module] [-a] [-l detail-level] [-f file]"
    echo " -- detail-level default is 0, 0 = basic, 3 = detail, 5 = everything"
    echo
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

# Compress the file
# Parameters $1 = dir
#            $2 = filename 
function doCompress ()
{
   tar -C $1 -zcvf $2.tar.gz $2
}


while getopts "hm:al:f:" opt; do

  case $opt in

      h)
	  usage
	  exit 0
	  ;;

      m)
	  if [ $MODE ]
	  then
	      echo "Only one of -m or -a may be selected"
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

      f)
	  if [ ${OPTARG#-} != $OPTARG ]
	  then
	      echo "Argument required for -f."
	      usage
	      exit 1
	  fi
          DIR=$(dirname "$OPTARG")
          FILENAME=$(basename "$OPTARG")

	  ;;

      \?)	  
	  usage
	  exit 1

	  ;;

  esac

done

if [ $DEBUG -eq 1 ] 
then
    echo "Running Mode: $MODE"
    echo "MODULE: $MODULE"
    echo "GROUP: $GROUP"
    echo "LEVEL: $LEVEL"
    echo "DIR: $DIR"
    echo "FILENAME: $FILENAME"
fi

if [ ! -d "$DIR" ]
then
    echo "$DIR doesn't exist."
    exit 1
fi

if [ ! -w "$DIR" ]
then
    echo "You don't have write access to $DIR"
    exit 1
fi

FILE="$DIR/$FILENAME"

if [ $DEBUG -eq 1 ] 
then
    echo "FILE: $FILE"
fi



cp /dev/null $FILE

if [ $MODE == "module" ]
then
    doModule $MODULE $LEVEL | tee $FILE
    doCompress $DIR $FILENAME

elif [ $MODE == "all" ]
then
    doAll $LEVEL | tee $FILE
    doCompress $DIR $FILENAME
fi

