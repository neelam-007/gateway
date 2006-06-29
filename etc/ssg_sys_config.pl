#!/usr/bin/perl -w

use strict;
use FileHandle;

my @commandArray;
my $hostnameToUse = "";
my $ntpServerToUse = "";
my @filesToDelete;

my $netConfigCommand = "/usr/sbin/netconfig";
my $netConfigPattern = "configfiles/netconfig_*";

my $inputFh = new FileHandle;
my %inputFiles = (
    "HOSTNAMEFILE" => "configfiles/hostname",
    "NTPFILE" => "configfiles/ntpconfig"
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
		my $params = " ";
		while(my $line = <$inputFh>) {
            chomp($line);
            $params = $params."$line ";
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
	$inputFh->close();
}

if ($inputFh->open("<$inputFiles{'NTPFILE'}")) {
	my @serverNames = $inputFh->getlines();
	$ntpServerToUse = $serverNames[0];
	chomp($ntpServerToUse);
	push (@filesToDelete, $inputFiles{'NTPFILE'});
	$inputFh->close();
}

#configure the network interfaces
for my $command (@commandArray) {
	`$netConfigCommand $command`;
}

#enable networking and setup hostname
if ($outputFh->open(">$outputFiles{'NETFILE'}")) {
	$outputFh->print("NETWORKING=yes\n");
	$outputFh->print("HOSTNAME=$hostnameToUse\n");
	$outputFh->close();
}

#configure NTP
if ($outputFh->open(">$outputFiles{'NTPCONFFILE'}")) {
	$outputFh->print("server $ntpServerToUse\n");
	$outputFh->print("restrict $ntpServerToUse mask 255.255.255.255 nomodify notrap noquery\n");
	$outputFh->close();
}

if ($outputFh->open(">$outputFiles{'STEPTICKERSFILE'}")) {
    $outputFh->print("$ntpServerToUse\n");
    $outputFh->close();
}

my $backupFilename = "configfiles/sysconfigsettings.tgz";
unlink(@filesToDelete);


