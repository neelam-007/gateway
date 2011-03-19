package com.l7tech.gateway.config.manager;

import com.l7tech.util.*;
import org.xml.sax.SAXException;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Prompts the user to change the master passphrase.
 */
public class MasterPassphraseChanger {
    private static final Logger logger = Logger.getLogger(MasterPassphraseChanger.class.getName());

    public static final int EXIT_STATUS_USAGE = 2;

    private static final byte[] DEFAULT_MASTER_PASSPHRASE = "7layer".getBytes(Charsets.UTF8);
    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 128;
    private static final String CONFIG_PATH = "../node/default/etc/conf";
    static final String[] PASSWORD_PROPERTIES = new String[]{ "node.cluster.pass", "node.db.config.main.pass" };

    private final String configurationDirPath;
    private final String command;
    private final String subcommand;
    private final String[] passwordProperties;

    public MasterPassphraseChanger(String configurationDirPath, String[] passwordProperties, String command, String subcommand) {
        this.configurationDirPath = configurationDirPath;
        this.passwordProperties = passwordProperties;
        this.command = command;
        this.subcommand = subcommand;
    }

    public static void main(String[] argv) {
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties", false, true);
        try {
            LinkedList<String> args = new LinkedList<String>(Arrays.asList(argv));

            if (args.isEmpty()) {
                // No args -- run in interactive mode
                new MasterPassphraseChanger(CONFIG_PATH, PASSWORD_PROPERTIES,  null, null).run();
            } else {
                String command = args.removeFirst();
                String subcommand = args.removeFirst();
                if (!args.isEmpty())
                    usage("Too many arguments.");

                // Args provided -- execute requested command and return
                new MasterPassphraseChanger(CONFIG_PATH, PASSWORD_PROPERTIES, command, subcommand).run();
            }
        } catch (NoSuchElementException e) {
            usage("Not enough arguments.");
        } catch (Throwable e) {
            String msg = "Unable to configure master passphrase: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, e);
            System.err.println(msg);
            System.exit(1);
        }
    }

    private boolean isMatchingMasterPassphrase(String currentObfuscated, byte[] candidatePlaintext) throws IOException {
        long currentSalt = ObfuscatedFileMasterPasswordFinder.getSalt(currentObfuscated);
        String candidateObfuscated = ObfuscatedFileMasterPasswordFinder.obfuscate(candidatePlaintext, currentSalt);
        return currentObfuscated.equals(candidateObfuscated);
    }

    // Never returns; calls System.exit
    private static void usage(String prefix) {
        String p = prefix == null ? "" : prefix + "\n";
        fatal(p + "Usage: MasterPassphraseChanger [kmp <generateAll|migrateFromOmpToKmp|migrateFromKmpToOmp>]", null, EXIT_STATUS_USAGE);
    }

    // Never returns; calls System.exit
    private static void fatal(String msg, Throwable t, int status) {
        logger.log(Level.WARNING, msg, t);
        System.err.println(msg);
        System.exit(status);
    }

    private void exitOnQuit( final String perhapsQuit ) {
        if ( "quit".equals(perhapsQuit) ) {
            System.exit(1);
        }
    }

    void run() throws Exception {
        if (command != null) {
            runCommand(command, subcommand);
            return;
        }

        File configDirectory = new File(configurationDirPath);
        File ompCurFile= new File( configDirectory, "omp.dat" );
        if (ompCurFile.exists()) {
            requirePromptForExistingOmp(ompCurFile);
        }

        String newMasterPass;
        String confirm;
        boolean matched;
        do {
            newMasterPass = promptForPassword("Enter the new master passphrase (" + MIN_LENGTH + " - " + MAX_LENGTH + " characters, 'quit' to quit): ");
            confirm = promptForPassword("Confirm new master passphrase ('quit' to quit): ");
            matched = confirm.equals(newMasterPass);
            if (!matched)
                System.out.println("The passphrases do not match.");
        } while (!matched);


        // Create decryptor with current master passphrase, and save the current one in a backup file
        File ompOldFile = new File(ompCurFile.getParent(), "omp.dat.old");
        File ompNewFile = new File(ompCurFile.getParent(), "omp.dat.new");

        if (ompOldFile.exists() && !ompCurFile.exists()) {
            // Clean up possible previous failed password change
            FileUtils.copyFile(ompOldFile, ompCurFile);
        }

        ompOldFile.delete();
        FileUtils.copyFile(ompCurFile, ompOldFile);
        FileUtils.touch(ompNewFile);

        // Save new password so admin can manually recover if we go haywire during the operation
        new ObfuscatedFileMasterPasswordFinder(ompNewFile).saveMasterPassword(newMasterPass.getBytes(Charsets.UTF8));

        final MasterPasswordManager decryptor = ompCurFile.exists() ? new MasterPasswordManager(new DefaultMasterPasswordFinder(ompOldFile)) : null;
        final MasterPasswordManager encryptor = new MasterPasswordManager(newMasterPass.getBytes(Charsets.UTF8));

        PasswordPropertyCrypto passwordCrypto = new PasswordPropertyCrypto(encryptor, decryptor);
        passwordCrypto.setPasswordProperties( passwordProperties );

        findPropertiesFilesAndReencryptPasswords(configDirectory, passwordCrypto);

        // Commit the new master password
        new ObfuscatedFileMasterPasswordFinder(ompCurFile).saveMasterPassword(newMasterPass.getBytes(Charsets.UTF8));

        // Clean up
        deleteLockFiles(ompCurFile, ompOldFile, ompNewFile);
        FileUtils.deleteFileSafely(ompNewFile.getAbsolutePath());

        // we'll back up the .old file, in case the admin needs to recover something by hand someday
        ompOldFile.renameTo(new File(ompOldFile.getParent(), ompOldFile.getName() + "-" + System.currentTimeMillis()));

        System.out.println("Master passphrase changed successfully.");
    }

    private void requirePromptForExistingOmp(File ompCurFile) throws IOException {
        String ompCurStr = loadFileText(ompCurFile);
        if (!isMatchingMasterPassphrase(ompCurStr, DEFAULT_MASTER_PASSPHRASE)) {
            String curMasterPass = promptForPassword("Enter the existing master passphrase ('quit' to quit): ");
            if (!isMatchingMasterPassphrase(ompCurStr, curMasterPass.getBytes(Charsets.UTF8))) {
                System.out.println("Entered passphrase does not match the current master passphrase.");
                System.exit(3);
            }
        }
    }

    private void runCommand(String command, String subcommand) throws Exception {
        if ("kmp".equals(command)) {
            runKmpSubcommand(subcommand);
        } else {
            usage("Unknown command: " + command);
        }
    }

    private void runKmpSubcommand(String subcommand) throws Exception {
        if ("generateAll".equals(subcommand)) {
            runKmpGenerateAll();
        } else if ("migrateFromOmpToKmp".equals(subcommand)) {
            runKmpMigrateFromOmpToKmp();
        } else if ("migrateFromKmpToOmp".equals(subcommand)) {
            runKmpMigrateFromKmpToOmp();
        } else {
            usage("Unknown kmp subcommand: " + subcommand);
        }
    }

    private void runKmpGenerateAll() throws IOException, GeneralSecurityException, SAXException {
        File configDirectory = new File(configurationDirPath);
        File ompFile = new File(configDirectory, "omp.dat" );

        // Decryptor to decrypt passwords encrypted with old obfuscated master passphrase
        final MasterPasswordManager decryptor = ompFile.exists() && ompFile.length() > 0 ? new MasterPasswordManager(new ObfuscatedFileMasterPasswordFinder(ompFile)) : null;

        new KeyStorePrivateKeyMasterPasswordUtil(configDirectory).generateNewMasterPassword();
        File kmpFile = KeyStorePrivateKeyMasterPasswordUtil.findPropertiesFile(ompFile);

        // Encryptor to re-encrypt passwords with new keystore-protected master passphrase
        final MasterPasswordManager encryptor = new MasterPasswordManager(new KeyStorePrivateKeyMasterPasswordFinder(kmpFile));

        PasswordPropertyCrypto passwordCrypto = new PasswordPropertyCrypto(encryptor, decryptor);
        passwordCrypto.setPasswordProperties( passwordProperties );
        findPropertiesFilesAndReencryptPasswords(configDirectory, passwordCrypto);

        if (ompFile.exists())
            FileUtils.save(new byte[0], ompFile); // Truncate omp but leave in place for compatibility

        deleteLockFiles(ompFile, kmpFile);
    }

    private void runKmpMigrateFromOmpToKmp() throws Exception {
        // TODO remove this if we turn out not to need it
        File configDirectory = new File(configurationDirPath);
        File ompFile = new File( configDirectory, "omp.dat" );
        if (ompFile.exists() && ompFile.length() > 0) {
            // Require current contents of omp.dat to be enered in order to do migration
            requirePromptForExistingOmp(ompFile);
        }
        File kmpFile = KeyStorePrivateKeyMasterPasswordUtil.findPropertiesFile(ompFile);

        // Decryptor for existing passwords
        final MasterPasswordManager decryptor = ompFile.exists() && ompFile.length() > 0 ? new MasterPasswordManager(new ObfuscatedFileMasterPasswordFinder(ompFile)) : null;

        // Create/edit kmp.properties
        final KeyStorePrivateKeyMasterPasswordUtil kmpUtil = new KeyStorePrivateKeyMasterPasswordUtil(new File(configurationDirPath));
        kmpUtil.generateNewMasterPassword();

        final MasterPasswordManager encryptor = new MasterPasswordManager(new KeyStorePrivateKeyMasterPasswordFinder(ompFile));

        PasswordPropertyCrypto passwordCrypto = new PasswordPropertyCrypto(encryptor, decryptor);
        passwordCrypto.setPasswordProperties( passwordProperties );

        findPropertiesFilesAndReencryptPasswords(configDirectory, passwordCrypto);

        deleteLockFiles(ompFile, kmpFile);
    }

    private void runKmpMigrateFromKmpToOmp() throws IOException, SAXException {
        File configDirectory = new File(configurationDirPath);
        File ompFile = new File( configDirectory, "omp.dat" );
        File kmpFile = KeyStorePrivateKeyMasterPasswordFinder.findPropertiesFile(ompFile);

        // Don't bother re-encrypting properties files since we will be keeping the same master passphrase bytes
        final KeyStorePrivateKeyMasterPasswordFinder finder = new KeyStorePrivateKeyMasterPasswordFinder(kmpFile);
        final byte[] masterPassphraseBytes = finder.findMasterPasswordBytes();
        new ObfuscatedFileMasterPasswordFinder(ompFile).saveMasterPassword(masterPassphraseBytes);

        // Clean up
        deleteLockFiles(ompFile, kmpFile);

        // we'll back up the kmp file, in case the admin needs to recover something by hand someday
        kmpFile.renameTo(new File(kmpFile.getParent(), kmpFile.getName() + "-" + System.currentTimeMillis()));
    }

    private void deleteLockFiles(File... files) {
        for (File file : files) {
            File lock = new File(file.getParent(),  file.getName() + ".LCK");
            lock.delete();
        }
    }

    /**
     * Finds all properties files in the specified directory (and all subdirectories) and
     * reencrypts passwords in them.
     *
     * @param configDirectory  the directory whose properties files to reencrypt.
     * @param passwordCrypto   object that decrypts and reencrypts passwords.  Required.
     * @throws IOException    on error reading or writing a file
     * @throws SAXException   on error parsing or reserializing an XML config file
     */
    private void findPropertiesFilesAndReencryptPasswords( File configDirectory , PasswordPropertyCrypto passwordCrypto) throws IOException, SAXException {
        recursivelyFindPropertiesFilesAndReencryptPasswords(configDirectory, passwordCrypto);
    }

    private void recursivelyFindPropertiesFilesAndReencryptPasswords(File dir, PasswordPropertyCrypto passwordEncryptor)
            throws IOException, SAXException
    {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                recursivelyFindPropertiesFilesAndReencryptPasswords(file, passwordEncryptor);
            } else {
                if (file.getName().endsWith("properties"))
                    PropertyHelper.reencryptPasswordsInPlace(file, passwordEncryptor);
            }
        }
    }

    private String promptForPassword(String prompt) throws IOException {
        String password = null;

        Console console = System.console();
        Pattern pattern = Pattern.compile("^.{" + MIN_LENGTH + "," + MAX_LENGTH + "}$", Pattern.DOTALL);
        while( password == null  ) {
            System.out.println( prompt );
            password = new String(console.readPassword());

            exitOnQuit( password );

            if ( !pattern.matcher(password).matches() ) {
                System.out.println( "Master passphrase should be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters long.\n" );
                password = null;
            }
        }

        return password;
    }

    private String loadFileText(File ompfile) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream( ompfile );
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read;
            while ( (read = in.read()) != -1 ) {
                baos.write( read );
            }
            return new String(baos.toByteArray(), Charsets.UTF8).trim();
        } finally {
            ResourceUtils.closeQuietly(in);
        }
    }    
}
