#!/usr/bin/perl

require 5.005;
use strict;

use constant DEBUG => 1;

use constant OK => 0; 
use constant UNKNOWN_ERR => 1;
use constant SUBPROCESS_ERR_LSOF => 2;
use constant SUBPROCESS_ERR_RMMOD => 3;
use constant SUBPROCESS_ERR_MODPROBE => 4;
use constant NO_DEVICE_NODE => 8;
use constant SUBPROCESS_ERR_STTY => 10;

use constant STTY_SETTINGS => '1:0:cbd:0:3:1c:7f:15:4:5:1:0:11:13:1a:0:12:f:17:16:0:0:0:0:0:0:0:0:0:0:0:0:0:0:0:0';

sub dprint {
    my $mess = shift;
    print $mess, "\n" if DEBUG;
}

sub fatal($$) {
    my $status = shift;
    my $mess = shift;

    warn $mess . "\n";
    exit $status;
}

sub checkSubprocess($$) {
    my $mess = shift;
    my $procname = shift;
    fatal $mess, "$procname binary was not found" if $? < 0;
    fatal $mess, "$procname exited abnormally with status " . ($? >> 8) if $?;
}

sub clearStalePortUsers() {
    my @lsoflines = `lsof -n`;
    checkSubprocess SUBPROCESS_ERR_LSOF, "lsof";
    my @procs = ();
    for (@lsoflines) {
       m#\s/dev/ttyUSB\d+\s*$# && do {
         push @procs, ((split)[1]);
       };
    }
    my $pid = $$;
    @procs = grep {$_ != $pid} @procs;

    if (@procs) {
        warn "killing stale pids: @procs\n";
        kill 9, @procs;
        sleep 2;
    }
}

sub doStty { 
    my $device = shift;
    my @args = ('stty', '-F', $device, STTY_SETTINGS, "raw", "-echo");
    dprint join(" ", @args), "\n"; 
    $? = system @args;
    checkSubprocess SUBPROCESS_ERR_STTY, "stty";
}

sub findDev() {
    for (my $i = 0; $i < 99; ++$i) {
        my $dev = "/dev/ttyUSB" . $i;
        return $dev if (-c $dev); 
    }
    fatal NO_DEVICE_NODE, "Unable to find a character device in the pattern /dev/ttyUSBN -- adaptor not plugged in?";
}   


MAIN: {
    my ($device) = @ARGV;

    die "Usage: $0 device\n\ndevice is the TTY driver device to reset, ie pl2303\n" unless $device;

    clearStalePortUsers();

    $? = system 'rmmod', $device;
    checkSubprocess SUBPROCESS_ERR_RMMOD, "rmmod $device";

    $? = system 'modprobe', $device;
    checkSubprocess SUBPROCESS_ERR_MODPROBE, "modprobe $device";
    
    sleep 2;

    my $ttySpecial = findDev();
    doStty($ttySpecial);

    print $ttySpecial, "\n";
    exit(0);
}

