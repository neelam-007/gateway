#!/usr/bin/perl -w

use strict;

sub checkForNewFailedTests {
	open(FH, "results/new_failures.txt") or return -1;

	my $inHeader = 0;
	my $section = 'none';
	my $foundFailures = 0;
	while(my $line = <FH>) {
		if($line =~ /^-+\s*$/) {
			if($inHeader == 0) {
				$inHeader = 1;
			} else {
				$inHeader = 0;
			}
			next;
		}

		if($inHeader == 1) {
			if($line =~ /^\s*New Failures\s*$/) {
				$section = 'new failures';
			}

			next;
		} elsif($section eq 'new failures') {
			if($line =~ /^\S+\(\)\s*$/) {
				$foundFailures = 1;
				last;
			}
		}
	}

	close(FH);

	return $foundFailures;
}

sub loadAverageRequestTime {
	my $file = shift @_;

	open(FH, $file) or return -1;
	my $avgRequestTime = 0;
	while(my $line = <FH>) {
		if($line =~ /Time per request:\s+([0-9]+\.[0-9]+) \[([a-zA-Z])\] \(mean\)\s*$/) {
			if($2 eq 's') {
				$avgRequestTime = $1 * 1000;
			} else {
				$avgRequestTime = $1;
			}
			last;
		}
	}
	close(FH);

	return $avgRequestTime;
}

sub updateResolutionPerformance {
	my $uriAverage = loadAverageRequestTime("results/uri_resolution.txt");
	if($uriAverage == -1) {
		return -1;
	}
	my $soapActionAverage = loadAverageRequestTime("results/soap_action_resolution.txt");
	if($soapActionAverage == -1) {
		return -1;
	}
	my $namespaceAverage = loadAverageRequestTime("results/namespace_resolution.txt");
	if($namespaceAverage == -1) {
		return -1;
	}

	my(@uriValues) = ();
	my(@soapActionValues) = ();
	my(@namespaceValues) = ();
	my $uriMean = 0;
	my $soapActionMean = 0;
	my $namespaceMean = 0;
	if(-e "previous_resolution_times.txt") {
		open(FH, "previous_resolution_times.txt") or return -1;
		while(my $line = <FH>) {
			if($line =~ /^([0-9]+\.[0-9]*)\s+([0-9]\.[0-9]*)\s+([0-9]\.[0-9]*)\s*$/) {
				push(@uriValues, $1);
				$uriMean += $1;
				push(@soapActionValues, $2);
				$soapActionMean += $2;
				push(@namespaceValues, $3);
				$namespaceMean += $3;
			}
		}
		close(FH);
	}

	# Add the new values to the resolution times file
	open(FH, ">>", "previous_resolution_times.txt") or return -1;
	print FH "$uriAverage $soapActionAverage $namespaceAverage\n";
	close(FH);

	push(@uriValues, $uriAverage);
	$uriMean = ($uriMean + $uriAverage) / scalar @uriValues;
	push(@soapActionValues, $soapActionAverage);
	$soapActionMean = ($soapActionMean + $soapActionAverage) / scalar @soapActionValues;
	push(@namespaceValues, $namespaceAverage);
	$namespaceMean = ($namespaceMean + $namespaceAverage) / scalar @namespaceValues;

	# Calculate standard deviation
	my $uriVariance = 0;
	my $soapActionVariance = 0;
	my $namespaceVariance = 0;
	for(my $i = 0;$i <= $#uriValues;$i++) {
		$uriVariance += ($uriValues[$i] - $uriMean) * ($uriValues[$i] - $uriMean);
		$soapActionVariance += ($soapActionValues[$i] - $soapActionMean) * ($soapActionValues[$i] - $soapActionMean);
		$namespaceVariance += ($namespaceValues[$i] - $namespaceMean) * ($namespaceValues[$i] - $namespaceMean);
	}
	$uriVariance = $uriVariance / scalar @uriValues;
	$soapActionVariance = $soapActionVariance / scalar @soapActionValues;
	$namespaceVariance = $namespaceVariance / scalar @namespaceValues;

	my $uriSD = sqrt($uriVariance);
	my $soapActionSD = sqrt($soapActionVariance);
	my $namespaceSD = sqrt($namespaceVariance);

	if($uriAverage < $uriMean - $uriSD || $uriAverage > $uriMean + $uriSD ||
	   $soapActionAverage < $soapActionMean - $soapActionSD || $soapActionAverage > $soapActionMean + $soapActionSD ||
	   $namespaceAverage < $namespaceMean - $namespaceSD || $namespaceAverage > $namespaceMean + $namespaceSD)
	{
		return 1;
	} else {
		return 0;
	}
}

my $retVal = checkForNewFailedTests();
if($retVal == 0) {
	$retVal = updateResolutionPerformance();
	exit $retVal;
} else {
	exit $retVal;
}

