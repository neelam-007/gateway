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
my $hostnameToUse = undef;
my $ntpServerToUse = undef;
my @filesToDelete;

my $netConfigCommand = "/usr/sbin/netconfig";
my $netConfigPattern = "/ssg/sysconfigwizard/configfiles/netconfig_*";

my $inputFh = new FileHandle;
my %inputFiles = (
    "HOSTNAMEFILE" => "/ssg/sysconfigwizard/configfiles/hostname",
    "NTPFILE" => "/ssg/sysconfigwizard/configfiles/ntpconfig"
);

my $outputFh = new FileHandle;
my %outputFiles = (
    "NETFILE" => "/etc/sysconfig/network",
    "NTPCONFFILE" => "/etc/ntp.conf",
    "STEPTICKERSFILE" => "/etc/ntp/step-tickers"
);


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

if ($inputFh->open("<$inputFiles{'HOSTNAMEFILE'}")) {
	my @hostnames = $inputFh->getlines();
	$hostnameToUse = $hostnames[0];
	chomp($hostnameToUse);
	push (@filesToDelete, $inputFiles{'HOSTNAMEFILE'});
	$logFile->print("$timestamp: hostname found: $hostnameToUse\n");
	$inputFh->close();
} else {
	$logFile->print("$timestamp: $inputFiles{'HOSTNAMEFILE'} not found. The hostname will not be changed.\n");
}

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

#configure the network interfaces
for my $command (@commandArray) {
	$logFile->print("$timestamp: executing network configuration command:\n");
	$logFile->print("\t$netConfigCommand $command\n");
	`$netConfigCommand $command`;
}

#enable networking and setup hostname
if (defined($hostnameToUse)) {
	if ($outputFh->open(">$outputFiles{'NETFILE'}")) {
	   $outputFh->print("NETWORKING=yes\n");
	   $outputFh->print("HOSTNAME=$hostnameToUse\n");
	   $outputFh->close();

	   system("hostname $hostnameToUse");

	   $logFile->print("$timestamp: Wrote $outputFiles{'NETFILE'} with:\n");
	   $logFile->print("\tNETWORKING=yes\n");
	   $logFile->print("\tHOSTNAME=$hostnameToUse\n");

        for my $whichInterface(@dhcpInterfaces){
            if ($outputFh->open(">>/etc/sysconfig/network-scripts/ifcfg-$whichInterface")) {
                $outputFh->print("DHCP_HOSTNAME=$hostnameToUse\n");
                $outputFh->close();
            }
        }

	} else {
		$logFile->print("$timestamp: Couldn't open $outputFiles{'NETFILE'}. Skipping Network configuration\n");
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
		$logFile->print("$timestamp: Couldn't open $outputFiles{'NTPCONFGILE'}. Skipping NTP configuration\n");
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

