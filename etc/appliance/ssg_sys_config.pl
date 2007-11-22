#!/usr/bin/perl -w

use strict;
use FileHandle;

my $timestamp = localtime time;
my $logFile = new FileHandle;
if ($logFile->open(">>/ssg/sysconfigwizard/ssgsysconfig.log")) {
	$logFile->print("\n-------------------------------------------------------------------\n");
	$logFile->print("$timestamp: System Configuration Started\n");
	$logFile->print("-------------------------------------------------------------------\n");
} else {
 	print("Could not open log file. Will not proceed with system configuration\n");
}

my @commandArray;
my @dhcpInterfaces;
my %hostnameInfo = ();
my $ntpServerToUse = undef;
my $timezone = undef;
my @filesToDelete;

my $netConfigCommand = "/usr/sbin/netconfig";
my $netConfigPattern = "/ssg/sysconfigwizard/configfiles/netconfig_*";

my $inputFh = new FileHandle;
my %inputFiles = (
    "HOSTNAMEFILE" => "/ssg/sysconfigwizard/configfiles/hostname",
    "NTPFILE" => "/ssg/sysconfigwizard/configfiles/ntpconfig",
    "TIMEZONEFILE" => "/ssg/sysconfigwizard/configfiles/timezone"
);

my $outputFh = new FileHandle;
my %outputFiles = (
    "NETFILE" => "/etc/sysconfig/network",
    "NTPCONFFILE" => "/etc/ntp.conf",
    "STEPTICKERSFILE" => "/etc/ntp/step-tickers",
    "TZCLOCKFILE" => "/etc/sysconfig/clock",
    "TZSYMLINK" => "/etc/localtime",
    "TZBASEDIR" => "/usr/share/zoneinfo/"
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
            $params = $params."$line ";
            if ($line =~ /bootproto=dhcp/) {push(@dhcpInterfaces,$ifName);}
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

#ntp server
if ($inputFh->open("<$inputFiles{'NTPFILE'}")) {
	my @serverNames = $inputFh->getlines();
	$ntpServerToUse = $serverNames[0];
	chomp($ntpServerToUse);
	push (@filesToDelete, $inputFiles{'NTPFILE'});
	$logFile->print("$timestamp: NTP server found: $ntpServerToUse\n");
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

#enable networking and setup hostname
if (defined($hostnameInfo{'hostname'}) && $hostnameInfo{'hostname'} ne "") {
	if ($outputFh->open(">$outputFiles{'NETFILE'}")) {
       my ($hostname, $domain) = ($hostnameInfo{'hostname'}, $hostnameInfo{'domain'});
       if (defined($domain) && $domain ne "") {
           $hostname .= ".$domain";
       }

	   $outputFh->print("NETWORKING=yes\n");
	   $outputFh->print("HOSTNAME=$hostname\n");
	   $outputFh->close();

	   system("hostname $hostnameInfo{'hostname'}");

	   $logFile->print("$timestamp: Wrote $outputFiles{'NETFILE'} with:\n");
	   $logFile->print("\tNETWORKING=yes\n");
	   $logFile->print("\tHOSTNAME=$hostname\n");

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
if (defined($ntpServerToUse)) {
	if ($outputFh->open(">$outputFiles{'NTPCONFFILE'}")) {
	    $outputFh->print("server $ntpServerToUse\n");
	    $outputFh->print("restrict $ntpServerToUse mask 255.255.255.255 nomodify notrap noquery\n");
	    $outputFh->close();

	    $logFile->print("$timestamp: Wrote $outputFiles{'NTPCONFFILE'} with:\n");
	    $logFile->print("\tserver $ntpServerToUse\n");
	    $logFile->print("\trestrict $ntpServerToUse mask 255.255.255.255 nomodify notrap noquery\n");
	} else {
		$logFile->print("$timestamp: Couldn't open $outputFiles{'NTPCONFFILE'}. Skipping NTP configuration\n");
	}

	if ($outputFh->open(">$outputFiles{'STEPTICKERSFILE'}")) {
    		$outputFh->print("$ntpServerToUse\n");
    		$outputFh->close();

    		$logFile->print("$timestamp: Wrote $outputFiles{'STEPTICKERSFILE'} with:\n");
    		$logFile->print("\t$ntpServerToUse\n");
	} else {
		$logFile->print("$timestamp: Couldn't open $outputFiles{'STEPTICKERSFILE'}. Skipping step tickers configuration\n");
	}
}

unlink(@filesToDelete);
$logFile->close();

