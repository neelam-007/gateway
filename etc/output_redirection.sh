#!/bin/bash

# Valid values for $LOG_REDIRECTION_OPERATOR are '|' and '>'. Use '|' to pipe
# the output to a program. Use '>' to redirect the output to a file.
export LOG_REDIRECTION_OPERATOR=">"

# The program to pipe output to, or the file to redirect output to. The
# string '<PARTITION_NAME>' will be replaced with the name of the partition.
export LOG_REDIRECTION_DEST="/dev/null"