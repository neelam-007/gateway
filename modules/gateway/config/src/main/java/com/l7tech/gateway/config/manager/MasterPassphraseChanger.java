package com.l7tech.gateway.config.manager;

import com.l7tech.common.io.CertUtils;
import com.l7tech.util.*;
import org.xml.sax.SAXException;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Prompts the user to change the master passphrase.
 */
public class MasterPassphraseChanger {
    private static final Logger logger = Logger.getLogger(MasterPassphraseChanger.class.getName());

    public static final int EXIT_STATUS_USAGE = 2;

    static final String COMMAND_KMP = "kmp";
    static final String SC_KMP_MIGRATE_FROM_KMP_TO_OMP = "migrateFromKmpToOmp";
    static final String SC_KMP_GENERATE_ALL = "generateAll";
    static final String SC_KMP_LIST_KEYSTORE_CONTENTS = "listKeystoreContents";
    static final String ARG_SC_KMP_GEN_BASE_ON_EXISTING_KMP = "baseOnExistingKmp";
    static final String ARG_SC_KMP_LIST_NOFILE = "ignoreKmpFile";

    private static final byte[] DEFAULT_MASTER_PASSPHRASE = "7layer".getBytes(Charsets.UTF8);
    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 128;
    private static final String CONFIG_PATH = "../node/default/etc/conf";
    static final String[] PASSWORD_PROPERTIES = new String[]{ "node.cluster.pass", "node.db.config.main.pass" };

    private final String configurationDirPath;
    private final String command;
    private final String subcommand;
    private final List<String> arguments;
    private final String[] passwordProperties;
    PrintStream out = System.out;

    public MasterPassphraseChanger(String configurationDirPath, String[] passwordProperties, String command, String subcommand, List<String> arguments) {
        this.configurationDirPath = configurationDirPath;
        this.passwordProperties = passwordProperties;
        this.command = command;
        this.subcommand = subcommand;
        this.arguments = arguments == null ? Collections.<String>emptyList() : arguments;
    }

    public static void main(String[] argv) {
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties", false, true);
        try {
            LinkedList<String> args = new LinkedList<String>(Arrays.asList(argv));

            if (args.isEmpty()) {
                // No args -- run in interactive mode
                new MasterPassphraseChanger(CONFIG_PATH, PASSWORD_PROPERTIES,  null, null, null).run();
            } else {
                String command = args.removeFirst();
                String subcommand = args.removeFirst();

                // Args provided -- execute requested command and return
                new MasterPassphraseChanger(CONFIG_PATH, PASSWORD_PROPERTIES, command, subcommand, args).run();
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
        fatal(p + "Usage: MasterPassphraseChanger [kmp <generateAll <configProfileName>|migrateFromKmpToOmp|listKeystoreContents]", null, EXIT_STATUS_USAGE);
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
        File kmpFile = KeyStorePrivateKeyMasterPasswordUtil.findPropertiesFile(configDirectory);
        if (kmpFile.exists()) {
            throw new RuntimeException("The Gateway is currently configured to use a keystore-protected master passphrase.");
        }

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
        if (COMMAND_KMP.equals(command)) {
            runKmpSubcommand(subcommand);
        } else {
            usage("Unknown command: " + command);
        }
    }

    private void runKmpSubcommand(String subcommand) throws Exception {
        if (SC_KMP_GENERATE_ALL.equals(subcommand)) {
            runKmpGenerateAll();
        } else if (SC_KMP_MIGRATE_FROM_KMP_TO_OMP.equals(subcommand)) {
            runKmpMigrateFromKmpToOmp();
        } else if (SC_KMP_LIST_KEYSTORE_CONTENTS.equals(subcommand)) {
            runKmpListKeystoreContents();
        } else {
            usage("Unknown kmp subcommand: " + subcommand);
        }
    }

    private void runKmpListKeystoreContents() throws Exception {
        final File configDirectory = new File(configurationDirPath);
        final KeyStore ks;

        LinkedList<String> args = new LinkedList<String>(arguments);
        if (args.size() > 0) {
            String s = args.removeFirst();
            if (!ARG_SC_KMP_LIST_NOFILE.equals(s))
                usage("Use argument \"" + ARG_SC_KMP_LIST_NOFILE + "\" to specify kmp properties on command line");

            Properties props = new Properties();
            for (String arg : args) {
                String[] keyval = arg.split("\\s*=\\s*");
                if (2 != keyval.length)
                    usage("invalid kmp property format; should be propname=propvalue: " + arg);
                String key = keyval[0];
                String val = keyval[1];
                props.setProperty(key, val);
            }
            ks = new KeyStorePrivateKeyMasterPasswordUtil(configDirectory, props).getExistingKeyStore();
        } else {
            ks = new KeyStorePrivateKeyMasterPasswordUtil(configDirectory).getExistingKeyStore();
        }

        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            String keyLabel = ks.isKeyEntry(alias) ? "Private Key: " : "";
            String certLabel = ks.isCertificateEntry(alias) ? "Trusted Cert: " : "";

            String keyType = "<Unknown key type>";
            String dn = "<No Certificate>";
            Certificate cert = ks.getCertificate(alias);
            if (cert instanceof X509Certificate) {
                X509Certificate x509 = (X509Certificate) cert;
                dn = x509.getSubjectDN().getName();

                PublicKey publicKey = x509.getPublicKey();
                if (publicKey instanceof RSAPublicKey) {
                    RSAPublicKey key = (RSAPublicKey) publicKey;
                    int bits = CertUtils.getRsaKeyBits(key);
                    keyType = bits + " bit RSA";
                }
            }

            out.println(keyLabel + certLabel + alias + ", " + keyType + ", " + dn);
        }
    }

    private void runKmpGenerateAll() throws IOException, GeneralSecurityException, SAXException {
        final String needProfileMsg = "An additional non-empty argument for config profile name is required with " + COMMAND_KMP + " " + SC_KMP_GENERATE_ALL + " (eg, ncipher.sworld.rsa)";
        if (arguments.isEmpty())
            usage(needProfileMsg);
        String configProfileName = arguments.get(0);
        if (configProfileName == null || configProfileName.trim().length() < 1)
            usage(needProfileMsg);

        final File configDirectory = new File(configurationDirPath);
        final File ompFile = new File(configDirectory, "omp.dat" );
        final File kmpFile = KeyStorePrivateKeyMasterPasswordUtil.findPropertiesFile(ompFile);

        // Decryptor to decrypt passwords encrypted with old obfuscated master passphrase
        final MasterPasswordManager decryptor = ompFile.exists() && ompFile.length() > 0 ? new MasterPasswordManager(new ObfuscatedFileMasterPasswordFinder(ompFile)) : null;

        boolean baseOnExistingKmp = ARG_SC_KMP_GEN_BASE_ON_EXISTING_KMP.equals(configProfileName);
        boolean haveKmp = kmpFile.exists();
        if (baseOnExistingKmp) {
            if (!haveKmp)
                throw new FileNotFoundException("No existing config file to use: " + kmpFile);
            new KeyStorePrivateKeyMasterPasswordUtil(configDirectory).generateNewMasterPassword();
        } else {
            if (haveKmp)
                usage("Config file already exists.  Use " + ARG_SC_KMP_GEN_BASE_ON_EXISTING_KMP + " argument to base the new config on it: " + kmpFile);
            Properties properties = new Properties();
            properties.setProperty(KeyStorePrivateKeyMasterPasswordUtil.PROP_PROFILE, configProfileName);
            new KeyStorePrivateKeyMasterPasswordUtil(configDirectory, properties).generateNewMasterPassword();
        }


        // Encryptor to re-encrypt passwords with new keystore-protected master passphrase
        final MasterPasswordManager encryptor = new MasterPasswordManager(new KeyStorePrivateKeyMasterPasswordFinder(kmpFile));

        PasswordPropertyCrypto passwordCrypto = new PasswordPropertyCrypto(encryptor, decryptor);
        passwordCrypto.setPasswordProperties( passwordProperties );
        findPropertiesFilesAndReencryptPasswords(configDirectory, passwordCrypto);

        if (ompFile.exists())
            FileUtils.save(new byte[0], ompFile); // Truncate omp but leave in place for compatibility

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
        kmpFile.delete();
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
