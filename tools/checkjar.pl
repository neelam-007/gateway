#!/usr/bin/perl

require 5.005;
use strict;

my ($jar, $manifest) = @ARGV;
die "Usage: $0 jarfile manifestfile\n" unless $manifest;

my $pathsep = `uname` =~ /cygwin/i ? ';' : ':';

my @stuff = grep {/^Class-Path:\s*/} `cat $manifest`;
my $stuff = shift @stuff;
$stuff =~ s/^Class-Path:\s*|\s+$|^\s+//gi;
$stuff =~ s/\s+/$pathsep/g;

system qq{$ENV{JAVA_HOME}/bin/java -cp "tools$pathsep$stuff$pathsep$jar" com.l7tech.tools.JarChecker $jar};
my $exit_value  = $? >> 8;
die "JarChecker exited nonzero\n" if $exit_value;

exit(0);
