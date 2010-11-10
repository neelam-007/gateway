#!/usr/bin/perl -T

require 5.8.0;
use strict;
use warnings;

BEGIN {
    # Secure the PATH at compile time, before any libraries get included.  sudo is expected to have already sanitized the rest of the environment.
    $ENV{PATH} = '/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin';

    # Clean env just in case sudo didn't do it
    delete @ENV{qw(IFS CDPATH ENV BASH_ENV PERLLIB PERL5LIB)};

    # Turn all warnings into fatal errors
    $SIG{__WARN__} = sub { die $_[0] };
};


# Include modules after the env has been secured
use English qw( -no_match_vars );
use Fatal qw( open close chdir );
use User::pwent;
use User::grent;
use IO::File;
use MIME::Base64;
use Cwd;

use constant KMDATA => '/opt/nfast/kmdata/local';
use constant KMDATA_WORLD => (KMDATA . '/world');
use constant NODE_PROPERTIES => '/opt/SecureSpan/Gateway/node/default/etc/conf/node.properties';
use constant NCIPHERDEFS => '/opt/SecureSpan/Gateway/runtime/etc/profile.d/ncipherdefs.sh';
use constant NCIPHERDEFS_CONTENTS => <<'EOM';
# Generated file; do not edit, as it may be deleted or regenerated by ncipherconfig
SSG_JAVA_OPTS="$SSG_JAVA_OPTS -Dcom.l7tech.common.security.jceProviderEngineName=ncipher ";
EOM

# already running as layer7, so no sudo needed for ssgconfig_launch actions
my @STOP_GATEWAY = qw(/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -lifecycle stop);
my $LOAD_WORLD_B64 = q[/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -keystoreProperty get 4 worldb64];
my @SAVE_WORLD_B64 = qw(/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -keystoreProperty set 4 worldb64);
my @CLEAR_WORLD_B64 = qw(/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -keystoreProperty clear 4 worldb64);
my @CLEAR_WORLD_DATABYTES = qw(/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -keystoreProperty clear 4 databytes);

# we will check that layer7 is in the nfast group, so no sudo needed for nfast commands
my @NOPCLEARFAIL = qw(/opt/nfast/bin/nopclearfail ca);
my @NEWWORLD = qw(/opt/nfast/bin/new-world -m 1 -s 0 -Q 2/3 -k rijndael);
my @PROGRAMWORLD = qw(/opt/nfast/bin/new-world --program --module=1);
my $CHECK_MODE = q[/opt/nfast/bin/enquiry -m 1 | grep "^ mode" | awk '{print $2}'];

sub stopGateway() {
    { local $/;  print "Stopping Gateway... "; }
    system(@STOP_GATEWAY) == 0
        or die "Failed to stop Gateway: $!\n";
    print "Done\n";
}


sub checkUid() {
    die "This program must not be run as root.  Run as layer7 instead.\n"
        if $REAL_USER_ID == 0 || $EFFECTIVE_USER_ID == 0;

    my $user_layer7 = getpwnam('layer7')
        or die "There is no layer7 user on this system.\n";

    die "This program must be run as the layer7 user.\n"
        if $REAL_USER_ID != $user_layer7->uid;

    my $group_nfast = getgrnam('nfast')
        or die "There is no nfast group on this system.\n";

    die "The layer7 user must be a member of the nfast group.\n"
        unless grep { $_ == $group_nfast->gid } split / /, $EFFECTIVE_GROUP_ID;

    die "The directory " . KMDATA . " must exist.\n"
        unless -d KMDATA;

    die "The directory " . KMDATA . " must be readable by the nfast group.\n"
        unless -x _ && -r _;

    die "The directory " . KMDATA . " must be writable by the nfast group.\n"
        unless -w _;

    chdir KMDATA;

    die "The Gateway node is not yet configured.\n"
        unless -f NODE_PROPERTIES;
}

sub prompt() {
    print 'Please make a selection (X to exit): ';
    my $in = <STDIN>;
    chomp $in;
    $in;
}

sub pressEnter() {
    print "Press enter to continue: ";
    <STDIN>;
}

sub pressEnterOrCancel() {
    print "Press enter to continue, or X to cancel: ";
    my $in = <STDIN>;
    chomp $in;
    if ( uc($in) eq 'X' ) {
        return undef;
    } elsif ( $in eq '' ) {
        return 1;
    } else {
        print "Invalid selection.\n";
        return undef;
    }
}

sub proceedOrCancel() {
    print "Please type 'proceed' to continue, or anything else to cancel: ";
    my $in = <STDIN>;
    chomp $in;
    if ( lc($in) eq 'proceed' ) {
       return 1;
    }
    return undef;
}


# Caller must do NOPCLEARFAIL first
sub getCurrentModuleMode() {
    my $mode = `$CHECK_MODE`;
    chomp($mode);
    die "Failed to check module mode: $?" if $?;
    return $mode;
}


# Load the world information from the database.
# Returns the binary content of the "world" file from the database, or undef.
sub loadDatabaseWorld() {
    my $worldb64 = `$LOAD_WORLD_B64 2>&1`;
    my $result = $?;
    my $status = $? >> 8;
    chomp($worldb64);
    # Status of 11 just means "property not found"
    return undef if $status == 11;
    die "Failed to load world data: $?: $worldb64\n" if $result;

    if ( length($worldb64) > 0 ) {
        return decode_base64($worldb64);
    }
    return undef;
}


sub saveDatabaseWorld($) {
    my $world = shift;
    die "world required" unless $world;
    my $worldb64 = encode_base64($world, '');

    my @cmd = (@SAVE_WORLD_B64, $worldb64);
    system(@cmd) == 0
        or die "Failed to save world data to database: $?\n";
}


sub deleteDatabaseWorld() {
    system(@CLEAR_WORLD_B64) == 0
        or die "Failed to clear world data in database (worldb64): $?\n";
    system(@CLEAR_WORLD_DATABYTES) == 0
        or die "Failed to clear world data in database (databytes): $?\n";
}


sub hasDatabaseWorld() {
    my $world = loadDatabaseWorld();
    return defined($world) && length($world) > 0;
}


sub hasLocalWorld() {
    return -f KMDATA_WORLD;
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


sub putFile($$) {
    my $path = shift;
    my $content = shift;

    my $fh = new IO::File();
    $fh->open(">$path") and do {
        print $fh $content;
        undef $fh;
        return 1;
    };
    return undef;
}


sub copyLocalWorldToDatabase() {
    my $world = slurpFile(KMDATA_WORLD);
    die "No local world file found\n" unless defined($world);
    die "Local world file is empty\n" if length($world) < 1;
    saveDatabaseWorld($world);
}


sub copyDatabaseWorldToLocal() {
    my $world = loadDatabaseWorld();
    die "No world information present in database\n" unless defined($world) && length($world) > 0;
    putFile(KMDATA_WORLD, $world) or die "Failed to save local world file: $!\n";
}


sub deleteLocalWorld() {
    chdir KMDATA;
    getcwd eq KMDATA or die "Failed to chdir to " . KMDATA . "\n";
    system "rm -f *";
}


sub createNewWorld() {
    stopGateway();
    print <<'EOM';

About to create a new security world.  Please ensure that:

* You have at least three blank cards for the card reader
* The card reader is connected to the nCipher HSM on the back of this Gateway appliance
* The module switch on the back of the HSM is in the "I" position (pre-initialization mode)

EOM

    if (pressEnterOrCancel()) {
        if (hasLocalWorld() || hasDatabaseWorld()) {
             print "\nWARNING: There is already a security world present on this node and/or in the database.\n\nDo you want to destroy the existing security world?\n\n";
             if (!proceedOrCancel()) {
                 die "Operation canceled by user.\n";
             }

             deleteLocalWorld();
             deleteDatabaseWorld();
        }

        doCreateNewWorld();
    }
}


sub doCreateNewWorld() {

    ensureModeIsPreInitialization();

    system(@NEWWORLD) == 0
        or die "new-world utility failed: $!\n";

    copyLocalWorldToDatabase();

    print "\nSecurity world created successfully.\n\n",
          "It is now safe to disconnect the card reader.\n\n",
          "Please label the administrator cards and keep them in a safe place.\n",
          "Be sure that any card passphrases are stored securely.\n\n";

    ensureModeIsOperational();

    manageHsmMenu();
}


sub ensureModeIsPreInitialization() {
    system(@NOPCLEARFAIL) == 0
       or die "Unable to reset board: $!\n";

    my $currentMode = getCurrentModuleMode();
    return if "pre-initialization" eq $currentMode;

    for (;;) {
        print <<"EOM";

The module is not in pre-initialization mode.  Its current mode is: $currentMode

Please ensure that:

* The module switch on the back of the HSM is in the "I" position (pre-initialization mode)

EOM
        if (!pressEnterOrCancel()) {
            die "Operation canceled by user.\n";
        }

        system(@NOPCLEARFAIL) == 0
            or die "Unable to reset board: $!\n";

        $currentMode = getCurrentModuleMode();
        return if "pre-initialization" eq $currentMode;
    }
}


sub ensureModeIsOperational() {
    for (;;) {
        print <<'EOM';
Please ensure that:

* The module switch on the back of the HSM is in the "O" position (operational mode)

EOM
        if (!pressEnterOrCancel()) {
            die "Operation canceled by user.\n";
        }

        system(@NOPCLEARFAIL) == 0
            or die "Unable to reset board: $!\n";

        my $currentMode = getCurrentModuleMode();
        return if "operational" eq $currentMode;

        print "The module is not in operational mode.  Its current mode is: $currentMode\n";
    }
}


sub programExistingWorld() {
    stopGateway();
    print <<'EOM';

About to program the module into an existing security world.  Please ensure that:

* The Gateway is configured with a database that already contains an nCipher security world
* You have at least two cards from the world's administrator cardset, along with their passphrases
* The card reader is connected to the nCipher HSM on the back of this Gateway appliance
* The module switch on the back of the HSM is in the "I" position (pre-initialization mode)

EOM

    if (pressEnterOrCancel()) {

        if (!hasDatabaseWorld()) {
            die "The current Gateway database does not appear to contain an nCipher security world.\n";
        }

        if (hasLocalWorld()) {
             print "\nWARNING: There is already a security world present on this local node.\n\nDo you want to destroy the local security world and replace it with the one from the database?\n\n";
             if (!proceedOrCancel()) {
                 die "Operation canceled by user.\n";
             }

             deleteLocalWorld();
        }

        doProgramExistingWorld();
    }
}


sub doProgramExistingWorld() {
    copyDatabaseWorldToLocal();

    ensureModeIsPreInitialization();

    system(@PROGRAMWORLD) == 0
        or die "new-world utility failed: $!\n";

    system(@NOPCLEARFAIL) == 0
        or die "Unable to reset board: $!\n";

    print <<'EOM';
Security world programmed successfully.

It is now safe to disconnect the card reader.

EOM

    ensureModeIsOperational();

    print "Security world programmed successfully.\n\nIt is now safe to disconnect the card reader.\n\n";

    manageHsmMenu();
}


sub manageHsmMenu() {

    my $msg = -f NCIPHERDEFS
        ? "The Gateway is currently configured to use the nCipher HSM."
        : "The Gateway is currently NOT configured to use the nCipher HSM.";

    print "\n$msg\n";

    print <<'EOM';

What would you like to do?

 1) Enable Gateway use of the nCipher HSM
 2) Disable Gateway use of the nCipher HSM
 X) Exit menu

EOM

    my $input = prompt();
    if ( uc($input) eq 'X' ) {
        return;
    } elsif ( $input eq '1' ) {
        doEnableNcipher();
    } elsif ( $input eq '2' ) {
        doDisableNcipher();
    } else {
        print "Invalid selection.\n";
    }
    pressEnter();
}


sub doEnableNcipher() {
    if (-f NCIPHERDEFS) {
        print "\nThe Gateway is already configured to use the nCipher HSM.\n\n";
        return;
    }

    if (!hasLocalWorld()) {
        print "\nThere is no nCipher security world present on this local node.\n";
        print "\nPlease either create a new world or program an existing world first.\n";
        return;
    }

    stopGateway();
    unlink(NCIPHERDEFS);
    die "Unable to recreate " . NCIPHERDEFS . " as it already exists and cannot be deleted: $!\n"
        if (-e NCIPHERDEFS);
    my $fh = new IO::File();
    $fh->open(">" . NCIPHERDEFS) and do {
        print $fh NCIPHERDEFS_CONTENTS;
        undef $fh;
    };
    print "\nThe Gateway is now configured to use the nCipher HSM.\n\n";
}


sub doDisableNcipher() {
    if (!-f NCIPHERDEFS) {
        print "\nThe Gateway is not currently using the nCipher HSM.\n\n";
        return;
    }

    stopGateway();
    unlink(NCIPHERDEFS) == 1
        or die "Unable to delete " . NCIPHERDEFS . ": $!\n";
    print "\nThe Gateway is no longer configured to use the nCipher HSM.\n\n";
}


sub showMenu() {
    print <<'EOM';
This menu allows you to configure the nCipher Hardware Security Module
on the SecureSpan Gateway Appliance

What would you like to do?

 1) Manage Gateway nCipher HSM status
 2) Create new security world
 3) Program into existing security world
 X) Exit menu

EOM
}


MAIN: eval {
    checkUid();

    my $badsleft = 30;
    my $quit = 0;

    showMenu();
    while (!$quit) {
        my $input = prompt();
        if ( $input eq '1' ) {
            manageHsmMenu();
            showMenu();
        } elsif ( $input eq '2' ) {
            createNewWorld();
            showMenu();
        } elsif ( $input eq '3' ) {
            programExistingWorld();
            showMenu();
        } elsif ( uc($input) eq 'X' ) {
            $quit = 1;
        } else {
            print "Invalid selection.\n";
            $badsleft--;
            die "Exiting.\n" if $badsleft < 1;
            showMenu() if $badsleft % 5 == 0;
        }
    }
};

my $err = $@;

if ($err =~ /operation canceled/i) {
  print $err, "\n";
  exit 0;
}

die $err if $err;
