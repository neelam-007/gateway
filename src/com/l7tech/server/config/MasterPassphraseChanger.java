package com.l7tech.server.config;

import com.l7tech.common.security.DefaultMasterPasswordFinder;
import com.l7tech.common.security.MasterPasswordManager;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.FileUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.ui.console.ConsoleWizardUtils;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Prompts the user to change the master passphrase.
 */
public class MasterPassphraseChanger {
    protected static final Logger logger = Logger.getLogger(MasterPassphraseChanger.class.getName());
    private static final String DEFAULT_MASTER_PASSPHRASE = "7layer";
    private static final String EOL = System.getProperty("line.separator");
    private static final String HEADER_SELECT_PARTITION = "-- Select The Partition To Configure --" + EOL;
    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 128;

    private final ConsoleWizardUtils wizardUtils;

    private MasterPassphraseChanger(InputStream in, PrintStream out) {
        wizardUtils = ConsoleWizardUtils.getInstance(in, out);
    }

    public static void main(String[] args) {
        try {
            LogManager.getLogManager().getLogger("").setLevel(Level.SEVERE);
            new MasterPassphraseChanger(System.in, System.out).run();
        } catch (Throwable e) {
            String msg = "Unable to change master passphrase: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, e);
            System.err.println(msg);
            System.exit(1);
        }
    }

    private boolean isMatchingMasterPassphrase(String currentObfuscated, String candidatePlaintext) throws IOException {
        long currentSalt = DefaultMasterPasswordFinder.getSalt(currentObfuscated);
        String candidateObfuscated = DefaultMasterPasswordFinder.obfuscate(candidatePlaintext, currentSalt);
        return currentObfuscated.equals(candidateObfuscated);
    }

    private void run() throws IOException, SAXException {
        PartitionInformation partition = choosePartition();
        OSSpecificFunctions osFunctions = OSDetector.getOSSpecificFunctions(partition.getPartitionId());

        try {
            if (osFunctions.isPartitionRunning(partition.getPartitionId())) {
                wizardUtils.printText(EOL + "Unable to continue -- this partition is currently running." + EOL);
                System.exit(2);
            }
        } catch (OSSpecificFunctions.OsSpecificFunctionUnavailableException e) {
            wizardUtils.printText(EOL + "Please ensure that the partition '" + partition.getPartitionId() + "' is stopped before continuing." + EOL);
            wizardUtils.printText("Press Enter to continue");
            wizardUtils.readLine();
        }

        File ompCurFile = osFunctions.getMasterPasswordFile();
        if (ompCurFile.exists()) {
            String ompCurStr = new String(HexUtils.slurpFile(ompCurFile), "UTF-8").trim();
            if (!isMatchingMasterPassphrase(ompCurStr, DEFAULT_MASTER_PASSPHRASE)) {
                String curMasterPass = promptForPassword("Enter the existing master passphrase ('quit' to quit): ");
                if (!isMatchingMasterPassphrase(ompCurStr, curMasterPass)) {
                    wizardUtils.printText(EOL + "Entered passphrase does not match the current master passphrase." + EOL);
                    System.exit(3);                    
                }
            }
        }


        String newMasterPass;
        String confirm;
        boolean matched = false;
        do {
            newMasterPass = promptForPassword("Enter the new master passphrase (" + MIN_LENGTH + " - " + MAX_LENGTH + " characters, 'quit' to quit): ");
            confirm = promptForPassword("Confirm new master passphrase ('quit' to quit): ");
            matched = confirm.equals(newMasterPass);
            if (!matched)
                wizardUtils.printText("The passphrases do not match." + EOL);
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
        new DefaultMasterPasswordFinder(ompNewFile).saveMasterPassword(newMasterPass.toCharArray());

        final MasterPasswordManager decryptor = ompCurFile.exists() ? new MasterPasswordManager(new DefaultMasterPasswordFinder(ompOldFile).findMasterPassword()) : null;
        final MasterPasswordManager encryptor = new MasterPasswordManager(newMasterPass.toCharArray());

        PasswordPropertyCrypto passwordCrypto = new PasswordPropertyCrypto(encryptor, decryptor);
        passwordCrypto.setPasswordProperties(osFunctions.getPasswordPropertyCrypto().getPasswordProperties());

        findPropertiesFilesAndReencryptPasswords(partition, passwordCrypto);

        // Commit the new master password
        new DefaultMasterPasswordFinder(ompCurFile).saveMasterPassword(newMasterPass.toCharArray());

        // Clean up
        deleteLockFiles(ompCurFile, ompOldFile, ompNewFile);
        FileUtils.deleteFileSafely(ompNewFile.getAbsolutePath());

        // we'll back up the .old file, in case the admin needs to recover something by hand someday
        ompOldFile.renameTo(new File(ompOldFile.getParent(), ompOldFile.getName() + "-" + System.currentTimeMillis()));

        wizardUtils.printText(EOL + "Master passphrase changed successfully." + EOL);
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
     * @param partition  the partition whose properties files to reencrypt.
     * @param passwordCrypto   object that decrypts and reencrypts passwords.  Required.
     * @throws IOException    on error reading or writing a file
     * @throws SAXException   on error parsing or reserializing an XML config file
     */
    private void findPropertiesFilesAndReencryptPasswords(PartitionInformation partition, PasswordPropertyCrypto passwordCrypto) throws IOException, SAXException {
        recursivelyFindPropertiesFilesAndReencryptPasswords(new File(partition.getOSSpecificFunctions().getConfigurationBase()), passwordCrypto);
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

    private PartitionInformation choosePartition() throws IOException {
        List<String> promptList =  new ArrayList<String>();

        promptList.add(HEADER_SELECT_PARTITION);
        String defaultValue = "1";
        int index = 1;

        List<String> partitions = new ArrayList<String>(PartitionManager.getInstance().getPartitionNames());

        for (String partitionName : partitions)
            promptList.add(String.valueOf(index++) + ") " + partitionName + EOL);

        promptList.add("Please make a selection: [" + defaultValue + "]");

        List<String> allowedEntries = new ArrayList<String>();

        for (int i = 1; i < index; i++) {
            allowedEntries.add(String.valueOf(i));
        }

        String input = promptForMenuOption(promptList, defaultValue, allowedEntries.toArray(new String[0]));
        PartitionInformation pinfo = null;
        if (input != null) {
            String whichPartition = partitions.get(Integer.parseInt(input) -1);
            pinfo  = PartitionManager.getInstance().getPartition(whichPartition);
        }
        return pinfo;
    }

    private String promptForPassword(String prompt) throws IOException {
        String[] promptLines = new String[] { prompt };
        Pattern pattern = Pattern.compile("^.{" + MIN_LENGTH + "," + MAX_LENGTH + "}$", Pattern.DOTALL);
        try {
            return wizardUtils.getSecretData(promptLines, null, false, pattern, "Master passphrase should be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters long.");
        } catch (WizardNavigationException e) {
            throw new CausedIOException(e); // can't happen
        }
    }

    private String promptForMenuOption(List<String> promptLines, String defaultValue, String[] allowedEntries) throws IOException {
        if (promptLines == null) return "";
        try {
            return wizardUtils.getData(promptLines.toArray(new String[]{}), defaultValue, false, allowedEntries, null);
        } catch (WizardNavigationException e) {
            throw new CausedIOException(e); // can't happen
        }
    }
}
