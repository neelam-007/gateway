#!/usr/bin/perl
# $Id$
# Script to prompt for configuration values
# save a text file 
# and then apply

use strict;
my @list=(
	hostname      => "Hostname for this SSG node (eg. hostname.company.com)                                                     ",
	host_ip       => "Host IP for this SSG node (eg. 192.168.1.9)                                                               ",
	gc_cluster    => "Cluster / Non Cluster Gateway Env - Node is part of a cluster gateway (y/n)                               ",
	gc_firstnode  => "--Cluster Gateway Env, First node in gateway cluster (y/n)                                                ",
	gc_masternip  => "--Cluster Gateway Env, IP (reachable by this host) of the first node (eg. 10.0.0.2)                       ",
        gc_clusternm  => "--Cluster Gateway Env, Cluster host name (eg. cluster_hostname.company.com)                               ",
	dc_cluster    => "Cluster / Non Cluster DB Env - Node is to be connected to a cluster database environemnt (y/n)            ",
	dc_dbserver   => "--Cluster DB Env, Node is a DB Server (y/n)                                                               ",
	dc_firstnode  => "--Cluster DB Env, First node in DB cluster (y/n)                                                          ",
        dc_dbip       => "--Cluster DB Env, DB cluster IP (eg. 10.0.0.10)                                                           ",
	dc_email      => "--Cluster DB Env, DB cluster Email (email upon failover)                                                  ",
	dblocal       => "--Non Cluster DB Env, DB is on the localhost (database must able to login as localhost) (y/n)             ",
	dbhostname    => "--Non cluster DB Env, DB hostname or IP (must be reach-able by this node) if DB is not on localhost       ",
	dbuser        => "SSG Database - Username                                                                                   ",
	dbpass        => "SSG Database - Password for the above user                                                                ",
	net_def_rt    => "Network Configruation - IP of Load Balancer between bridge/client and gateway (eg. 192.168.1.1)           ",
	net_front_ip  => "Network Configuration - Node Public IP (eg. 192.168.1.8)                                                  ",
	net_front_mask=> "Network Configuration - Public Network Mask (eg. 255.255.255.0)                                           ",
	net_back_ip   => "Network Configruation - Node Private IP (eg. 10.0.0.8)                                                    ",
	net_back_net  => "Network configuration - Node private Network for /etc/init.d/back_route (eg. 224.0.0.0)                   ",
	net_back_mask => "Network configuration - Private Network Mask (eg. 255.255.255.0)                                          ",
	net_back_rt   => "Network configuration - Back End Router (eg. 10.0.0.1)                                                    "
);

my %Conf=();
my $pid = $$;
my $save_file="/etc/SSG_INSTALL";
my $group_name="gateway";
my $user_name="gateway";
# then change the local files as appropriate

if ($ARGV[0] eq '-usage' ) {
        print usage();
        exit;
} elsif ($ARGV[0] eq '-apply' ) {
        if ( -e $save_file ) {
                readconfig();
                change_os_config();
        } else {
                die "ERROR: $save_file does not exist\n";
        }
} else {
	if ( -e $save_file ) {
                print "WARNING: SSG has been installed and configured - $save_file already existed!\n";
		print "Do you want to re-installing/re-configuring SSG? (y/n)";
                my $ans_proceed=<STDIN>;
                chomp $ans_proceed;
                $ans_proceed = trimwhitespace($ans_proceed);
                if ($ans_proceed ne 'y') {
                        print "INFO: $0 exits\n";
                        exit;
		}
		readconfig();
	 }

LOOP:
        getparams();
        print display();
        print "Is this correct?(y/n)";
        my $ans=<STDIN>;
        chomp $ans;
        $ans = trimwhitespace($ans);
        if ($ans eq 'y') {
                print "Writing Config file $save_file\n";
                writefile();
                change_os_config();
        } else {
                goto LOOP;
        }
}

sub change_os_config {
	my $host = (split(/\./,$Conf{hostname}))[0];
	my $cluster = (split(/\./,$Conf{gc_clusternm}))[0];

	print ("INFO: Current PID and files will be back up suffixed with PID of $pid\n");
	# lets start small

	# 1. the front network config - file affected: $front_conf
	print "INFO: Starting Front End Network Configuration... \n";
	my $front_conf = "/etc/sysconfig/network-scripts/ifcfg-eth1";
	
	if ($Conf{net_front_ip} && $Conf{net_front_mask} && $Conf{net_def_rt}) {
		if ( -e $front_conf ) {
			print "INFO: $front_conf existed, now renaming $front_conf to $front_conf.bckup_$pid\n";
			rename( $front_conf, "$front_conf.bckup_$pid");
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
		print "WARNING: No front end network defined, front end network remains $front_conf\n";
	}

	# 2. the back network config - file affected: $back_conf, $back_route
        print "INFO: Starting Back End Network Configuration... \n";
	my $back_conf = "/etc/sysconfig/network-scripts/ifcfg-eth0";
 	my $back_route = "/etc/init.d/back_route";	

	if ( -e $back_conf ) {
        	print "INFO: $back_conf existed, now renaming $back_conf to $back_conf.bckup_$pid\n";
               	rename( $back_conf, "$back_conf.bckup_$pid");
       	} 

	if ($Conf{net_back_ip} && $Conf{net_back_mask}) {
		open (OUT, ">$back_conf");
		print OUT <<EOF;
DEVICE=eth0
ONBOOT=yes
BOOTPROTO=static
IPADDR=$Conf{"net_back_ip"}
NETMASK=$Conf{"net_back_mask"}

EOF
		close OUT;
                print "INFO: Back end network configured $back_conf - IPADDR of $Conf{net_back_ip}; NETMASK of $Conf{net_back_mask}\n";
	} else {
                open (OUT, ">$back_conf");
                print OUT <<EOF;
DEVICE=eth0
ONBOOT=yes
BOOTPROTO=dhcp
TYPE=Ethernet
DHCP_HOSTNAME=$host

check_link_down() {
        return 1;
}

EOF
		close OUT;
		print "INFO: Back end network configured $back_conf - BOOTPROTO of dhcp; DHCP_HOSTNAME of $host\n";
	}	

	if ($Conf{net_back_net} && $Conf{net_back_mask} && $Conf{net_back_rt}) {
		if ( -e $back_route ) {
			print "INFO: $back_route existed, now renaming $back_route to $back_route.bckup_$pid";
			rename( $back_route, "$back_route.bckup_$pid" );
		}
		open (IN, "<$back_route.bckup_$pid");
		open (OUT, ">$back_route");
		while (<IN>) {
                        s/start\(\)\s*\{\s*\n/start\(\) \{\n\t\$route add -net $Conf{net_back_net} netmask $Conf{net_back_mask} gw $Conf{net_back_rt}\n/;
                        s/stop\(\)\s*\{\s*\n/stop\(\) \{\n\t\$route del -net $Conf{net_back_net} netmask $Conf{net_back_mask} gw $Conf{net_back_rt}\n/;
                        print OUT;
		}
		close IN;
		close OUT;
                print "INRO: Back end network configured $back_route - back_net of $Conf{net_back_net}; back_mask of $Conf{net_back_mask}; back_rt of $Conf{net_back_rt}\n";
		print "INFO: Restarting $back_route\n";
		print "MANUAL TASK: Please review and restart $back_route";	
	} else {
		print "WARNING: Back end network $back_route remains\n";
	}

	# 3. hostname - file affected:$hosts_file 
	# 3a. hostname for cluster - file affected: $cluster_hostname_file 
	my $cluster_hostname_file = "/ssg/etc/conf/cluster_hostname";
	my $hosts_file = "/etc/hosts";
	print "INFO: Configuring cluster hostname for $cluster_hostname_file & $hosts_file\n";
	if ($Conf{gc_clusternm} && lc($Conf{gc_cluster}) eq "y") {
                if ( -e $cluster_hostname_file ) {
	    		print "INFO: $cluster_hostname_file existed, now renaming $cluster_hostname_file to $cluster_hostname_file.bckup_$pid\n";
              		rename( $cluster_hostname_file, "$cluster_hostname_file.bckup_$pid");
		}

                print "INFO: Updating $cluster_hostname_file with $Conf{gc_clusternm}\n";

		open (OUT, ">$cluster_hostname_file") or die "Can't open $cluster_hostname_file";
		print OUT "$Conf{gc_clusternm}\n";
		close OUT;

	        print "INFO: Changing owner user/group to $user_name/$group_name for $cluster_hostname_file\n";
       		system ("chown $user_name.$group_name $cluster_hostname_file");

                if ( -e $hosts_file ) {
                        print "INFO: $hosts_file existed, now renaming $hosts_file to $hosts_file.bckup_$pid\n";
                        rename( $hosts_file, "$hosts_file.bckup_$pid");
                }

		open (HOST, ">$hosts_file") or die "Can't open $hosts_file!\n";
		print HOST <<EOF;
# Modified by /ssg/bin/install.pl
# Will be replaced if you rerun!
127.0.0.1	localhost localhost.localdomain
$Conf{net_back_ip}	$Conf{gc_clusternm} $Conf{hostname}

EOF
		close HOST;
		chmod 0644, "$hosts_file";
		print "INFO: $cluster_hostname_file and $hosts_file files replaced\n";
	} else {
		print "WARNING: $cluster_hostname_file not set\n";
	}

	if ( $Conf{gc_cluster} eq "n") {
		if ( -e $cluster_hostname_file) {
			rename( $cluster_hostname_file, "$cluster_hostname_file.bckup_$pid")  #non cluster ssg should not have this file
		}

                if ( -e $hosts_file ) {
                        print "INFO: $hosts_file existed, now renaming $hosts_file to $hosts_file.bckup_$pid\n";
                        rename( $hosts_file, "$hosts_file.bckup_$pid");
                }

                open (HOST, ">$hosts_file") or die "Can't open $hosts_file!\n";
                print HOST <<EOF;
# Modified by /ssg/bin/install.pl
# Will be replaced if you rerun!
127.0.0.1       localhost localhost.localdomain
$Conf{host_ip}      $Conf{hostname} $host

EOF
                close HOST;
                chmod 0644, "/etc/hosts";
                print "INFO: $cluster_hostname_file removed (backup as $cluster_hostname_file.bckup_$pid) and $hosts_file files replaced\n";
	}

	# 4. local config of db connection
	#
	print "\nINFO: DB Connection Parameters...\n";

	# 4a. db url, username, password configuration to /ssg/etc/conf/hibernate.properties - file affected: $dbconfig
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

	my $dbconfig="/ssg/etc/conf/hibernate.properties";
	if ($Conf{dbuser}) {
                print "INFO: Now update $dbconfig\n";
		rename ($dbconfig,"$dbconfig.bckup_$pid");
		open (IN, "<$dbconfig.bckup_$pid");
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
	        print "INFO: Changing owner user/group to $user_name/$group_name for $dbconfig\n";
       		system ("chown $user_name.$group_name $dbconfig");

	} else {
		print "WARNING: $dbconfig file not set\n";
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
 
	# 4c. do the config for db cluster on my.cnf and on dbfaildetect.sh? - file affected: $cnf, $dbfaildetect_file
        # assumption: my.cnf contains server-id=1, server-id=2, log-bin, log-slave-update
        #             dbfaildetect.sh contains DBHOST=, EMAIL=
	my $cnf = "/etc/my.cnf";
	my $cnf_rpm = "/etc/my.cnf.ssg";
        my $dbfaildetect_file = "/ssg/bin/dbfaildetect.sh";

	if (! -e $cnf_rpm ) {
                print "ERROR: $cnf_rpm distribution missing from rpm!\n";
		exit;
	}

        if ( -e $cnf ) {
                print "INFO: $cnf existed, now renaming $cnf to $cnf.bckup_$pid\n";
                rename( $cnf, "$cnf.bckup_$pid");
        }

	if ( -e $dbfaildetect_file ) {
		print "INFO: $dbfaildetect_file existed, now renaming $dbfaildetect_file to $dbfaildetect_file.bckup_$pid\n";
                rename( $dbfaildetect_file, "$dbfaildetect_file.bckup_$pid");
	}
	
	if ( ($Conf{dc_cluster} eq "y") && ($Conf{dc_dbserver} eq "y") && ($Conf{dc_dbip})) {
        	print "INFO: Now updating $cnf\n";
		open(CNF_IN, "<$cnf_rpm");
		open(CNF_OUT, ">$cnf");
		while(<CNF_IN>) {
			if ($Conf{dc_firstnode} eq "y" ) {
				s/^#\s*server-id=1/server-id=1/; #uncomment server-id 1
				s/^\s*server-id=2/#server-id=2/; #comment out server-id 2
			} else {
                                s/^#\s*server-id=2/server-id=2/; #uncomment server-id 2
                                s/^\s*server-id=1/#server-id=1/; #comment out server-id 1
			}
			s/^#\s*log-bin/log-bin/; #ensure uncomment
			s/^#\s*log-slave-update/log-slave-update/; #ensure uncomment
			print CNF_OUT;
		}		
		close CNF_IN;
		close CNF_OUT;
		# server id is set. Need to run the sync; then set the master/slave relationship
		# perhaps this is best left to the user.
		print "INFO: DB cluster $cnf updated cluster server id, log-bin, log-slave-update - please check $cnf for accuracy\n";
                print "MANUAL TASK: Server id is set - need to manually run the sync, then set the master/slave relationship\n";
                print "MANUAL TASK: $cnf is configured - need to manually restart MYSQL\n";
	
                print "INFO: Now updating $dbfaildetect_file\n";
		open (FDF_IN,"<$dbfaildetect_file.bckup_$pid");
		open (FDF_OUT,">$dbfaildetect_file");
                while (<FDF_IN>) {
                        s/^DBHOST=(.*)/DBHOST=$Conf{dc_dbip}/;
			s/^EMAIL=(.*)/EMAIL=$Conf{dc_email}/;
                        print FDF_OUT;
                }
                close FDF_IN;
                close FDF_OUT;
		print "INFO: DB cluster $dbfaildetect_file updated with DBHOST $Conf{dc_dbip}, EMAIL $Conf{dc_email}\n";
                print "MANUAL TASK: $dbfaildetect_file is configured - need to manually start service when ready\n";
	}

        if ($Conf{dc_cluster} eq "n") {  #reset 
                print "INFO: Now updating $cnf\n";
                open(CNF_IN, "<$cnf_rpm");
                open(CNF_OUT, ">$cnf");
                while(<CNF_IN>) {
                        s/^\s*server-id=1/#server-id=1/; #comment out
			s/^\s*server-id=2/#server-id=2/; #comment out
                        s/^\s*log-bin/#log-bin/; #comment out
                        s/^\s*log-slave-update/#log-slave-update/; #comment out
                        print CNF_OUT;
                }
                close CNF_IN;
                close CNF_OUT;
		print "INFO: DB cluster $cnf updated by commenting out server-id, log-bin, log-slave-update\n";
                print "MANUAL TASK: $cnf is configured - need to manually restart MYSQL\n";

                print "INFO: Now updating $dbfaildetect_file\n";
                open (FDF_IN,"<$dbfaildetect_file.bckup_$pid");
                open (FDF_OUT,">$dbfaildetect_file");
                while (<FDF_IN>) {
                        s/^DBHOST=(.*)$/DBHOST="NEED_A_REAL_DB_CLUSTER_ADDRESS"/;
                        s/^EMAIL=(.*)$/EMAIL="NEED_A_REAL_EMAIL"/;
                        print FDF_OUT;
                }
                close FDF_IN;
                close FDF_OUT;
                print "INFO: DB cluster $dbfaildetect_file DBHOST, EMAIL values removed\n";

	}

	print "INFO: Changing owner user/group to $user_name/$group_name for $dbfaildetect_file\n";
        system ("chown $user_name.$group_name $dbfaildetect_file");

	# 5. gen keys?  (keys should be generated only after hostname/cluster hostname confirmed)
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
	        print "INFO: Changing owner user/group to $user_name/$group_name for /ssg/etc, /ssg/tomcat/conf/server.xml, /ssg/etc/conf/keystore.properties\n";
		system ("chown -R $user_name.$group_name /ssg/etc");
		system ("chown $user_name.$group_name /ssg/tomcat/conf/server.xml");
		system ("chown $user_name.$group_name /ssg/etc/conf/keystore.properties");
	}
}
	

sub readconfig {
	open (SV,"<$save_file");
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
	open (SV, ">$save_file");
 	my @fieldlist=@list;
	{ my $i; @fieldlist= grep { ++$i % 2 } @fieldlist; }
	foreach  my $f (@fieldlist) {
		print SV "$f=$Conf{$f}\n";
	}
	close SV;
}

sub usage {
	return <<EOF;
$0: [-apply] [-usage]
-apply     Don't ask for config, run OS config using $save_file
-usage     This message.
$0 can be run without any option
  - if $save_file does not exist, script will invoke initial install 
  - if $save_file already existed (implies reinstall/reconfigure SSG), script will ask for configuration again to change any values, then re-run OS configuration
EOF

}

sub trimwhitespace($) {
# Remove whitespace from the start and end of the string
	my $string = shift;
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	return $string;
}
