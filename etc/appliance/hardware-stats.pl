#!/usr/bin/perl -w
# (c) Layer 7 Technologies, 2009
# Alan Bailward <abailward@layer7tech.com>
# Script to return hardware data to the SSG.  Returns in the format of 
# [type]|[degrees C]
# IE: 
# CPU|30
# If there's an error the [degrees C] is replaced by "null" followed by an
# error number.  IE:
# CPU|null3
# Makes call to 
#  > ipmitool sdr get MB/T_AMBx
#  (where x is 0-3)
# to retrieve the information
# Additional checks include determining if this is a Sun x4150 or not, if not
# then bail out with an error as the x4100 and other systems will have different 
# IPMI calls
# 
# Error numbers:
# 1 - not a 4150 or otherwise not able to retrieve the CPU temp data
# 2 - cannot open temperature retrival tool (ipmitool)
# 3 - format of ipmitool data isn't correct (no 'degrees' in second field)

use strict;

# Secure the PATH at compile time, before any libraries get included.  sudo is expected to have already sanitized the rest of the environment.
BEGIN { $ENV{'PATH'} = '/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin' };

#use Data::Dumper;
my $cputemp = "null";
my $errornum = "";
my @cputemps = ();

my @sensors = qw(MB/T_AMB0 MB/T_AMB1 MB/T_AMB2 MB/T_AMB3);

# first determine the hardware make
#my $dmidecode=`./dmidecode 2>&1 | grep Product | uniq`;

my $hardware = &get_hardware;

if( $hardware =~ /x4150/i ) {
	foreach my $sensordata ( @sensors ) {
		my $command = "ipmitool sdr get $sensordata | grep 'Sensor Reading' |";
		# should return
		#  Sensor Reading        : 42 (+/- 0) degrees C
		open SENSOR, $command or $errornum = 2;
		if( not $errornum ) {
			while( defined( my $line = <SENSOR> )) {
				my @cpuinfo = split(/ +/, $line );
				if( $cpuinfo[7] =~ /degrees/i ) {
					push( @cputemps, $cpuinfo[4] );
				} else {
					$errornum = 3;
				}
			}
		}
	}
	my @sorted = reverse sort @cputemps;
	$cputemp = $sorted[0];
} else {
	$errornum = 1;
}

# were any errors encoutered?
if( $errornum ) {
	$cputemp = "null" . $errornum;
}

print "CPU|$cputemp";

#################

sub get_hardware {
	my $s = "undefined";
	my $ret = `cat /proc/cpuinfo | grep vendor_id | uniq -c`;
	my ($num,$vendor) = (split(/ +/,$ret))[1,3];
	if( int $num == 8 and $vendor =~ /GenuineIntel/i ) {
		$s = "x4150";
	}
	elsif( int $num == 4 and $vendor =~ /AuthenticAMD/i ) {
		$s = "x4100";
	}
	elsif( int $num == 1 ) {
		$s = "vmware";
	}
	else {
		$s = "unknown";
	}
	return $s;
}
