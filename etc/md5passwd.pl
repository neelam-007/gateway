#!/usr/bin/perl
# -----------------------------------------------------------------------------
# FILE [md5passwd.pl]
# LAYER 7 TECHNOLOGIES
# 30-06-2003, flascelles
#
# THIS SCRIPT PRODUCES THE HEX MD5 OF WHATEVER IS PASSED AS AN ARGUMENT
#
# this had been placed in an individual script because I was getting shell quoting issues
#
# -----------------------------------------------------------------------------
use Digest::MD5 qw(md5_hex);
print md5_hex($ARGV[0]);