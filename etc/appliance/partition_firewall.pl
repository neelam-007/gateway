#!/usr/bin/perl

use strict;

my $DEBUG=0;

# Runs from the partition startup "ssg-init"
#
# Ruleset is $argv[1]

my $fname=shift(@ARGV);
my $mode=shift(@ARGV);

# $mode is stop or start


if ( -e $fname ) {
	open RULES, $fname;
} else {
	print "File does not exist: $fname $!\n";
	exit 1;
}

if ( $mode !~/[(stop)|(start)|(run)]/ ) {
	print "Mode is incorrect: $mode\n";
	exit 1;
}

my $existing_rules=`/sbin/iptables-save | grep INPUT |grep -v ":INPUT"`;

print "Existing rules $existing_rules" if $DEBUG;

my @existing_rules= split(/\n/,$existing_rules);

my $line_counter=0;
my $insert_point=0;

foreach my $rule (@existing_rules) {
	$line_counter++; 
	# find the first drop all ports rule 
	if ( $rule =~/--dport 1:65535/ ) {
		$insert_point=$line_counter;
		last;
	}
}

if ( $insert_point == 0) {
	# Didn't find the rule
	print "Correct ruleset not found\n";
	exit 1;
}

# expected Format:
# [0:0] -I INPUT $Rule_Insert_Point -p tcp -m tcp --dport $HTTP_PORT -j ACCEPT
my $err=0;
my $line="";

while (<RULES>) {
	if ( /\[0:0\] -I INPUT \$Rule_Insert_Point (.*)/ ) {
		# good rule format
		if ( $mode eq "start") {
			$line="/sbin/iptables -I INPUT $insert_point $1";
		} else {
			$line="/sbin/iptables -D INPUT $1";
		}
	
		print "$line\n" if $DEBUG;
		print `$line`;
	}
}


print "Partition Firewall Done\n";

