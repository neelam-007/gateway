#!/usr/bin/perl -w

use strict;
use FileHandle;

my @commandArray;
my $hostnameToUse;
my $ntpServerToUse;
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
	chomp($hostnameToUse = $hostnames[0]);
	push (@filesToDelete, $inputFiles{'HOSTNAMEFILE'});
	$inputFh->close();
}

if ($inputFh->open("<$inputFiles{'NTPFILE'}")) {
	my @serverNames = $inputFh->getlines();
	chomp($ntpServerToUse = $serverNames[0]);
	push (@filesToDelete, $inputFiles{'NTPFILE'});
	$inputFh->close();
}

#configure the network interfaces
for my $command (@commandArray) {
	`$netConfigCommand $command`;
}

#enable networking and setup hostname
if ($outputFh->open(">$outputFiles{'NETFILE'}")) {
	print $outputFh "NETWORKING=yes\n";
	print $outputFh "HOSTNAME=$hostnameToUse\n";
	push (@filesToDelete, $outputFiles{'NETFILE'});
	$outputFh->close();
}

#configure NTP
if ($outputFh->open(">$outputFiles{'NTPCONFFILE'}")) {
	print $outputFh "server $ntpServerToUse\n";
	print $outputFh "restrict $ntpServerToUse mask 255.255.255.255 nomodify notrap noquery\n";
	push (@filesToDelete, $outputFiles{'NTPCONFFILE'});
	$outputFh->close();
}

if ($outputFh->open(">$outputFiles{'STEPTICKERSFILE'}")) {
    print $outputFh "$ntpServerToUse\n";
    push (@filesToDelete, $outputFiles{'STEPTICKERSFILE'});
    $outputFh->close();
}

my $backupFilename = "configfiles/sysconfigsettings.tgz";
system("tar cfz $backupFilename @filesToDelete");
unlink(@filesToDelete);


