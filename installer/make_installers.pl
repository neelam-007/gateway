#!/usr/bin/perl

require 5.005;
use strict;

use File::Copy;
use Getopt::Std;

my $MAKENSIS = "c:/NSIS/makensis.exe";
my @BZIP = ("/XSetCompressor bzip2");
my $BUILD_PATH = "../build";

my %nsis = (
  Agent => "proxy/win32/Agent",
  Manager => "console/win32/Manager"
);
my @FILES = keys %nsis;
my %fileversions = ();
my $MANIFEST_PATH = "../etc/";
my $MANIFEST_EXT = ".mf";

my %opt = ();

# Returns a list of jar files from the class path in the manifest.
# Example:  a manifest containing a line 
#    "Class-Path: lib/ant.jar lib/commons-collections.jar lib/commons-dbcp.jar"
#   will return ("ant.jar", "commons-collections.jar", "commons-dbcp.jar")
# Usage: @jarnames = get_jarlist_from_manifest("../etc/whatever.mf");
sub get_jarlist_from_manifest {
	my $file = shift;
	local $_;
	open(MANIFEST, "<$file") or die "unable to read $file: $!";
	my @jars;
	while (<MANIFEST>) {
		next unless /^Class-Path: (.*)/i;
	    my $jarstr = $1;
		$jarstr =~ s{lib/}{}g;  # filter the dot
		@jars = grep {!/^\.$/} split " ", $jarstr;
	}
	close(MANIFEST);
	return @jars;
}

# Usage: nsi_file_replace("proxy/win32/Agent.nsi", \@jarfiles);
sub nsi_file_replace {
	my $file = shift;
	my $nsi = shift;
	my $jars = shift;
	local $_;
	my $new = "${nsi}_new.nsi";
	my $nsi = "${nsi}.nsi";

	open(NSI, "<$nsi") or die "unable to read $nsi: $!";
	open(NEW, ">$new") or die "unable to write $new: $!";
	while (<NSI>) {
		if (/%%%JARFILE_FILE_LINES%%%/) {
			foreach my $jar (@$jars) {
				print NEW "  File \"\$\{BUILD_DIR\}\\lib\\$jar\"\n";
			}
		} elsif (/%%%JARFILE_DELETE_LINES%%%/) {
			foreach my $jar (@$jars) {
				print NEW "  Delete \"\$INSTDIR\\lib\\$jar\"\n";
			}
		} else {
			print NEW $_ or die "unable to write $new: $!";
		}
		if (/^\!define\s+MUI_VERSION\s+\"(.*)\"/) {
			$fileversions{$file} = $1;
		}
	}
	close(NEW) or die "unable to write $new: $!";
	close(NSI);
	return $new;
}

sub dosystem {
	my ($prog, @args) = @_;
	my $ret = system $prog, @args;
	my $exit_value = $ret >> 8;
	die "FATAL: $prog exited $exit_value" if $exit_value;
}

sub build_installer {
	my $nsi = shift;
	my $dir;
	if ($nsi =~ m|(.*?)([^/\\]+\.nsi)$|) {
		$dir = $1;
		$nsi = $2;
	} else {
		die "Can't find NSI directory: $!";
	}
	my $here = `pwd`;
	chomp($here);
	$here ||= '../..';
	print "chdir $dir\n";
	chdir($dir) or die "unable to chdir $dir: $!";

	if ($opt{N}) {
		print "SKIPPING $MAKENSIS $nsi\n";
	} else {
		if ($opt{b}) {
			print "$MAKENSIS ", join(" ", @BZIP), " $nsi\n";
			dosystem $MAKENSIS, @BZIP, $nsi;
		} else {
			print "$MAKENSIS $nsi\n";
			dosystem $MAKENSIS, $nsi;
		}
	}

	print "chdir $here\n";
	chdir($here) or die "unable to chdir back to $here: $!";
}

sub docopy {
	my ($src, $dst) = @_;
	print "Copying $src -> $dst\n";
	copy($src, $dst) or die "Unable to copy $src to $dst: $!";
}

sub create_file {
	my ($path, $contents, $mode) = @_;
	open(NEWFILE, ">$path") or die "Unable to create $path: $!";
	print NEWFILE $contents or die "Unable to write to $path: $!";
	close NEWFILE or die "Unable to write to $path: $!";
	chmod $mode, $path if defined($mode);
}

sub make_tar_file {
	my ($file, $jars) = @_;
	my $version = $fileversions{$file} || "unknownversion";
	my $dir = "${file}-${version}";
	system "rm", "-rf", "./$dir";
	mkdir($dir) or die "Unable to mkdir ./$dir: $!";
	mkdir("./$dir/lib") or die "Unable to mkdir ./$dir/lib: $!";
	docopy("$BUILD_PATH/$file.jar", $dir);
	foreach my $jar (@$jars) {
		docopy("$BUILD_PATH/lib/$jar", "./$dir/lib");
	}
	create_file("./$dir/$file.sh", <<"EOM", 0755);
#!/bin/sh

cd `dirname \$0`
java -jar $file.jar
EOM

	unlink("$dir.tar.gz");
	dosystem "tar", "cvzf", "$dir.tar.gz", $dir;
}

# -b   use bzip2
# -N   skip Nullsoft installer step
getopts('bN', \%opt);

foreach my $file (@FILES) {
	my @jars = get_jarlist_from_manifest("$MANIFEST_PATH/$file$MANIFEST_EXT");

	my $nsi = $nsis{$file};
	die "No NSI path for $file" unless $nsi;
	print "Replacing NSI info for $nsi ...\n";
	my $newnsi = nsi_file_replace($file, $nsi, \@jars);
	print "Building installer for $nsi ...\n";
	build_installer($newnsi);
	make_tar_file($file, \@jars);
}
