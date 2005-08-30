package com.l7tech.server.config.commands;

import com.l7tech.common.util.FileUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.config.KeyStoreConstants;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.beans.KeystoreConfigBean;
import com.l7tech.server.util.MakeLunaCerts;
import com.l7tech.server.util.SetKeys;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 17, 2005
 * Time: 3:43:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class KeystoreConfigCommand extends BaseConfigurationCommand {
    private static final String BACKUP_FILE_NAME = "keystore_config_backups";

    private static final String CA_KEYSTORE_FILE = "ca.ks";
    private static final String SSL_KEYSTORE_FILE = "ssl.ks";
    private static final String CA_CERT_FILE = "ca.cer";
    private static final String SSL_CERT_FILE = "ssl.cer";

    private static final String PROPERTY_COMMENT = "this file was updated by the SSG configuration utility";


    private static final String PROP_KEYSTORE_DIR = "keystoredir";
    private static final String PROP_SSL_KS_PASS = "sslkspasswd";
    private static final String PROP_CA_KS_PASS = "rootcakspasswd";
    private static final String PROP_KS_TYPE = "keystoretype";


    private static final String XML_KSFILE = "keystoreFile";
    private static final String XML_KSPASS = "keystorePass";
    private static final String XML_KSTYPE = "keystoreType";
    private static final String LUNA_SYSTEM_CMU_PROPERTY = "lunaCmuPath";

    private static final String PROPKEY_SECURITY_PROVIDER = "security.provider";
    private static final String PROPKEY_JCEPROVIDER = "com.l7tech.common.security.jceProviderEngine";
    private static final String PROPERTY_LUNA_JCEPROVIDER_VALUE = "com.l7tech.common.security.prov.luna.LunaJceProviderEngine";

    private static final String[] SECURITY_PROVIDERS =
            {
                "com.chrysalisits.crypto.LunaJCAProvider",
                "com.chrysalisits.cryptox.LunaJCEProvider",
                "sun.security.provider.Sun",
                "com.sun.net.ssl.internal.ssl.Provider"
            };


    public KeystoreConfigCommand(ConfigurationBean bean) {
        super(bean, bean.getOSFunctions());
    }

    public void execute() {
        KeystoreConfigBean ksBean = (KeystoreConfigBean) configBean;
        if (ksBean.isDoKeystoreConfig()) {

            String ksType = ksBean.getKeyStoreType();
            if (ksType.equalsIgnoreCase(KeyStoreConstants.DEFAULT_KEYSTORE_NAME)) {
                doDefaultKeyConfig(ksBean);
            } else {
                doLunaKeyConfig(ksBean);
            }
        }
    }

    private void doDefaultKeyConfig(KeystoreConfigBean ksBean) {
        char[] ksPassword = ksBean.getKsPassword();
        boolean doBothKeys = ksBean.isDoBothKeys();

        String ksDir = osFunctions.getKeystoreDir();
        File keystorePropertiesFile = new File(osFunctions.getKeyStorePropertiesFile());
        File tomcatServerConfigFile = new File(osFunctions.getTomcatServerConfig());
        File caKeyStoreFile = new File( ksDir + CA_KEYSTORE_FILE);
        File sslKeyStoreFile = new File(ksDir + SSL_KEYSTORE_FILE);
        File caCertFile = new File(ksDir + CA_CERT_FILE);
        File sslCertFile = new File(ksDir + SSL_CERT_FILE);

        File[] files = new File[]
        {
            keystorePropertiesFile,
            tomcatServerConfigFile,
            caKeyStoreFile,
            sslKeyStoreFile,
            caCertFile,
            sslCertFile
        };

        try {
            backupFiles(files, BACKUP_FILE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean keysDone = makeDefaultKeys(doBothKeys, ksBean, ksDir, ksPassword);
        if (keysDone) {
            updateKeystoreProperties(keystorePropertiesFile, ksPassword);
            updateServerConfig(tomcatServerConfigFile, sslKeyStoreFile.getAbsolutePath(), ksPassword);
        }
    }

    private void doLunaKeyConfig(KeystoreConfigBean ksBean) {
        char[] ksPassword = ksBean.getKsPassword();
        //we don't actually care what the password is for Luna, so make it obvious.
        ksPassword = new String("ignoredbyluna").toCharArray();

        String ksDir = osFunctions.getKeystoreDir();

        File newJavaSecFile = new File(osFunctions.getPathToJavaSecurityFile() + ".new");

        File javaSecFile = new File(osFunctions.getPathToJavaSecurityFile());
        File systemPropertiesFile = new File(osFunctions.getSsgSystemPropertiesFile());
        File keystorePropertiesFile = new File(osFunctions.getKeyStorePropertiesFile());
        File tomcatServerConfigFile = new File(osFunctions.getTomcatServerConfig());
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

        try {
            backupFiles(files, BACKUP_FILE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //prepare the JDK and the Luna environment before generating keys
        setLunaSystemProps(ksBean);
        copyLunaJars(ksBean);
        updateJavaSecurity(ksBean, javaSecFile, newJavaSecFile);
        makeLunaKeys(ksBean);

        updateKeystoreProperties(keystorePropertiesFile, ksPassword);
        updateServerConfig(tomcatServerConfigFile, sslKeyStoreFile.getAbsolutePath(), ksPassword);
        updateSystemPropertiesFile(ksBean, systemPropertiesFile);
    }

    private boolean makeLunaKeys(KeystoreConfigBean ksBean) {
        boolean success = true;

        String hostname = ksBean.getHostname();

        try {
            MakeLunaCerts.realMain(hostname, true);
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        if (success) {
            moveLunaCerts();
        }
        return success;
    }

    private void updateSystemPropertiesFile(KeystoreConfigBean ksBean, File systemPropertiesFile) {
        FileInputStream fis = null;
        FileOutputStream fos = null;

        Properties props = new Properties();
        try {
            fis = new FileInputStream(systemPropertiesFile);
            props.load(fis);
            fis.close();
            fis = null;

            props.setProperty(PROPKEY_JCEPROVIDER, PROPERTY_LUNA_JCEPROVIDER_VALUE);

            fos = new FileOutputStream(systemPropertiesFile);
            props.store(fos, PROPERTY_COMMENT);
            fos.close();
            fos = null;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateJavaSecurity(KeystoreConfigBean ksBean, File javaSecFile, File newJavaSecFile) {
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(javaSecFile));
            writer= new PrintWriter(newJavaSecFile);

            String line = null;
            int secProviderIndex = 0;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#") && line.startsWith(PROPKEY_SECURITY_PROVIDER)) {
                    if (secProviderIndex < SECURITY_PROVIDERS.length) {
                        line = new String(PROPKEY_SECURITY_PROVIDER + "." + String.valueOf(secProviderIndex + 1) + "=" + SECURITY_PROVIDERS[secProviderIndex]);
                        secProviderIndex++;
                    } else {
                        continue;
                    }
                }
                writer.println(line);
            }
            reader.close();
            reader = null;
            writer.close();
            writer = null;

            newJavaSecFile.renameTo(javaSecFile);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (writer != null) {
                writer.close();
            }
        }
    }

    private void copyLunaJars(KeystoreConfigBean ksBean) {
        String lunaJarSourcePath = ksBean.getLunaJspPath() + "/lib/";
        String destination = osFunctions.getPathToJreLibExt();
        File srcDir = new File(lunaJarSourcePath);
        File destDir = new File(destination);

        File[] fileList = srcDir.listFiles();
        for (int i = 0; i < fileList.length; i++) {
            File file = fileList[i];
            File destFile = new File(destDir, file.getName());
            try {
                FileUtils.copyFile(file, destFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setLunaSystemProps(KeystoreConfigBean ksBean) {
        System.setProperty(LUNA_SYSTEM_CMU_PROPERTY, ksBean.getLunaInstallationPath() + osFunctions.getLunaCmuPath());
    }

    private void moveLunaCerts() {

    }

    private boolean makeDefaultKeys(boolean doBothKeys, KeystoreConfigBean ksBean, String ksDir, char[] ksPassword) {
        boolean keysDone = false;
        String args[] = new String[]
        {
                ksBean.getHostname(),
                ksDir,
                new String(ksPassword),
                new String(ksPassword),
                getKsType()
        };
        try {
            if (doBothKeys) {
                System.out.println("Generating both keys");
                SetKeys.NewCa.main(args);
                keysDone = true;
            } else {
                System.out.println("Generating only SSL key");
                SetKeys.ExistingCa.main(args);
                keysDone = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return keysDone;
    }



    private void updateServerConfig(File tomcatServerConfigFile, String sslKeyStorePath, char[] ksPassword) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(tomcatServerConfigFile);
            Document doc = XmlUtil.parse(fis);
            NodeList list = doc.getDocumentElement().getElementsByTagName("Connector");
            for (int i = 0; i < list.getLength(); i++) {
                Element el = (Element)list.item(i);
                if (el.hasAttribute("secure") && el.getAttribute("secure").equalsIgnoreCase("true")) {
                    el.setAttribute(XML_KSFILE, sslKeyStorePath);
                    el.setAttribute(XML_KSPASS, new String(ksPassword));
                    el.setAttribute(XML_KSTYPE, getKsType());
                }
            }
            fis.close();
            fis = null;

            fos = new FileOutputStream(tomcatServerConfigFile);
            XmlUtil.nodeToOutputStream(doc, fos);
            fos.close();
            fos = null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }

    private void updateKeystoreProperties(File keystorePropertiesFile, char[] ksPassword) {
        FileInputStream fis = null;
        FileOutputStream fos = null;

        Properties keystoreProps = new Properties();
        try {
            fis = new FileInputStream(keystorePropertiesFile);
            keystoreProps.load(fis);
            keystoreProps.setProperty(PROP_KS_TYPE, getKsType());
            keystoreProps.setProperty(PROP_KEYSTORE_DIR, osFunctions.getKeystoreDir());
            keystoreProps.setProperty(PROP_CA_KS_PASS, new String(ksPassword));
            keystoreProps.setProperty(PROP_SSL_KS_PASS, new String(ksPassword));

            fis.close();
            fis = null;

            fos = new FileOutputStream(keystorePropertiesFile);
            keystoreProps.store(fos, PROPERTY_COMMENT);
            fos.close();
            fos = null;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getKsType() {
        String ksTypeFromBean = ((KeystoreConfigBean)configBean).getKeyStoreType();
        if (ksTypeFromBean.equalsIgnoreCase(KeyStoreConstants.LUNA_KEYSTORE_NAME)) {
            return "Luna";
        }
        else {
            String version = System.getProperty("java.version");
            boolean java15 = version.startsWith("1.5");
            if (java15) {
                return "PKCS12";
            }
            else {
                return "BCPKCS12";
            }
        }
    }
}
