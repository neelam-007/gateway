#!/usr/bin/perl -w

use strict;
require 5.005;
use Getopt::Std;
use DBI;
my $Dbh;
my $Dsn;
my $Host;
my $Database;
my $Username;
my $Password;
my $Debug;
my %opts;

# FIXME: 7 days hardcoded lifetime;
getopt('fd', \%opts);

my $days = $opts{"d"} ||= "7";
my $file = $opts{"f"};

if (! $file ) {
	print <<EOF;
Usage: $0 -f hibernate_properties [-d days]
default is 7 days
EOF
	exit;
}

readproperties($file,\%opts);


# -f hibernate.properties -d days 

$Username = $opts{"u"};
$Password = $opts{"p"};

defined $Username or die "No username specified";
defined $Password or die "No default password";


$Dbh = DBI->connect($opts{"DSN"}, $Username, $Password)
or die "Couldn't get DB connection: " . DBI->errstr;


my $dt= (time - ($days * 24 * 60 * 60) ) * 1000 ;

# specify database and table name in the sql statement 
my $sql="DELETE FROM ssg.ssg_logs WHERE millis < $dt" ;

# connect , or reuse $dbh, prepare and execute

my $sth = $Dbh->prepare($sql);
	
my $r = $sth->execute or die "Query: $sql :: execute failed: $DBI::errstr";

print "$sql\nResult: $r\n"; 


sub readproperties {
	my $file=shift;
	my $opts=shift;
	my %temp;
	open (CONFIG, $file) or die "File $file not found\n";
	while (<CONFIG>) {
		if ( m/^hibernate\.connection\.(.*) = (.*)/ ) {
			$temp{$1}=$2;	
		}	
	}
	close CONFIG;
	$opts->{"p"} = $temp{"password"};
	$opts->{"u"} = $temp{"username"};
	if ( $temp{"url"} &&
		 $temp{"url"} =~ m/jdbc:(.*):\/\/(.*)\/(.*)/ ) {
		my $dbtype   =$1;
		my $Host     =$2;
		my $Database =$3;
		$opts->{"DSN"} = "DBI:$dbtype:$Database;host=$Host",
	} else {
		die "Error: DB url not found: $temp{url}\n";
	}


}
