#!/usr/bin/perl
# $Id$
# Script to prompt for configuration values
# save a text file 
use strict;
my @list=(
	cluster   => "Node is in a cluster       ",
	firstnode => "This is first Node         ",
	master_n  => "First node of cluster      ",
	hostname  => "Hostname for this Node     ",
	dbserver  => "Node is a DB Server        ",
	db_otherip=> "IP of Other DB Server      ",
	db_user   => "Database Username          ",
	db_pass   => "Database Password          ",
	dbclip    => "Database cluster IP        ",
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
		readconfig();
	} elsif ($ARGV[0] eq '-apply' ) {
		readconfig();
		change_os_config();
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
	# lets start small
	# 1. the front network config
	print "Starting Network config: ";
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
		print "Front: Eth1 ";	
	} else {
		print "WARNING: No front end network defined";
	}
	
	# 2. the back network config

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
		print "Back: Eth0 ";	
	} else {
		print "WARNING: No back end network defined ";
	}
	print " ... Done\n"

	# cluster stuff
	# 3 . hostname
	print "Cluster Hostname: ";
	if ($Conf{clusternm} && lc($Conf{cluster}) eq "y") {
		open (OUT, "/ssg/etc/conf/cluster_hostname");
		print OUT "$Conf{clusternm}\n";
		close OUT;
		print "$Conf{clusternm}\n";
	else {
		print "WARNING: Cluster hostname not set\n";
	}
	
	print "Not Implemented: mess around with my.cnf. gotta think on that\n";
	# 4. local config of db connection
	# Only works with a distro hib properties, it has placeholders
	#
	print "DB Connection Parameters: ";
	if ($Conf{db_user) ) {
		my $dbconfig="/ssg/etc/conf/hibernate.properties";
		rename ($dbconfig,"$dbconfig.orig");
		open (IN, "<$dbconfig.orig");
		open (OUT,">$dbconfig");
		while (<IN>) {
			s/DB_USER/$Conf{db_user}/;
			s/DB_PASSWORD/$Conf{db_pass}/;
			print OUT;
		}
		close IN;
		close OUT;
		my $sql = "grant all on ssg.* to $Conf{db_user}@'%' identified by '$Conf{db_pass}';\n";
		$sql .="grant all on ssg.* to $Conf{db_user}@'localhost' identified by '$Conf{db_pass}';\n";
		open (TMP, ">/tmp/sql.grants");
		print TMP $sql;
		close TMP;
		my $success=`mysql -u root </tmp/sql.grants`;
		unlink "/tmp/sql.grants";
		print "Set \n";
	} else {
		print "WARNING: Db connection not set\n";
	}
	# 4. do the config for db cluster id?
	print "DB Cluster Parameters: ";
	if ( $Conf{dbserver} eq "y" ) {
		my $s_id=2;
		if ($Conf{firstnode} eq "y" ) {
			$s_id=1;
		}
	
		my $cnf="/etc/my.cnf";
				
		rename ($cnf,"$cnf.orig");
		open (IN,"$CNF.orig");
		open(CNF, ">$cnf");
		while(<IN>) {
			s/^#server-id=1$/server-id=$s_id/;
			print CNF;
		}		
		close CNF;
		# server id is set. Need to run the sync; then set the master/slave relationship
		# perhaps this is best left to the user.
		print "cluster id: $s_id\n";
	} else {
		print "WARNING: Db cluster id not set\n";
	}
		
	
	# 5. gen keys?
	if ($Conf{cluster} ) {
		if ( $Conf{firstnode} ) {
			# gen the keys, otherwise copy them
			`su - gateway -c /ssg/bin/setkeys.sh`;
		else {
			print "supply gateway user password of first node\n";
			`scp gateway@$Conf{master_n}/ssg/etc/keys/* /ssg/etc/keys`;
			`chmod -R gateway.gateway /ssg/etc`;
		}
	}
	# 
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
