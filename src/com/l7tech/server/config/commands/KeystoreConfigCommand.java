package com.l7tech.server.config.commands;

import com.l7tech.common.security.prov.luna.LunaCmu;
import com.l7tech.common.util.FileUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.server.config.*;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.beans.KeystoreConfigBean;
import com.l7tech.server.util.MakeLunaCerts;
import com.l7tech.server.util.SetKeys;
import com.l7tech.server.partition.PartitionManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.ConfigurationException;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: megery
 */
public class KeystoreConfigCommand extends BaseConfigurationCommand {

    private static final Logger logger = Logger.getLogger(KeystoreConfigCommand.class.getName());

    private static final String BACKUP_FILE_NAME = "keystore_config_backups";

    private static final String CA_KEYSTORE_FILE = "ca.ks";
    private static final String SSL_KEYSTORE_FILE = "ssl.ks";
    private static final String CA_CERT_FILE = "ca.cer";
    private static final String SSL_CERT_FILE = "ssl.cer";

    private static final String PROPERTY_COMMENT = "This file was updated by the SSG configuration utility";

    private static final String XML_KSFILE = "keystoreFile";
    private static final String XML_KSPASS = "keystorePass";
    private static final String XML_KSTYPE = "keystoreType";
    private static final String XML_KSALIAS  = "keyAlias";

    private static final String LUNA_SYSTEM_CMU_PROPERTY = "lunaCmuPath";

    private static final String PROPKEY_SECURITY_PROVIDER = "security.provider";
    private static final String PROPKEY_JCEPROVIDER = "com.l7tech.common.security.jceProviderEngine";
    private static final String PROPERTY_LUNA_JCEPROVIDER_VALUE = "com.l7tech.common.security.prov.luna.LunaJceProviderEngine";

    private static final String[] LUNA_SECURITY_PROVIDERS =
            {
                "com.chrysalisits.crypto.LunaJCAProvider",
                "com.chrysalisits.cryptox.LunaJCEProvider",
                "sun.security.provider.Sun",
                "com.sun.net.ssl.internal.ssl.Provider"
            };

    private static final String[] DEFAULT_SECURITY_PROVIDERS =
            {
                "sun.security.provider.Sun",
                "sun.security.rsa.SunRsaSign",
                "com.sun.net.ssl.internal.ssl.Provider",
                "com.sun.crypto.provider.SunJCE",
                "sun.security.jgss.SunProvider",
                "com.sun.security.sasl.Provider"
            };

    private static final String PKCS11_CFG_FILE = "/ssg/pkcs11.cfg;";
    private static final String[] HSM_SECURITY_PROVIDERS =
            {
                "sun.security.pkcs11.SunPKCS11 " + PKCS11_CFG_FILE,
                "sun.security.provider.Sun",
                "com.sun.net.ssl.internal.ssl.Provider",
                "com.sun.crypto.provider.SunJCE"
            };

    public KeystoreConfigCommand(ConfigurationBean bean) {
        super(bean);
    }

    public boolean execute() {
        boolean success = true;
        KeystoreConfigBean ksBean = (KeystoreConfigBean) configBean;
        if (ksBean.isDoKeystoreConfig()) {
            KeystoreType ksType = ksBean.getKeyStoreType();
            try {
                 if (ksType == KeystoreType.DEFAULT_KEYSTORE_NAME) {
                    doDefaultKeyConfig(ksBean);
                    success = true;
                } else if (ksType == KeystoreType.LUNA_KEYSTORE_NAME) {
                    doLunaKeyConfig(ksBean);
                    success = true;
                } else if (ksType == KeystoreType.SCA6000_KEYSTORE_NAME) {
                     doHSMConfig(ksBean);
                     success = true;
                 }
            } catch (Exception e) {
                success = false;
            }
        } else {
            if (PartitionManager.getInstance().getActivePartition().isNewPartition()) {
                String partitionName = PartitionManager.getInstance().getActivePartition().getPartitionId();
                logger.warning("The \"" + partitionName + "\" partition has been created but no keystore has been specified.");
                logger.warning("The \"" + partitionName + "\" partition will not be able to start without a keystore");
            }
        }
        return success;
    }

    private void doHSMConfig(KeystoreConfigBean ksBean) throws Exception {
        char[] ksPassword = ksBean.getKsPassword();
        String ksDir = getOsFunctions().getKeystoreDir();
        File keystoreDir = new File(ksDir);
        if (!keystoreDir.exists() && !keystoreDir.mkdir()) {
            String msg = "Could not create the directory: \"" + ksDir + "\". Cannot generate keystores";
            logger.severe(msg);
            throw new IOException(msg);
        }

        File keystorePropertiesFile = new File(getOsFunctions().getKeyStorePropertiesFile());
        File tomcatServerConfigFile = new File(getOsFunctions().getTomcatServerConfig());
        File caKeyStoreFile = new File( ksDir + CA_KEYSTORE_FILE);
        File sslKeyStoreFile = new File(ksDir + SSL_KEYSTORE_FILE);
        File caCertFile = new File(ksDir + CA_CERT_FILE);
        File sslCertFile = new File(ksDir + SSL_CERT_FILE);
        File javaSecFile = new File(getOsFunctions().getPathToJavaSecurityFile());
        File systemPropertiesFile = new File(getOsFunctions().getSsgSystemPropertiesFile());

        //TODO make this location independant in case the ssg isn't installed at /ssg
        File pkcs11ConfigFile = new File(PKCS11_CFG_FILE);

        File newJavaSecFile  = new File(getOsFunctions().getPathToJavaSecurityFile() + ".new");

        File[] files = new File[]
        {
            javaSecFile,
            pkcs11ConfigFile,
            keystorePropertiesFile,
            tomcatServerConfigFile,
            caKeyStoreFile,
            sslKeyStoreFile,
            caCertFile,
            sslCertFile,
            systemPropertiesFile
        };

        backupFiles(files, BACKUP_FILE_NAME);
        if (ksBean.isInitializeHSM()) {
            logger.info("Initializing HSM");
            try {
                startSCA();
                initializeSCA();

//                prepareJvm(KeystoreType.SCA6000_KEYSTORE_NAME);
//                updateJavaSecurity(ksBean, javaSecFile, newJavaSecFile, HSM_SECURITY_PROVIDERS);
//                writePKCS11Config(pkcs11ConfigFile);
//                makeHSMKeys(ksBean, ksDir, ksPassword);
//                updateKeystoreProperties(keystorePropertiesFile, ksPassword);
//                updateServerConfig(tomcatServerConfigFile, sslKeyStoreFile.getAbsolutePath(), ksPassword);
//                updateSystemPropertiesFile(ksBean, systemPropertiesFile);
            } catch (Exception e) {
                logger.severe("problem initializing the HSM - skipping HSM configuration");
                logger.severe(e.getMessage());
                throw e;
            }
        } else {
            logger.info("Restoring HSM Backup");
        }
    }

    private void initializeSCA() {
        if (getOsFunctions().isUnix()) {
            String zeroHSM = "scadiag -z mca0";
            logger.info("Execute \"" + zeroHSM + "\"");

            String initializeScript = getOsFunctions().getSsgInstallRoot() + "bin/" + "initialize-hsm.expect <keystorepassword>";
            logger.info("Execute \"" + initializeScript + "\"");
        }
    }

    private void startSCA() {
        if (getOsFunctions().isUnix()) {
            String startScaCommand = null;

            if (OSDetector.isLinux()) {
                startScaCommand = "service sca start";
            } else {
                startScaCommand = "scvadmin start sca";
            }
            logger.info("Starting the SCA.");
            logger.info("Execute \"" + startScaCommand + "\"");
        }
    }

    private void makeHSMKeys(KeystoreConfigBean ksBean, String ksDir, char[] ksPassword) {
    }

    private void writePKCS11Config(File pkcs11ConfigFile) {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(pkcs11ConfigFile);
            pw.println("name=SCA6000");
            pw.println("library=/usr/local/lib/pkcs11/PKCS11_API.so64");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            ResourceUtils.closeQuietly(pw);
        }
    }

    private void prepareJvm(KeystoreType ksType) throws IllegalAccessException, InstantiationException, FileNotFoundException, ClassNotFoundException {
        Provider[] currentProviders = Security.getProviders();
        for (Provider provider : currentProviders) {
            Security.removeProvider(provider.getName());
        }

        if(ksType == KeystoreType.SCA6000_KEYSTORE_NAME) {


        } else if (ksType == KeystoreType.DEFAULT_KEYSTORE_NAME || ksType == KeystoreType.SCA6000_KEYSTORE_NAME) {
            String[] whichProviders = null;
            if (ksType == KeystoreType.DEFAULT_KEYSTORE_NAME)
                whichProviders = DEFAULT_SECURITY_PROVIDERS;
            else
                whichProviders = HSM_SECURITY_PROVIDERS;

            for (String providerName : whichProviders) {
                try {
                    Security.addProvider((Provider) Class.forName(providerName).newInstance());
                } catch (ClassNotFoundException e) {
                    logger.severe("Could not instantiate the " + providerName + " security provider. Cannot proceed");
                    throw e;
                }
            }
        } else if (ksType == KeystoreType.LUNA_KEYSTORE_NAME) {
            File classDir = new File(getOsFunctions().getPathToJreLibExt());
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
    }

    private void doDefaultKeyConfig(KeystoreConfigBean ksBean) throws Exception {
        char[] ksPassword = ksBean.getKsPassword();
        boolean doBothKeys = ksBean.isDoBothKeys();

        String ksDir = getOsFunctions().getKeystoreDir();
        File keystoreDir = new File(ksDir);
        if (!keystoreDir.exists() && !keystoreDir.mkdir()) {
            String msg = "Could not create the directory: \"" + ksDir + "\". Cannot generate keystores";
            logger.severe(msg);
            throw new IOException(msg);
        }

        File keystorePropertiesFile = new File(getOsFunctions().getKeyStorePropertiesFile());
        File tomcatServerConfigFile = new File(getOsFunctions().getTomcatServerConfig());
        File caKeyStoreFile = new File( ksDir + CA_KEYSTORE_FILE);
        File sslKeyStoreFile = new File(ksDir + SSL_KEYSTORE_FILE);
        File caCertFile = new File(ksDir + CA_CERT_FILE);
        File sslCertFile = new File(ksDir + SSL_CERT_FILE);
        File javaSecFile = new File(getOsFunctions().getPathToJavaSecurityFile());
        File systemPropertiesFile = new File(getOsFunctions().getSsgSystemPropertiesFile());

        File newJavaSecFile  = new File(getOsFunctions().getPathToJavaSecurityFile() + ".new");

        File[] files = new File[]
        {
            javaSecFile,
            keystorePropertiesFile,
            tomcatServerConfigFile,
            caKeyStoreFile,
            sslKeyStoreFile,
            caCertFile,
            sslCertFile,
            systemPropertiesFile
        };

        backupFiles(files, BACKUP_FILE_NAME);

        try {
            prepareJvm(KeystoreType.DEFAULT_KEYSTORE_NAME);
            makeDefaultKeys(doBothKeys, ksBean, ksDir, ksPassword);
            updateJavaSecurity(ksBean, javaSecFile, newJavaSecFile,DEFAULT_SECURITY_PROVIDERS );
            updateKeystoreProperties(keystorePropertiesFile, ksPassword);
            updateServerConfig(tomcatServerConfigFile, sslKeyStoreFile.getAbsolutePath(), ksPassword);
            updateSystemPropertiesFile(ksBean, systemPropertiesFile);
        } catch (Exception e) {
            logger.severe("problem generating keys or keystore - skipping keystore configuration");
            logger.severe(e.getMessage());
            throw e;
        }
    }

    private void doLunaKeyConfig(KeystoreConfigBean ksBean) throws Exception {
        char[] ksPassword = ksBean.getKsPassword();
        //we don't actually care what the password is for Luna, so make it obvious.
        ksPassword = "ignoredbyluna".toCharArray();

        String ksDir = getOsFunctions().getKeystoreDir();

        File newJavaSecFile = new File(getOsFunctions().getPathToJavaSecurityFile() + ".new");

        File javaSecFile = new File(getOsFunctions().getPathToJavaSecurityFile());
        File systemPropertiesFile = new File(getOsFunctions().getSsgSystemPropertiesFile());
        File keystorePropertiesFile = new File(getOsFunctions().getKeyStorePropertiesFile());
        File tomcatServerConfigFile = new File(getOsFunctions().getTomcatServerConfig());
        File caKeyStoreFile = new File( ksDir + CA_KEYSTORE_FILE);
        File sslKeyStoreFile = new File(ksDir + SSL_KEYSTORE_FILE);
        File caCertFile = new File(ksDir + CA_CERT_FILE);
        File sslCertFile = new File(ksDir + SSL_CERT_FILE);

        File[] files = new File[]
        {
            javaSecFile,
            systemPropertiesFile,
            keystorePropertiesFile,
            tomcatServerConfigFile,
            caKeyStoreFile,
            sslKeyStoreFile,
            caCertFile,
            sslCertFile
        };

        backupFiles(files, BACKUP_FILE_NAME);


        try {
            //prepare the JDK and the Luna environment before generating keys
            setLunaSystemProps(ksBean);
            copyLunaJars(ksBean);
            prepareJvm(KeystoreType.LUNA_KEYSTORE_NAME);
            makeLunaKeys(ksBean, caCertFile, sslCertFile, caKeyStoreFile, sslKeyStoreFile);
            updateJavaSecurity(ksBean, javaSecFile, newJavaSecFile, LUNA_SECURITY_PROVIDERS);
            updateKeystoreProperties(keystorePropertiesFile, ksPassword);
            updateServerConfig(tomcatServerConfigFile, sslKeyStoreFile.getAbsolutePath(), ksPassword);
            updateSystemPropertiesFile(ksBean, systemPropertiesFile);
        } catch (Exception e) {
            String mess = "problem generating keys or keystore - skipping keystore configuration: ";
            logger.log(Level.SEVERE, mess + e.getMessage(), e);
            throw e;
        }
    }

    private boolean makeLunaKeys(KeystoreConfigBean ksBean, File caCertFile, File sslCertFile, File caKeyStoreFile, File sslKeyStoreFile) throws Exception {
        boolean success = false;
        String hostname = ksBean.getHostname();
        boolean exportOnly = false;
        try {
            exportOnly = (ksBean.getClusteringType() == ClusteringType.CLUSTER_JOIN);
            MakeLunaCerts.makeCerts(hostname, true, exportOnly, caCertFile, sslCertFile);
            success = true;
        } catch (LunaCmu.LunaCmuException e) {
            logger.severe("Could not locate the Luna CMU. Please rerun the wizard and check the Luna paths");
            logger.severe(e.getMessage());
            throw e;
        } catch (KeyStoreException e) {
            logger.severe("There has been a problem creating the Luna Keystore, or in creating/locating a key within it");
            logger.severe(e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.severe("Could not write the certificates to disk");
            logger.severe(e.getMessage());
            throw e;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();    //should not happen
            throw e;
        } catch (CertificateException e) {
            e.printStackTrace(); //should not happen
            throw e;
        } catch (ClassNotFoundException e) {
            logger.severe("Luna classes not found in the classpath - cannot generate Luna certs");
            logger.severe(e.getMessage());
            throw e;
        } catch (MakeLunaCerts.CertsAlreadyExistException e) {
            logger.severe("Luna certificates already exist - new certs will not be written");
            throw e;
        } catch (LunaCmu.LunaTokenNotLoggedOnException e) {
            logger.severe("This SSG is not already logged into the luna partition, please log in and re-run");
            logger.severe(e.getMessage());
            throw e;
        }

        if (success) {
            truncateKeystores(caKeyStoreFile, sslKeyStoreFile);
        }
        return success;
    }

    private void updateSystemPropertiesFile(KeystoreConfigBean ksBean, File systemPropertiesFile) throws IOException {

        BufferedReader reader = null;
        PrintWriter writer = null;
        File newFile = new File(getOsFunctions().getSsgSystemPropertiesFile() + ".confignew");

        try {
            if (!systemPropertiesFile.exists()) {
                systemPropertiesFile.createNewFile();
            }
            reader = new BufferedReader(new FileReader(systemPropertiesFile));
            writer = new PrintWriter(newFile);
            String line = null;
            boolean jceProviderFound = false;

            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#") && line.startsWith(PROPKEY_JCEPROVIDER)) {
                    jceProviderFound = true;
                    if (ksBean.getKeyStoreType() == KeystoreType.LUNA_KEYSTORE_NAME) {
                        line = PROPKEY_JCEPROVIDER + "=" + PROPERTY_LUNA_JCEPROVIDER_VALUE;
                    }
                    else {
                        continue;
                    }
                }
                writer.println(line);
            }
            if (ksBean.getKeyStoreType() == KeystoreType.LUNA_KEYSTORE_NAME) {
                String lunaPropLine = PROPKEY_JCEPROVIDER + "=" + PROPERTY_LUNA_JCEPROVIDER_VALUE;
                if (!jceProviderFound) {
                    writer.println(lunaPropLine);
                }
                logger.info("Writing " + lunaPropLine + " to system.properties file");
            }
            reader.close();
            reader = null;
            writer.close();
            writer = null;

            logger.info("Updating the system.properties file");
            renameFile(newFile, systemPropertiesFile);

        } catch (FileNotFoundException e) {
            logger.severe("Error while updating the file: " + systemPropertiesFile.getAbsolutePath());
            logger.severe(e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.severe("Error while updating the file: " + systemPropertiesFile.getAbsolutePath());
            logger.severe(e.getMessage());
            throw e;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {}
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void updateJavaSecurity(KeystoreConfigBean ksBean, File javaSecFile, File newJavaSecFile, String[] providersList) throws IOException {
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(javaSecFile));
            writer= new PrintWriter(newJavaSecFile);

            String line = null;
            int secProviderIndex = 0;
            while ((line = reader.readLine()) != null) { //start looking for the security providers section
                if (!line.startsWith("#") && line.startsWith(PROPKEY_SECURITY_PROVIDER)) { //ignore comments and match if we find a security providers section
                    while ((line = reader.readLine()) != null) { //read the rest of the security providers list, esssentially skipping over it
                        if (!line.startsWith("#") && line.startsWith(PROPKEY_SECURITY_PROVIDER)) {
                        }
                        else {
                            break; //we're done with the sec providers
                        }
                    }

                    //now write out the new ones
                    while (secProviderIndex < providersList.length) {
                        line = PROPKEY_SECURITY_PROVIDER + "." + String.valueOf(secProviderIndex + 1) + "=" + providersList[secProviderIndex];
                        writer.println(line);
                        secProviderIndex++;
                    }
                }
                else {
                    writer.println(line);
                }
            }
            reader.close();
            reader = null;
            writer.close();
            writer = null;

            logger.info("Updating the java.security file");
            renameFile(newJavaSecFile, javaSecFile);

        } catch (FileNotFoundException e) {
            logger.severe("Error while updating the file: " + javaSecFile.getAbsolutePath());
            logger.severe(e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.severe("Error while updating the file: " + javaSecFile.getAbsolutePath());
            logger.severe(e.getMessage());
            throw e;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {}
            }

            if (writer != null) {
                writer.close();
            }
        }
    }

    private void renameFile(File srcFile, File destFile) throws IOException {
        String backupName = destFile.getAbsoluteFile() + ".bak";

        logger.info("Renaming: " + destFile + " to: " + backupName);
        File backupFile = new File(backupName);
        if (backupFile.exists()) {
            backupFile.delete();
        }

        //copy the old file to the backup location

        FileUtils.copyFile(destFile, backupFile);
        try {
            destFile.delete();
            FileUtils.copyFile(srcFile, destFile);
            srcFile.delete();
            logger.info("Successfully updated the " + destFile.getName() + " file");
        } catch (IOException e) {
            throw new IOException("You may need to restore the " + destFile.getAbsolutePath() + " file from: " + backupFile.getAbsolutePath() + "reason: " + e.getMessage());
        }
    }

    private void copyLunaJars(KeystoreConfigBean ksBean) {
        String lunaJarSourcePath = ksBean.getLunaJspPath() + "/lib/";
        String jreLibExtdestination = getOsFunctions().getPathToJreLibExt();
        String javaLibPathDestination = getOsFunctions().getPathToJavaLibPath();

        File srcDir = new File(lunaJarSourcePath);

        File destJarDir = new File(jreLibExtdestination);
        File destLibDir = new File(javaLibPathDestination);
        if (!destLibDir.exists()) {
            destLibDir.mkdir();
        }

        File[] fileList = srcDir.listFiles(new FilenameFilter() {
            public boolean accept(File file, String s) {
                return s.toUpperCase().endsWith(".JAR");
            }
        });

        File[] dllFileList = srcDir.listFiles(new FilenameFilter() {
            public boolean accept(File file, String s) {
                return s.toUpperCase().endsWith(".DLL") || s.toUpperCase().endsWith(".SO");
            }
        });

        try {
            //copy jars
            for (File file : fileList) {
                File destFile = new File(destJarDir, file.getName());
                logger.info("Copying " + file.getAbsolutePath() + " to " + destFile.getAbsolutePath());
                FileUtils.copyFile(file, destFile);
            }

            //copy luna shared libs (dll or so)
            for (File file : dllFileList) {
                File destFile = new File(destLibDir, file.getName());
                logger.info("Copying " + file.getAbsolutePath() + " to " + destFile.getAbsolutePath());
                FileUtils.copyFile(file, destFile);
            }

        } catch (IOException e) {
            logger.warning("Could not copy the Luna libraries - the files are possibly in use. Please close all programs and retry.");
            logger.warning(e.getMessage());
        }
    }

    private void setLunaSystemProps(KeystoreConfigBean ksBean) {
        OSSpecificFunctions.KeystoreInfo lunaInfo = getOsFunctions().getKeystore(KeystoreType.LUNA_KEYSTORE_NAME);
        if (lunaInfo != null)
            System.setProperty(LUNA_SYSTEM_CMU_PROPERTY, ksBean.getLunaInstallationPath() + lunaInfo.getMetaInfo("CMU_PATH"));
    }

    private void truncateKeystores(File caKeyStore, File sslKeyStore) {
        FileOutputStream emptyCaFileStream = null;
        FileOutputStream emptySslFileStream = null;

        try {
            emptyCaFileStream = new FileOutputStream(caKeyStore);
            emptyCaFileStream.close();
            emptyCaFileStream = null;

            emptySslFileStream = new FileOutputStream(sslKeyStore);
            emptySslFileStream.close();
            emptySslFileStream = null;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } finally {
            ResourceUtils.closeQuietly(emptyCaFileStream);
            ResourceUtils.closeQuietly(emptySslFileStream);
        }


    }

    private boolean makeDefaultKeys(boolean doBothKeys, KeystoreConfigBean ksBean, String ksDir, char[] ksPassword) throws Exception {
        boolean keysDone;
        String args[] = new String[]
        {
                ksBean.getHostname(),
                ksDir,
                new String(ksPassword),
                new String(ksPassword),
                getKsType()
        };

        if (doBothKeys) {
            logger.info("Generating both keys");
            SetKeys.NewCa.main(args);
            keysDone = true;
        } else {
            logger.info("Generating only SSL key");
            SetKeys.ExistingCa.main(args);
            keysDone = true;
        }

        return keysDone;
    }



    private void updateServerConfig(File tomcatServerConfigFile, String sslKeyStorePath, char[] ksPassword) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(tomcatServerConfigFile);
            Document doc = XmlUtil.parse(fis);

            doConnectorElementInServerConfig(doc, sslKeyStorePath, ksPassword);
            fis.close();
            fis = null;

            fos = new FileOutputStream(tomcatServerConfigFile);
            XmlUtil.nodeToOutputStream(doc, fos);
            logger.info("Updating the server.xml");
            fos.close();
            fos = null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } finally {
            ResourceUtils.closeQuietly(fis);
            ResourceUtils.closeQuietly(fos);
        }
    }

    private void doConnectorElementInServerConfig(Document doc, String sslKeyStorePath, char[] ksPassword) {
        NodeList list = doc.getDocumentElement().getElementsByTagName("Connector");
        for (int i = 0; i < list.getLength(); i++) {
            Element el = (Element)list.item(i);
            if (el.hasAttribute("secure") && el.getAttribute("secure").equalsIgnoreCase("true")) {
                el.setAttribute(XML_KSFILE, sslKeyStorePath);
                el.setAttribute(XML_KSPASS, new String(ksPassword));
                el.setAttribute(XML_KSTYPE, getKsType());
                el.setAttribute(XML_KSALIAS, KeyStoreConstants.PROP_KS_ALIAS_DEFAULTVALUE);
            }
        }
    }

    private void updateKeystoreProperties(File keystorePropertiesFile, char[] ksPassword) throws IOException {

        FileOutputStream fos = null;
        try {
            PropertiesConfiguration keystoreProps = PropertyHelper.mergeProperties(
                    keystorePropertiesFile,
                    new File(keystorePropertiesFile.getAbsolutePath() + "." + getOsFunctions().getUpgradedNewFileExtension()),
                    true, true);

            keystoreProps.setProperty(KeyStoreConstants.PROP_KS_TYPE, getKsType());
            keystoreProps.setProperty(KeyStoreConstants.PROP_KEYSTORE_DIR, getOsFunctions().getKeystoreDir());
            keystoreProps.setProperty(KeyStoreConstants.PROP_CA_KS_PASS, new String(ksPassword));
            keystoreProps.setProperty(KeyStoreConstants.PROP_SSL_KS_PASS, new String(ksPassword));
            keystoreProps.setProperty(KeyStoreConstants.PROP_KS_ALIAS, new String(KeyStoreConstants.PROP_KS_ALIAS_DEFAULTVALUE));

            fos = new FileOutputStream(keystorePropertiesFile);
            keystoreProps.setHeader(PROPERTY_COMMENT + "\n" + new Date());
            keystoreProps.save(fos, "iso-8859-1");
            logger.info("Updating the keystore.properties file");
            fos.close();
            fos = null;

        } catch (FileNotFoundException fnf) {
            logger.severe("error while updating the keystore properties file");
            logger.severe(fnf.getMessage());
            throw fnf;
        } catch (ConfigurationException ce) {
            logger.severe("error while updating the keystore properties file");
            logger.severe(ce.getMessage());
            throw new CausedIOException(ce);
        } catch (IOException ioex) {
            logger.severe("error while updating the keystore properties file");
            logger.severe(ioex.getMessage());
            throw ioex;
        } finally {
            ResourceUtils.closeQuietly(fos);
        }
    }

    private String getKsType() {
        KeystoreType ksTypeFromBean = ((KeystoreConfigBean)configBean).getKeyStoreType();
        if (ksTypeFromBean == KeystoreType.LUNA_KEYSTORE_NAME) {
            return "Luna";
        } else if (ksTypeFromBean == KeystoreType.DEFAULT_KEYSTORE_NAME) {
                return "PKCS12";
        } else {
            return "PKCS11";
        }
    }
}
