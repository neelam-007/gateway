#!/usr/bin/perl


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
		#$jarstr =~ s{lib/}{}g;  # filter the dot
		@jars = grep {!/^\.$/} split " ", $jarstr;
	}
	close(MANIFEST);
	return @jars;
}

sub zkm_file_replace {
	my $file = shift;
	my $zkm = ${file};
	my $new = shift;
	my $jars = shift;
	local $_;

	$zkm = "${zkm}";

	open(ZKM, "<$zkm") or die "unable to read $zkm: $!";
	open(NEW, ">$new") or die "unable to write $new: $!";
	while (<ZKM>) {
		if (/%%%JARFILE_FILE_LINES%%%/) {
		    print NEW "classpath";
			foreach my $jar (@$jars) {
				print NEW "     \"build/$jar\"\n" or die "unable to write $new: $!";
			}
			print NEW ";";
		} else {
			print NEW $_ or die "unable to write $new: $!";
		}
		if (/^\!define\s+MUI_VERSION\s+\"(.*)\"/) {
			$fileversions{$file} = $1;
		}
	}
	close(NEW) or die "unable to write $new: $!";
	close(ZKM);
	return $new;
}

my @jarsbridge = get_jarlist_from_manifest("etc/Bridge.mf");
zkm_file_replace("etc/obfuscation/bridge.zkm", "etc/obfuscation/bridge_new.zkm", \@jarsbridge);

my @jarsmanager = get_jarlist_from_manifest("etc/Manager.mf");
zkm_file_replace("etc/obfuscation/manager.zkm", "etc/obfuscation/manager_new.zkm", \@jarsmanager);
