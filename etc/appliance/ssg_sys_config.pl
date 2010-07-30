#!/usr/bin/perl -w

use strict;
use FileHandle;

my $timestamp = localtime time;
my $logFile = new FileHandle;
if ($logFile->open(">>/opt/SecureSpan/Appliance/config/ssgsysconfig.log")) {
	$logFile->print("\n-------------------------------------------------------------------\n");
	$logFile->print("$timestamp: System Configuration Started\n");
	$logFile->print("-------------------------------------------------------------------\n");
} else {
 	print("Could not open log file. Will not proceed with system configuration\n");
}

my @commandArray;
my @dhcpInterfaces;
my @staticNameservers;

my %hostnameInfo = ();
my %gatewayInfo = ();
my $foundNtp = undef;
my @ntpServerToUse;
my $timezone = undef;
my @filesToDelete;

my $netConfigCommand = "/opt/SecureSpan/Appliance/config/netconfig.pl";
my $netConfigPattern = "/opt/SecureSpan/Appliance/config/configfiles/netconfig_*";

my $inputFh = new FileHandle;
my %inputFiles = (
    "HOSTNAMEFILE" => "/opt/SecureSpan/Appliance/config/configfiles/hostname",
    "NTPFILE" => "/opt/SecureSpan/Appliance/config/configfiles/ntpconfig",
    "TIMEZONEFILE" => "/opt/SecureSpan/Appliance/config/configfiles/timezone",
    "DEFAULTGATEWAYFILE" => "/opt/SecureSpan/Appliance/config/configfiles/default_gateway"
);

my $outputFh = new FileHandle;
my %outputFiles = (
    "NETFILE" => "/etc/sysconfig/network",
    "NTPCONFFILE" => "/etc/ntp.conf",
    "STEPTICKERSFILE" => "/etc/ntp/step-tickers",
    "TZCLOCKFILE" => "/etc/sysconfig/clock",
    "TZSYMLINK" => "/etc/localtime",
    "TZBASEDIR" => "/usr/share/zoneinfo/",
    "RESOLVCONF" => "/etc/resolv.conf"
);

##
## READ ALL THE INPUTS
##

#network configurations
my @netConfigFiles = glob($netConfigPattern);
for my $configFile(@netConfigFiles) {
    if($inputFh->open("<$configFile")) {
        my (undef, $ifName) = split("_", $configFile);
        my $params = " ";
        while(my $line = <$inputFh>) {
            chomp($line);
            if ($line =~ /(.*--nameserver)\=(.*)/i) {
                push(@staticNameservers,$2);
            }

            $params = $params."$line ";
            if ($line =~ /bootproto=dhcp/i) {
                push(@dhcpInterfaces,$ifName);
            }
        }
        push(@commandArray, $params);
        push (@filesToDelete, $configFile);
        $inputFh->close();
    }
}

#hostname
if ($inputFh->open("<$inputFiles{'HOSTNAMEFILE'}")) {
	my $fileContent = do { local( $/ ) ; <$inputFh> } ;
    %hostnameInfo = $fileContent =~ /^(\w+)=(.+)$/mg ;
    push (@filesToDelete, $inputFiles{'HOSTNAMEFILE'});
	my $hostnameToUse = $hostnameInfo{'hostname'};
	if ($hostnameToUse ne "") {
	   $logFile->print("$timestamp: hostname found: $hostnameToUse\n");
	} else {
	   $logFile->print("$timestamp: no hostname found. Hostname will not be set\n");
	   %hostnameInfo = undef;
	}
	$inputFh->close();
} else {
	$logFile->print("$timestamp: $inputFiles{'HOSTNAMEFILE'} not found. The hostname will not be changed.\n");
}

#default gateway
if ($inputFh->open("<$inputFiles{'DEFAULTGATEWAYFILE'}")) {
	my $fileContent = do { local( $/ ) ; <$inputFh> } ;
    %gatewayInfo = $fileContent =~ /^(\w+)=(.+)$/mg ;
    push (@filesToDelete, $inputFiles{'DEFAULTGATEWAYFILE'});
	my $gatewayIp= $gatewayInfo{'gateway'};
	if ($gatewayIp ne "") {
	   $logFile->print("$timestamp: Gateway IP found: $gatewayIp\n");
	} else {
	   $logFile->print("$timestamp: no gateway IP found. Gateway will not be set\n");
	   %gatewayInfo = undef;
	}
	my $gatewayDevice= $gatewayInfo{'gatewaydev'};
	if ($gatewayIp ne "") {
	   $logFile->print("$timestamp: gateway device found: $gatewayDevice\n");
	} else {
	   $logFile->print("$timestamp: no gateway device found. Gateway device will not be set\n");
	   %gatewayInfo = undef;
	}
	$inputFh->close();
} else {
	$logFile->print("$timestamp: $inputFiles{'DEFAULTGATEWAYFILE'} not found. The gateway will not be changed.\n");
}

#timezone
if ($inputFh->open("<$inputFiles{'TIMEZONEFILE'}")) {
    $timezone = do  { local( $/ ) ; <$inputFh> } ;
    chomp($timezone);
    push (@filesToDelete, $inputFiles{'TIMEZONEFILE'});
    if ($timezone ne "") {
        $logFile->print("$timestamp: timezone found: $timezone\n");
    } else {
        $logFile->print("$timestamp: no new timezone found. Timezone will not be changed.\n");
    }
    $inputFh->close();
} else {
	$logFile->print("$timestamp: $inputFiles{'TIMEZONEFILE'} not found. The timezone will not be changed.\n");
}

#ntp servers
if ($inputFh->open("<$inputFiles{'NTPFILE'}")) {
	my @lines = $inputFh->getlines();
	for my $ntpLine(@lines) {
	    chomp($ntpLine);
	    $logFile->print("$timestamp: NTP server(s) found: $ntpLine\n");
	    push(@ntpServerToUse,$ntpLine)
	}
	push (@filesToDelete, $inputFiles{'NTPFILE'});
	$inputFh->close();
} else {
	$logFile->print("$timestamp: $inputFiles{'NTPFILE'} not found. The NTP configuration will not be changed.\n");
}

##
## APPLY THE CONFIGURATION BASED ON THE INPUTS
##

#configure the network interfaces
for my $command (@commandArray) {
	$logFile->print("$timestamp: executing network configuration command:\n");
	$logFile->print("\t$netConfigCommand $command\n");
	`$netConfigCommand $command`;
}

#update the resolv.conf in case this is a static configuration where netconfig doesn't do the right thing

#if we have static nameservers configured, put them in resolv.conf
if (! @dhcpInterfaces) {
	if (@staticNameservers && defined($hostnameInfo{'domain'}) ) {
   	    $logFile->print("$timestamp: Static interfaces are being configured, $outputFiles{'RESOLVCONF'} will be configured\n");		
	    if ($outputFh->open(">$outputFiles{'RESOLVCONF'}")) {
                $outputFh->print("; generated by Layer 7 System Configuration Tool\n");
                $outputFh->print("search $hostnameInfo{'domain'}\n");
                for my $ns (@staticNameservers) {
                   $outputFh->print("nameserver $ns\n");
                }
                $outputFh->close();
	   	$logFile->print("$timestamp: Wrote $outputFiles{'RESOLVCONF'} (DHCP configurations will overwrite this on reboot)\n");
            } else {
                $logFile->print("$timestamp: Couldn't open $outputFiles{'RESOLVCONF'}. Skipping resolv.conf configuration\n");
            }
	}
} else {
	$logFile->print("$timestamp: DHCP interfaces are being configured, skipping $outputFiles{'RESOLVCONF'} configuration\n");
}

#enable networking and setup hostname, default network gateway, and gateway device
if (defined($hostnameInfo{'hostname'}) && $hostnameInfo{'hostname'} ne "") {
	if ($outputFh->open(">$outputFiles{'NETFILE'}")) {
       my ($hostname, $domain) = ($hostnameInfo{'hostname'}, $hostnameInfo{'domain'});
       if (defined($domain) && $domain ne "") {
           $hostname .= ".$domain";
       }

	   $outputFh->print("NETWORKING=yes\n");
	   $outputFh->print("HOSTNAME=$hostname\n");

	   my ($gateway, $gatewaydev) = ($gatewayInfo{'gateway'}, $gatewayInfo{'gatewaydev'});
       $outputFh->print("GATEWAY=$gateway\n") if (defined($gateway) && $gateway ne "");
       $outputFh->print("GATEWAYDEV=$gatewaydev\n") if (defined($gatewaydev) && $gatewaydev ne "");

	   $outputFh->close();

	   system("hostname $hostnameInfo{'hostname'}");

	   $logFile->print("$timestamp: Wrote $outputFiles{'NETFILE'} with:\n");
	   $logFile->print("\tNETWORKING=yes\n");
	   $logFile->print("\tHOSTNAME=$hostname\n") if (defined($hostname));
	   $logFile->print("\tGATEWAY=$gateway\n") if (defined($gateway));
	   $logFile->print("\tGATEWAYDEV=$gatewaydev\n") if (defined($gatewaydev));

       for my $whichInterface(@dhcpInterfaces){
            if ($outputFh->open(">>/etc/sysconfig/network-scripts/ifcfg-$whichInterface")) {
                $outputFh->print("DHCP_HOSTNAME=$hostnameInfo{'hostname'}\n");
                $outputFh->close();
            }
        }

	} else {
		$logFile->print("$timestamp: Couldn't open $outputFiles{'NETFILE'}. Skipping Network configuration\n");
	}
}

#configure the timezone
if (defined($timezone)) {
    my $originalFile = new FileHandle;
    if ($originalFile->open("<$outputFiles{'TZCLOCKFILE'}")) {
        my $originalFileContent = do { local( $/ ); <$originalFile> } ;
        $originalFile->close();
        $originalFileContent =~ s/(ZONE=).*/$1\"$timezone\"/g;

        if ( $outputFh->open(">$outputFiles{'TZCLOCKFILE'}")) {
            $outputFh->print($originalFileContent);
            $outputFh->close();
            unlink($outputFiles{'TZSYMLINK'});
            my $oldFile = $outputFiles{'TZBASEDIR'}.$timezone;
            symlink($oldFile, $outputFiles{'TZSYMLINK'});
        } else {
            $logFile->print("$timestamp: Couldn't open $outputFiles{'TZCLOCKFILE'}. Skipping timezone configuration\n");
        }
    } else {
        $logFile->print("$timestamp: Couldn't open $outputFiles{'TZCLOCKFILE'}. Skipping timezone configuration\n");
    }
}

#configure NTP
if (@ntpServerToUse) {
	if ($outputFh->open(">$outputFiles{'NTPCONFFILE'}")) {
	    #the ntpfile that is output will have the following contents
        #restrict default nomodify notrap noquery
        #
        #restrict 127.0.0.1
        #
	    #server $ntpServerToUse
        #
        #server  127.127.1.0     # local clock
        #fudge   127.127.1.0 stratum 10
        #
        #driftfile /var/lib/ntp/drift

        $outputFh->print("#NTP configuration generated by the Layer 7 System Configuration wizard.\n");
        $outputFh->print("#This file will be overwritten if the utility is re-run\n");
        $outputFh->print("\n");
        $outputFh->print("restrict default nomodify notrap noquery\n");
        $outputFh->print("\n");
        $outputFh->print("restrict 127.0.0.1\n");
        for my $whichNtpHost(@ntpServerToUse){
            $outputFh->print("server $whichNtpHost\n");
        }

        $outputFh->print("server  127.127.1.0     # local clock\n");
        $outputFh->print("fudge   127.127.1.0 stratum 10\n");
        $outputFh->print("driftfile /var/lib/ntp/drift\n");

	    $outputFh->close();

	    $logFile->print("$timestamp: Wrote $outputFiles{'NTPCONFFILE'} with:\n");
	    $logFile->print("\trestrict default nomodify notrap noquery\n");
        $logFile->print("\n");
        $logFile->print("\trestrict 127.0.0.1\n");
        for my $whichNtpHost(@ntpServerToUse){
            $logFile->print("\tserver $whichNtpHost\n");
        }
        $logFile->print("\tserver  127.127.1.0     # local clock\n");
        $logFile->print("\tfudge   127.127.1.0 stratum 10\n");
        $logFile->print("\tdriftfile /var/lib/ntp/drift\n");
	} else {
		$logFile->print("$timestamp: Couldn't open $outputFiles{'NTPCONFFILE'}. Skipping NTP configuration\n");
	}

	if ($outputFh->open(">$outputFiles{'STEPTICKERSFILE'}")) {
    		for my $whichNtpHost(@ntpServerToUse){
                $outputFh->print("$whichNtpHost\n");
            }
    		$outputFh->close();

    		$logFile->print("$timestamp: Wrote $outputFiles{'STEPTICKERSFILE'} with:\n");
    		for my $whichNtpHost(@ntpServerToUse){
                $logFile->print("\t$whichNtpHost\n");
            }
    		$outputFh->close();
	} else {
		$logFile->print("$timestamp: Couldn't open $outputFiles{'STEPTICKERSFILE'}. Skipping step tickers configuration\n");
	}
} else {
		$logFile->print("no ntp found\n");
}

unlink(@filesToDelete);
$logFile->close();

