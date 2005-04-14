#!/usr/bin/perl
# Script to prompt for configuration values
# save a text file 
# and then apply

use strict;

# following hash keys are used by getparams sub routine for pattern matching, if you are modifying the key, please update getparams sub routine
my @list=(
	hostname      => "Hostname for this SSG node (eg. hostname.company.com)                                                        ",
	host_ip       => "Host IP for this SSG node (eg. 192.168.1.9)                                                                  ",
	gc_cluster    => "Cluster / Non Cluster Gateway Env - Node is part of a cluster gateway (y/n)                                  ",
	gc_firstnode  => "--Cluster Gateway Env, First node in gateway cluster (y/n)                                                   ",
	gc_masternip  => "--Cluster Gateway Env, IP (reachable by this host) of the first node (eg. 10.0.0.7) to copy keys             ",
        gc_clusternm  => "--Cluster Gateway Env, Cluster host name (eg. cluster_hostname.company.com)                                  ",
	dc_cluster    => "Cluster / Non Cluster DB Env - Node is to be connected to a cluster database environment (y/n)               ",
	dc_dbserver   => "--Cluster DB Env, Node is a DB Server (y/n)                                                                  ",
        dc_repluser   => "--Cluster DB Env, Replicator Username (eg. repl)                                                             ",
        dc_replpass   => "--Cluster DB Env, Replicator Password (eg. replpass)                                                         ", 
	dc_firstnode  => "--Cluster DB Env, First node in DB cluster (y/n)                                                             ",
        dc_dbip       => "--Cluster DB Env, DB cluster IP (eg. 10.0.0.25)                                                              ",
	dc_email      => "--Cluster DB Env, DB cluster Email (email upon failover)                                                     ",
	dblocal       => "--Non Cluster DB Env, DB is on the localhost (database must able to login as localhost) (y/n)                ",
	dbhostname    => "--Non cluster DB Env, DB hostname or IP (must be reach-able by this node) if DB is not on localhost          ",
	dbuser        => "SSG Database - Username                                                                                      ",
	dbpass        => "SSG Database - Password for the above user                                                                   ",
	net_front     => "Public Network Configuration (Front), /etc/sysconfig/network-scripts/ifcfg-eth1 (y/n)                        ",
        net_front_dhcp=> "Public Network Config - Network is DHCP; ifcfg-eth1 BOOTPROTO (reply 'y' to dhcp , 'n' to static IP) (y/n)   ",
	net_front_ip  => "--Public Network Config - Node Public IP; ifcfg-eth1 IPADDR (eg. 192.168.1.8)                                ",
	net_front_mask=> "--Public Network Config - Network Mask; ifcfg-eht1 NETMASK (eg. 255.255.255.0)                               ",
        net_front_rt  => "--Public Network Config - Gateway Load Balancer IP SSB/client<->gateway; ifcfg-eth1 GATEWAY (eg. 192.168.1.1)",
        net_back      => "Private Network Configuration (Back), /etc/sysconfig/network-scripts/ifcfg-eth0, /etc/init.d/back_route (y/n)",
        net_back_dhcp => "--Private Network Config - Network is DHCP; ifcfg-eth0 BOOTPROTO (reply 'y' to dhcp, 'n' to static IP) (y/n) ",
	net_back_ip   => "--Private Network Config - Node Private IP; back_route, ifcfg-eth0 IPADDR (eg. 10.0.0.8)                     ",
	net_back_mask => "--Private Network Config - Private Network Mask; back_route netmask, ifcfg-eth0 NETMASK (eg. 255.255.255.0)  ",
	net_back_rt   => "--Private Network Config - Private Gateway; back_route gw (eg. 10.0.0.1)                                     ",
        net_back_net  => "--Private Network Config - Node private Network; back_route -net                                             ",
        tserver_cnt   => "Number of time servers - preferably in geographical disparate locations (0 to 4)                             ",
        tserver_ip1   => "--Time server number 1 IP (eg. 128.101.101.101)                                                              ",
        tserver_mask1 => "----Time server number 1 mask (eg. 255.255.255.255)                                                          ",
        tserver_ip2   => "--Time server number 2 IP (eg. 207.200.81.113)                                                               ",
        tserver_mask2 => "----Time server number 2 mask (eg. 255.255.255.255)                                                          ",
        tserver_ip3   => "--Time server number 3 IP (eg. 199.240.130.1)                                                                ",
        tserver_mask3 => "----Time server number 3 mask (eg. 255.255.255.255)                                                          ",
        tserver_ip4   => "--Time server number 4 IP (eg. 216.204.156.2)                                                                ",
	tserver_mask4 => "----Time server number 4 mask (eg. 255.255.255.255)                                                          "
);

my %Conf=();
my $pid = $$;
my $save_file="/etc/SSG_INSTALL";
my $group_name="gateway";
my $user_name="gateway";
my $setkey="/ssg/bin/setkeys.sh";
my $script_log="/tmp/install_ssg.log";

# Files subject to configuration
my $front_conf = "/etc/sysconfig/network-scripts/ifcfg-eth1";
my $back_conf = "/etc/sysconfig/network-scripts/ifcfg-eth0";
my $cnf = "/etc/my.cnf";
my $dbfaildetect_file = "/ssg/bin/dbfaildetect.sh";
my $cluster_hostname_file = "/ssg/etc/conf/cluster_hostname";
my $hosts_file = "/etc/hosts";
my $network_file = "/etc/sysconfig/network";
my $dbconfig = "/ssg/etc/conf/hibernate.properties";
my $ntp_config = "/etc/ntp.conf";
my $ntp_step_tickers = "/etc/ntp/step-tickers";

# then change the local files as appropriate

open (STDOUT, "| tee $script_log");
print "INFO: STDOUT will be logged to <$script_log>\n"; 

if ($ARGV[0] eq '-usage' ) {
        print usage();
	close (STDOUT);
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
		print "INFO: If you want to remove all default values from last installation, please exit from re-installing, manually remove $save_file, then re-invoke $0\n"; 
		print "Do you want to re-installing/re-configuring SSG? (y/n)";
                my $ans_proceed=<STDIN>;
                chomp $ans_proceed;
                $ans_proceed = trimwhitespace($ans_proceed);
                if ($ans_proceed ne 'y') {
                        print "INFO: $0 exits\n";
			close (STDOUT);
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

close (STDOUT);

sub change_os_config {
	my $host = (split(/\./,$Conf{hostname}))[0];
	my $cluster = (split(/\./,$Conf{gc_clusternm}))[0];

	print ("INFO: Current PID and files will be back up suffixed with PID of $pid\n");
	# lets start small

	# 1. the front network config - file affected: $front_conf
	print "INFO: Starting Front End Network Configuration... \n";

	if ($Conf{net_front} eq 'y') {
		if (($Conf{net_front_dhcp} eq 'n') && $Conf{net_front_ip} && $Conf{net_front_mask} && $Conf{net_front_rt}) {
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
GATEWAY=$Conf{"net_front_rt"}

EOF
			close OUT;
			print "INFO: Front end network configured $front_conf - IPADDR of $Conf{net_front_ip}; NETMASK of $Conf{net_front_mask}; GATEWAY of $Conf{net_front_rt}";	
		} else {
                	open (OUT, ">$front_conf");
                	print OUT <<EOF;
DEVICE=eth1
ONBOOT=yes
BOOTPROTO=dhcp
TYPE=Ethernet
DHCP_HOSTNAME=$host

check_link_down() {
        return 1;
}

EOF
                	close OUT;
                	print "INFO: Front end network configured $front_conf - BOOTPROTO of dhcp; DHCP_HOSTNAME of $host\n";

		}
	} else {
               	print "WARNING: No front end network defined, front end network remains $front_conf\n";
	}

	# 2. the back network config - file affected: $back_conf
        print "INFO: Starting Back End Network Configuration... \n";

	if ($Conf{net_back} eq 'y') {
		if ( -e $back_conf ) {
        		print "INFO: $back_conf existed, now renaming $back_conf to $back_conf.bckup_$pid\n";
               		rename( $back_conf, "$back_conf.bckup_$pid");
       		} 

		if (($Conf{net_back_dhcp} eq 'n') && $Conf{net_back_ip} && $Conf{net_back_mask}) {
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
       	} else {
               	print "WARNING: No back end network defined, back end network remains $back_conf\n";
       	}

	# 3. hostname - file affected:$hosts_file, $network_file
	# 3a. hostname for cluster - file affected: $cluster_hostname_file
	print "INFO: Configuring node hostname for $network_file\n";
        if ( -e $network_file ) {
                print "INFO: $network_file existed, now renaming $network_file to $network_file.bckup_$pid\n";
                rename( $network_file, "$network_file.bckup_$pid");
        }

        open (NETWORK_FILE, ">$network_file") or die "Can't open $network_file!\n";
        print NETWORK_FILE <<EOF;
# Modified by /ssg/bin/install.pl
# Will be replaced if you rerun!
NETWORKING=yes
HOSTNAME=$Conf{hostname}

EOF
        close NETWORK_FILE;
        chmod 0644, "$network_file";

        print "INFO: Gateway - $network_file file replaced\n";

        print "INFO: Setting hostname to $Conf{hostname}\n";
        system ("hostname $Conf{hostname}");

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

		print "INFO: Cluster Gateway - $cluster_hostname_file updated with content of $Conf{gc_clusternm}\n";
	        print "INFO: Cluster Gateway - Changing owner user/group to $user_name/$group_name for $cluster_hostname_file\n";
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
$Conf{net_front_ip}	$Conf{gc_clusternm} $Conf{hostname}

EOF
		close HOST;
		chmod 0644, "$hosts_file";

		print "INFO: Cluster Gateway - $cluster_hostname_file and $hosts_file files replaced\n";
	} else {
		print "WARNING: $cluster_hostname_file not set (OK if SSG is not part of a cluster)\n";
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

                print "INFO: Non Cluster Gateway - $cluster_hostname_file removed (backup as $cluster_hostname_file.bckup_$pid) and $hosts_file files replaced\n";
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
		print "INFO: Database Hibernate Connection - $dbconfig updated with url/user/password of $db_url/$Conf{dbuser}/$Conf{dbpass}\n";
	        print "INFO: Database Hibernate Connection - Changing user/group to $user_name/$group_name for $dbconfig\n";
       		system ("chown $user_name.$group_name $dbconfig");

	} else {
		print "WARNING: $dbconfig file not set\n";
	}

	# 4b. grant users to local database

	# grant database user
	if (($Conf{dbuser}) && (($Conf{dc_dbserver} eq "y") || ($Conf{dblocal} eq "y"))) {
		print "INFO: Now grant username & password to local database\n";
		my $sql = "grant all on ssg.* to $Conf{dbuser}\@'%' identified by '$Conf{dbpass}';\n\n";
		$sql .="grant all on ssg.* to $Conf{dbuser}\@'localhost' identified by '$Conf{dbpass}';\n\n";
		open (TMP, ">/tmp/sql.grants");
		print TMP $sql;
		close TMP;
		print "INFO: Now run following SQL... \n$sql\n";
		my $success=`mysql -u root </tmp/sql.grants`;
		#unlink "/tmp/sql.grants";
	} else {
		print "WARNING: DB user username/password not granted to local database\n";
	}

	# grant replicator user
	if (($Conf{dc_cluster} eq "y") && ($Conf{dc_dbserver} eq "y") && ($Conf{dc_repluser}) && ($Conf{dc_replpass})) {
		print "INFO: Now grant replicator username & password to local database\n";
                my $sql = "grant replication slave on *.* to $Conf{dc_repluser}\@'%' identified by '$Conf{dc_replpass}';\n\n";
                open (TMP, ">/tmp/sqlReplicator.grants");
                print TMP $sql;
                close TMP;
                print "INFO: Now run following SQL... \n$sql\n";
                my $success=`mysql -u root </tmp/sqlReplicator.grants`;
                #unlink "/tmp/sqlReplicator.grants";
        } else {
                print "WARNING: DB replicator username/password not granted to local database (OK for non cluster database, and this host is not a database server)\n";
        }
 
	# 4c. do the config for db cluster on my.cnf and on dbfaildetect.sh? - file affected: $cnf, $dbfaildetect_file
        # assumption: my.cnf contains server-id=1, server-id=2, log-bin, log-slave-update
        #             dbfaildetect.sh contains DBHOST=, EMAIL=
	my $cnf_rpm = "/etc/my.cnf.ssg";

	if (! -e $cnf_rpm ) {
                print "ERROR: $cnf_rpm distribution missing from rpm!\n";
		print "INFO: $0 exits; please re-run $0\n";
		close (STDOUT);
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
		print "INFO: Cluster DB - $cnf updated cluster server id, log-bin, log-slave-update - please check $cnf for accuracy\n";
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
		print "INFO: Cluster DB - $dbfaildetect_file updated with DBHOST of $Conf{dc_dbip}, EMAIL of $Conf{dc_email}\n";
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
		print "INFO: DB cluster - $cnf updated by commenting out server-id, log-bin, log-slave-update\n";
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
                print "INFO: Non Cluster DB - $dbfaildetect_file DBHOST, EMAIL values removed\n";

	}

	print "INFO: Changing owner user/group to $user_name/$group_name for $dbfaildetect_file\n";
        system ("chown $user_name.$group_name $dbfaildetect_file");

        # 5. time servers configuration
	my $ntp_update_proc = 0;
	if (($Conf{"tserver_cnt"} > 0) 
		&& (-e $ntp_config)
		&& (-e $ntp_step_tickers) 
		&& ($Conf{"tserver_ip1"} ne '') 
		&& ($Conf{"tserver_mask1"} ne '')) {
		$ntp_update_proc = 1;
	}

        if ($ntp_update_proc == 1) {
		print "INFO: $ntp_config existed, now backup $ntp_config to $ntp_config.bckup_$pid\n";
		rename($ntp_config, "$ntp_config.bckup_$pid");
	}
        if ($ntp_update_proc == 1) {
                print "INFO: $ntp_step_tickers existed, now backup $ntp_step_tickers to $ntp_step_tickers.bckup_$pid\n";
                rename($ntp_step_tickers, "$ntp_step_tickers.bckup_$pid");
        }

	if ($ntp_update_proc == 1){
		open (NTP_CONF_FILE, ">$ntp_config") or die "Can't open $ntp_config!\n";
		print NTP_CONF_FILE <<EOF;
server 127.127.1.0 # local clock
fudge 127.127.1.0 stratum 10 
driftfile /var/lib/ntp/drift
restrict default ignore
restrict 127.0.0.0 mask 255.0.0.0
authenticate no
EOF
		open (NTP_TICKERS_FILE, ">$ntp_step_tickers") or die "Can't open $ntp_step_tickers!\n";

		if (($Conf{"tserver_cnt"} >= 1) && ($Conf{"tserver_ip1"} ne '') && ($Conf{"tserver_mask1"} ne '')) {
                	print NTP_CONF_FILE <<EOF;
server $Conf{"tserver_ip1"}
restrict $Conf{"tserver_ip1"} mask $Conf{"tserver_mask1"} nomodify notrap noquery
EOF
			print NTP_TICKERS_FILE <<EOF;
$Conf{"tserver_ip1"}
EOF
                	print "INFO: Network Time Protocol (ntp) - time server 1 - $ntp_config updated with content of $Conf{tserver_ip1}\n";
                        print "INFO: Network Time Protocol (ntp) - time server 1 - $ntp_step_tickers updated with content of $Conf{tserver_mask1}\n";
		}

                if (($Conf{"tserver_cnt"} >= 2) && ($Conf{"tserver_ip2"} ne '') && ($Conf{"tserver_mask2"} ne '')) {
                        print NTP_CONF_FILE <<EOF;
server $Conf{"tserver_ip2"}
restrict $Conf{"tserver_ip2"} mask $Conf{"tserver_mask1"} nomodify notrap noquery
EOF
                        print NTP_TICKERS_FILE <<EOF;
$Conf{"tserver_ip2"}
EOF

                        print "INFO: Network Time Protocol (ntp) - time server 2 - $ntp_config updated with content of $Conf{tserver_ip2}\n";
                        print "INFO: Network Time Protocol (ntp) - time server 2 - $ntp_step_tickers updated with content of $Conf{tserver_mask2}\n";
                }

                if (($Conf{"tserver_cnt"} >= 3) && ($Conf{"tserver_ip3"} ne '') && ($Conf{"tserver_mask3"} ne '')) {
                        print NTP_CONF_FILE <<EOF;
server $Conf{"tserver_ip3"}
restrict $Conf{"tserver_ip3"} mask $Conf{"tserver_mask1"} nomodify notrap noquery
EOF
                        print NTP_TICKERS_FILE <<EOF;
$Conf{"tserver_ip3"}
EOF
                        print "INFO: Network Time Protocol (ntp) - time server 3 - $ntp_config updated with content of $Conf{tserver_ip3}\n";
                        print "INFO: Network Time Protocol (ntp) - time server 3 - $ntp_step_tickers updated with content of $Conf{tserver_mask3}\n";
                }

                if (($Conf{"tserver_cnt"} >= 4) && ($Conf{"tserver_ip4"} ne '') && ($Conf{"tserver_mask4"} ne '')) {
                        print NTP_CONF_FILE <<EOF;
server $Conf{"tserver_ip4"}
restrict $Conf{"tserver_ip4"} mask $Conf{"tserver_mask4"} nomodify notrap noquery
EOF
                        print NTP_TICKERS_FILE <<EOF;
$Conf{"tserver_ip4"}
EOF
                        print "INFO: Network Time Protocol (ntp) - time server 4 - $ntp_config updated with content of $Conf{tserver_ip4}\n";
                        print "INFO: Network Time Protocol (ntp) - time server 4 - $ntp_step_tickers updated with content of $Conf{tserver_mask4}\n";
                }

		close NTP_CONF_FILE;
		close NTP_TICKERS_FILE;
		chmod 0644, "$ntp_config";
		chmod 0644, "$ntp_step_tickers";
       		print "INFO: Network Time Protocol (ntp) - $ntp_config updated\n";
		print "INFO: Network Time Protocol (ntp) - $ntp_step_tickers updated\n";
		system ("chown ntp.ntp $ntp_config");
		system ("chown ntp.ntp $ntp_step_tickers");
		print "INFO: Network Time Protocol - Restarting ntpd (synchronization will take some time, ***please wait...***)\n";
		system ("/sbin/service ntpd restart");
		system ("/sbin/service ntpd status");
                print "INFO: Please ignore any \"shutting down ntpd [FAILED]\" (result depends on whether ntpd has been started initially), we are only interested in \"synchronizing with time server\" and \"starting ntpd processes\" are [OK]\n";
		print "INFO: Network Time Protocol - ntpd is configured to be started during system boot-up (chkconfig) at init levels 3,4,5\n";
		system ("chkconfig --level 345 ntpd on");
		system ("chkconfig --list ntpd");
	
	} else {
		print "WARNING: SecureSpan Gateway (TM) requires time being sychronized for Federated Identity Provider (SAML) running properly.\n";
		print "ERROR: Network Time Protocol (ntp) not installed/configured. Please install and configure\n";
		print "MANUAL TASK: Please install ntp, and configure <$ntp_config> and <$ntp_step_tickers>, and (service ntpd start).\n";  
		print "MANUAL TASK: Please have ntpd started at boot time - (chkconfig ntpd on).\n"; 
	}

	# 6. gen keys?  (keys should be generated only after hostname/cluster hostname confirmed)
	if (($Conf{gc_cluster} eq "n") || ($Conf{gc_cluster} eq "y" && $Conf{gc_firstnode} eq "y")) {
		# gen the keys, otherwise copy them
		print "Invoke $setkey script to generate keys\n";
		system("su - gateway -c $setkey");

	} else {
		print "Copy keys & keys configuration files from the first node...\n";
		print "Supply gateway user password of first node - $Conf{gc_masternip}\n";
                print "Now to copy /ssg/etc/keys/*:\n"; 
		system("scp root\@$Conf{gc_masternip}:/ssg/etc/keys/* /ssg/etc/keys");
		print "Now to copy /ssg/tomcat/conf/server.xml:\n";
		system("scp root\@$Conf{gc_masternip}:/ssg/tomcat/conf/server.xml /ssg/tomcat/conf/server.xml");
		print "Now to copy /ssg/etc/conf/keystore.properties:\n";
		system("scp root\@$Conf{gc_masternip}:/ssg/etc/conf/keystore.properties /ssg/etc/conf/keystore.properties");
	        print "INFO: Changing owner user/group to $user_name/$group_name for /ssg/etc/keys/, /ssg/tomcat/conf/server.xml, /ssg/etc/conf/keystore.properties\n";
		system ("chown -R $user_name.$group_name /ssg/etc/keys");
		system ("chown $user_name.$group_name /ssg/tomcat/conf/server.xml");
		system ("chown $user_name.$group_name /ssg/etc/conf/keystore.properties");
	}

        # 6. inform user that the script is done
        print "INFO: If <$setkey> script being invoked by <$0> and error occurs, you just need to re-run <$setkey>\n";
	print "MANUAL TASK: MySQL - Re-start database service (as </etc/my.cnf> has just been configured by this script), then create ssg database, and ssg database tables if applicable:\n";
        print "---(1)  re-start MySQL datbase service and purge database (if existed)\n";
	print "------# service mysql stop\n";
    	print "------# rm /var/lib/mysql/ib*\n";
    	print "------# rm -Rf /var/lib/mysql/ssg\n";
    	print "------# service mysql start\n";
        print "---(2)  Observe MySQL log (tail -f /var/log/mysqld.log) to wait until the /var/lib/mysql/ib* schema is completed before continuing\n";
	print "---(3)  Following steps of creating SSG database and tables only applicable if ssg database is running on this node - non cluster database, or first node of a cluster database, see note 1)\n"; 
	print "---(3a) Create SSG database\n" ;
	print "------# mysqladmin create ssg\n";
	print "------or\n";
	print "------# mysqladmin --password='<database root user password>' create ssg\n";
        print "---(3b) Create SSG database tables with correct version of SQL script file\n";
        print "------# mysql ssg < /ssg/etc/sql/ssg.sql\n";
	print "------or\n";
        print "------# mysql -p<database root user password> ssg < /ssg/etc/sql/ssg.sql\n";
	print "note 1: if it is not the first node of the cluster database - base on the </etc/my.cnf>, SSG database will be replicating after you've started mysql on node 2; SSG database and tables will be created by the replicator as well, so you don't need to manually creating SSG database and tables on node 2\n";
        print "MANUAL TASK: After you are satisfied with the installation, you now can delete any backup files that install.pl have backed up during the installation - /etc/sysconfig/network-scripts/*.bckup_*; /etc/sysconfig/*.bckup_*; /etc/hosts.bckup_*; /etc/my.cnf.bckup_*; /etc/ntp.conf.bckup_*; /etc/ntp/step-tickers.bckup_*\n"; 
        print "INFO: <$0> script done\n"; 
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
NEXT_PARAM:
	foreach  my $f (@fieldlist) {
REPEAT_PARAM:
		# following pattern logic for prompt filtering depends on hash key of the @list, ensure this is updated when you modify hash key
                if ( ($Conf{gc_cluster} eq 'n' && $f ne "gc_cluster" && $f =~ /gc_(.*)/) #if not gateway cluster, omit cluster gateway prompts
			|| ($Conf{dc_cluster} eq 'n' && $f ne "dc_cluster" && $f =~ /dc_(.*)/) #if not database cluster, omit cluster database prompts
			|| ($Conf{dc_cluster} eq 'y' && $f ne "dc_cluster" && ($f eq "dblocal" || $f eq "dbhostname")) #if database cluster, omit standalone database prompts
			|| ($Conf{dblocal} eq 'y' && $f eq "dbhostname") #if local database, omit remote database prompt
			|| ($Conf{net_front} eq 'n' && $f =~ /net_front_(.*)/) #if no net_front, omit net_front prompts
			|| ($Conf{net_back} eq 'n' && $f =~ /net_back_(.*)/) #if no net_back, omit net_back prompts
                        || ($Conf{tserver_cnt} == 0 && ($f =~ /tserver_ip(.*)/ || $f =~ /tserver_mask(.*)/)) #if no time servers, omit tserver prompts 

                        || ($Conf{tserver_cnt} == 1 && ($f =~ /tserver_ip[2-4]/ || $f =~ /tserver_mask[2-4]/)) #if 1 time server, omit tserver prompts for 2-4 

                        || ($Conf{tserver_cnt} == 2 && ($f =~ /tserver_ip[3-4]/ || $f =~ /tserver_mask[3-4]/)) #if 2 time servers, omit tserver prompt for 3-4 

                        || ($Conf{tserver_cnt} == 3 && ($f =~ /tserver_ip4/ || $f =~ /tserver_mask4/)) ) { #if 3 time servers, omit tserver prompt for 4 
			$Conf{$f}=''; #purge omit values in case they are set by prior installation (reinstall)
			next NEXT_PARAM;
		} else { 
			print "$c{$f} ($Conf{$f}) :";
		}

		my $newval=<STDIN>;
		chomp $newval;
		$newval = trimwhitespace($newval);
		if ( ($f eq "hostname" && $Conf{$f} eq "" && $newval eq "")
 			|| ($f eq "host_ip" && $Conf{$f} eq "" && $newval eq "") 
			|| ($f eq "gc_cluster" && ($Conf{$f} ne "y" && $Conf{$f} ne "n") && ($newval ne "y" && $newval ne "n")) 
			|| ($f eq "dc_cluster" && ($Conf{$f} ne "y" && $Conf{$f} ne "n") && ($newval ne "y" && $newval ne "n"))
			|| ($f eq "dbuser" && $Conf{$f} eq "" && $newval eq "")
			|| ($f eq "dbpass" && $Conf{$f} eq "" && $newval eq "")
			|| ($f eq "net_front" && ($Conf{$f} ne "y" && $Conf{$f} ne "n") && ($newval ne "y" && $newval ne "n")) 
			|| ($f eq "net_back" && ($Conf{$f} ne "y" && $Conf{$f} ne "n") && ($newval ne "y" && $newval ne "n")) ) {
			print "ERROR: Value for $f is mandatory\n";
                        goto REPEAT_PARAM;
		}
		if ($newval ne "") {
			$Conf{$f}=$newval;
		}
	}
}


sub display {
print "====================================================================================\n";
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
===========================
NAME
    $0

SYNOPSIS
    $0 [OPTION]

    For example,
      $0 -usage
      $0 -apply

DESCRIPTION
    Configure Secure Span Gateway (SSG) property files after installing with RPM distribution

    After this script has been run, the input parameters are saved at $save_file.  
    If you re-run the script, the $save_file will be read in as default values.  
    However, if you want to remove the default values, you may like to remove the $save_file before re-running the script. 
    $save_file is also used by "/etc/init.d/back_route" file; therefore, $save_file must contain the proper values in order for "/etc/init.d/back_route" working properly.

    Private network HAS TO BE on eth0, NOT eth1.

    Network Architecture Assumption: 
    [Load Balancer] <-Public Network (eth1)-> [SSG Cluster] <-Private Network (eth0)-> [Router] <-> [WebServices]

    List of property files may subject to be configured base on input parameters:
    * $front_conf
    * $back_conf
    * $cnf 
    * $dbfaildetect_file
    * $cluster_hostname_file
    * $hosts_file
    * $network_file
    * $dbconfig
    * $ntp_config
    * $ntp_step_tickers

    Base on input parameters, this script may grant database access for MySQL database:
    * GRANT ALL ON ssg.* to <db username>@\'%\' identified by \'<db user password>\';
    * GRANT ALL ON ssg.* to <db_username>@\'localhost\' identified by \'<db user password>\';
    * GRANT REPLICATION SLAVE ON *.* to <db replicator username>@\'%\' identified by \'<db replicator password>\';
    However, this script will not revoke any access that has been granted. You will need to perform manual revoke access if necessary.

    $setkey will be run to generate CA and SSL keys if this is a non cluster SSG, or this node is the first node of a cluster SSG. 
    For the rest of the nodes (not the first node), CA and SSL keys will be copied from the first node of the cluster.

    The STDOUT of $0 will be logged at $script_log

OPTION
    -apply     Don't ask for config, run OS config using $save_file
    -usage     This message.

    $0 can be run without any option
      - if $save_file does not exist, script will invoke initial install 
      - if $save_file already existed (implies reinstall/reconfigure SSG), script will ask for configuration again to change any values, then re-run OS configuration
===========================

EOF

}

sub trimwhitespace($) {
# Remove whitespace from the start and end of the string
	my $string = shift;
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	return $string;
}
