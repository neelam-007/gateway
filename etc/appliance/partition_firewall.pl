#!/usr/bin/perl

require 5.8.5;
use strict;

my $startmatch = qr/-A INPUT -i eth0 -p tcp -m tcp --dport 22 -j ACCEPT/;
my $endmatch = qr/--dport 1:65535 -j portdrop/;

sub lock_firewall() {
  my $fh;
  my $lockfile = "/ssg/bin";
  open $fh, "<$lockfile" or die "open $lockfile: $!";
  flock $fh, 2 or die "flock $lockfile: $!";
}

sub load_existing_rules() {
  `/sbin/iptables-save`;
}

sub find_partitions() {
  my @partdirs = glob '/ssg/etc/conf/partitions/*';
  s{.*/}{}g for @partdirs;
  @partdirs;
}

sub slurp($) {
  my $path = shift;
  -f $path and `cat $path`;
}

sub is_partition_active($) {
  my $pid = slurp "/ssg/etc/conf/partitions/$_[0]/ssg.pid";
  0+$pid and kill 0, $pid;
}

sub load_rules_for_partition($) {
  slurp "/ssg/etc/conf/partitions/$_[0]/firewall_rules";
}

sub load_partition_rules() {
  map { load_rules_for_partition($_) } 
    grep { is_partition_active($_) } 
      find_partitions();
}

sub build_new_rules(\@\@) {
  my ($oldrules, $partrules) = ($_[0], $_[1]);

  my ($sawstart, $sawend, $skipping, $inserted, @buildrules);

  for my $oldrule (@$oldrules) {
    if ($oldrule =~ $startmatch) {
      push @buildrules, $oldrule;
      $sawstart = 1;
      $skipping = 1;
    } elsif ($oldrule =~ $endmatch && !$inserted) {
      for my $partrule (@$partrules) {
        if ($partrule =~ /\[0:0\] -I INPUT \$Rule_Insert_Point (.*)/s) {
          push @buildrules, "-A INPUT $1";
        }
      }
      push @buildrules, $oldrule;
      $sawend = 1;
      $inserted = 1;
      $skipping = 0;
    } elsif (!$skipping) {
      push @buildrules, $oldrule;
    }
  }

  # Prevent wackiness if the existing rule format wasn't recognized
  die "Did not find a matching start rule in current iptables config\n" unless $sawstart;
  die "Did not find a matching end rule in current iptables config\n" unless $sawend;

  @buildrules;
}

sub save_new_rules(\@) {
  my $buildrules = shift;
  my $restore;
  open $restore, "|/sbin/iptables-restore" or die "iptables-restore: $!";
  print $restore @$buildrules or die "iptables-restore: $!";
  close $restore or die "iptables-restore: $!";
}

MAIN: {
  lock_firewall();
  my @oldrules = load_existing_rules();
  my @partrules = load_partition_rules();
  my @buildrules = build_new_rules(@oldrules, @partrules);
  save_new_rules(@buildrules);
}
