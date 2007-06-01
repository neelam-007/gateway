#!/usr/bin/perl -w
use strict;
use File::Basename;
use FileHandle;

# WAN interface
my $interface = "e1000g0";

my $outputFh = new FileHandle;

# The ruleset that partitionControl will load. Use full path.
#my $ruleset="ssg-ipf.conf";
my $ruleset="/ssg/etc/ssg-ipf.conf";
my @rules;

#Read in the rules, because we need to no matter what.
if (open(RULESET, $ruleset)) {
	@rules = <RULESET>;	
} else {
	print "File does not exist: $ruleset $!\n";
	exit 1;
}

# Ruleset is $argv[1]
my $fname=shift(@ARGV);
if ( -e $fname ) {
	open RULES, $fname;
} else {
	print "File does not exist: $fname $!\n";
	exit 1;
}

# Assume my partition name is the rules' parent dir
# There is probably something prettier than this.
my $partition = dirname($fname);
$partition =~ m/(.*\/)(.*)$/;
$partition = $2;

# $mode is stop or start
my $mode=shift(@ARGV);
if ( $mode !~/[(stop)|(start)|(run)]/ ) {
	print "Mode is incorrect: $mode\n";
	exit 1;
}

# expected Format:
# [0:0] -I INPUT $Rule_Insert_Point -p tcp -m tcp --dport $HTTP_PORT -j ACCEPT

my $flag = "#" . $partition . "L7";

#print "flag = $flag\n";

if ($outputFh->open(">$ruleset")) {
	my $line = "";

	# Write out the existing rules, skipping any with current flag
	# (even if starting, as we'll write them a new in the block below)
	foreach $line (@rules) {
		if ($line =~ /$flag/) {
			# This is an existing rule with the current flag. DO NOT WANT
			#print "Removing $line\n";
		} else {
			$outputFh->print("$line");
		}
	}

	# Write out the new rules only if we're starting
	if ( $mode eq "start") {
		while (<RULES>) {
			if ( /\[0:0\] -I INPUT \$Rule_Insert_Point (.*)/ ) {
				$line=$1;
				$line =~ /(\d+)/;

				# This is our template rule here, $flag is appened for later removal
				$line = "pass in quick on $interface proto tcp from any to any port = $1 flags S keep state\t$flag\n";

				#print "$line\n";
				$outputFh->print("$line");
			} else {
				print "Invalid!\t$line\n";
			}
		}
	}
	$outputFh->close();
} else {
	print "Couldn't open $ruleset: $!\n";
	exit 1;
}

#print "Partition Firewall Done\n";
