#!/usr/bin/perl
# $Id$
# Script to prompt for configuration values
# save a text file 
# and then apply

use strict;
my @list=(
	hostname      => "Hostname for this node (eg. hostname.company.com)                                                         ",
	gc_cluster    => "Node is part of a cluster gateway (y/n)                                                                   ",
	gc_firstnode  => "Cluster gateway env, First node in gateway cluster (y/n)                                                  ",
	gc_masternip  => "Cluster gateway env, IP (reachable by this host) of the first node (eg. 10.0.0.2)                         ",
        gc_clusternm  => "Cluster gateway env, Cluster host name (eg. cluster_hostname.company.com)                                 ",
	dc_cluster    => "Node is to be connected to a cluster database environemnt (y/n)                                           ",
	dc_dbserver   => "Cluster DB env, Node is a DB Server (y/n)                                                                 ",
	dc_firstnode  => "Cluster DB env, First node in DB cluster (y/n)                                                            ",
        dc_dbip       => "Cluster DB env, DB cluster IP (eg. 10.0.0.10)                                                             ",
	dblocal       => "Non cluster DB env, DB is on the localhost (database must able to login as localhost) (y/n)               ",
	dbhostname    => "Non cluster DB env, DB hostname (must be reach-able by this node) if DB is not on localhost (eg. dbserver)",
	dbuser        => "Database Username (eg. dbuser)                                                                            ",
	dbpass        => "Database Password (eg. dbpassword)                                                                        ",
	net_def_rt    => "IP of Load Balancer between bridge/client and gateway (eg. 192.168.1.1)                                   ",
	net_front_ip  => "Network configuration, Node Public IP (eg. 192.168.1.8)                                                   ",
	net_front_mask=> "Network configuration, Public Network Mask (eg. 255.255.255.0)                                            ",
	net_back_ip   => "Network configruation, Node Private IP (eg. 10.0.0.8)                                                     ",
	net_back_net  => "Network configuration, Node private Network for /etc/init.d/back_route (eg. 224.0.0.0)                    ",
	net_back_mask => "Network configuration, Private Network Mask (eg. 255.255.255.0)                                           ",
	net_back_rt   => "Network configuration, Back End Router (eg. 10.0.0.1)                                                     ",
);
my %Conf=();

my $SAVE_FILE="/var/log/SSG_INSTALL";

my $cnf_src="/ssg/bin/my.cnf";
my $cnf_target="/etc/my.cnf";

# then change the local files as appropriate
if ( -e $SAVE_FILE ) {
	if ( $ARGV[0] eq "-reinstall" ) {
		readconfig();
LOOP:
		getparams();
		print display();
		print "Is this correct?(y/n)";
		my $ans=<STDIN>;
		chomp $ans;
		$ans = trimwhitespace($ans);
		if ($ans eq 'y') {
        		print "Writing Config file $SAVE_FILE\n";
        		writefile();
        		change_os_config();
		} else {
        		goto LOOP;
		}
	} elsif ($ARGV[0] eq '-apply' ) {
		readconfig();
		change_os_config();
	} elsif ($ARGV[0] eq '-usage' ) {
		print usage();
		exit;
	} else {
		die "Already Installed";
	}
} 


sub change_os_config {
	# lets start small
	# 1. the front network config
	print "Starting Network config: \n";
	my $front_conf = "/etc/sysconfig/network-scripts/ifcfg-eth1";
	
	if ($Conf{"net_front_ip"} ) {
		if ( -e $front_conf ) {
			`mv $front_conf $front_conf.save`;
		}
		open (OUT, ">$front_conf");
		print OUT <<EOF;
DEVICE=eth1
ONBOOT=yes
BOOTPROTO=static
IPADDR=$Conf{"net_front_ip"}
NETMASK=$Conf{"net_front_mask"}
GATEWAY=$Conf{"net_def_rt"}
EOF
		close OUT;
		print "Front: Eth1 ";	
	} else {
		print "WARNING: No front end network defined\n";
	}
	
	# 2. the back network config

	my $back_conf = "/etc/sysconfig/network-scripts/ifcfg-eth0";
	
	if ($Conf{"net_back_ip"} ) {
		if ( -e $back_conf ) {
			`mv $back_conf $back_conf.save`;
		}
		open (OUT, ">$back_conf");
		print OUT <<EOF;
DEVICE=eth0
ONBOOT=yes
BOOTPROTO=static
IPADDR=$Conf{"net_back_ip"}
NETMASK=$Conf{"net_back_mask"}
EOF
		close OUT;
		print "Back: Eth0 ";	
	} else {
		print "WARNING: No back end network defined\n";
	}
	print " ... Done\n";

	# cluster stuff
	# 3 . hostname
	print "Cluster Hostname: ";
	if ($Conf{gc_clusternm} && lc($Conf{gc_cluster}) eq "y") {
		open (OUT, ">/ssg/etc/conf/cluster_hostname") or die "Can't open cluster hostname file";
		print OUT "$Conf{gc_clusternm}\n";
		close OUT;
		print "$Conf{gc_clusternm}\n";
		open (HOST, ">/etc/hosts") or die "Can't open /etc/hosts!";
		print HOST <<EOF;
# Modified by /ssg/bin/install.pl
# Will be replaced if you rerun!
127.0.0.1	localhost localhost.localdomain
$Conf{net_back_ip}	$Conf{gc_clusternm} $Conf{hostname}

EOF
		close HOST;
		chmod 0644, "/etc/hosts";
		print ".... Cluster Hostname and hosts files replaced\n";
	} else {
		print "WARNING: Cluster hostname not set\n";
	}
	
	print "Not Implemented: mess around with my.cnf. gotta think on that\n";
	# 4. local config of db connection
	# Only works with a distro hib properties, it has placeholders
	#
	print "\nINFO: DB Connection Parameters...\n";

	# 4a. db url, username, password configuration to /ssg/etc/conf/hibernate.properties
	my $db_url;
        # non cluster DB
        if ($Conf{dc_cluster} eq "n") {
                if ($Conf{dblocal} eq "y") {
                        $db_url = "localhost";
                } elsif ($Conf{dbhostname}) {
                        $db_url = $Conf{dbhostname};
                } else {
                        print "WARNING: DB url not set, as database hostname not provided\n";
                }
        } else {  # cluster DB
                if ($Conf{dc_dbip}) {
                        $db_url = $Conf{dc_dbip};
                } else {
                        print "WARNING: DB url not set, as cluster database IP not provided\n";
                }
        } 

	if ($Conf{dbuser}) {
		print "INFO: Now update /ssg/etc/conf/hibernate.properties\n";
		my $dbconfig="/ssg/etc/conf/hibernate.properties";
		rename ($dbconfig,"$dbconfig.orig");
		open (IN, "<$dbconfig.orig");
		open (OUT,">$dbconfig");
		while (<IN>) {
			if ($db_url) {
				if ($_ =~ m/url(.*)\/\/(.*)\//) {
					s/$2/$db_url/;
				}
			}
			s/username(.*)$/username = $Conf{dbuser}/;
			s/password(.*)$/password = $Conf{dbpass}/;
			print OUT;
		}
		close IN;
		close OUT;
	} else {
		print "WARNING: /ssg/etc/conf/hibernate.properties not set";
	}

	# 4b. grant user to local database
	if (($Conf{dbuser}) && (($Conf{dc_dbserver} eq "y") || ($Conf{dblocal} eq "y"))) {
		# grant user to local database
		print "INFO: Now grant username & password to local database\n";
		my $sql = "grant all on ssg.* to $Conf{dbuser}\@'%' identified by '$Conf{dbpass}';\n\n";
		$sql .="grant all on ssg.* to $Conf{dbuser}\@'localhost' identified by '$Conf{dbpass}';\n\n";
		open (TMP, ">/tmp/sql.grants");
		print TMP $sql;
		close TMP;
		print "INFO: Now run following SQL... \n$sql\n";
		my $success=`mysql -u root </tmp/sql.grants`;
		# unlink "/tmp/sql.grants";
	} else {
		print "WARNING: DB username/password not granted to local database\n";
	}
 
	# 4c. copy $cnf_src to $cnf_target 
	print "INFO: Now copy $cnf_src to $cnf_target\n";
        if (-e $cnf_target) {
		print "WARNING: $cnf_target existed, now renaming $cnf_target to $cnf_target.orig\n";
	        rename ($cnf_target,"$cnf_target.orig");
        }
	print "INFO: Copying $cnf_src to $cnf_target\n";
       `cp -f $cnf_src $cnf_target`;
 
	# 4d. do the config for db cluster id?
	print "INFO: Now update $cnf_target\n";
	if ( ($Conf{dc_cluster} eq "y") && ($Conf{dc_dbserver} eq "y") ) {
		my $s_id=2;
                if ($Conf{dc_firstnode} eq "y" ) {
                        $s_id=1;
                }
		open (IN,"$cnf_target");
		open(CNF, ">$cnf_target");
		while(<IN>) {
                        s/^#server-id=1$/server-id=$s_id/;   #uncomment it and set server id value if commented out
			s/^server-id=(.*)$/server-id=$s_id/; #set server id value if already uncomment, also purge any server-id were set to other values
			print CNF;
		}		
		close CNF;
		# server id is set. Need to run the sync; then set the master/slave relationship
		# perhaps this is best left to the user.
		print "INFO: Server id is set - need to manually runt the sync, then set the master/slave relationship\n";
		print "INFO: $cnf_target updated with cluster server id: $s_id\n";
	} else {
		print "WARNING: DB cluster server id not set\n";
	}
	
	# 5. gen keys? 
	if (($Conf{gc_cluster} eq "n") || ($Conf{gc_cluster} eq "y" && $Conf{gc_firstnode} eq "y")) {
		# gen the keys, otherwise copy them
		print "Invoke /ssg/bin/setkeys.sh script to generate keys\n";
		system("su - gateway -c /ssg/bin/setkeys.sh");

	} else {
		print "Copy keys & keys configuration files from the first node...\n";
		print "Supply gateway user password of first node - " + $Conf{gc_masternip} + "\n";
                print "Now to copy /ssg/etc/keys/*:\n"; 
		`cp gateway\@$Conf{gc_masternip}/ssg/etc/keys/* /ssg/etc/keys`;
		print "Now to copy /ssg/tomcat/conf/server.xml:\n";
		`scp gateway\@$Conf{gc_masternip}/ssg/tomcat/conf/server.xml /ssg/tomcat/conf/server.xml`;
		print "Now to copy /ssg/etc/conf/keystore.properties:\n";
		`scp gateway\@$Conf{gc_masternip}/ssg/etc/conf/keystore.properties /ssg/etc/conf/keystore.properties`;
		`chmod -R gateway.gateway /ssg/etc`;
		`chmod gateway.gateway /ssg/tomcat/conf/server.xml`;
		`chmod gateway.gateway /ssg/etc/conf/keystore.properties`;
	}
}
	

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
		$newval = trimwhitespace($newval);
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

sub usage {
	return <<EOF;
$0: [-reinstall] [-apply] [-usage]
-reinstall Ask for configuration again to change any values, then re-run OS configuration
-apply     Don't ask for config, run OS config using $SAVE_FILE
-usage     This message.

EOF

}

sub trimwhitespace($) {
# Remove whitespace from the start and end of the string
	my $string = shift;
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	return $string;
}
