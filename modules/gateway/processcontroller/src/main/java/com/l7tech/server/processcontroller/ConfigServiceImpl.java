/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.host.IpAddressConfig;
import com.l7tech.server.management.config.host.PCHostConfig;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.PCNodeConfig;
import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Top-level (possibly only) DAO for management of Process Controller configuration entities
 * @author alex
 */
public class ConfigServiceImpl implements ConfigService {
    private static final Logger logger = Logger.getLogger(ConfigServiceImpl.class.getName());
    private static final String SLASH = System.getProperty("file.separator");

    private final File processControllerHomeDirectory;
    private final File nodeBaseDirectory;
    private final HostConfig host;
    private final MasterPasswordManager masterPasswordManager;
    private final Pair<X509Certificate[], RSAPrivateKey> sslKeypair;
    private final Set<X509Certificate> trustedRemoteNodeManagementCerts;
    private final int sslPort;
    private final String sslIPAddress;
    private final File configDirectory;
    private final File javaBinary;
    private final Map<String, NodeInfo> nodeInfos;

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

        File configDirectory = new File(processControllerHomeDirectory, SLASH + "etc" + SLASH + "conf");
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

        PCHostConfig hostConfig = new PCHostConfig();
        hostConfig.setGuid(getRequiredProperty(hostProps, HOSTPROPERTIES_ID));
        hostConfig.setLocalHostname(getLocalHostname(hostProps));
        hostConfig.setHostType(getHostType(hostProps));
        if (OSDetector.isLinux()) {
            hostConfig.setOsType(HostConfig.OSType.RHEL);
        } else if (OSDetector.isSolaris()) {
            hostConfig.setOsType(HostConfig.OSType.SOLARIS);
        } else if (OSDetector.isWindows()) {
            hostConfig.setOsType(HostConfig.OSType.WINDOWS);
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
        this.sslIPAddress = hostProps.getProperty(HOSTPROPERTIES_SSL_IPADDRESS, "127.0.0.1");
        this.sslKeypair = readSslKeypair(hostProps);
        this.trustedRemoteNodeManagementCerts = readTrustedNodeManagementCerts(hostProps);

        reverseEngineerIps(hostConfig);

        Map<String, NodeInfo> infos = new HashMap<String, NodeInfo>();
        try {
            for (Pair<NodeConfig, File> pair : NodeConfigurationManager.loadNodeConfigs(false)) {
                NodeConfig config = pair.left;
                logger.log(Level.INFO, "Detected node ''{0}''.", config.getName());
                config.setHost(hostConfig);
                infos.put(config.getName(), new NodeInfo((PCNodeConfig)config, pair.right));
                hostConfig.getNodes().put(config.getName(), config);
            }
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error when detecting nodes.", ioe);
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, "Error when detecting nodes.", nfe);
        }

        this.nodeInfos = new ConcurrentHashMap<String, NodeInfo>(infos);
        this.host = hostConfig;
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

    @Override
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


    private static class NodeInfo {
        private final PCNodeConfig config;
        private final File nodeConfigFile;

        private NodeInfo(PCNodeConfig config, File nodeConfigFile) {
            this.config = config;
            this.nodeConfigFile = nodeConfigFile;
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

    @Override
    public void addServiceNode(NodeConfig node) {
        host.getNodes().put(node.getName(), node);
    }

    @Override
    public void updateServiceNode(NodeConfig node) {
        // TODO what kinds of NodeConfig changes might necessitate a restart?
        host.getNodes().put(node.getName(), node);
    }

    @Override
    public File getNodeBaseDirectory() {
        return nodeBaseDirectory;
    }

    @Override
    public Pair<X509Certificate[], RSAPrivateKey> getSslKeypair() {
        return sslKeypair;
    }

    public File getProcessControllerHomeDirectory() {
        return processControllerHomeDirectory;
    }

    @Override
    public int getSslPort() {
        return sslPort;
    }

    @Override
    public String getSslIPAddress() {
        return sslIPAddress;
    }

    @Override
    public Set<X509Certificate> getTrustedRemoteNodeManagementCerts() {
        return trustedRemoteNodeManagementCerts;
    }

    @Override
    public File getJavaBinary() {
        return javaBinary;
    }

    @Override
    public synchronized void deleteNode(final String nodeName) throws DeleteException, IOException {
        NodeInfo info = nodeInfos.get(nodeName);
        if (info == null) return;
        final String propfile = info.nodeConfigFile.getAbsolutePath();
        if (!info.nodeConfigFile.renameTo(new File(propfile + "-deleted"))) throw new DeleteException("Unable to rename " + propfile);
        nodeInfos.remove(nodeName);
        host.getNodes().remove(nodeName);
    }

    @Override
    public MonitoringConfiguration getCurrentMonitoringConfiguration() {
        return null;
    }

    @Override
    public boolean isResponsibleForClusterMonitoring() {
        return false;
    }
}
