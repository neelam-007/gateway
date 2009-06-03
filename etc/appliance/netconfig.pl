#!/usr/bin/perl
use strict;
my $DEBUG=0;

# Grab args
my $netcommand="";

foreach my $argnum (0 .. $#ARGV) { $netcommand .= " " . $ARGV[$argnum]; }
print STDERR "command: $netcommand\n" if $DEBUG;

my $dev="";
if ($netcommand =~/--device (eth\d+)/) {
   print STDERR "Found Dev: $1\n" if $DEBUG;
   $dev=$1;
} else {
   die "Device not specified";
}

my $proto="";
if ($netcommand =~/--bootproto=(none|dhcp|static)/ ) {
   print STDERR "Found proto: $1\n" if $DEBUG;
   $proto=$1;
} 

my $ip="";
if ($netcommand =~/--ip=(\d+\.\d+\.\d+\.\d+)/) {
   print STDERR "Found ip: $1\n" if $DEBUG;
   $ip=$1;
}
my $netm="";
if ($netcommand =~/--netmask=(\d+\.\d+\.\d+\.\d+)/) {
   print STDERR "Found nm: $1\n" if $DEBUG;
   $netm=$1;
}
my $gw="";
if ($netcommand =~/--gateway=(\d+\.\d+\.\d+\.\d+)/) {
   $gw=$1;
}

my $out="DeviceList.Ethernet.$dev.BootProto=$proto\n";
$out  .="DeviceList.Ethernet.$dev.OnBoot=true\n";

if ($proto ne "dhcp" ) {
   $out .= "DeviceList.Ethernet.$dev.IP=$ip\nDeviceList.Ethernet.$dev.Netmask=$netm\nDeviceList.Ethernet.$dev.Gateway=$gw\n"
}

print STDERR "Assembled Command:\n$out\n" if $DEBUG;

open (COMMAND ,"| /usr/sbin/system-config-network-cmd -i ") || die "couldn't open config subshell";
print COMMAND $out || die "writing to subshell";

