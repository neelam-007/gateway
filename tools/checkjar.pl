#!/usr/bin/perl

require 5.005;
use strict;

my ($jar, $manifest) = @ARGV;
die "Usage: $0 jarfile manifestfile\n" unless $manifest;

my $JAVA_HOME = $ENV{JAVA_HOME};

unless ($JAVA_HOME) {
	$JAVA_HOME = "c:/j2sdk1.4.2";
	warn "WARNING: Guessing JAVA_HOME=$JAVA_HOME\n";
} else {
	$JAVA_HOME =~ tr#\\#/#;
	warn "Using JAVA_HOME=$JAVA_HOME\n";
}

my $pathsep = `uname` =~ /cygwin/i ? ';' : ':';

my @stuff = grep {/^Class-Path:\s*/} `cat $manifest`;
my $stuff = shift @stuff;
$stuff =~ s/^Class-Path:\s*|\s+$|^\s+//gi;
$stuff =~ s/\s+/$pathsep/g;

my $cmd = qq{$JAVA_HOME/bin/java -cp "tools$pathsep$stuff$pathsep$jar" -Djava.awt.headless=true com.l7tech.tools.JarChecker $jar};
warn "Running command: $cmd\n";

system $cmd;
my $exit_value  = $? >> 8;
die "JarChecker exited nonzero\n" if $exit_value;

exit(0);
