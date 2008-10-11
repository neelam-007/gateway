/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.common.io.CertUtils;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.management.SoftwareVersion;
import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.host.IpAddressConfig;
import com.l7tech.server.management.config.host.PCHostConfig;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.PCNodeConfig;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;

import javax.ejb.TransactionAttribute;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Top-level (possibly only) DAO for management of Process Controller configuration entities
 * @author alex
 */
@TransactionAttribute
public class ConfigServiceImpl implements ConfigService {
    private static final Logger logger = Logger.getLogger(ConfigServiceImpl.class.getName());

    private final File processControllerHomeDirectory;
    private final File nodeBaseDirectory;
    private final HostConfig host;
    private final MasterPasswordManager masterPasswordManager;
    private final Pair<X509Certificate[], RSAPrivateKey> sslKeypair;
    private final Set<X509Certificate> trustedRemoteNodeManagementCerts;
    private final int sslPort;
    private final File configDirectory;
    private final File javaBinary;
    private static final String SLASH = System.getProperty("file.separator");

    public ConfigServiceImpl() throws IOException, GeneralSecurityException {
        // TODO maybe just pass the host.properties path instead, and put the nodeBaseDirectory in there
        processControllerHomeDirectory = getHomeDirectory();
        String s = System.getProperty("com.l7tech.server.processcontroller.nodeBaseDirectory");
        if (s == null) {
            File parent = processControllerHomeDirectory.getParentFile();
            nodeBaseDirectory = new File(parent, "node");
        } else {
            nodeBaseDirectory = new File(s);
        }

        File configDirectory = new File(processControllerHomeDirectory, "/etc/conf");
        if (!configDirectory.exists() || !configDirectory.isDirectory()) 
            throw new IllegalStateException("Configuration directory " + configDirectory.getAbsolutePath() + " does not exist");
        this.configDirectory = configDirectory;
        this.masterPasswordManager = new MasterPasswordManager( new DefaultMasterPasswordFinder( new File(configDirectory, "omp.dat") ) );

        final File hostPropsFile = new File(getProcessControllerHomeDirectory(), "etc" + SLASH + "host.properties");
        if (!hostPropsFile.exists())
            throw new IllegalStateException("Couldn't find " + hostPropsFile.getAbsolutePath());

        final FileInputStream is;
        final Properties hostProps;
        try {
            is = new FileInputStream(hostPropsFile);
            hostProps = new Properties();
            hostProps.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't load " + hostPropsFile.getAbsolutePath(), e);
        }

        PCHostConfig config = new PCHostConfig();
        config.setGuid(getRequiredProperty(hostProps, HOSTPROPERTIES_ID));
        config.setLocalHostname(getLocalHostname(hostProps));
        config.setHostType(getHostType(hostProps));
        if (OSDetector.isLinux()) {
            config.setOsType(HostConfig.OSType.RHEL);
        } else if (OSDetector.isSolaris()) {
            config.setOsType(HostConfig.OSType.SOLARIS);
        } else if (OSDetector.isWindows()) {
            config.setOsType(HostConfig.OSType.WINDOWS);
        } else {
            throw new IllegalStateException("Unsupported operating system"); // TODO muddle through?
        }

        String jrePath = hostProps.getProperty(HOSTPROPERTIES_JRE);
        if (jrePath == null) jrePath = System.getProperty("java.home");
        final File javaBinary = new File(new File(jrePath), "bin" + SLASH + "java");
        if (!javaBinary.exists() || !javaBinary.canExecute()) throw new IllegalStateException(javaBinary.getCanonicalPath() + " is not executable");
        this.javaBinary = javaBinary.getCanonicalFile();
        logger.info("Using java binary: " + javaBinary.getPath());

        this.sslPort = Integer.valueOf(hostProps.getProperty(HOSTPROPERTIES_SSL_PORT, "8765"));
        this.sslKeypair = readSslKeypair(hostProps);
        this.trustedRemoteNodeManagementCerts = readTrustedNodeManagementCerts(hostProps);

        reverseEngineerIps(config);
        reverseEngineerNodes(config);
        this.host = config;
    }

    static File getHomeDirectory() {
        String s = System.getProperty("com.l7tech.server.processcontroller.homeDirectory");
        if (s == null) {
            final File f = new File(System.getProperty("user.dir"));
            logger.info("Assuming Process Controller home directory is " + f.getAbsolutePath());
            return f;
        } else {
            return new File(s);
        }
    }

    public HostConfig getHost() {
        return host;
    }

    private HostConfig.HostType getHostType(Properties props) {
        String type = props.getProperty(HOSTPROPERTIES_TYPE);
        if (type == null) {
            logger.info(HOSTPROPERTIES_TYPE + " not set; assuming appliance");
            return HostConfig.HostType.APPLIANCE;
        } else try {
            return HostConfig.HostType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Unsupported " + HOSTPROPERTIES_TYPE + " " + type + "; assuming \"software\"");
            return HostConfig.HostType.SOFTWARE;
        }
    }

    private Pair<X509Certificate[], RSAPrivateKey> readSslKeypair(Properties hostProps) throws IOException, GeneralSecurityException {
        final String keystoreFilename = getRequiredProperty(hostProps, HOSTPROPERTIES_SSL_KEYSTOREFILE);
        final File keystoreFile = new File(keystoreFilename);

        final String encryptedPassword = getRequiredProperty(hostProps, HOSTPROPERTIES_SSL_KEYSTOREPASSWORD);
        char[] keystorePass = masterPasswordManager.decryptPasswordIfEncrypted(encryptedPassword);

        String keystoreType = hostProps.getProperty(HOSTPROPERTIES_SSL_KEYSTORETYPE, "PKCS12");

        final KeyStore ks = KeyStore.getInstance(keystoreType);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(keystoreFile);
            ks.load(fis, keystorePass);
        } finally {
            ResourceUtils.closeQuietly(fis);
        }

        final String alias = hostProps.getProperty(HOSTPROPERTIES_SSL_KEYSTOREALIAS);
        final Key key;
        final Certificate[] chain;
        if (alias != null) {
            if (!ks.containsAlias(alias)) throw new IllegalStateException(keystoreFile.getAbsolutePath() + " does not contain an entry with the alias " + alias);
            key = ks.getKey(alias, keystorePass);
            if (key == null) throw new IllegalStateException("Couldn't get private key for " + alias + " in " + keystoreFile.getAbsolutePath());
            if (!(key instanceof RSAPrivateKey)) throw new IllegalStateException(alias + " in " + keystoreFile.getAbsolutePath() + " is not an RSAPrivateKey");
            chain = ks.getCertificateChain(alias);
            if (chain == null) throw new IllegalStateException("Couldn't get certificate chain for " + alias + " in " + keystoreFile.getAbsolutePath());
        } else {
            // Try to find a single usable keyEntry
            RSAPrivateKey gotKey = null;
            Certificate[] gotChain = null;
            GeneralSecurityException thrown = null;

            final Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String s = aliases.nextElement();

                final Key tempKey;
                try {
                    tempKey = ks.getKey(s, keystorePass);
                    if (!(tempKey instanceof RSAPrivateKey)) continue;
                } catch (GeneralSecurityException e) {
                    thrown = e;
                    continue;
                }

                final Certificate[] tempChain = ks.getCertificateChain(s);
                if (tempChain == null) continue;

                if (gotKey != null) throw new IllegalStateException(keystoreFile.getAbsolutePath() + " contains multiple usable key entries; must specify one using host.keystore.alias");
                gotKey = (RSAPrivateKey)tempKey;
                gotChain = tempChain;
            }

            if (gotKey == null) {
                if (thrown != null) throw thrown;
                throw new IllegalStateException(keystoreFile.getAbsolutePath() + " did not contain a usable key entry");
            }
            chain = gotChain;
            key = gotKey;
        }
        X509Certificate[] xchain = new X509Certificate[chain.length];
        //noinspection SuspiciousSystemArraycopy
        System.arraycopy(chain, 0, xchain, 0, chain.length);
        return new Pair<X509Certificate[], RSAPrivateKey>(xchain, (RSAPrivateKey)key);
    }

    private String getRequiredProperty(Properties hostProps, final String what) {
        final String that = hostProps.getProperty(what);
        if (that == null) throw new IllegalArgumentException(what + " not found");
        return that;
    }

    private Set<X509Certificate> readTrustedNodeManagementCerts(Properties hostProps) throws GeneralSecurityException, IOException {
        final String trustStoreFilename = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_FILE);
        String trustStoreType;
        File trustStoreFile;
        if (trustStoreFilename == null) {
            trustStoreFile = new File(configDirectory, DEFAULT_REMOTE_MANAGEMENT_TRUSTSTORE_FILENAME);
            if (!trustStoreFile.exists()) {
                logger.info("No remote node management truststore found; continuing with remote node management disabled");
                return Collections.emptySet();
            }
            trustStoreType = "PKCS12";
        } else {
            trustStoreFile = new File(trustStoreFilename);
            if (!trustStoreFile.exists()) throw new IllegalArgumentException("Can't find remote node management truststore at " + trustStoreFile.getAbsolutePath());

            trustStoreType = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_TYPE, "PKCS12");
        }

        final String encryptedPassword = getRequiredProperty(hostProps, HOSTPROPERTIES_NODEMANAGEMENTTRUSTSTORE_PASSWORD);
        char[] keystorePass = masterPasswordManager.decryptPasswordIfEncrypted(encryptedPassword);

        final KeyStore ks = KeyStore.getInstance(trustStoreType);
        FileInputStream fis = null;
        Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();
        try {
            fis = new FileInputStream(trustStoreFile);
            ks.load(fis, keystorePass);
            final Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isCertificateEntry(alias)) {
                    final Certificate cert = ks.getCertificate(alias);
                    if (cert instanceof X509Certificate) {
                        final X509Certificate xc = (X509Certificate)cert;
                        trustedCerts.add(xc);
                        logger.log(Level.INFO,
                                   "Certificate trusted for remote node management: dn=\"{0}\", serial=\"{1}\", thumbprintSha1=\"{2}\"",
                                   new Object[] {xc.getSubjectDN().getName(), xc.getSerialNumber(), CertUtils.getThumbprintSHA1(xc)});
                    } else {
                        logger.warning("Skipping non-X.509 certificate with alias " + alias);
                    }
                }
            }
            return trustedCerts;
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
    }

    private String getLocalHostname(Properties hostProps) {
        String hostname = (String)hostProps.get("host.hostname");
        if (hostname != null) {
            logger.info("hostname is " + hostname);
            return hostname;
        }

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.log(Level.WARNING, "Unable to get local hostname; using localhost", e);
            hostname = "localhost";
        }
        return hostname;
    }

    private void reverseEngineerNodes(PCHostConfig g) {
        try {
            for (File nodeDirectory : nodeBaseDirectory.listFiles()) {
                File nodeConfigFile = new File(nodeDirectory, "etc/conf/node.properties");
                if (!nodeConfigFile.isFile()) continue;

                Properties nodeProperties = new Properties();
                InputStream in = null;
                try {
                    nodeProperties.load(in = new FileInputStream(nodeConfigFile));
                } finally {
                    ResourceUtils.closeQuietly(in);
                }

                if (!nodeProperties.containsKey(NODEPROPERTIES_ENABLED) || !nodeProperties.containsKey(NODEPROPERTIES_ID)) {
                    logger.log(Level.WARNING, "Ignoring node ''{0}'' due to invalid properties.", nodeDirectory.getName());
                    continue;
                }

                final PCNodeConfig node = new PCNodeConfig();
                node.setHost(g);
                node.setName(nodeDirectory.getName());
                node.setSoftwareVersion(SoftwareVersion.fromString("4.7.0")); //TODO get version for node
                node.setEnabled(Boolean.valueOf(nodeProperties.getProperty(NODEPROPERTIES_ENABLED)));
                node.setGuid(nodeProperties.getProperty(NODEPROPERTIES_ID));

//                    final DatabaseConfig db = new DatabaseConfig();
//                    db.setType(DatabaseType.NODE_ALL);
//                    db.setHost( nodeProperties.getProperty() );
//                    db.setPort( Integer.parseInt(nodeProperties.getProperty()) );
//                    db.setName( nodeProperties.getProperty() );
//                    db.setNodeUsername( nodeProperties.getProperty() );
//                    db.setNodePassword( nodeProperties.getProperty() );
//                    node.getDatabases().add(db);

                logger.log(Level.INFO, "Detected node ''{0}''.", nodeDirectory.getName());
                g.getNodes().put(node.getName(), node);
            }
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error when detecting nodes.", ioe);
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, "Error when detecting nodes.", nfe);
        }
    }

    private void reverseEngineerIps(PCHostConfig g) {
        final Set<IpAddressConfig> ips = g.getIpAddresses();

        final Enumeration<NetworkInterface> nifs;
        try {
            nifs = NetworkInterface.getNetworkInterfaces();
            while (nifs != null && nifs.hasMoreElements()) {
                final NetworkInterface nif = nifs.nextElement();
                if (nif.isLoopback()) continue;
                final Enumeration<InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    final InetAddress addr = addrs.nextElement();
                    final IpAddressConfig iac = new IpAddressConfig(g);
                    iac.setHost(g);
                    iac.setInterfaceName(nif.getName());
                    iac.setIpAddress(addr.getHostAddress());
                    ips.add(iac);
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e); // Unlikely
        }
    }

    public void addServiceNode(NodeConfig node) {
        host.getNodes().put(node.getName(), node);
    }

    public void updateServiceNode(NodeConfig node) {
        // TODO what kinds of NodeConfig changes might necessitate a restart?
        host.getNodes().put(node.getName(), node);
    }

    public File getNodeBaseDirectory() {
        return nodeBaseDirectory;
    }

    public Pair<X509Certificate[], RSAPrivateKey> getSslKeypair() {
        return sslKeypair;
    }

    public File getProcessControllerHomeDirectory() {
        return processControllerHomeDirectory;
    }

    public int getSslPort() {
        return sslPort;
    }

    public Set<X509Certificate> getTrustedRemoteNodeManagementCerts() {
        return trustedRemoteNodeManagementCerts;
    }

    public File getJavaBinary() {
        return javaBinary;
    }
}
