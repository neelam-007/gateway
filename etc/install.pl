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
	dbhostname    => "Non cluster DB env, DB hostname or IP (must be reach-able by this node) if DB is not on localhost         ",
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
	print "INFO: Starting Front End Network Configuration... \n";
	my $front_conf = "/etc/sysconfig/network-scripts/ifcfg-eth1";
	
	if ($Conf{"net_front_ip"} ) {
		if ( -e $front_conf ) {
			print "INFO: $front_conf existed, now renaming $front_conf to $front_conf.save\n";
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
		print "INFO: Front end network configured $front_conf - IPADDR of $Conf{net_front_ip}; NETMASK of $Conf{net_front_mask}; GATEWAY of $Conf{net_def_rt}";	
	} else {
		print "WARNING: No front end network defined\n";
	}

	# 2. the back network config
        print "INFO: Starting Front End Network Configuration... \n";
	my $back_conf = "/etc/sysconfig/network-scripts/ifcfg-eth0";
 	my $back_route = "/etc/init.d/back_route";	
	if ($Conf{"net_back_ip"} ) {
		if ( -e $back_conf ) {
                        print "INFO: $front_conf existed, now renaming $front_conf to $front_conf.save\n";
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
	if ($Conf{net_back_net} && $Conf{net_back_mask} && $Conf{net_back_rt}) {
		if ( -e $back_route ) {
			`mv $back_route $back_route.save`;
		}
		open (IN, "<$back_route.save");
		open (OUT, ">$back_route");
		while (<IN>) {
                        s/start\(\)\s*\{\s*\n/start\(\) \{\n\t\$route add -net $Conf{net_back_net} netmask $Conf{net_back_mask} gw $Conf{net_back_rt}\n/;
                        s/stop\(\)\s*\{\s*\n/stop\(\) \{\n\t\$route del -net $Conf{net_back_net} netmask $Conf{net_back_mask} gw $Conf{net_back_rt}\n/;
                        print OUT;
		}
		close IN;
		close OUT;
	}
		print "INFO: Restarting $back_route\n";
		print "MANUAL TASK: Please review and restart $back_route";	
                print "INFO: Back end network configured $back_conf - IPADDR of $Conf{net_back_ip}; NETMASK of $Conf{net_back_mask}\n";
		print "INRO: Back end network configured $back_route - back_net of $Conf{net_back_net}; back_mask of $Conf{net_back_mask}; back_rt of $Conf{net_back_rt}\n";
	} else {
		print "WARNING: No back end network defined\n";
	}

	# 3. hostname
	# 3a. hostname for cluster
	my $cluster_hostname_file = "/ssg/etc/conf/cluster_hostname";
	my $hosts_file = "/etc/hosts";
	print "INFO: Configuring cluster hostname for /ssg/etc/conf/cluster_hostname & /etc/hosts";
	if ($Conf{gc_clusternm} && lc($Conf{gc_cluster}) eq "y") {
		open (OUT, ">$cluster_hostname_file") or die "Can't open cluster hostname file";
		print OUT "$Conf{gc_clusternm}\n";
		close OUT;
		print "$Conf{gc_clusternm}\n";
		open (HOST, ">$hosts_file") or die "Can't open /etc/hosts!\n";
		print HOST <<EOF;
# Modified by /ssg/bin/install.pl
# Will be replaced if you rerun!
127.0.0.1	localhost localhost.localdomain
$Conf{net_back_ip}	$Conf{gc_clusternm} $Conf{hostname}

EOF
		close HOST;
		chmod 0644, "/etc/hosts";
		print "INFO: $cluster_hostname_file and $hosts_file files replaced\n";
	} else {
		print "WARNING: $cluster_hostname_file not set\n";
	}

	if ( $Conf{gc_cluster} eq "n") {
		if ( -e $cluster_hostname_file) {
			`rm -f $cluster_hostname_file`  #non cluster ssg should not have this file
		}
                open (HOST, ">$hosts_file") or die "Can't open /etc/hosts!\n";
                print HOST <<EOF;
# Modified by /ssg/bin/install.pl
# Will be replaced if you rerun!
127.0.0.1       localhost localhost.localdomain
$Conf{host_ip}      $Conf{hostname}

EOF
                close HOST;
                chmod 0644, "/etc/hosts";
                print "INFO: $cluster_hostname_file removed and $hosts_file files replaced\n";
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
 
	# 4c. do the config for db cluster on my.cnf and on dbfaildetect.sh?
	my $cnf = "/etc/my.cnf";
        my $dbfaildetect_file = "/ssg/bin/dbfaildetect.sh";

	if ( ($Conf{dc_cluster} eq "y") && ($Conf{dc_dbserver} eq "y") && ($Conf{dc_dbip})) {
        	print "INFO: Now updating $cnf\n";
		my $s_id=2;
                if ($Conf{dc_firstnode} eq "y" ) {
                        $s_id=1;
                }
		`mv $cnf $cnf.orig`;
		open(CNF_IN, "<$cnf.orig");
		open(CNF_OUT, ">$cnf");
		while(<CNF_IN>) {
                        s/^#server-id=1$/server-id=$s_id/;   #uncomment it and set server id value if commented out
			s/^server-id=(.*)$/server-id=$s_id/; #set server id value if already uncomment, also purge any server-id were set to other values
			s/#\s*log-bin/log-bin/; #ensure uncomment
			s/#\s*log-slave-update/log-slave-update/; #ensure comment
			print CNF_OUT;
		}		
		close CNF_IN;
		close CNF_OUT;
		# server id is set. Need to run the sync; then set the master/slave relationship
		# perhaps this is best left to the user.
		print "MANUAL TASK: Server id is set - need to manually run the sync, then set the master/slave relationship\n";
		print "INFO: $cnf updated with cluster server id: $s_id\n";
		
                print "INFO: Now updating $dbfaildetect_file\n";
		`mv $dbfaildetect_file $dbfaildetect_file.orig`;		
		open (FDF_IN,"$dbfaildetect_file.orig");
		open (FDF_OUT,">$dbfaildetect_file");
                while (<FDF_IN>) {
                        s/DBHOST/^DBHOST=$Conf{dc_dbip}/;
                        print <FDF_OUT>;
                }
                close FDF_IN;
                close FDF_OUT;
		print "INFO: DB cluster $cnf updated with server $s_id\n";
		print "INFO: DB cluster $dbfaildetect_file updated with DBHOST $Conf{dc_dbip}\n";
	} else {
		print "WARNING: DB cluster $cnf for server id is not updated\n";
		print "WARNING: DB cluster $dbfaildetect_file is not updated\n";
	}
	
	# 5. gen keys?  (only generate keys after hostname/cluster hostname confirmed)
	if (($Conf{gc_cluster} eq "n") || ($Conf{gc_cluster} eq "y" && $Conf{gc_firstnode} eq "y")) {
		# gen the keys, otherwise copy them
		print "Invoke /ssg/bin/setkeys.sh script to generate keys\n";
		system("su - gateway -c /ssg/bin/setkeys.sh");

	} else {
		print "Copy keys & keys configuration files from the first node...\n";
		print "Supply gateway user password of first node - $Conf{gc_masternip}\n";
                print "Now to copy /ssg/etc/keys/*:\n"; 
		system("scp gateway\@$Conf{gc_masternip}:/ssg/etc/keys/* /ssg/etc/keys");
		print "Now to copy /ssg/tomcat/conf/server.xml:\n";
		system("scp gateway\@$Conf{gc_masternip}:/ssg/tomcat/conf/server.xml /ssg/tomcat/conf/server.xml");
		print "Now to copy /ssg/etc/conf/keystore.properties:\n";
		system("scp gateway\@$Conf{gc_masternip}:/ssg/etc/conf/keystore.properties /ssg/etc/conf/keystore.properties");
		`chown -R gateway.gateway /ssg/etc`;
		`chown gateway.gateway /ssg/tomcat/conf/server.xml`;
		`chown gateway.gateway /ssg/etc/conf/keystore.properties`;
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
