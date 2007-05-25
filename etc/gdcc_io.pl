#!/usr/bin/perl -w

require 5.005;
use strict;

use constant DEBUG => 1;

use constant OK => 0;
use constant UNKNOWN_ERR => 1;
use constant SUBPROCESS_ERR_LSOF => 2;
use constant PORT_OPEN_READ => 3;
use constant PORT_OPEN_WRITE => 4;
use constant PORT_READ => 5;
use constant PORT_WRITE => 6;
use constant FORK_FAILED => 7;
use constant NO_DEVICE_NODE => 8;
use constant BROKEN_PIPE => 9;
use constant SUBPROCESS_ERR_STTY => 10;

use vars qw/*IN *OUT/;
my $kid = undef;


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
        kill 15, @procs;
        sleep 1;
        kill 9, @procs;
        sleep 1;
    }
}

sub findDev() {
    for (my $i = 0; $i < 99; ++$i) {
        my $dev = "/dev/ttyUSB" . $i;
        return $dev if (-c $dev);
    }
    fatal NO_DEVICE_NODE, "Unable to find a character device in the pattern /dev/ttyUSBN -- adaptor not plugged in?";
}

END {
    close IN;
    close OUT;
    clearStalePortUsers();
    kill 15, $kid if $kid;
}

sub doStty {
    my $device = shift;
    my @args = ('stty', '-F', $device, qw#9600 -parenb -parodd cs8 -hupcl -cstopb cread clocal -crtscts raw#);
    dprint join(" ", @args), "\n";
    $? = system @args;
    checkSubprocess SUBPROCESS_ERR_STTY, "stty";
}

sub doRead {
    close OUT;
    local $| = 1;
    while (<IN>) {
        print STDOUT $_ or fatal BROKEN_PIPE, "Unable to write STDOUT: $!";
    }
}

sub doWrite {
    close IN;
    local $| = 1;
    while (<STDIN>) {
        print OUT $_ or fatal PORT_WRITE, "Unable to write to port: $!";
    }
}


MAIN: {
    clearStalePortUsers();
    my $device = findDev();
    
    doStty($device);

    open IN, "<$device" or fatal PORT_OPEN_READ, "Unable to open $device for reading: $!";
    open OUT, ">$device" or fatal PORT_OPEN_WRITE, "Unable to open $device for writing: $!";

    $kid = fork;
    fatal FORK_FAILED, "fork: $!" unless defined($kid);

    doRead() unless $kid;
    doWrite();
}
