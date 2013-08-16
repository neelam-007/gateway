package com.l7tech.gateway.config.manager;

import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.util.*;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.Console;
import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Prompts the user to change the cluster passphrase.
 * <p/>
 * <p>This will update the encrypted shared key in the DB and also the
 * node.properties file for the new cluster passphrase.</p>
 */
public class ClusterPassphraseChanger {
    private static final Logger logger = Logger.getLogger(ClusterPassphraseChanger.class.getName());
    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 128;
    //    private static final String CIPHER = "PBEWithSHA1AndDESede";
    private static final String DEFAULT_CONFIG_PATH = "../node/{0}/etc/conf";
    private static final String DEFAULT_NODE = "default";

    private static final String NODE = SyspropUtil.getString("com.l7tech.config.node", DEFAULT_NODE);
    private static final String CONFIG_PATH = SyspropUtil.getString("com.l7tech.config.path", DEFAULT_CONFIG_PATH);

    public static void main(final String[] args) {
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties");
        try {
            new ClusterPassphraseChanger().run(MessageFormat.format(CONFIG_PATH, NODE));
        } catch (Throwable e) {
            String msg = "Unable to change cluster passphrase: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, e);
            System.err.println(msg);
            System.exit(1);
        }
    }

    private void exitOnQuit(final String perhapsQuit) {
        if ("quit".equals(perhapsQuit)) {
            System.exit(1);
        }
    }

    private void run(final String configurationDirPath) throws IOException, SAXException, NodeConfigurationManager.NodeConfigurationException {
        File configDirectory = new File(configurationDirPath);
        File ompFile = new File(configDirectory, "omp.dat");
        if (!ompFile.exists()) {
            throw new FileNotFoundException("Node is not configured (missing obfuscated master password).");
        }

        DatabaseConfig config;
        File nodePropsFile = new File(configDirectory, "node.properties");
        if (nodePropsFile.exists()) {
            NodeConfig nodeConfig = NodeConfigurationManager.loadNodeConfig(NODE, true);
            config = nodeConfig.getDatabase(DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER);
            if (config == null) {
                throw new CausedIOException("Database configuration not found.");
            }
        } else {
            throw new FileNotFoundException("Node is not configured.");
        }

        // load and decrypt DB connnection info
        final MasterPasswordManager masterPasswordManager =
                new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile));
        config.setNodePassword(new String(masterPasswordManager.decryptPasswordIfEncrypted(config.getNodePassword())));


        logger.info("Using database host '" + config.getHost() + "'.");
        logger.info("Using database port '" + config.getPort() + "'.");
        logger.info("Using database name '" + config.getName() + "'.");
        logger.info("Using database user '" + config.getNodeUsername() + "'.");

        // Get existing pass and validate against DB
        ClusterPassphraseManager cpm = new ClusterPassphraseManager(config);
        byte[] sharedKey = null;
        while (sharedKey == null) {
            String existingClusterPass = promptForPassword("Enter the cluster passphrase, or 'quit' to quit): ");
            sharedKey = cpm.getDecryptedSharedKey(existingClusterPass);
            if (sharedKey == null) System.out.println("Incorrect cluster passphrase, please try again.");
        }

        // Get new pass
        String newClusterPass;
        boolean matched;
        do {
            newClusterPass = promptForPassword("Enter the new cluster passphrase (" + MIN_LENGTH + " - " + MAX_LENGTH + " characters, 'quit' to quit): ");
            String confirm = promptForPassword("Confirm new cluster passphrase ('quit' to quit): ");
            matched = confirm.equals(newClusterPass);
            if (!matched)
                System.out.println("The passphrases do not match.");
        } while (!matched);

        cpm.saveAndEncryptSharedKey(newClusterPass);

        // Update properties file with new passphrase
        if (nodePropsFile.exists()) {
            NodeConfigurationManager.configureGatewayNode(NODE, null, newClusterPass, false, Option.<DatabaseConfig>none(), Option.<DatabaseConfig>none());
        }

        System.out.println("Cluster passphrase changed successfully.");
    }

    private String promptForPassword(final String prompt) throws IOException {
        String password = null;

        Console console = System.console();
        Pattern pattern = Pattern.compile("^.{" + MIN_LENGTH + "," + MAX_LENGTH + "}$", Pattern.DOTALL);
        while (password == null) {
            System.out.println(prompt);
            password = TextUtils.string( console.readPassword() );

            exitOnQuit(password);

            if (!pattern.matcher(password).matches()) {
                System.out.println("Cluster passphrase should be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters long.\n");
                password = null;
            }
        }

        return password;
    }
}
