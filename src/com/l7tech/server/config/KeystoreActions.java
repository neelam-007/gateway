package com.l7tech.server.config;

import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.ProcResult;
import com.l7tech.common.util.ProcUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.beans.KeystoreConfigBean;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.config.exceptions.KeystoreActionsException;
import com.l7tech.server.config.exceptions.WrongKeystorePasswordException;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Provider;
import java.security.Security;
import java.sql.*;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: May 22, 2007
 * Time: 3:03:50 PM
 */
public class KeystoreActions {
    private static final Logger logger = Logger.getLogger(KeystoreActions.class.getName());
    private OSSpecificFunctions osFunctions;


    public KeystoreActions(OSSpecificFunctions osFunctions) {
        this.osFunctions = osFunctions;
    }

    private byte[] spawnSubProcessAndGetSharedKey(String ksType, char[] ksPassword, File keystoreFile) throws KeystoreActionsException, IOException {
        DBInformation dbInfo = SharedWizardInfo.getInstance().getDbinfo();
        String launcher = osFunctions.getConfigWizardLauncher();
        String ksPath = keystoreFile.getAbsolutePath();
        String[] args = ProcUtils.args(
                ConfigurationWizardLauncher.EXPORT_SHARED_KEY,
                new String(ksPassword),
                ksType,
                ksPath,
                dbInfo.getHostname(),
                dbInfo.getDbName(),
                dbInfo.getUsername(),
                dbInfo.getPassword());

        ProcResult result = ProcUtils.exec(
                null,
                new File(launcher),
                args, null, true);

        byte[] sharedKey = null;
        if (result.getExitStatus() == 0) {
            sharedKey = result.getOutput();
        } else if (result.getExitStatus() == 1) {
            throw new WrongKeystorePasswordException(MessageFormat.format("Attempt to retrieve the cluster shared key using the provided keystore type and password failed. - {0}", new String(result.getOutput())));
        } else {
            throw new KeystoreActionsException(MessageFormat.format("Attempting to retrieve the cluster shared key resulted in the following error code and message: {0}, {1}", result.getExitStatus(), new String(result.getOutput())));
        }
        return sharedKey;
    }

    public void prepareJvmForNewKeystoreType(KeystoreType ksType) throws IllegalAccessException, InstantiationException, FileNotFoundException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException {
        Provider[] currentProviders = Security.getProviders();
        for (Provider provider : currentProviders) {
            Security.removeProvider(provider.getName());
        }

       switch (ksType) {
            case DEFAULT_KEYSTORE_NAME:
                prepareProviders(KeystoreConfigBean.DEFAULT_SECURITY_PROVIDERS);
                System.setProperty(JceProvider.ENGINE_PROPERTY, JceProvider.BC_ENGINE);
                break;
            case SCA6000_KEYSTORE_NAME:
                prepareProviders(KeystoreConfigBean.HSM_SECURITY_PROVIDERS);
                System.setProperty(JceProvider.ENGINE_PROPERTY, JceProvider.PKCS11_ENGINE);
                break;
            case LUNA_KEYSTORE_NAME:
                prepareLunaProviders();
                System.setProperty(JceProvider.ENGINE_PROPERTY, JceProvider.LUNA_ENGINE);
                break;
        }
    }

    private void prepareProviders(String[] securityProviders) throws IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        for (String providerName : securityProviders) {
            try {
                Provider p = null;
                if (providerName.contains(" ")) {
                    String[] splitz = providerName.split(" ");
                    logger.info("Adding " + splitz[0]);
                    Class providerClass = Class.forName(splitz[0]);
                    Constructor ctor = providerClass.getConstructor(String.class);
                    p = (Provider) ctor.newInstance(splitz[1]);
                } else {
                    p = (Provider) Class.forName(providerName).newInstance();
                }
                Security.addProvider(p);
            } catch (ClassNotFoundException e) {
                logger.severe("Could not instantiate the " + providerName + " security provider. Cannot proceed");
                throw e;
            } catch (NoSuchMethodException e) {
                logger.severe("Could not instantiate the " + providerName + " security provider. Cannot proceed");
                throw e;
            } catch (InvocationTargetException e) {
                logger.severe("Could not instantiate the " + providerName + " security provider. Cannot proceed");
                throw e;
            }
        }
    }

    private void prepareLunaProviders() throws FileNotFoundException, IllegalAccessException, InstantiationException {
        File classDir = new File(osFunctions.getPathToJreLibExt());
        if (!classDir.exists()) {
            throw new FileNotFoundException("Could not locate the directory: \"" + classDir + "\"");
        }

        File[] lunaJars = classDir.listFiles(new FilenameFilter() {
            public boolean accept(File file, String s) {
                return  s.toUpperCase().startsWith("LUNA") &&
                        s.toUpperCase().endsWith(".JAR");
            }
        });

        if (lunaJars == null) {
            throw new FileNotFoundException("Could not locate the Luna jar files in the specified directory: \"" + classDir + "\"");
        }

        URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
        Class sysclass = URLClassLoader.class;
        //this is a necessary hack to be able to hotplug some jars into the classloaders classpath.
        // On linux, this happens already, but  not on windows

        try {
            Class[] parameters = new Class[]{URL.class};
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            for (File lunaJar : lunaJars) {
                URL url = lunaJar.toURI().toURL();
                method.invoke(sysloader, new Object[]{url});
            }
            Class lunaJCAClass;
            String lunaJCAClassName = "com.chrysalisits.crypto.LunaJCAProvider";
            Class lunaJCEClass;
            String lunaJCEClassName = "com.chrysalisits.cryptox.LunaJCEProvider";

            try {
                lunaJCAClass = sysloader.loadClass(lunaJCAClassName);
                Object lunaJCA = lunaJCAClass.newInstance();
                Security.addProvider((Provider) lunaJCA);

                lunaJCEClass = sysloader.loadClass(lunaJCEClassName);
                Object lunaJCE = lunaJCEClass.newInstance();
                Security.addProvider((Provider) lunaJCE);

            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        Security.addProvider(new sun.security.provider.Sun());
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
    }

    public byte[] getSharedKey(KeystoreActionsListener listener) throws KeystoreActionsException {

        String ksType = null;
        char[] ksPassword = null;
        String ksDir = null;
        String ksFilename = null;

        byte[] sharedKey = null;
        try {
            Map<String, String> props = PropertyHelper.getProperties(osFunctions.getKeyStorePropertiesFile(), new String[]{
                    KeyStoreConstants.PROP_KS_TYPE,
                    KeyStoreConstants.PROP_SSL_KS_PASS,
                    KeyStoreConstants.PROP_KEYSTORE_DIR,
                    KeyStoreConstants.PROP_SSL_KEYSTORE_FILE
            });
            ksType = props.get(KeyStoreConstants.PROP_KS_TYPE);
            ksPassword = props.get(KeyStoreConstants.PROP_SSL_KS_PASS).toCharArray();
            ksDir = props.get(KeyStoreConstants.PROP_KEYSTORE_DIR);
            ksFilename = props.get(KeyStoreConstants.PROP_SSL_KEYSTORE_FILE);
        } catch (IOException e) {
            logger.warning("Keystore configuration could not be loaded. Assuming defaults");
        } finally {
            if (StringUtils.isEmpty(ksType)) ksType = "PCKS12";
            if (StringUtils.isEmpty(ksDir)) ksDir = osFunctions.getKeystoreDir();
            if (StringUtils.isEmpty(ksFilename)) ksFilename = KeyStoreConstants.SSL_KEYSTORE_FILE;
        }

        File existingKeystoreFile = new File(ksDir, ksFilename);
        if (!existingKeystoreFile.exists()) {
            logger.info(MessageFormat.format("No existing keystore found. No need to backup shared key. (tried {0})", existingKeystoreFile.getAbsolutePath()));
        } else {
            String msg = MessageFormat.format("An existing keystore was found at {0}. Attempting to back up shared key.", existingKeystoreFile.getAbsolutePath());
            if (listener != null) listener.printKeystoreInfoMessage(msg);
            logger.info(msg);
            sharedKey = getExistingSharedKey(new File(ksDir, ksFilename), ksType, ksPassword, true, listener);
            if (sharedKey != null) {
                if (sharedKey.length == 0) {
                    logger.info("No shared key was found in the database. No need to back it up.");
                } else {
                    logger.info("Found a shared key in the database. Backing it up.");
                }
            }
        }

        return sharedKey;
    }

    private byte[] getExistingSharedKey(File keystoreFile, String ksType, char[] ksPassword, boolean tryAgain, KeystoreActionsListener listener) throws KeystoreActionsException {
        byte[] sharedKey = null;
        try {
            sharedKey = retrieveExistingSharedKeyBytes(ksType, ksPassword, keystoreFile, tryAgain, listener);
        } catch (IOException e) {
            logger.severe("Error loading existing keystore and retrieving cluster shared key: " + e.getMessage());
            throw new KeystoreActionsException("Error loading existing keystore and retrieving cluster shared key: " + e.getMessage());
        } catch (KeystoreActionsException e) {
            logger.severe("Error loading existing keystore and retrieving cluster shared key: " + e.getMessage());
            throw new KeystoreActionsException("Error loading existing keystore and retrieving cluster shared key: " + e.getMessage());
        }
        return sharedKey;
    }

    private byte[] retrieveExistingSharedKeyBytes(String ksType, char[] ksPassword, File keystoreFile, boolean shouldTryAgain, KeystoreActionsListener listener) throws KeystoreActionsException, IOException {
        if (ksType == null || ksPassword == null) {
            if (listener != null) {
                List<String> answers = listener.promptForKeystoreTypeAndPassword();
                ksType = answers.get(1);
                ksPassword = answers.get(0).toCharArray();
            }
        }

        byte[] sharedKey = null;
        if (ksType != null || ksPassword != null) {
            try {
                sharedKey = spawnSubProcessAndGetSharedKey(ksType, ksPassword, keystoreFile);
            } catch (WrongKeystorePasswordException passwdEx) {
                if (shouldTryAgain) {
                    if (listener != null) {
                        return retrieveExistingSharedKeyBytes(null, null, keystoreFile, false, listener);
                    } else {
                        throw new WrongKeystorePasswordException("Could not load the keystore with the given password and type");
                    }
                } else {
                    throw new WrongKeystorePasswordException("Could not load the keystore with the given password and type");
                }
            }
        }
        return sharedKey;
    }

    public void backupHsm() {
    }

    public void restoreHsm() {

    }

    public void probeUSBBackupDevice() throws KeystoreActionsException {
        String prober = osFunctions.getSsgInstallRoot() + KeystoreConfigBean.MASTERKEY_MANAGE_SCRIPT;
        try {
            ProcResult result = ProcUtils.exec(null, new File(prober), ProcUtils.args("probe"), null, true);
            if (result.getExitStatus() == 0) {
                //all is well, USB stick is there
                logger.info("Detected a supported backup storage device.");
            } else {
                String message = "A supported backup storage device was not found.";
                logger.warning(MessageFormat.format(message + " Return code = {0}, Message={1}", result.getExitStatus(), new String(result.getOutput())));
                throw new KeystoreActionsException(message);
            }
        } catch (IOException e) {
            throw new KeystoreActionsException("An error occurred while attempting to find a supported backup storage device: " + e.getMessage());
        }
    }

    public byte[] getKeydataFromDatabase(DBInformation dbinfo) throws KeystoreActionsException {
        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;

        byte[] databytes = null;
        try {
            DBActions dba = new DBActions(osFunctions);
            connection = dba.getConnection(dbinfo);
            stmt = connection.createStatement();
            rs = stmt.executeQuery("select databytes from keystore_file where objectid=1 and name=\"HSM\"");
            databytes = null;
            while (rs.next()) {
                databytes = rs.getBytes(1);
            }
            if (databytes == null)
                throw new KeystoreActionsException("Could not fetch the keystore information from the database. No keydata was found in the database");
            logger.info("Got the keystore info from the db: " + new String(databytes));
        } catch (SQLException e) {
            throw new KeystoreActionsException("Could not fetch the keystore information from the database. " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new KeystoreActionsException("Could not fetch the keystore information from the database. " + e.getMessage());
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(connection);
            ResourceUtils.closeQuietly(stmt);
        }
        return databytes;
    }

    private int getOriginalVersion(Connection connection) throws SQLException {
        int originalVersion = -1;
        String getVersionSql = new String("Select version from keystore_file where objectid=1 and name=\"HSM\"");

        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(getVersionSql);
            while(rs.next()) {
                originalVersion = rs.getInt("version");
            }
        } finally {
            ResourceUtils.closeQuietly(stmt);
            if (originalVersion == -1) {
                logger.warning("Could not find an existing version for the HSM keystore in the database. Defaulting to 0");
                originalVersion = 0;
            } else {
                logger.info("Found an existing version for the HSM keystore in the database [" + originalVersion + "]");
            }
        }
        return originalVersion;
    }

    public void putKeydataInDatabase(DBInformation dbInfo, byte[] keyData) throws KeystoreActionsException {

        ByteArrayInputStream is = new ByteArrayInputStream(keyData);
        Connection conn = null;
        PreparedStatement preparedStmt = null;
        int originalVersion = -1;
        try {
            DBActions dba = new DBActions(osFunctions);

            conn = dba.getConnection(dbInfo);
            try {
                originalVersion = getOriginalVersion(conn);
            } catch (SQLException e) {
                throw new KeystoreActionsException("Could not determine the current version of the HSM keystore in the database: " + e.getMessage());
            }

            preparedStmt = conn.prepareStatement("update keystore_file set version=?, databytes=? where objectid=1 and name=\"HSM\"");
            preparedStmt.setInt(1, originalVersion+1);
            preparedStmt.setBinaryStream(2, is, keyData.length);
            preparedStmt.addBatch();
            preparedStmt.executeBatch();
            logger.info("succesfully inserted the HSM keystore information into the database.");
        } catch (ClassNotFoundException e) {
            throw new KeystoreActionsException("Error while inserting the HSM keystore informaiton into the database: " + e.getMessage());
        } catch (SQLException e) {
            throw new KeystoreActionsException("Error while inserting the HSM keystore informaiton into the database: " + e.getMessage());
        } finally {
            ResourceUtils.closeQuietly(preparedStmt);
            ResourceUtils.closeQuietly(conn);
        }
    }
}
