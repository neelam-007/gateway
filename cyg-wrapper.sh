#!/bin/sh
# ======================================================================
# AUTHOR:	Luc Hermitte <EMAIL:hermitte at free.fr>
# 		<URL:http://hermitte.free.fr/cygwin/cyg-wrapper.sh>
# CREDITS:	Jonathon M. Merz -> --fork option
# 
# LAST UPDATE:	05th dec 2002
version="2.2"
#
# NOTE:    {{{
#   IMPORTANT: this shell script is for cygwin *ONLY*!
#
#   This version requires cygUtils ; and more precisally: realpath (1)
#
#   A slower version not requiring cyg-utils is available at:
#   http://hermitte.free.fr/cygwin/cyg-wrapper-1.1.sh
#
# PURPOSE: {{{1
#   This script wraps the calls to native win32 tools from cygwin. The
#   pathnames of the file-like arguments for the win32 tools are
#   translated in order to be understandable by the tools. 
#
#   It converts any pathname into its dos (short) from.
#   Symbolic links are also resolved, whatever their indirection level is.
#
#   It accepts windows pathnames (relative or absolute) (backslashed or
#   forwardslashed), *nix pathnames (relative or absolute). 
#
# USAGE:   {{{1

usageversion() {
    prog=`basename $0`
    usage="Usage: $prog PROGRAM [OPTIONS] [PROG-PARAMETERS...]"
    if [ $# = 0 ] ; then
	echo $usage >&2
    else
	cat <<END
# $prog $version
Copyright (c) 2001-2002 Luc Hermitte
  This is free software; see the GNU General Public Licence version 2 or later
  for copying conditions.  There is NO warranty.

Purpose:
   This script wraps the calls to native win32 programs from cygwin. 
   The pathnames of the file-like arguments for the win32 programs are
   translated in order to be understandable by the programs. 

$usage
Parameters:
    PROGRAM
	Pathname to the native win32 program to execute.
	Must be specified in the DOS (short) form on MsWindows 9x systems.
    PROG-PARAMETERS
	List of parameters passed to PROGRAM.
Options:
    --slashed-opt
	Program-parameters expressed as '-param' will be converted to '/param' 
    --binary-opt=BIN-OPTIONS
	BIN-OPTIONS is a comma separated list of binary options for PROGRAM
	Only list options that are not expecting a file as the second element
	of the pair.
    --cyg-verbose
	Display the command really executed
    --fork=<0 or 1>
	1: Forks the new process so the shell is still accessible.
	0: (default) Does not fork the new process.  Can be used to
	             override a previous --fork=1 argument.

Typical use:
  The easiest way to use this script is to define aliases. 
  ~/.profile is a fine place to do so.

Examples:
  alias gvim='cyg-wrapper.sh "C:/Progra~1/Edition/vim/vim61/gvim.exe" 
	--binary-opt=-c,--cmd,-T,-t,--servername,--remote-send,--remote-expr
	--fork=1'

  alias explorer='cyg-wrapper.sh "explorer" --slashed-opt'
END
    fi
}

if [ $# = 0 ] ; then
    usageversion
    exit 1
elif [ `expr "$1" : "-h$\|--help$"` -gt 0 ] ; then 
    usageversion -h
    exit 0
fi

# TODO: {{{1
# (*) convert parameters like: '--xxx={path}' or 'X{path}'
#
# }}}1
# ======================================================================
# Function WindowsPath() {{{
# Computes a windows path (somehow like cygpath)
WindowsPath() {
    # Problem1: some programs see "xxx\ yyy" as two parameters, hence the
    # path will be expressed in the windows short form.
    # Problem2: windows short names can't be deduced for files that do
    # not exist yet, hence the file name will be expressed completly.

    # a- rebuild pathname that arrives split between the different $i
    pathname=
    while [ $# -gt 0 ] ; do
	# This can't be reduced to 'pathname="${pathname} $1"' only
	# because of the spaces that will be added.
	if [ -n $pathname"" ] ; then
	    pathname="${pathname} $1"
	else
	    pathname="$1"
	fi
	shift
    done
    # b- compute dirname and basename because of Pb2
    # Note the double quotes that are required
    file=`basename "$pathname"`
    folder=`dirname "$pathname"`
    # c- compute the folder path in windows short form
    shortfolder=`cygpath -ws "$folder"`
    # d- return the result
    # sed changes: 
    # 	'\\' -> '\' ; because of files like 'c:\autoexec.bat'
    # 	and then '\' -> '\\' ; because of the way xarg is called.
    # [so, at the end there is no '\\\\']
    echo "$shortfolder\\$file" | sed 's#\\\\#\\#g;s#\\#\\\\#g'
}
# }}}
# ======================================================================



# ======================================================================
# I- Initialisations: {{{
# I.a- Store the program name.
param="$1 "
shift;

# Special arguments:
# --slashed-opt	 : says that the program wait parameters of the form '/opt'
# --binary-opt={opts} : list of binary options whose 2nd element is not a file
# --cyg-verbose  : force echo of the command really executed
# --fork=<[0]|1> : explicitly fork the process
slashed_opt=0
binary_args=""
cyg_verb=0
fork_option=0

# I.b- checks cyg-wrapper.sh's arguments
while [ $# -gt 0 ] ; do
    if [ $1"" = "--slashed-opt" ] ; then
	slashed_opt=1
	shift;
    elif [ $1"" = "--cyg-verbose" ] ; then
	cyg_verb=1
	shift;
    elif [ `expr "$1" : '--binary-opt=.*'` -gt 0 ] ; then
	binary_args=`expr $1 : '--binary-opt=\(.*\)'`
	shift;
    elif [ `expr "$1" : '--fork=[01]'` -gt 0 ] ; then
	fork_option=`expr $1 : '--fork=\(.\)'`
	shift;
    else
	break;
    fi
done
# }}}
# ======================================================================
# II- Main loop {{{
# For each argument ...
while [ $# -gt 0 ] ; do
    if [ `expr ",$binary_args," : ".*,$1,"` -gt 0 ] ; then
	# Binary arguments : the next is to be ignored (not a file).
	# echo "<$1> found into <$binary_args>"
	ptransl="$1 '$2'"
	shift;
    elif [ `expr "$1" : "[+-].*"` -gt 0 ] ; then
	# Program arguments : must be ignored (not passed to cygpath)
	if [ $slashed_opt = 1 ] ; then
	    ptransl=`echo "K$1" | sed 's#K-#/#g'`
	else
	    ptransl="$1"
	fi
    else
	# Convert pathname "$1" to absolute path (*nix form) and resolve
	# all the symbolic links
	ptransl=`realpath "$1"`
	# Convert the pathname to DOS (short) form
	ptransl=`WindowsPath "$ptransl"`
    fi
    # Build the parameters-string
    param="$param $ptransl"
    shift;
done
# }}}
# ======================================================================
# III- Execute the proper tool with the proper arguments. {{{
# comment the next line if you don't want anything echoed
if [ $cyg_verb = 1 ] ; then
    echo "$param" 
fi
# call PROGRAM
if [ $fork_option = 1 ] ; then
    `echo "$param" | xargs` &
else
    `echo "$param" | xargs`
fi
# }}}
# ======================================================================
# vim600: set fdm=marker tw=79:
