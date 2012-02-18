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
use constant KMPFILE => '/opt/SecureSpan/Gateway/node/default/etc/conf/kmp.properties';

# already running as layer7, so no sudo needed for ssgconfig_launch actions
my @STOP_GATEWAY = qw(/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -lifecycle stop);
my @START_GATEWAY = qw(/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -lifecycle start);
my $LOAD_WORLD_B64 = q[/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -keystoreProperty get 4 worldb64];
my @SAVE_WORLD_B64 = qw(/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -keystoreProperty set 4 worldb64);
my $LOAD_WORLD_KEYSTOREID = q[/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -keystoreProperty get 4 initialKeystoreId];
my @SAVE_WORLD_KEYSTOREID = qw(/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -keystoreProperty set 4 initialKeystoreId);
my $LOAD_IGNORE_KEYSTOREIDS = q[/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -keystoreProperty get 4 ignoreKeystoreIds];
my @SAVE_IGNORE_KEYSTOREIDS = qw(/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -keystoreProperty set 4 ignoreKeystoreIds);
my @CLEAR_WORLD_B64 = qw(/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -keystoreProperty clear 4 worldb64);
my @CLEAR_WORLD_KEYSTOREID = qw(/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -keystoreProperty clear 4 initialKeystoreId);
my @CLEAR_WORLD_DATABYTES = qw(/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -keystoreProperty clear 4 databytes);
my @CLEAR_IGNORE_KEYSTOREIDS = qw(/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -keystoreProperty clear 4 ignoreKeystoreIds);
my @GENERATE_NEW_KMP = qw(/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -changeMasterPassphrase kmp generateAll);
my @MIGRATE_KMP_TO_OMP = qw(/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -changeMasterPassphrase kmp migrateFromKmpToOmp);
my $LIST_KEYSTORE_CONTENTS = q[/opt/SecureSpan/Appliance/libexec/ssgconfig_launch -changeMasterPassphrase kmp listKeystoreContents ignoreKmpFile config.profile=ncipher.sworld.rsa keystore.contents.base64=];

# we will check that layer7 is in the nfast group, so no sudo needed for nfast commands
my @NOPCLEARFAIL = qw(/opt/nfast/bin/nopclearfail ca);
my @NEWWORLD = qw(/opt/nfast/bin/new-world -m 1 -s 0 -Q 2/3 -k rijndael);
my @PROGRAMWORLD = qw(/opt/nfast/bin/new-world --program --module=1);
my $CHECK_MODE = q[/opt/nfast/bin/enquiry -m 1 | grep "^ mode" | awk '{print $2}'];
my $CHECK_FIPS = q[/opt/nfast/bin/nfkminfo -w | grep ' StrictFIPS140'];

sub stopGateway() {
    { local $/;  print "Stopping Gateway... "; }
    system(@STOP_GATEWAY) == 0
        or die "Failed to stop Gateway: $!\n";
    print "Done\n";
}


sub startGateway() {
    { local $/;  print "Starting Gateway... "; }
    system(@START_GATEWAY) == 0
        or die "Failed to start Gateway: $!\n";
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
    getcwd eq KMDATA or die "Failed to chdir to " . KMDATA . "\n";

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

sub isStrictFipsWorld() {
    my $fips = `$CHECK_FIPS`;
    die "Failed to check for StrictFIPS140 world: $?" if $?;
    chomp($fips);
    my $empty = $fips =~ /^\s*$/;
    return !$empty;
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


sub loadDatabaseKeystoreId() {
    my $keystoreId = `$LOAD_WORLD_KEYSTOREID 2>&1`;
    my $result = $?;
    my $status = $? >> 8;
    chomp($keystoreId);
    # Status of 11 just means "property not found"
    return undef if $status == 11;
    die "Failed to load database keystore IDa: $?: $keystoreId\n" if $result;
    return $keystoreId;
}


sub saveDatabaseKeystoreId($) {
    my $keystoreId = shift;
    die "keystoreId required" unless $keystoreId;

    my @cmd = (@SAVE_WORLD_KEYSTOREID, $keystoreId);
    system(@cmd) == 0
        or die "Failed to save world keystore ID to database: $?\n";
}


sub loadDatabaseIgnoreIds() {
    my $keystoreId = `$LOAD_IGNORE_KEYSTOREIDS 2>&1`;
    my $result = $?;
    my $status = $? >> 8;
    chomp($keystoreId);
    # Status of 11 just means "property not found"
    return () if $status == 11;
    die "Failed to load database keystore IDa: $?: $keystoreId\n" if $result;

    my @ids = split '\s*,\s*', $keystoreId;
    my @ret;

    foreach my $id (@ids) {
        if ($id =~ /^([a-f0-9]{40})$/) {
            push @ret, $1;
        }
    }

    return @ret;
}


sub saveDatabaseIgnoreIds {
    my @ignoreIds = @_;

    my $ids = join ",", @ignoreIds;
    my @cmd = (@SAVE_IGNORE_KEYSTOREIDS, $ids);
    system(@cmd) == 0
        or die "Failed to save ignore keystore IDs to database: $?\n";
}


sub addDatabaseIgnoreId($) {
    my $id = shift;

    my @ids = loadDatabaseIgnoreIds();
    push @ids, $id;
    saveDatabaseIgnoreIds(@ids);
}


sub removeDatabaseIgnoreId($) {
    my $id = shift;

    my @ids = loadDatabaseIgnoreIds();
    @ids = grep { $id ne $_ } @ids;
    if (@ids) {
        saveDatabaseIgnoreIds(@ids);
    } else {
        deleteDatabaseIgnoreIds();
    }
}


sub deleteDatabaseKeystoreId() {
    system(@CLEAR_WORLD_KEYSTOREID) == 0
        or die "Failed to clear world data in database (initialKeystoreId): $?\n";
}


sub deleteDatabaseIgnoreIds() {
    system(@CLEAR_IGNORE_KEYSTOREIDS) == 0
        or die "Failed to clear world data in database (ignoreids): $?\n";
}


sub deleteDatabaseWorld() {
    system(@CLEAR_WORLD_B64) == 0
        or die "Failed to clear world data in database (worldb64): $?\n";
    system(@CLEAR_WORLD_DATABYTES) == 0
        or die "Failed to clear world data in database (databytes): $?\n";
    deleteDatabaseKeystoreId();
    deleteDatabaseIgnoreIds();
}


sub hasDatabaseWorld() {
    my $world = loadDatabaseWorld();
    return defined($world) && length($world) > 0;
}


sub hasLocalWorld() {
    return -f KMDATA_WORLD;
}


sub loadLocalWorld() {
    my $world = slurpFile(KMDATA_WORLD);
    die "No local world file found\n" unless defined($world);
    die "Local world file is empty\n" if length($world) < 1;
    return $world;
}


sub doesDatabaseWorldMatchLocalWorld() {
    my $localWorld = loadLocalWorld();
    my $databaseWorld = loadDatabaseWorld();
    return $localWorld eq $databaseWorld;
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


# Get a list of all keystore IDs currently on disk.  May be empty.
sub localKeystoreIds() {
    return map { /key_jcecsp_(.*)/; $1 } grep { !/-key-/ } glob KMDATA . "/key_jcecsp_*";
}


# Get the number of objects that appear to be in the keystore with the specified keystore ID
sub numObjectsInKeystore($) {
    my $keystoreId = shift;
    my @files = glob KMDATA . "/key_jcecsp_" . $keystoreId . "-key-*";
    return scalar @files;
}


sub copyLocalWorldToDatabase() {
    saveDatabaseWorld(loadLocalWorld());
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


sub lookupKmpKeystoreId() {
    die "File not found: " . KMPFILE unless -f KMPFILE;
    my $kmpcontents = slurpFile(KMPFILE);
    my %props = map {
        my ($k, $v) = split /=/;
        $k ? ($k, $v || "") : ();
    } split /[\015\012]+/, $kmpcontents;
    my $b64 = $props{'keystore.contents.base64'};
    return $b64 ? decode_base64($b64) : undef;
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

        pressEnter();

        system(@NOPCLEARFAIL) == 0
            or die "Unable to reset board: $!\n";

        my $currentMode = getCurrentModuleMode();
        return if "operational" eq $currentMode;

        print "The module is not in operational mode.  Its current mode is: $currentMode\n";
    }
}


sub listKeystoreIds(@) {
    my @keystoreIds = @_;
    foreach (@keystoreIds) {
        my $keystoreId = $_;
        my $numObjects = numObjectsInKeystore($keystoreId);
        my $suffix = $numObjects == 1 ? "" : "s";
        print "  $_ (contains $numObjects object$suffix)\n";
    }
}


sub listKeystoreContents($) {
    my $keystoreId = shift;
    my $keystoreIdB64 = encode_base64($keystoreId, '');

    my $contents = `$LIST_KEYSTORE_CONTENTS$keystoreIdB64 2>&1`;
    my $result = $?;
    my $status = $? >> 8;
    chomp($contents);
    if ($result) {
        print "\nUnable to list keystore contents: exit status $status: $contents\n";
        return;
    }

    my @lines = split /[\015\012]+/, $contents;
    my $numEntries = scalar @lines;
    my $entries = $numEntries == 1 ? "entry" : "entries";
    print "Keystore ID $keystoreId contains $numEntries $entries:\n\n";
    print join("\n", @lines);
    print "\n";
}


sub matchPrefix(\@$) {
    my $elements = shift;
    my $prefix = shift;
    my $prefixlen = length $prefix;
    return grep { substr($_, 0, $prefixlen) eq $prefix } @$elements;
}


sub matchKeystoreId(\@$) {
    my $keystoreIds = shift;
    my $prefix = shift;
    my $block = shift;

    my %knownIds = map { $_ => 1 } @$keystoreIds;
    if ($knownIds{$prefix}) {
        # Exact match of a keystore ID
        return $prefix;
    }

    # Check for prefix match
    my @matches = matchPrefix(@$keystoreIds, $prefix);

    if (!@matches) {
        print "Prefix does not match any known keystore ID.  Use \"list\" command to list keystore IDs.\n";
        return undef;
    } elsif (@matches == 1) {
        return $matches[0];
    } else {
        print "Prefix matches more than one keystore ID.  Use \"list\" command to list keystore IDs.\n";
        return undef;
    }
}


sub chooseKeystoreId(@) {
    my @keystoreIds = @_;


    print <<'EOM';

More than one keystore ID is present on the local node.  Please choose a keystore ID for the Gateway
to use as its "nCipher HSM" keystore:

EOM

    listKeystoreIds(@keystoreIds);

    for (;;) {
        print <<'EOM';

Enter the first few unique digits of the keystore ID to use that keystore ID with the Gateway.
Enter "list" to redisplay the list of available IDs.  Enter "X" to cancel.
Enter "list " followed by a keystore ID to attempt to list its contents (assumes module-protection).

EOM
        print "Choice (list|<ID>|list <ID>|X): ";
        my $cmd = <STDIN>;
        chomp($cmd);

        if ("x" eq lc $cmd) {
            die "Operation canceled by user.\n";
        } elsif ("list" eq lc $cmd) {
            listKeystoreIds(@keystoreIds);
        } elsif ($cmd =~ /^list (.+)$/i) {
            my $idprefix = $1;
            my $id = matchKeystoreId(@keystoreIds, $idprefix);
            if ($id) {
                listKeystoreContents($id);
            }
        } else {
            my $id = matchKeystoreId(@keystoreIds, $cmd);
            if ($id) {
                # Done
                return $id;
            }

            print "Unrecognized command.\n";
        }
    }
}


sub programExistingWorld() {
    stopGateway();
    print <<'EOM';

About to program the module into a security world already present in the database.  Please ensure that:

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


sub useAlreadyProgrammedWorld() {
    stopGateway();
    print <<'EOM';

About to configure the Gateway to use a security world that has already been manually programmed.  Please ensure that:

* The world file has been copied to the kmdata/local directory
* The module been programmed into the security world
* Any desired keystore and key objects have been copied to kmdata/local using the application prefix "key_jcecsp"
* There is a single keystore file (no suffix) and all key files (-key-NNN suffix) use a matching keystore ID

EOM

    if (pressEnterOrCancel()) {
        if (!hasLocalWorld()) {
            die "There does not appear to be a security world already present on this local node."
        }

        my $keystoreId;
        my @localKeystoreIds = localKeystoreIds();
        if (@localKeystoreIds > 1) {
            $keystoreId = chooseKeystoreId(@localKeystoreIds);
        } elsif (@localKeystoreIds) {
            $keystoreId = $localKeystoreIds[0];
        }

        my $hasDatabaseWorld = hasDatabaseWorld();

        if ($hasDatabaseWorld && !doesDatabaseWorldMatchLocalWorld()) {
            print "\nWARNING: There is already a security world present in the database that does not match the world on this local node.\n\n",
                  "Do you want to destroy the security world in the database?\n\n";
            if (!proceedOrCancel()) {
                die "Operation canceled by user.\n";
            }

            deleteDatabaseWorld();
        }

        if ($keystoreId) {
            my $dbKeystoreId = loadDatabaseKeystoreId();
            if ($dbKeystoreId && $dbKeystoreId ne $keystoreId) {
                print "\nWARNING: The database already contains a keystore ID that does not match the keystore ID on this local node.\n\n",
                        "Do you want to destroy the keystore in the database?\n\n";
                if (!proceedOrCancel()) {
                    die "Operation canceled by user.\n";
                }
                deleteDatabaseWorld();
            }

            saveDatabaseKeystoreId($keystoreId);
            print "\nInitial keystore ID has been set to $keystoreId\n";
        } else {
            deleteDatabaseKeystoreId();
        }

        my $copiedToDb = 0;
        if (!hasDatabaseWorld()) {
            copyLocalWorldToDatabase();
            $copiedToDb = 1;
            print "\nThe world file has been copied to the database.\n";
        } else {
            print "\nThe database already contains a copy of the current world file.\n";
        }

        print "\nNext time it starts up configured to use the nCipher HSM, the Gateway will sync any keystore objects\n",
              "that use the application name \"key_jcesp\" with a matching keystore ID between the local node and the database.\n\n";

        if ($copiedToDb) {
            print "IMPORTANT: World was copied to DB for first time -- please ensure this node has been started at least once\nusing the nCipher HSM before configuring any other cluster nodes to do so.\n\n";
        }

        pressEnter();
        manageHsmMenu();
    }
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


sub offerToStartGateway() {
    print "\n\nWould you like to start the Gateway now (y/n): ";
    my $in = <STDIN>;
    chomp($in);
    if (lc($in) eq 'y') {
        startGateway();
    }
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

    doGenerateNewKmp();

    my $kmpKeystoreId = lookupKmpKeystoreId();
    die "Unable to enable keystore-protected master passphrase: could not determine keystore ID in use by current KMP\n"
        unless $kmpKeystoreId =~ /^([a-f0-9]{40})$/;
    addDatabaseIgnoreId($kmpKeystoreId);

    unlink(NCIPHERDEFS);
    die "Unable to recreate " . NCIPHERDEFS . " as it already exists and cannot be deleted: $!\n"
        if (-e NCIPHERDEFS);
    my $fh = new IO::File();
    $fh->open(">" . NCIPHERDEFS) and do {
        print $fh NCIPHERDEFS_CONTENTS;
        undef $fh;
    };
    print "\nThe Gateway is now configured to use the nCipher HSM.\n\n";
    offerToStartGateway();
}

sub doGenerateNewKmp() {
    die "Unable to generate new keystore-protected master passphrase: the file " . KMPFILE . " already exists\n"
        if -e KMPFILE;

    my $profile = isStrictFipsWorld() ? "ncipher.fipssworld.rsa" : "ncipher.sworld.rsa";

    system(@GENERATE_NEW_KMP, $profile) == 0
        or die "Failed to generate a new keystore-protected master passphrase: $?\n";
}

sub doDisableNcipher() {
    if (!-f NCIPHERDEFS) {
        print "\nThe Gateway is not currently using the nCipher HSM.\n\n";
        return;
    }

    stopGateway();

    doMigrateKmpToOmp() if -f KMPFILE;

    unlink(NCIPHERDEFS) == 1
        or die "Unable to delete " . NCIPHERDEFS . ": $!\n";
    print "\nThe Gateway is no longer configured to use the nCipher HSM.\n\nNote: master passphrase restored to default value -- use main menu to change it.\n\n";
    offerToStartGateway();
}

sub doMigrateKmpToOmp() {
    my $kmpKeystoreId = lookupKmpKeystoreId();
    die "Unable to turn off keystore-protected master passphrase: could not determine keystore ID in use by current KMP\n"
        unless $kmpKeystoreId =~ /^([a-f0-9]{40})$/;
    my $keystoreId = $1;

    system(@MIGRATE_KMP_TO_OMP) == 0
        or die "Failed to recover keystore-protected passphrase: $?\n";
    unlink(KMPFILE);
    die "Unable to delete file: " . KMPFILE . "\n"
        if (-e KMPFILE);

    # Clean out the keystore we created for KMP purposes
    my $keystoreFiles = KMDATA . "/key_jcecsp_$keystoreId*";
    system("rm -f $keystoreFiles") == 0
        or die "Unable to remove key blobs for previous keystore-protected master passphrase: $keystoreFiles\n";

    removeDatabaseIgnoreId($keystoreId);
}


sub showMenu() {
    print <<'EOM';

This menu allows you to configure the nCipher Hardware Security Module
on the SecureSpan Gateway Appliance

What would you like to do?

 1) Manage Gateway nCipher HSM status
 2) Create new security world
 3) Program into existing security world
 4) Use manually-programmed security world
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
            if (-f NCIPHERDEFS) {
                print "\nThe Gateway is currently configured to use the nCipher HSM.\nPlease turn off Gateway use of nCipher HSM before creating a new security world.\n\n";
                pressEnter();
            } else {
                createNewWorld();
            }
            showMenu();
        } elsif ( $input eq '3' ) {
            if (-f NCIPHERDEFS) {
                print "\nThe Gateway is currently configured to use the nCipher HSM.\nPlease turn off Gateway use of nCipher HSM before programming a security world.\n\n";
                pressEnter();
            } else {
                programExistingWorld();
            }
            showMenu();
        } elsif ( $input eq '4' ) {
            if (-f NCIPHERDEFS) {
                print "\nThe Gateway is currently configured to use the nCipher HSM.\nPlease turn off Gateway use of nCipher HSM before changing its security world.\n\n";
                pressEnter();
            } else {
                useAlreadyProgrammedWorld();
            }
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
