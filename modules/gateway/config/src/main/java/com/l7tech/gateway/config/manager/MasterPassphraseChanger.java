package com.l7tech.gateway.config.manager;

import com.l7tech.util.*;
import org.xml.sax.SAXException;

import java.io.*;
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

    private static final String DEFAULT_MASTER_PASSPHRASE = "7layer";
    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 128;
    private static final String CONFIG_PATH = "../node/default/etc/conf";

    private final String command;
    private final String subcommand;

    public MasterPassphraseChanger(String command, String subcommand) {
        this.command = command;
        this.subcommand = subcommand;
    }

    public static void main(String[] argv) {
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties", false, true);
        try {
            LinkedList<String> args = new LinkedList<String>(Arrays.asList(argv));

            if (args.isEmpty()) {
                // No args -- run in interactive mode
                new MasterPassphraseChanger(null, null).run(CONFIG_PATH, new String[]{ "node.cluster.pass", "node.db.config.main.pass" });
            } else {
                String command = args.removeFirst();
                String subcommand = args.removeFirst();
                if (!args.isEmpty())
                    usage("Too many arguments.");

                // Args provided -- execute requested command and return
                new MasterPassphraseChanger(command, subcommand).run(CONFIG_PATH, new String[]{ "node.cluster.pass", "node.db.config.main.pass" });
            }
        } catch (NoSuchElementException e) {
            usage("Not enough arguments.");
        } catch (Throwable e) {
            String msg = "Unable to change master passphrase: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, e);
            System.err.println(msg);
            System.exit(1);
        }
    }

    private boolean isMatchingMasterPassphrase(String currentObfuscated, String candidatePlaintext) throws IOException {
        long currentSalt = ObfuscatedFileMasterPasswordFinder.getSalt(currentObfuscated);
        String candidateObfuscated = ObfuscatedFileMasterPasswordFinder.obfuscate(candidatePlaintext, currentSalt);
        return currentObfuscated.equals(candidateObfuscated);
    }

    // Never returns; calls System.exit
    private static void usage(String prefix) {
        String p = prefix == null ? "" : prefix + "\n";
        fatal(p + "Usage: MasterPassphraseChanger [kmp <generateAll|eraseAll>]", null, EXIT_STATUS_USAGE);
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

    private void run( String configurationDirPath, String[] passwordProperties ) throws IOException, SAXException {
        if (command != null) {
            runCommand(command, subcommand);
            return;
        }

        File configDirectory = new File(configurationDirPath);
        File ompCurFile= new File( configDirectory, "omp.dat" );
        if (ompCurFile.exists()) {
            String ompCurStr = loadFileText(ompCurFile);
            if (!isMatchingMasterPassphrase(ompCurStr, DEFAULT_MASTER_PASSPHRASE)) {
                String curMasterPass = promptForPassword("Enter the existing master passphrase ('quit' to quit): ");
                if (!isMatchingMasterPassphrase(ompCurStr, curMasterPass)) {
                    System.out.println("Entered passphrase does not match the current master passphrase.");
                    System.exit(3);
                }
            }
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
        new ObfuscatedFileMasterPasswordFinder(ompNewFile).saveMasterPassword(newMasterPass.toCharArray());

        final MasterPasswordManager decryptor = ompCurFile.exists() ? new MasterPasswordManager(new DefaultMasterPasswordFinder(ompOldFile)) : null;
        final MasterPasswordManager encryptor = new MasterPasswordManager(newMasterPass.getBytes(Charsets.UTF8));

        PasswordPropertyCrypto passwordCrypto = new PasswordPropertyCrypto(encryptor, decryptor);
        passwordCrypto.setPasswordProperties( passwordProperties );

        findPropertiesFilesAndReencryptPasswords(configDirectory, passwordCrypto);

        // Commit the new master password
        new ObfuscatedFileMasterPasswordFinder(ompCurFile).saveMasterPassword(newMasterPass.toCharArray());

        // Clean up
        deleteLockFiles(ompCurFile, ompOldFile, ompNewFile);
        FileUtils.deleteFileSafely(ompNewFile.getAbsolutePath());

        // we'll back up the .old file, in case the admin needs to recover something by hand someday
        ompOldFile.renameTo(new File(ompOldFile.getParent(), ompOldFile.getName() + "-" + System.currentTimeMillis()));

        System.out.println("Master passphrase changed successfully.");
    }

    private void runCommand(String command, String subcommand) {
        if ("ks".equals(command)) {
            runKsSubcomand(subcommand);
        } else {
            usage("Unknown command: " + command);
        }
    }

    private void runKsSubcomand(String subcommand) {
        if ("generateAll".equals(subcommand)) {
            runKsGenerateAll();
        } else if ("eraseAll".equals(subcommand)) {
            runKsEraseAll();
        } else {
            usage("Unknown ks subcommand: " + subcommand);
        }
    }

    private void runKsGenerateAll() {
        // TODO
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    private void runKsEraseAll() {
        // TODO
        throw new UnsupportedOperationException("Not yet implemented.");
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
