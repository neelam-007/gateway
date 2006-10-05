#!/usr/bin/perl

require 5.005;
use strict;

use File::Copy;
use Getopt::Std;

chdir("../");
my $ret = system "./build.sh", "make_installers";
my $exit_value = $ret;
die "FATAL: build.sh exited $exit_value" if $exit_value;
