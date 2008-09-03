package com.l7tech.gateway.config.manager;

import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FileUtils;
import com.l7tech.common.io.IOUtils;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Console;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Prompts the user to change the master passphrase.
 *
 * TODO [steve] fix changing master passphrase
 */
public class MasterPassphraseChanger {
    protected static final Logger logger = Logger.getLogger(MasterPassphraseChanger.class.getName());
    private static final String DEFAULT_MASTER_PASSPHRASE = "7layer";
    private static final String EOL = System.getProperty("line.separator");
    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 128;

    public static void main(String[] args) {
        try {
            LogManager.getLogManager().getLogger("").setLevel(Level.SEVERE);
            new MasterPassphraseChanger().run("/opt/SecureSpan/Gateway/Nodes/default/etc/conf", new String[]{ "node.cluster.pass", "node.db.pass" });
        } catch (Throwable e) {
            String msg = "Unable to change master passphrase: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, e);
            System.err.println(msg);
            System.exit(1);
        }
    }

//    private boolean isMatchingMasterPassphrase(String currentObfuscated, String candidatePlaintext) throws IOException {
//        long currentSalt = DefaultMasterPasswordFinder.getSalt(currentObfuscated);
//        String candidateObfuscated = DefaultMasterPasswordFinder.obfuscate(candidatePlaintext, currentSalt);
//        return currentObfuscated.equals(candidateObfuscated);
//    }

    private void run( String configurationDirPath, String[] passwordProperties ) throws IOException, SAXException {
        File configDirectory = new File(configurationDirPath);
        File ompCurFile= new File( configDirectory, "omp.dat" );
        if (ompCurFile.exists()) {
//            String ompCurStr = new String( IOUtils.slurpFile(ompCurFile), "UTF-8").trim();
//            if (!isMatchingMasterPassphrase(ompCurStr, DEFAULT_MASTER_PASSPHRASE)) {
//                String curMasterPass = promptForPassword("Enter the existing master passphrase ('quit' to quit): ");
//                if (!isMatchingMasterPassphrase(ompCurStr, curMasterPass)) {
//                    System.out.println("Entered passphrase does not match the current master passphrase.");
//                    System.exit(3);
//                }
//            }
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
//        new DefaultMasterPasswordFinder(ompNewFile).saveMasterPassword(newMasterPass.toCharArray());
//
//        final MasterPasswordManager decryptor = ompCurFile.exists() ? new MasterPasswordManager(new DefaultMasterPasswordFinder(ompOldFile).findMasterPassword()) : null;
//        final MasterPasswordManager encryptor = new MasterPasswordManager(newMasterPass.toCharArray());
//
//        PasswordPropertyCrypto passwordCrypto = new PasswordPropertyCrypto(encryptor, decryptor);
//        passwordCrypto.setPasswordProperties( passwordProperties );
//
//        findPropertiesFilesAndReencryptPasswords(configDirectory, passwordCrypto);
//
//        // Commit the new master password
//        new DefaultMasterPasswordFinder(ompCurFile).saveMasterPassword(newMasterPass.toCharArray());

        // Clean up
        deleteLockFiles(ompCurFile, ompOldFile, ompNewFile);
        FileUtils.deleteFileSafely(ompNewFile.getAbsolutePath());

        // we'll back up the .old file, in case the admin needs to recover something by hand someday
        ompOldFile.renameTo(new File(ompOldFile.getParent(), ompOldFile.getName() + "-" + System.currentTimeMillis()));

        System.out.println("Master passphrase changed successfully.");
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

            if ( !pattern.matcher(password).matches() ) {
                System.out.println( "Master passphrase should be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters long.\n" );
                password = null;
            }
        }

        return password;
    }
}
