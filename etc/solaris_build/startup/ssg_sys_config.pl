#!/usr/bin/perl -w

use strict;
use FileHandle;

my $timestamp = localtime time;
my $logFile = new FileHandle;
my $hostsFH = new FileHandle;
my $outputFh = new FileHandle;
my $inputFh = new FileHandle;

if ($logFile->open(">>/ssg/sysconfigwizard/ssgsysconfig.log")) {
	$logFile->print("\n-------------------------------------------------------------------\n");
	$logFile->print("$timestamp: System Configuration Started\n");
	$logFile->print("-------------------------------------------------------------------\n");
} else {
 	print("Could not open log file. Will not proceed with system configuration\n");
}

my %hostnameInfo = ();
my $ntpServerToUse = undef;
my @filesToDelete;
my ($hostname, $domain) = ($hostnameInfo{'hostname'}, $hostnameInfo{'domain'});

my $netConfigPattern = "/ssg/sysconfigwizard/configfiles/netconfig_*";

my %inputFiles = (
	"HOSTNAMEFILE" => "/ssg/sysconfigwizard/configfiles/hostname",
	"NTPFILE" => "/ssg/sysconfigwizard/configfiles/ntpconfig",
	"TZFILE" => "/ssg/sysconfigwizard/configfiles/timezone"
);

my %outputFiles = (
	"NTP" 		=> "/etc/inet/ntp.conf",
	"TZ" 		=> "/etc/default/init",
	"GATEWAY" 	=> "/etc/defaultrouter",
	"RESOLV" 	=> "/etc/resolv.conf",
	"HOSTNAME" 	=> "/etc/nodename",
	"DOMAIN" 	=> "/etc/defaultdomain",
	"HOSTS"		=> "/etc/hosts",
	"ETC" 		=> "/etc/"
);

##########################http://xkcd.com/c208.html#####################

#Setup hostname, domain name
if ($inputFh->open("<$inputFiles{'HOSTNAMEFILE'}")) {
	my $fileContent = do { local( $/ ) ; <$inputFh> } ;
	%hostnameInfo = $fileContent =~ /^(\w+)=(.+)$/mg ;
	my $hostnameToUse = $hostnameInfo{'hostname'};
	if ($hostnameToUse ne "") {
		$logFile->print("$timestamp: hostname found: $hostnameToUse\n");
		if (defined($hostnameInfo{'hostname'}) && $hostnameInfo{'hostname'} ne "") {
			($hostname, $domain) = ($hostnameInfo{'hostname'}, $hostnameInfo{'domain'});
		
			if ($outputFh->open(">$outputFiles{'HOSTNAME'}")) {
				$outputFh->print("$hostname\n");
				$outputFh->close();
				$logFile->print("$timestamp: Wrote $outputFiles{'HOSTNAME'} with:\n");
				$logFile->print("\t$hostname\n");
			} else {
				$logFile->print("$timestamp: Couldn't open $outputFiles{'HOSTNAME'}. Unable to set nodename (hostname)\n");
			}
			if ($outputFh->open(">$outputFiles{'DOMAIN'}")) {
				$outputFh->print("$domain\n");
				$outputFh->close();
				$logFile->print("$timestamp: Wrote $outputFiles{'DOMAIN'} with:\n");
				$logFile->print("\t$domain\n");
			} else {
				$logFile->print("$timestamp: Couldn't open $outputFiles{'DOMAIN'}. Unable to set domain name\n");
			}
		}
		if ($hostsFH->open(">$outputFiles{'HOSTS'}")) {
			$hostsFH->print("127.0.0.1\t\t\tlocalhost loghost $hostname\n");
			$hostsFH->print("::1\t\t\tlocalhost localhost6\n");
			$hostsFH->close();
		} else {
			$logFile->print("$timestamp: couldn't write hosts file\n");
		}

	} else {
		$logFile->print("$timestamp: no hostname found. Hostname will not be set\n");
		%hostnameInfo = undef;
	}
	$inputFh->close();
	push (@filesToDelete, $inputFiles{'HOSTNAMEFILE'});
} else {
	$logFile->print("$timestamp: $inputFiles{'HOSTNAMEFILE'} not found. The hostname will not be changed.\n");
}

#Setup Interfaces & Routes
my @netConfigFiles = glob($netConfigPattern);
for my $configFile(@netConfigFiles) {
	if($inputFh->open("<$configFile")) {
		my (undef, $ifName) = split("_", $configFile);
		my $params = " ";
		while(my $line = <$inputFh>) {
			chomp($line);
			$params = $params."$line ";
		}

		my %opts;
		while ($params =~ /--([^=\s]+)(?:=|\s+)(\S+)/g) {
			my ($option, $value) = ($1, $2);
			if ($option eq "nameserver") {
				push @{ $opts{$option} }, $value;
			} else {
				$opts{$option} = $value;
			}
		}

		#Read in the existing netmasks file
		my @raw_data;
		open(DAT, "$outputFiles{'ETC'}netmasks") || die("Could not open file! Your Solaris Installation is Damaged!\n");
		@raw_data=<DAT>;
		close(DAT);
		
		# Enable pfil hooks for card driver
		my $PfilFh=undef;
		if($PfilFh->open("/etc/ipf/pfil.ap")) {
			my @pfil=(<$PfilFh>);
			if ( grep /$opts{device}/, @pfil ) {
				# Its there
				# so we don't have to add it
			} else {
				# Append it
				$PfilFh->close();
				$PfilFh->open(">>/etc/ipf/pfil.ap");
				$PfilFh->print("$opts{device} -1      0       pfil\n");
			}
			$PfilFh->close();
		} else {
			print "Unable to add $opts{device} to firewall config\n";
			$logFile->print("Unable to add $opts{device} to firewall\n");
		}

		# DHCP is easy, assuming it works on boot, 
		# just write hostnames to the file and that's it.
		if ($opts{bootproto} eq "dhcp") {
			# turns out you HAVE to have a hostname.int file 
			# otherwise the interface doesn't get plumbed.
			# remove previous
			unlink("$outputFiles{'ETC'}hostname.$opts{device}");
			# write a new one with "inet hostname" to tickle DDNS in bind 9
			if ($outputFh->open(">$outputFiles{'ETC'}hostname.$opts{device}")) {
				$outputFh->print("inet $hostname\n");
				$outputFh->close();
			}

			if($outputFh->open(">$outputFiles{'ETC'}dhcp.$opts{device}")) {
				$outputFh->close();
			} else {
				print "Unable to configure DHCP for $opts{device}, weird.\n";
				$logFile->print("Unable to configure DHCP for $opts{device}, weird.\n");
			}
			##### Edit /etc/netmasks here. (Just incase there's a mask for this interface existing.)

			#Write out the file with the changed masks
			if ($outputFh->open(">$outputFiles{'ETC'}netmasks")) {
				my $maskline;
				my $flag = "L7flag.$opts{device}";
				foreach $maskline (@raw_data) {
					if ($maskline =~ /$flag/) {
						#print "flag $flag\n";
					} else {
						$outputFh->print("$maskline");
					}
				}
				$outputFh->close();
			} else {
				print "Couldn\'t open $outputFiles{'ETC'}netmasks, unable to set timezone.\n";
			}
			$logFile->print("Set: DHCP device $opts{device}, mask $opts{netmask}, nameservers @{$opts{nameserver}}\n");
			
		} elsif ($opts{bootproto} eq "static") {

		# Static config is a bit of a nightmare, mostly because you need to manually edit /etc/netmasks to
		# correspond with reality; set /etc/hostname.interface, /etc/defaultrouter, /etc/resolv.conf...
		# We blow away /etc/hosts (above) and add suffixed lines for each static interface. This is an
		# acceptable method. Using 127.0.0.1 for nodename... I'm not so sure, but it works :)

			my $Tip = pack "C4", split (/\./, $opts{ip}), shift;
			my $Tmask = pack "C4", split (/\./, $opts{netmask}), shift;
			$opts{network} = join ".", unpack "C4", ($Tip & $Tmask);
			unlink("$outputFiles{'ETC'}dhcp.$opts{device}");

			if ($outputFh->open(">$outputFiles{'ETC'}hostname.$opts{device}")) {
				$outputFh->print("$hostname-$opts{device}\n");
				$outputFh->close();
	                	if ($hostsFH->open(">>$outputFiles{'HOSTS'}")) {
	                	        $hostsFH->print("$opts{ip}\t\t\t$hostname-$opts{device}\n");
	                	        $hostsFH->close();
				} else {
					$logFile->print("$timestamp: couldn't write hosts file for $opts{device}\n");
                		}
			}

			if ($outputFh->open(">$outputFiles{'ETC'}defaultrouter")) {
				$outputFh->print($opts{gateway} . "\n");
				$outputFh->close();
			}

			if ($outputFh->open(">$outputFiles{'ETC'}resolv.conf")) {
				my $ns;
				foreach $ns (@{$opts{nameserver}}) {
					#print "$ns\n";
					$outputFh->print("nameserver $ns\n");
				}
				$outputFh->print("search $hostnameInfo{'domain'}\n");
				$outputFh->close();
			}

			##### Edit /etc/netmasks here.

			#Write out the file with the changed masks
			if ($outputFh->open(">$outputFiles{'ETC'}netmasks")) {
				my $maskline;
				my $flag = "L7flag.$opts{device}";
				foreach $maskline (@raw_data) {
					if ($maskline =~ /$flag/) {
						#print "flag $flag\n";
					} else {
						$outputFh->print("$maskline");
					}
				}
				$outputFh->print("$opts{network} $opts{netmask} #$flag\n");
				$outputFh->close();
			} else {
					print "Couldn\'t open $outputFiles{'ETC'}netmasks, unable to set timezone.\n";
			}

			$logFile->print("Set: Static device $opts{device}, IP $opts{ip}, mask $opts{netmask}, net $opts{network}, gw $opts{gateway}, nameservers @{$opts{nameserver}}\n");
		}
		push (@filesToDelete, $configFile);
		$inputFh->close();
	}

	$logFile->print("$timestamp: executing network configuration command:\n");
}

########################################################################


#Setup ntp.conf
if ($inputFh->open("<$inputFiles{'NTPFILE'}")) {
	my @serverNames = $inputFh->getlines();
	$ntpServerToUse = $serverNames[0];
	chomp($ntpServerToUse);
	$inputFh->close();

	if ($outputFh->open(">$outputFiles{'NTP'}")) {
		$outputFh->print("server $ntpServerToUse\n");
		$outputFh->close();

		push (@filesToDelete, $inputFiles{'NTPFILE'});
		$logFile->print("$timestamp: NTP server found: $ntpServerToUse\n");
	} else {
		$logFile->print("$timestamp: $inputFiles{'NTPFILE'} not writable. The NTP configuration will not be changed.\n");
	}
} else {
	$logFile->print("$timestamp: $inputFiles{'NTPFILE'} not found. The NTP configuration will not be changed.\n");
}

#Setup Timezone (which is actually embedded in a common config file. Sigh)
if($inputFh->open("<$inputFiles{'TZFILE'}")) {
#Get the TZ, only read first line.
	my $TZ = <$inputFh>;
	$inputFh->close();
	chomp($TZ);
	#print "TZ src: $TZ\n\n";

	#Read in the existing init file
	my @raw_data;
	open(DAT, $outputFiles{'TZ'}) || die("Could not open file! Your Solaris Installation is Damaged!\n");
	@raw_data=<DAT>;
	close(DAT);

	#Write out the file with the changed TZ
	if ($outputFh->open(">$outputFiles{'TZ'}")) {
		my $initline;
		foreach $initline (@raw_data) {
			$initline =~ s/TZ=.*/TZ=$TZ/;
			$outputFh->print("$initline");
		}
		$outputFh->close();
	} else {
		print "Couldn't open $outputFiles{'TZ'}, unable to set timezone.\n";
	}
}

unlink(@filesToDelete);
$logFile->close();
