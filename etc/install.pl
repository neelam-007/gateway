#!/usr/bin/perl
# $Id$
# Script to prompt for configuration values
# save a text file 
use strict;
my @list=(
	cluster   => "Node is in a cluster       ",
	firstnode => "This is first Node         ",
	hostname  => "Hostname for this Node     ",
	# firstip   => "IP of first node           ",
	# rootpass  => "Root Pass of first node    ",
	dbserver  => "Node is a DB Server        ",
	clusternm => "Cluster Host Name          ",
	def_rt    => "IP of Load Balancer Backend",
	front_ip  => "Node Public IP             ",
	front_mask=> "Public Network Mask        ",
	back_ip   => "Node Private IP            ",
	back_net  => "Node private Network       ",
	back_mask => "Private Network Mask       ",
	back_rt   => "Back End Router            ",
);
my %Conf=();

my $SAVE_FILE="/home/jay/SSG_INSTALL";
# then change the local files as appropriate
if ( -e $SAVE_FILE ) {
	if ( $ARGV[0] eq "-reinstall" ) {
		readconfig()
	} else {
		die "Already Installed";
	}
} 
	

LOOP:
getparams();
print display();
print "Is this correct?(y/N)";
my $ans=<STDIN>;
chomp $ans;
if ($ans eq 'y') {
	print "Writing Config file $SAVE_FILE\n";
	writefile();
	change_os_config();
} else {
	goto LOOP;
}

sub change_os_config {
	# Holy frig we're gonna actually do this all.
=pod
	cluster   => "Node is in a cluster       ",
	firstnode => "This is first Node         ",
	hostname  => "Hostname for this Node     ",
	# firstip   => "IP of first node           ",
	# rootpass  => "Root Pass of first node    ",
	dbserver  => "Node is a DB Server        ",
	clusternm => "Cluster Host Name          ",
	def_rt    => "IP of Load Balancer Backend",
	front_ip  => "Node Public IP             ",
	front_mask=> "Public Network Mask        ",
	back_ip   => "Node Private IP            ",
	back_net  => "Node private Network       ",
	back_mask => "Private Network Mask       ",
	back_rt   => "Back End Router            ",
=cut
	# lets start small
	# the front network config

	my $front_conf = "/etc/sysconfig/network-scripts/ifcfg-eth1"	
	
	if ($Conf{"front_ip"} ) {
		if ( -e $front_conf ) {
			`mv $front_conf $back_conf.save`;
		}
		open (OUT ">$front_conf");
		print OUT <<EOF;
DEVICE=eth1
ONBOOT=yes
BOOTPROTO=static
IPADDR=$Conf{"front_ip"}
NETMASK=$Conf{"front_mask"}
GATEWAY=$CONF{"def_rt"}
EOF
		close OUT;
		
	} else {
		print "WARNING: No front end network defined\n";
	}
	
	# the back network config

	my $back_conf = "/etc/sysconfig/network-scripts/ifcfg-eth0"	
	
	if ($Conf{"back_ip"} ) {
		if ( -e $back_conf ) {
			`mv $back_conf $back_conf.save`;
		}
		open (OUT ">$back_conf");
		print OUT <<EOF;
DEVICE=eth0
ONBOOT=yes
BOOTPROTO=static
IPADDR=$Conf{"back_ip"}
NETMASK=$Conf{"back_mask"}
EOF
		close OUT;
		
	} else {
		print "WARNING: No back end network defined\n";
	}

	# cluster stuff
	# 1. hostname
	if ($Conf{clusternm && lc($Conf{cluster}) eq "y") {
		open (OUT, "/ssg/etc/conf/cluster_hostname");
		print OUT "$Conf{clusternm}\n";
		close OUT;
	else {
		print "WARNING: Cluster hostname not set\n";
	}
	
	# 2. cluster config for db:
	print "Not Implemented: fuck around with my.cnf. gotta think on that\n"
	# 3. gen keys?
	# 
		
};
	

sub readconfig {
	open (SV,"<$SAVE_FILE");
	while (<SV>) {
		if ( /(.*)=(.*)/) {
			$Conf{$1} = $2;
		}
	}
	close SV;
}



sub getparams {
		print <<EOF;
SSG Configuration script
Current Configuration parameters:
EOF
	my %c=(@list);
	my @fieldlist=@list;
	{ my $i; @fieldlist= grep { ++$i % 2 } @fieldlist; }
	foreach  my $f (@fieldlist) {
		print "$c{$f} ($Conf{$f}):";
		my $newval=<STDIN>;
		chomp $newval;
		if ($newval ne "") {
			$Conf{$f}=$newval;
		}
	}
}


sub display {
	my $t = <<EOF;
SSG Configuration script
Current Configuration parameters:
EOF
	my %c=@list;
	my @fieldlist=@list;
	{ my $i; @fieldlist= grep { ++$i % 2 } @fieldlist; }
	foreach  my $f (@fieldlist) {
		$t .= "$c{$f} = $Conf{$f}\n";
	}
	return $t;
}

sub writefile {
	open (SV, ">$SAVE_FILE");
 	my @fieldlist=@list;
	{ my $i; @fieldlist= grep { ++$i % 2 } @fieldlist; }
	foreach  my $f (@fieldlist) {
		print SV "$f=$Conf{$f}\n";
	}
	close SV;
}
