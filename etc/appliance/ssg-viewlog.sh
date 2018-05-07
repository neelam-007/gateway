#!/bin/sh
#
# Read a range of bytes from a file
# Args: filename start bytes_to_read

[ ! $# -eq 3 ] && echo "Invalid number of arguments." && exit
[[ ! ($2 =~ ^[0-9]+$ && $3 =~ ^[0-9]+$) ]] && echo "Invalid argument value(s)." && exit
dd if="${1}" skip="${2}" count="${3}" bs=1 2>/dev/null
