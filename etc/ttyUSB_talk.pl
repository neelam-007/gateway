#!/usr/bin/perl -w

require 5.005;
use strict;
use IO::Handle;

use constant DEBUG => 1;

use constant OK => 0;
use constant UNKNOWN_ERR => 1;
use constant SUBPROCESS_ERR_STTY => 2;
use constant PORT_OPEN_READ => 3;
use constant PORT_OPEN_WRITE => 4;
use constant PORT_READ => 5;
use constant PORT_WRITE => 6;
use constant FORK_FAILED => 7;
use constant NO_DEVICE_NODE => 8;
use constant BROKEN_PIPE => 9;


use vars qw/*IN *OUT/;
my $parent = undef;
my $kid = undef;


sub dprint {
    my $mess = shift;
    print $mess, "\n" if DEBUG;
}

my $fatalExitStatus = undef;
sub fatal($$) {
    my $status = shift;
    my $mess = shift;

    warn $mess . "\n";
    $fatalExitStatus = $status;
    exit $status;
}

sub checkSubprocess($$) {
    my $mess = shift;
    my $procname = shift;
    fatal $mess, "$procname binary was not found" if $? < 0;
    fatal $mess, "$procname exited abnormally with status " . ($? >> 8) if $?;
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
    system 'stty', 'sane';
    kill 9, $kid if $kid;
    kill 9, $parent if $parent;
    exit $fatalExitStatus if $fatalExitStatus;
}

sub doRead {
    close OUT;

    for (;;) {
        my $c = getc IN;
        fatal PORT_READ, "Unable to read port: $!" unless defined($c);
        print STDOUT $c or fatal BROKEN_PIPE, "Unable to write STDOUT: $!";
    }
}

sub doWrite {
    close IN;

    for (;;) {
        my $c = getc STDIN;
        fatal BROKEN_PIPE, "Unable to read STDIN: $!" unless defined($c);
        print OUT $c or fatal PORT_WRITE, "Unable to write to port: $!";
    }
}


sub sttyRawStdin() {
    $? = system "stty", "-icanon", "-echo";
    checkSubprocess SUBPROCESS_ERR_STTY, "stty stdin";
}

MAIN: {
    my $device = findDev();

    sttyRawStdin();

    $parent = $$;
    $SIG{TERM} = sub {
        kill 15, $kid if $kid;
        kill 15, $parent if $parent;
        exit 1;
    };

    open IN, "<$device" or fatal PORT_OPEN_READ, "Unable to open $device for reading: $!";
    open OUT, ">$device" or fatal PORT_OPEN_WRITE, "Unable to open $device for writing: $!";

    STDOUT->autoflush(1);
    OUT->autoflush(1);
    

    $kid = fork;
    fatal FORK_FAILED, "fork: $!" unless defined($kid);

    doRead() unless $kid;
    $parent = undef;
    doWrite();
}

