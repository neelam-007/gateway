#!/usr/bin/perl
#
# expertise.pl - annotate tree and collect developer information
#

require 5.005;
use strict;
use File::Find;
use constant DEBUG => 1;

die "Usage: expertise.pl dir\n" unless @ARGV;
find({ wanted => \&process }, shift @ARGV);

my %totals;
my $totals_all;
my %byfile;
my %byfile_all;
my %bydir;
my %bydir_all;

sub process {
    return unless -f;
    return if /CVS/;
    return if $File::Find::dir =~ /CVS/;
    return unless /\.java$|\.pl$/i;
    warn "Annotating $_\n" if DEBUG;
    my @lines = `cvs annotate $_ 2>/dev/null`;
    foreach my $line (@lines) {
        next if $line =~ /^Annotations for /;
        next if $line =~ /^\*\*\*\*\*\*\*\*/;
        my $user = substr((split /\s+/, $line)[1], 1);
        $totals{$user}++;
        $totals_all++;
        $byfile{$File::Find::name}{$user}++;
        $byfile_all{$File::Find::name}++;
        $bydir{$File::Find::dir}{$user}++;
        $bydir_all{$File::Find::dir}++;
        my @comp = split /\//, $File::Find::dir;
        my $sofar = shift(@comp) . "/";
        for my $comp (@comp) {
          $bydir{$sofar}{$user}++;
          $bydir_all{$sofar}++;
          $sofar .= "$comp/";
        }
    }
    return;
}

sub percent {
  my ($a, $b) = @_;
  return 0 if $b == 0;
  my $p = ($a * 100.0) / $b;
  return $p;
}

print "FILE,", join(",", sort keys %totals), "\n";
foreach my $file (sort keys %byfile_all) {
    print "$file";
    my $total = $byfile_all{$file};
    foreach my $user (sort keys %totals) {
        my $lines = $byfile{$file}{$user};
        printf ",%3.2f", percent($lines, $total);
    }
    print "\n";
}
