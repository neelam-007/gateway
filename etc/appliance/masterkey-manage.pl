#!/usr/bin/perl

require 5.005;
use strict;

# Secure the PATH at compile time, before any libraries get included.  sudo is expected to have already sanitized the rest of the environment.
BEGIN { $ENV{'PATH'} = '/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin' };

use IO::File;
use Expect;
use warnings;

use constant OK                    => 0;
use constant ERR_BOARD_IS_UNINIT   => 5;
use constant ERR_BOARD_IS_INIT     => 6;
use constant ERR_WEAK_PASSWORD     => 7;
use constant ERR_KEYSTORE_LOCKED   => 8;
use constant ERR_PASSWORD_TOO_WEAK => 9;
use constant ERR_EXPECT_FAILED     => 10;
use constant ERR_NO_MOUNTPOINT     => 11;
use constant ERR_MOUNT_FAILED      => 12;
use constant ERR_UMOUNT_FAILED     => 13;
use constant ERR_WRITE_USB         => 14;
use constant ERR_SWAPON            => 15;
use constant ERR_SWAPOFF           => 16;
use constant ERR_SCAMGR_SPAWN      => 17;
use constant ERR_CREATING_USER     => 31; 
use constant ERR_CLEANUP_FAILED    => 88;

my $SWAPON='/sbin/swapon';
my $SWAPOFF='/sbin/swapoff';
my $MOUNTPOINT = '/ssg/etc/conf/partitions/default_/var/mnt/usbdrive';
my $USB_DEVICES = '/proc/bus/usb/devices';
my $SWAPFILES = '/proc/swaps';
my $DISK_BY_PATH = '/dev/disk/by-path';
my $SCAMGR = '/opt/sun/sca6000/bin/scamgr';
my $USBDRIVE_PP = '/etc/usbdrive_pp';

my $soUsername = 'so';
my $prompt = qr/scamgr.*> /;

my @onexit;
my $cleanupFailed;

sub doCleanup() {
    while (@onexit) {
        my $task = pop @onexit;
        eval {
            $task->();
        };
        if ($@) {
            $cleanupFailed = 1;
            warn "WARNING: cleanup failed: $@";
        }
    }
    !$cleanupFailed;
}

END {
    my $exitStatus = $?;
    eval { doCleanup() };
    $? = $exitStatus;
}

sub usage() {
    die "Usage: $0 [probe|init|backup|restore|lock] <SOPASSWORD> <BACKUPFILE> <BACKUPPASS>\n";
}

sub fatal($$) {
    my $status = shift;
    my $mess = shift;

    warn $mess . "\n";
    doCleanup();
    exit $status;
}

sub checkSubprocess($$) {
    my $status = shift;
    my $mess = shift;
    fatal $status, "$mess: command not found" if $? < 0;
    fatal $status, "$mess: command exited abnormally with status " . ($? >> 8) if $?;
}

# Silently attempt to slurp contents of the specified file.  Returns file content or undef on error.
sub slurpFile($) {
    my $path = shift;

    my $content;
    my $fh = new IO::File();
    $fh->open("<$path") and do {
        local $/ = undef;
        $content = <$fh>;
        undef $fh;
    };
    return $content;
}

sub findProductNamePrefix() {
 
    my $content = slurpFile($USBDRIVE_PP);

    $content =~ s/^\s+|\s+$//g if defined($content);
    if ($content) {
        print "Using overridden USB device Product name prefix: $content\n";
    } else {
        $content = "EHCI";
        print "Using default USB device Product name prefix: $content\n";
    }
    return $content;
}

sub findBlockDeviceNode($) {
    my $productNamePrefix = shift;

    my @usblines = `cat $USB_DEVICES`;

    die "cat $USB_DEVICES exited nonzero" if $?;
    die "No USB device information found" unless @usblines;

    my $line;
    my $foundProduct = 0;
    while (defined($line = shift @usblines)) {
        next unless $line =~ /^S:\s+Product=$productNamePrefix/;
        $foundProduct = 1;
        last;
    }

    die "Could not find any USB device with Product matching $productNamePrefix\n" unless $foundProduct;

    my $serial;
    while (defined($line = shift @usblines)) {
        next unless $line =~ /^S:\s+SerialNumber=(.*)/;
        $serial = $1;
        last;
    }

    die "Could not find SerialNumber for USB device with Product matching $productNamePrefix\n" unless $serial;

    $serial =~ s/^\s+|\s+$//g;

    my $dh = new IO::File;
    opendir $dh, $DISK_BY_PATH or die "opendir $DISK_BY_PATH: $!\n";
    my @entries = readdir $dh;
    closedir $dh;
    die "No block devices found" unless @entries;

    my @blockdevs = grep { /\-$serial\-usb\-/ } @entries;
    die "No block devices found with path containing -${serial}-usb-\n" unless @blockdevs;

    my @part1 = grep { /\-part1$/ } @blockdevs;
    die "No first partition found among " . join(" or ", @blockdevs) . "\n" unless @part1;
    die "Multiple first partitions found: " . join(" or ", @part1) . "\n"  if scalar(@part1) > 1;
    my $source = $part1[0];

    my $target = readlink "$DISK_BY_PATH/$source";
    die "readlink $DISK_BY_PATH/$target: $!" unless defined $target;

    # Canonicalize
    my $relpath = "$DISK_BY_PATH/$target";
    die "$relpath: not a block device node" unless -b $relpath;
    my $path = `cd -P -- "\$(dirname -- "$relpath")" && printf '%s\n' "\$(pwd -P)/\$(basename -- "$relpath")"`;
    chomp($path);
    die "$path: not a block device node" unless -b $path;

    $path;
}


#
# Mounts the specified block device, ie "/dev/sdc1", and returns the intended path to the master key file on
# the mounted filesystem.
#
sub mountBlockDevice($$) {
    my $blockDev = shift;
    my $backupFile = shift;

    $? = system 'mkdir', '-p', $MOUNTPOINT;
    checkSubprocess ERR_NO_MOUNTPOINT, "Mountpoint directory $MOUNTPOINT doesn't exist and can't be created";

    print "Mounting $blockDev on $MOUNTPOINT\n";
    $? = system 'mount', '-t', 'auto', $blockDev, $MOUNTPOINT;
    checkSubprocess ERR_MOUNT_FAILED, "Unable to mount USB device $blockDev on mountpoint $MOUNTPOINT";

    push(@onexit, sub {
        print "\n\nUnmounting $MOUNTPOINT\n";
        $? = system 'umount', '-l', $MOUNTPOINT;
        checkSubprocess ERR_UMOUNT_FAILED, "umount";
    });

    my $backupPath = "$MOUNTPOINT/$backupFile";

    return $backupPath;
}

sub dialog($@) {
    my $exp = shift;
    my @dlg = @_;

    my $timeout = 5;

    my $want;
    while (defined($want = shift @dlg)) {
        my ($pos, $err);
        if (ref($want) eq 'ARRAY') {
             my @wants = @$want;

             my @newWant = map {
                 my ($re, $reply, $more) = @$_;
                 $more ||= '';
                 [ $re => sub {
                     my $exp = shift;
                     if (ref($reply) eq 'CODE') {
                         $reply->($exp);
                     } else {
                         $exp->send($reply);
                     }
                     exp_continue if $more eq 'repeat';
                 }]
             } @wants;

             ($pos, $err) = $exp->expect($timeout, @newWant);
             fatal ERR_EXPECT_FAILED, "\n\nFailed to match expected result from script. Result: $err\n" if $err;
             next;
        }

        if ($want eq '-timeout') {
            $timeout = shift @dlg;
            next;
        }

        ($pos, $err) = $exp->expect($timeout, [ $want => sub {1} ] );
        fatal ERR_EXPECT_FAILED, "\n\nFailed to match $want: $err\n" if $err;

        my $answer = shift @dlg;
        last unless defined($answer);

        $exp->send("$answer");
    }
}

sub spawnScamgr() {
    my $exp = Expect->spawn($SCAMGR);
    fatal ERR_SCAMGR_SPAWN, "Couldn't exec $SCAMGR: $!" unless $exp;
    push @onexit, sub { 
        print "Closing scamgr process\n";
        $exp->soft_close(); 
    };
    $exp->log_stdout(1);
    return $exp;
}

my @trustTheTrustedKey= (
    [ qr/3. Trust the board for all future sessions.*> /s   => "3\n" => 'repeat' ],
    [ qr/3. Replace the trusted key with the new key.*> /s  => "3\n" => 'repeat' ],
);

sub backupMasterKeyToPath($$$) {
    my $soPass = shift;
    my $backupPath = shift;
    my $backupPass = shift;

    $? = system 'touch', $backupPath;
    checkSubprocess ERR_WRITE_USB, "Unable to touch $backupPath";

    my $exp = spawnScamgr();

    dialog $exp,
        [@trustTheTrustedKey,
	 ["This board is uninitialized."                         => sub { fatal ERR_BOARD_IS_UNINIT, 
                                                                          "Unable to back up key -- board not initialized." } ],
	 ["Security Officer Login: "                             => "$soUsername\n" ]],
	"Security Officer Password: "                            => "$soPass\n",
	$prompt                                                  => "backup $backupPath\n",
        "already exists.  Overwrite it.*: "                      => "y\n",
	"Enter a password to protect the data: "                 => "$backupPass\n",
        [[qr/^Passwords must have / => sub { fatal ERR_WEAK_PASSWORD, "Backup password too weak" } ],
	 ["Confirm password: "                                     => "$backupPass\n"]],
        '-timeout' => 35,
        [["Keystore is locked" => sub { fatal ERR_KEYSTORE_LOCKED, "Keystore is locked" } ],
	 ["Backup to .* successful."                               => "quit\n"]];
}

sub restoreMasterKeyFromPath($$$) {
    my $soPass = shift;
    my $backupPath = shift;
    my $backupPass = shift;
 
    my $exp = spawnScamgr();

    dialog $exp, 
        [@trustTheTrustedKey,
         ["Security Officer Login: "  =>  sub { fatal ERR_BOARD_IS_INIT,
                                                      "Unable to restore key -- board already initialized." } ],
         [qr/2. Initialize the board to use an existing keystore.*> /s => "2\n" ]],
        "Enter the path to the backup file: "                    => "$backupPath\n",
        "Password for restore file: "                            => "$backupPass\n",
        qr/Is this correct.*: /                                  => "y\n",
        '-timeout' => 45,
        qr/The board is ready to be administered./               => "\n";
}

sub initializeKeystore($) { 
    my $soPass = shift; 
  
    my $exp = spawnScamgr(); 
 
    my $keystoreName = 'ssg';
    my $soUsername   = 'so';
    my $userUsername = 'gateway';
    my $userPassword = $soPass;

    dialog $exp,  
        [@trustTheTrustedKey,
         ["Security Officer Login: "                              =>  sub { fatal ERR_BOARD_IS_INIT,
                                                                            "Unable to initialize -- board already initialized." }],
         [qr/1. Initialize the board with a new keystore.*> /s     => "1\n" ]],
        "Keystore Name:"                                          => "$keystoreName\n",
        qr/Run in FIPS 140-2 mode.*: /                            => "y\n",
        "Initial Security Officer Name: "                         => "$soUsername\n",
        "Initial Security Officer Password: "                     => "$soPass\n",
        [["Passwords must (be|have) at least"                     => sub { fatal ERR_PASSWORD_TOO_WEAK, 
                                                                           "New HSM password too weak" }],
         ["Confirm password: "                                    => "$soPass\n"]],
        qr/Board initialization parameters.*Is this correct?.*: /s => "y\n",
        '-timeout' => 120,
        qr/new remote access key has been/;

    # Restart dialog to reset timout to the default short one
    dialog $exp,
        "Security Officer Login: "                                => "$soUsername\n",
        "Security Officer Password: "                             => "$soPass\n",
        $prompt                                                   => "create user $userUsername\n",
        "Enter new user password: "                               => "$userPassword\n",
        "Confirm password: "                                      => "$userPassword\n",
        [["Failed creating user "                                 => sub { fatal ERR_CREATING_USER,
                                                                           "Unable to create new HSM user $userUsername" }],
         [qr/User .* created successfully.*> /s                   => "quit\n" ]];
} 

sub lockKeystore($) {
    my $soPass = shift;

    my $exp = spawnScamgr();

    dialog $exp,
        [@trustTheTrustedKey,
	 ["This board is uninitialized."                         => sub { fatal ERR_BOARD_IS_UNINIT, 
                                                                          "Unable to lock keystore -- board not initialized." } ],
	 ["Security Officer Login: "                             => "$soUsername\n" ]],
	"Security Officer Password: "                            => "$soPass\n",
	$prompt                                                  => "set lock\n",
        qr/Do you wish to lock the master key.*: /               => "y\n",
        qr/The master key is now locked.*> /s                    => "quit\n";
}

sub swapoff {
    my @swapfiles = map { /(^\S*)/ ? $1 : $_  } grep {!/^Filename\s/} `cat $SWAPFILES`;
    if (@swapfiles) {
        print "Disabling swapfiles: ", join(" ", @swapfiles), "\n";
        $? = system $SWAPOFF, @swapfiles;
        checkSubprocess ERR_SWAPOFF, "swapoff";
    }
    \@swapfiles;
}

sub swapon($) {
    my $swaps = shift;
    return unless scalar(@$swaps) > 0;    
    print "Reenabling swapfiles: ", join(" ", @$swaps), "\n";
    $? = system $SWAPON, @$swaps;
    checkSubprocess ERR_SWAPON, "swapon";
}

MAIN: {
    my ($command, $soPass, $backupFile, $backupPass) = @ARGV;

    $SIG{INT} = sub {
        doCleanup();
        warn "Canceled\n";
        exit 99;
    };

    usage() unless $command;

    my $blockDev;
    my $needBlockDev;
    my $needSwapoff;
    my $commandProc;
    if ($command eq 'probe') {
        $needBlockDev = 1;
        $commandProc = sub {
            if ($blockDev) {
                print "USB drive appears to be installed as $blockDev\n";
                exit 0;
            }
        };
    } elsif ($command eq 'init') {
        usage() unless $soPass;
        $commandProc = sub {
            initializeKeystore($soPass);
        };
    } elsif ($command eq 'backup') {
        usage() unless $backupPass;
        $needBlockDev = 1;
        $needSwapoff = 1;
        $commandProc = sub {
            my $backupPath = mountBlockDevice($blockDev, $backupFile);
            backupMasterKeyToPath($soPass, $backupPath, $backupPass) 
        };
    } elsif ($command eq 'restore') {
        usage() unless $backupPass;
        $needBlockDev = 1;
        $needSwapoff = 1;
        $commandProc = sub { 
            my $backupPath = mountBlockDevice($blockDev, $backupFile);
            restoreMasterKeyFromPath($soPass, $backupPath, $backupPass) 
        };
    } elsif ($command eq 'lock') {
        usage() unless ($soPass);
        $commandProc = sub {
            lockKeystore($soPass); 
        }
    } else {
        usage();
    }

    if ($needBlockDev) {
        $blockDev = findBlockDeviceNode(findProductNamePrefix());
    }

    if ($needSwapoff) {
        my $swaps = swapoff();
        push(@onexit, sub {
            swapon($swaps);
        });
    }

    $commandProc->();

    doCleanup() or fatal ERR_CLEANUP_FAILED, "Cleanup failed\n";
 
    print "\n\nSuccess.\n";
    exit 0;
}

