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
			my $out = "";
			my $begin = "";
			my $end = "";
			$line = "";
			if ( /\[0:0\] -I INPUT \$Rule_Insert_Point (.*)/ ) {
				$line=$1;

				# 4 forms of the line
				# -p tcp -m tcp --dport 8444 -j ACCEPT
				# Single port
				if ($line=~/^\s*-p tcp -m tcp --dport (\d+) -j ACCEPT/) {
					$out= "pass in quick proto tcp from any to any port = $1 flags S keep state\t$flag\n";
				} #continues...

				# -p tcp -m tcp --dport 8444:8555 -j ACCEPT
				# Port Range	
				elsif ($line=~/^\s*-p tcp -m tcp --dport (\d+)\:(\d+) -j ACCEPT/) {
					$begin=$1; $end=$2; $begin--; $end++;
					$out= "pass in quick proto tcp from any to any port $begin >< $end flags S keep state\t$flag\n";
				} #continues...

				# -d 192.168.1.186 -p tcp -m tcp --dport 8081 -j ACCEPT
				# IP addy and port
				elsif ($line=~/^\s*-d (\d+\.\d+\.\d+\.\d+) -p tcp -m tcp --dport (\d+) -j ACCEPT/) {
					$out= "pass in quick proto tcp from any to $1 port = $2 flags S keep state\t$flag\n";
				} #continues...

				# -d 192.168.1.186 -p tcp -m tcp --dport 9444:9555 -j ACCEPT
				# IP Addy and port range
				elsif ($line=~/^\s*-d (\d+\.\d+\.\d+\.\d+) -p tcp -m tcp --dport (\d+)\:(\d+) -j ACCEPT/) {
					$begin=$2; $end=$3; $begin--; $end++;
					$out= "pass in quick proto tcp from any to $1 port $begin >< $end flags S keep state\t$flag\n";
				} #OR ELSE! ;)	
				#print $line . "\n";
				#print "\t$out\n";
				$outputFh->print($out);
			} else {
				#Whining isn't required. I hope.
				print "Firewall item not understood:\t$line\n";
			}
		}
	}
	$outputFh->close();
} else {
	print "Couldn't open $ruleset: $!\n";
	exit 1;
}

#print "Partition Firewall Done\n";
