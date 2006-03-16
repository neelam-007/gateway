#!/usr/bin/perl
use strict;
# Local config
# number of files
my $MAX_FILES = 5;
# size (250 megs
my $MAX_SIZE = 250*1024*1024;  

my $h=`hostname| cut -d . -f 1`;
my $dir="/var/lib/mysql";
opendir(DIR, $dir) || die "can't opendir $dir: $!";
my @allfiles=readdir DIR;
closedir DIR;

my @binlogfiles= grep { /.*-bin\.0.*/ && ! /-relay-/ && -f "$dir/$_" } @allfiles; 
my @relayfiles = grep { /.*-relay-bin\.0.*/ && -f "$dir/$_" } @allfiles;

my $we_must_rotate=0;
	
# get highest number,

print "Binlog files: ";
print cleanup(\@binlogfiles, $dir);
print "Relay files:  ";
print cleanup(\@relayfiles,  $dir);

if ($we_must_rotate) {
	print rotate_files() . "\n";
}

# check its size. 
sub cleanup {
	my $list=shift;
	my $dir=shift;
	my @files=@$list;
	my @sorted= reverse sort @files;
	my $newest_file = @sorted[0];
	my $s = -s "$dir/$newest_file";
	foreach my $index ( $MAX_FILES..20 ) {
		my $fn = $sorted[$index];
		if ( $fn ne "" && -e "$dir/$fn" ) {
			print "Purge: deleting old binary log file $dir/$fn\n"; 
			unlink "$dir/$fn" || die "can't delete log file $dir/$fn: $!";
		}
	}
	if ( $s > $MAX_SIZE ) {
		# delete all files < thisnum - MAX_FILES 
		# so we have MAX_FILES + 1 in the dir
		print ".. newest File: $newest_file is: $s bytes\n";
		# Then call rotate if size > MAX_SIZE
		$we_must_rotate=1;
		return "File size limit exceeded: Rotating Logfiles\n";
	} else {
		return "Files okay\n";
	}
}

sub rotate_files {
	my $cmd="
         if test -x /usr/bin/mysqladmin && \
           /usr/bin/mysqladmin ping &>/dev/null
         then
           /usr/bin/mysqladmin refresh
         fi
	";
	return `$cmd`;
}

