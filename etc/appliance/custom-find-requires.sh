#! /bin/bash

debug() {
	echo "DEBUG: $@" 1>&2
}

REGULAR_FIND_REQUIRES_SCRIPT=$1

while read FILE_TO_SCAN; do
	if (echo "$FILE_TO_SCAN" | grep -F "opt/SecureSpan/JDK" > /dev/null); then
		continue
	fi

	# note that the regular find requires script outputs its results to stdout, so don't use stdout for debugging
	echo "$FILE_TO_SCAN" | $REGULAR_FIND_REQUIRES_SCRIPT
done
