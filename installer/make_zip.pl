#!/usr/bin/perl

#
# make_zip.pl -- create a zip file containing a .jar, it's libs, and a startup script.
#                Currently this is only used for the LicenseGenerator.
#                Everything else uses make_installer.pl instead, which does its own zipping.
#

require 5.005;
use strict;

use File::Copy;
use Getopt::Std;

my $BUILD_PATH = "../build";

my %fileversions = ();
my $MANIFEST_PATH = "../etc/";
my $MANIFEST_EXT = ".mf";
my $INCLUDES_EXT = ".installer.include";

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

# Returns a list of filess listed in the file. Each row represents a file name.
sub get_include_filelist {
	my $file = shift;
	my $ln;
	my @includes = ();

	open(INCLUDES, "<$file") or do {
      print STDOUT "Can't open $file: $!\n";
      return @includes; 
  };

	while (<INCLUDES>) {
    chomp;
    next if /^\s*$/;
    next if /^#/;
    s/\s//g;
    push @includes, $_ if $_;
	}
	close(INCLUDES);
	return @includes;
}


sub dosystem {
	my ($prog, @args) = @_;
	my $ret = system $prog, @args;
	my $exit_value = $ret >> 8;
	die "FATAL: $prog exited $exit_value" if $exit_value;
}

sub docopy {

        my $platform = `uname` =~ /cygwin/i ? 'cygwin' : 'noncygwin';
        my ($src, $dst) = @_;

	if (1 || $platform eq 'noncygwin') {   # XXX lyonsm: Disabling rsync hack just for me since it doesn't work on my machine
        	print "Copying $src -> $dst\n"; 
        	copy($src, $dst) or die "Unable to copy $src to $dst: $!";
	} else {
        	# use rsync instead of copy to be flexible to perform cross platform build - rsync provides modify-window options to work around NT and FAT don't store time to 1 second precision, making it it believe a source file (on Windows) and a destination file (on UNIX) had the same date 
        	print "Rsync'ing(copying) $src -> $dst\n"; 
        	system("rsync --modify-window=2 $src $dst") or die "Unable to rsync $src to $dst: $!"; 
	}
}

sub create_file {
	my ($path, $contents, $mode) = @_;
	open(NEWFILE, ">$path") or die "Unable to create $path: $!";
	print NEWFILE $contents or die "Unable to write to $path: $!";
	close NEWFILE or die "Unable to write to $path: $!";
	chmod $mode, $path if defined($mode);
}

sub make_tar_file {
	my ($file, $jars, $includes) = @_;

	my $version = $fileversions{$file} || "HEAD";
	my $dir = "${file}-${version}";
	system "rm", "-rf", "./$dir";
	mkdir($dir) or die "Unable to mkdir ./$dir: $!";
	mkdir("./$dir/lib") or die "Unable to mkdir ./$dir/lib: $!";
	docopy("$BUILD_PATH/$file.jar", $dir);

	foreach my $jar (@$jars) {
		docopy("$BUILD_PATH/lib/$jar", "./$dir/lib");
	}

	foreach my $include (@$includes) {
		docopy("$BUILD_PATH/$include", "./$dir");
	}

        # default options for both manager and bridge.
	my $start_options=<<EOF;
extra="-server -Dcom.l7tech.proxy.listener.maxthreads=300  -Dsun.net.inetaddr.ttl=10 -Dnetworkaddress.cache.ttl=10 -Dfile.encoding=UTF-8"

EOF
	if ($file eq "Bridge") {
	    # if we're making a bridge.sh then we need to make a section to allow
	    # "Bridge.sh -bd" to start the bridge with no gui 
	    # un under daemon mode if invoked as Bridge.sh -bd
	    $start_options.=<<EOF
if [ "\$1" = "-bd" ]; then
	run="-classpath $file.jar com.l7tech.proxy.Main"
else
	run="-jar $file.jar"
fi

EOF
	} else {
	    # if not, we're doing a Manager.sh file.
	    $start_options.=<<EOM;
run="-jar $file.jar";

EOM
        }
	create_file("./$dir/$file.sh", <<"EOM", 0755);
#!/bin/sh
# $file Startup script for *nix systems

if [ `expr "\$JAVA_OPTS" : ".*headless.*"` != 0 ]; then
	# We look in \$JAVA_OPTS ... if we've done java.awt.headless mode 
        # then we've likely got the default options for SSG and it would prevent a gui
        # from coming up. So we over-write them with the following
	JAVA_OPTS=" -Xms96M -Xmx96M -Xss256k -server -XX:NewSize=48M -XX:MaxNewSize=48M ";
fi

# if we don't have an L7_OPTS and we DO have a java opts, 
# e.g. from above code or from the user's environment
# we set the l7opts to be equal to java opts

if [ "\$L7_OPTS" = "" -a "\$JAVA_OPTS" != "" ]; then
	L7_OPTS=\$JAVA_OPTS
fi

# set current dir to where this script is

cd `dirname \$0`

# include startup options 
$start_options

\$JAVA_HOME/bin/java \$L7_OPTS \$extra \$run
EOM

	unlink("$dir.tar.gz");
	dosystem "tar", "cvzf", "$dir.tar.gz", $dir;
}

# -b   use bzip2
# -N   skip Nullsoft installer step
getopts('bN', \%opt);

my $file = shift;
die "Usage: $0 filename" unless $file;

my @jars = get_jarlist_from_manifest("$MANIFEST_PATH/$file$MANIFEST_EXT");
my @includes = get_include_filelist("$MANIFEST_PATH/$file$INCLUDES_EXT");
make_tar_file($file, \@jars, \@includes);
