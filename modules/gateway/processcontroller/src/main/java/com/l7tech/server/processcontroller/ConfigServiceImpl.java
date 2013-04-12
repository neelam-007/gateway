package com.l7tech.server.processcontroller;

import com.l7tech.common.io.CertUtils;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.host.IpAddressConfig;
import com.l7tech.server.management.config.host.PCHostConfig;
import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.processcontroller.monitoring.MonitoringKernel;
import com.l7tech.util.*;
import static com.l7tech.util.Option.optional;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.split;
import static com.l7tech.util.TextUtils.trim;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.bind.JAXB;
import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Top-level (possibly only) DAO for management of Process Controller configuration entities
 * @author alex
 */
public class ConfigServiceImpl implements ConfigService {
    private static final Logger logger = Logger.getLogger(ConfigServiceImpl.class.getName());
    private static final String SLASH = SyspropUtil.getProperty( "file.separator" );
    private static final String SERVICES_CONTEXT_BASE_PATH = "/services";
    private static final String PROCESSCONTROLLER_PROPERTIES = "com/l7tech/server/processcontroller/resources/processcontroller.properties";
    private static final String DEFAULT_SSL_CIPHERS = "TLS_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_256_CBC_SHA";
    private static final Pattern SPLITTER = Pattern.compile("\\s*,\\s*");

    private final File processControllerHomeDirectory;
    private final File nodeBaseDirectory;
    private final File patchesDirectory;
    private final String patchesLog;
    private final HostConfig host;
    private final MasterPasswordManager masterPasswordManager;
    private final Pair<X509Certificate[], PrivateKey> sslKeypair;
    private final Set<X509Certificate> trustedRemoteNodeManagementCerts;
    private final Set<String> trustedRemoteNodeManagementThumbprints;
    private final Set<X509Certificate> trustedPatchCerts;
    private final String secret;
    private final int sslPort;
    private final String sslIPAddress;
    private final Option<String[]> sslProtocols;
    private final Option<String[]> sslCiphers;
    private final File configDirectory;
    private final File applianceLibexecDirectory;
    private final File javaBinary;
    private final boolean useSca; // TODO this should go through ScaFeature
    private final Properties hostProps;
    private final Properties apiEndpointPaths = new Properties();

    private volatile boolean responsibleForClusterMonitoring;
    private volatile MonitoringConfiguration currentMonitoringConfiguration;

    @Inject
    private MonitoringKernel monitoringKernel;
    private final File monitoringConfigFile;

    public ConfigServiceImpl() throws IOException, GeneralSecurityException {
        // TODO maybe just pass the host.properties path instead, and put the nodeBaseDirectory in there
        processControllerHomeDirectory = getHomeDirectory();
        String s = ConfigFactory.getProperty( "com.l7tech.server.processcontroller.nodeBaseDirectory" );
        if (s == null) {
            File parent = processControllerHomeDirectory.getParentFile();
            nodeBaseDirectory = new File(parent, "Gateway"+SLASH+"node");
        } else {
            nodeBaseDirectory = new File(s);
        }

        File configDirectory = new File(processControllerHomeDirectory, SLASH + "etc" + SLASH + "conf");
        if (!configDirectory.exists() || !configDirectory.isDirectory())
            throw new IllegalStateException("Configuration directory " + configDirectory.getAbsolutePath() + " does not exist");
        this.configDirectory = configDirectory;

        File varDirectory = new File(processControllerHomeDirectory, SLASH + "var");
        if (!varDirectory.exists() || !varDirectory.isDirectory())
            logger.log(Level.WARNING, varDirectory.getAbsolutePath() + " does not exist; monitoring configurations will not be saved");
        this.monitoringConfigFile = varDirectory == null ? null : new File(varDirectory, "currentMonitoringConfig.xml");
        this.applianceLibexecDirectory = new File(processControllerHomeDirectory, ".." + SLASH + "Appliance" + SLASH + "libexec");
        this.masterPasswordManager = new MasterPasswordManager( new DefaultMasterPasswordFinder( new File(configDirectory, "omp.dat") ) );

        final Properties hostProps;
        try {
            hostProps = loadHostProperties();
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Couldn't find " + ExceptionUtils.getMessage( e ));
        } catch (IOException e) {
            throw new RuntimeException(ExceptionUtils.getMessage( e ), e);
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

        this.hostProps = hostProps;
        String jrePath = this.hostProps.getProperty(HOSTPROPERTIES_JRE);
        if (jrePath == null) jrePath = SyspropUtil.getProperty( "java.home" );
        final File javaBinary = new File(new File(jrePath), "bin" + SLASH + "java");
        if (!OSDetector.isWindows()) if (!javaBinary.exists() || !javaBinary.canExecute()) throw new IllegalStateException(javaBinary.getCanonicalPath() + " is not executable");
        this.javaBinary = javaBinary.getCanonicalFile();
        logger.info("Using java binary: " + javaBinary.getPath());

        this.patchesDirectory = this.hostProps.containsKey(HOSTPROPERTIES_PATCHES_DIR) ? new File(this.hostProps.getProperty(HOSTPROPERTIES_PATCHES_DIR)) :
                                new File(processControllerHomeDirectory, DEFAULT_PATCHES_SUBDIR);
        if (! patchesDirectory.isDirectory())
            throw new IllegalStateException("Invalid patches directory: " + patchesDirectory);

        this.patchesLog = this.hostProps.containsKey(HOSTPROPERTIES_PATCHES_LOG) ? this.hostProps.getProperty(HOSTPROPERTIES_PATCHES_LOG) :
                                processControllerHomeDirectory + SLASH + DEFAULT_PATCHES_LOGFILE;
        File logFile = new File(this.patchesLog);
        if (! logFile.exists() && ! logFile.getParentFile().exists() || logFile.isDirectory())
            throw new IllegalStateException("Invalid patch log file configured: " + patchesLog);

        this.sslPort = Integer.valueOf(hostProps.getProperty(HOSTPROPERTIES_SSL_PORT, Integer.toString(DEFAULT_SSL_REMOTE_MANAGEMENT_PORT)));
        this.sslIPAddress = hostProps.getProperty( HOSTPROPERTIES_SSL_IPADDRESS, getDefaultSslIpAddress() );
        this.sslKeypair = readSslKeypair( hostProps );
        this.sslProtocols = getStringArrayProperty( hostProps, HOSTPROPERTIES_SSL_PROTOCOLS, null );
        this.sslCiphers = getStringArrayProperty( hostProps, HOSTPROPERTIES_SSL_CIPHERS, DEFAULT_SSL_CIPHERS );
        this.trustedRemoteNodeManagementCerts = readTrustedNodeManagementCerts( hostProps );
        this.trustedRemoteNodeManagementThumbprints = readTrustedNodeManagementThumbprints( hostProps );
        this.trustedPatchCerts = readTrustedPatchCerts(hostProps);
        this.secret = hostProps.getProperty(HOSTPROPERTIES_SECRET);
        this.useSca = Boolean.valueOf(hostProps.getProperty("host.sca", "false"));

        reverseEngineerIps(hostConfig);
        this.apiEndpointPaths.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(PROCESSCONTROLLER_PROPERTIES));

        try {
            for (NodeConfig config : NodeConfigurationManager.loadNodeConfigs(false)) {
                logger.log(Level.INFO, "Detected node ''{0}''.", config.getName());
                config.setHost(hostConfig);
                hostConfig.getNodes().put(config.getName(), config);
            }
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error when detecting nodes.", ioe);
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, "Error when detecting nodes.", nfe);
        }

        this.host = hostConfig;
    }

    private Option<String[]> getStringArrayProperty( final Properties properties,
                                                     final String property,
                                                     final String defaultValue ) {
        return optional( properties.getProperty( property, defaultValue ) )
                .map( trim() )
                .filter( isNotEmpty() )
                .map( split( SPLITTER ) );
    }

    private Properties loadHostProperties() throws IOException {
        final File hostPropsFile = new File(getProcessControllerHomeDirectory(), "etc" + SLASH + "host.properties");
        if (!hostPropsFile.exists())
            throw new FileNotFoundException(hostPropsFile.getAbsolutePath());

        FileInputStream is = null;
        final Properties hostProps;
        try {
            is = new FileInputStream(hostPropsFile);
            hostProps = new Properties();
            hostProps.load(is);
        } catch (IOException e) {
            throw new IOException("Couldn't load " + hostPropsFile.getAbsolutePath(), e);
        } finally {
            ResourceUtils.closeQuietly( is );
        }

        return hostProps;
    }

    private void saveHostProperties( final Properties properties ) throws IOException {
        final File hostPropsFile = new File(getProcessControllerHomeDirectory(), "etc" + SLASH + "host.properties");
        if (!hostPropsFile.exists())
            throw new FileNotFoundException(hostPropsFile.getAbsolutePath());

        OutputStreamWriter osw = null;
        try {
            osw = new OutputStreamWriter( new FileOutputStream(hostPropsFile) );
            properties.store( osw, null );
        } catch (IOException e) {
            throw new IOException("Couldn't save " + hostPropsFile.getAbsolutePath(), e);
        } finally {
            ResourceUtils.closeQuietly( osw );
        }
    }

    private String getDefaultSslIpAddress() {
        return InetAddressUtil.getLocalHostAddress();
    }

    @PostConstruct
    void start() {
        final Pair<MonitoringConfiguration, Boolean> config = loadMonitoringConfigFromFile();
        if (config != null) {
            currentMonitoringConfiguration = config.left;
            responsibleForClusterMonitoring = config.right;
            monitoringKernel.setConfiguration(currentMonitoringConfiguration);
        }

    }

    private Pair<MonitoringConfiguration, Boolean> loadMonitoringConfigFromFile() {
        if (monitoringConfigFile == null) return null;
        if (!monitoringConfigFile.exists()) {
            logger.info("No monitoring configuration found in " + monitoringConfigFile);
            return null;
        }
        MonitoringConfiguration config = null;
        InputStream inputStream = null;
        try {
            inputStream = FileUtils.loadFileSafely(monitoringConfigFile.getPath());
            config = JAXB.unmarshal(inputStream, MonitoringConfiguration.class);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to read monitoring configuration", e);
            return null;
        } finally {
            ResourceUtils.closeQuietly(inputStream);
        }
        return new Pair<MonitoringConfiguration, Boolean>(config, true);
    }

    static File getHomeDirectory() {
        String s = ConfigFactory.getProperty( PC_HOMEDIR_PROPERTY );
        if (s == null) {
            final File f = new File( SyspropUtil.getProperty( "user.dir" ) );
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

    private Pair<X509Certificate[], PrivateKey> readSslKeypair(Properties hostProps) throws IOException, GeneralSecurityException {
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
            if (!(key instanceof PrivateKey)) throw new IllegalStateException(alias + " in " + keystoreFile.getAbsolutePath() + " is not an PrivateKey");
            chain = ks.getCertificateChain(alias);
            if (chain == null) throw new IllegalStateException("Couldn't get certificate chain for " + alias + " in " + keystoreFile.getAbsolutePath());
        } else {
            // Try to find a single usable keyEntry
            PrivateKey gotKey = null;
            Certificate[] gotChain = null;
            GeneralSecurityException thrown = null;

            final Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String s = aliases.nextElement();

                final Key tempKey;
                try {
                    tempKey = ks.getKey(s, keystorePass);
                    if (!(tempKey instanceof PrivateKey)) continue;
                    if (!("RSA".equalsIgnoreCase(tempKey.getAlgorithm()))) continue;
                } catch (GeneralSecurityException e) {
                    thrown = e;
                    continue;
                }

                final Certificate[] tempChain = ks.getCertificateChain(s);
                if (tempChain == null) continue;

                if (gotKey != null) throw new IllegalStateException(keystoreFile.getAbsolutePath() + " contains multiple usable key entries; must specify one using host.keystore.alias");
                gotKey = (PrivateKey)tempKey;
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
        return new Pair<X509Certificate[], PrivateKey>(xchain, (PrivateKey)key);
    }

    private String getRequiredProperty(Properties hostProps, final String what) {
        final String that = hostProps.getProperty(what);
        if (that == null) throw new IllegalArgumentException(what + " not found");
        return that;
    }

    private Set<X509Certificate> readTrustedNodeManagementCerts(Properties hostProps) throws GeneralSecurityException, IOException {
        final String trustStoreEnabled = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENT_ENABLED);
        if (!"true".equalsIgnoreCase(trustStoreEnabled)) {
            logger.info("Remote node management disabled");
            return new HashSet<X509Certificate>();
        }

        final String trustStoreFilename = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENT_TRUSTSTORE_FILE);
        String trustStoreType;
        File trustStoreFile;
        if (trustStoreFilename == null) {
            trustStoreFile = new File(configDirectory, DEFAULT_REMOTE_MANAGEMENT_TRUSTSTORE_FILENAME);
            if (!trustStoreFile.exists()) {
                logger.info("No remote node management truststore found; continuing with remote node management disabled");
                return new HashSet<X509Certificate>();
            }
            trustStoreType = "PKCS12";
        } else {
            trustStoreFile = new File(trustStoreFilename);
            if (!trustStoreFile.exists()) throw new IllegalArgumentException("Can't find remote node management truststore at " + trustStoreFile.getAbsolutePath());

            trustStoreType = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENT_TRUSTSTORE_TYPE, "PKCS12");
        }

        final String encryptedPassword = getRequiredProperty(hostProps, HOSTPROPERTIES_NODEMANAGEMENT_TRUSTSTORE_PASSWORD);

        return readCerts(trustStoreType, trustStoreFile, encryptedPassword);
    }

    private void saveTrustedNodeManagementCert( final Properties hostProps,
                                                final X509Certificate certificate ) throws GeneralSecurityException, IOException {
        final String trustStoreFilename = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENT_TRUSTSTORE_FILE);
        final File trustStoreFile;
        if ( trustStoreFilename == null ) {
            trustStoreFile = new File(configDirectory, DEFAULT_REMOTE_MANAGEMENT_TRUSTSTORE_FILENAME);
        } else {
            trustStoreFile = new File(trustStoreFilename);
        }

        final String trustStoreType = hostProps.getProperty(HOSTPROPERTIES_NODEMANAGEMENT_TRUSTSTORE_TYPE, "PKCS12");

        if ( !trustStoreFile.exists() ) {
            throw new FileNotFoundException( trustStoreFilename );
        }

        final String encryptedPassword = hostProps.getProperty( HOSTPROPERTIES_NODEMANAGEMENT_TRUSTSTORE_PASSWORD, "" );

        addCert(trustStoreType, trustStoreFile, encryptedPassword, certificate);
    }

    private Set<String> readTrustedNodeManagementThumbprints( final Properties hostProps ) {
        final Set<String> thumbprints = new HashSet<String>();

        final String value = hostProps.getProperty( HOSTPROPERTIES_NODEMANAGEMENT_THUMBPRINT );
        if ( value != null ) {
            thumbprints.add( value );
        }

        return thumbprints;
    }

    private Set<X509Certificate> readTrustedPatchCerts(Properties hostProps) throws IOException, GeneralSecurityException {
        String patchUserTrustStore = hostProps.getProperty(HOSTPROPERTIES_PATCH_TRUSTSTORE_FILE);
        if (patchUserTrustStore == null) {
            logger.log(Level.INFO, "No user trusted certificates for patch verification.");
            return new HashSet<X509Certificate>();
        }

        File trustedPatchCerts = new File(patchUserTrustStore);
        if (!trustedPatchCerts.exists())
            throw new IllegalArgumentException("User patch certificates truststore not found: " + patchUserTrustStore);
        return readCerts(
            getRequiredProperty(hostProps, HOSTPROPERTIES_PATCH_TRUSTSTORE_TYPE),
            trustedPatchCerts,
            getRequiredProperty(hostProps, HOSTPROPERTIES_PATCH_TRUSTSTORE_PASSWORD)
        );
    }

    private Set<X509Certificate> readCerts(String trustStoreType, File trustStoreFile, String encryptedPassword) throws GeneralSecurityException, IOException {

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
                                   "Read trusted certificate from {0}: dn=\"{1}\", serial=\"{2}\", thumbprintSha1=\"{3}\"",
                                   new Object[] {trustStoreFile.getName(), xc.getSubjectDN().getName(), xc.getSerialNumber(), CertUtils.getThumbprintSHA1(xc)});
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

    private void addCert( final String trustStoreType,
                          final File trustStoreFile,
                          final String encryptedPassword,
                          final X509Certificate certificate ) throws GeneralSecurityException, IOException {
        final char[] keystorePass = masterPasswordManager.decryptPasswordIfEncrypted( encryptedPassword );
        final KeyStore ks = KeyStore.getInstance(trustStoreType);
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(trustStoreFile);
            ks.load(fis, keystorePass);
            final String alias = "trustedCert-" + CertUtils.getThumbprintSHA1(certificate) + "-" + System.currentTimeMillis();
            ks.setCertificateEntry( alias, certificate );

            fos = new FileOutputStream(trustStoreFile);
            ks.store(fos, keystorePass);
        } finally {
            ResourceUtils.closeQuietly(fis);
            ResourceUtils.closeQuietly(fos);
        }
    }

    private String getLocalHostname(Properties hostProps) {
        String hostname = hostProps.getProperty("host.hostname");
        if (hostname != null) {
            logger.info("hostname is " + hostname);
            return hostname;
        }

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.log(Level.WARNING, "Unable to get local hostname '"+ExceptionUtils.getMessage(e)+"'; using localhost", ExceptionUtils.getDebugException(e));
            hostname = "localhost";
        }
        return hostname;
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
    public String getServicesContextBasePath() {
        return SERVICES_CONTEXT_BASE_PATH;
    }

    @Override
    public String getApiEndpoint(ApiWebEndpoint endpoint) {
        String address = getSslIPAddress();
        if (InetAddressUtil.isValidIpv6Address(address))
            address = "[" + address + "]";
        return "https://" + InetAddressUtil.getHostForUrl(address) + ":" + getSslPort() + SERVICES_CONTEXT_BASE_PATH + apiEndpointPaths.getProperty(endpoint.getPropName());
    }

    @Override
    public void addServiceNode( final NodeConfig node ) throws IOException {
        refreshNodeConfiguration( node.getName() );
    }

    @Override
    public void updateServiceNode( final NodeConfig node ) throws IOException {
        // TODO what kinds of NodeConfig changes might necessitate a restart?
        refreshNodeConfiguration( node.getName() );
    }

    @Override
    public File getNodeBaseDirectory() {
        return nodeBaseDirectory;
    }

    @Override
    public File getPatchesDirectory() {
        return patchesDirectory;
    }

    @Override
    public String getPatchesLog() {
        return patchesLog;
    }

    @Override
    public File getApplianceLibexecDirectory() {
        return applianceLibexecDirectory;
    }

    @Override
    public Pair<X509Certificate[], PrivateKey> getSslKeypair() {
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
    public Option<String[]> getSslProtocols() {
        return sslProtocols;
    }

    @Override
    public Option<String[]> getSslCiphers() {
        return sslCiphers;
    }

    @Override
    public Set<X509Certificate> getTrustedRemoteNodeManagementCerts() {
        return trustedRemoteNodeManagementCerts;
    }

    @Override
    public Set<String> getTrustedRemoteNodeManagementCertThumbprints() {
        return trustedRemoteNodeManagementThumbprints;
    }

    @Override
    public void acceptTrustedRemoteNodeManagementCert( final X509Certificate certificate ) {
        trustedRemoteNodeManagementCerts.add( certificate );
        trustedRemoteNodeManagementThumbprints.clear();

        logger.info( "Accepting certificate for remote management '"+certificate.getSubjectDN()+"'" );
        try {
            final Properties hostProperties = loadHostProperties();
            hostProperties.remove( HOSTPROPERTIES_NODEMANAGEMENT_THUMBPRINT );

            saveTrustedNodeManagementCert( hostProps, certificate );
            saveHostProperties( hostProperties );
        } catch ( IOException e ) {
            logger.log( Level.WARNING, "Error accepting certificate for configured thumbprint.", e );
        } catch ( GeneralSecurityException e ) {
            logger.log( Level.WARNING, "Error accepting certificate for configured thumbprint.", e );
        }
    }

    @Override
    public Set<X509Certificate> getTrustedPatchCerts() {
        return trustedPatchCerts;
    }

    @Override
    public String getHostSecret() {
        return secret;
    }

    @Override
    public File getJavaBinary() {
        return javaBinary;
    }

    @Override
    public synchronized void deleteNode(final String nodeName) throws DeleteException, IOException {
        try {
            NodeConfigurationManager.deleteNodeConfig( nodeName );
        } catch ( NodeConfigurationManager.DeleteNodeConfigurationException dnce ) {
            logger.warning("Unable to delete configuration for node '"+nodeName+"' file '"+dnce.getNodeConfigFilePath()+"'.");
            throw new DeleteException("Unable to delete configuration for node '"+nodeName+"'.");
        }
        logger.log(Level.INFO, "Removing node configuration ''{0}''.", nodeName);
        host.getNodes().remove(nodeName);
    }

    @Override
    public MonitoringConfiguration getCurrentMonitoringConfiguration() {
        return null;
    }

    @Override
    public boolean isResponsibleForClusterMonitoring() {
        return responsibleForClusterMonitoring;
    }

    @Override
    public boolean isUseSca() {
        return useSca;
    }

    @Override
    public int getIntProperty(String propertyName, int defaultValue) {
        Object val = hostProps.get(propertyName);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, "Bad value for node property: " + propertyName + ": " + ExceptionUtils.getMessage(nfe));
            return defaultValue;
        }
    }

    @Override
    public boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        boolean value = defaultValue;
        String val = hostProps.getProperty(propertyName);
        if ( val != null ) {
            if ( "true".equalsIgnoreCase(val) ) {
                value = true;
            } else if ( "false".equalsIgnoreCase(val) ) {
                value = false;
            } else {
                logger.warning("Ignoring invalid property value ('"+val+"') for '"+propertyName+"'.");
            }
        }

        return value;
    }

    @Override
    public void pushMonitoringConfiguration(final MonitoringConfiguration config) {
        this.responsibleForClusterMonitoring = config != null && config.isResponsibleForClusterMonitoring();
        this.currentMonitoringConfiguration = config;

        monitoringKernel.setConfiguration(config);

        if (monitoringConfigFile == null) return;

        if (config == null) {
            if (!monitoringConfigFile.exists()) {
                logger.log(Level.INFO, "Monitoring configuration has been unset, but configuration file was already deleted");
                return;
            }

            try {
                if (!FileUtils.deleteFileSafely(monitoringConfigFile.getPath())) {
                    logger.log(Level.WARNING, "Unable to delete unset monitoring configuration");
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to delete unset monitoring configuration", e);
            }
        } else {
            try {
                FileUtils.saveFileSafely(monitoringConfigFile.getPath(), new FileUtils.Saver() {
                    @Override
                    public void doSave(FileOutputStream fos) throws IOException {
                        JAXB.marshal(config, fos);
                    }
                });
            } catch (Exception e) {
                logger.log(Level.WARNING, "Couldn't save current monitoring configuration; it will need to be reconfigured on startup", e);
            }
        }
    }

    private void refreshNodeConfiguration( final String nodeName ) throws IOException  {
        NodeConfig config = NodeConfigurationManager.loadNodeConfig( nodeName, false );
        logger.log(Level.INFO, "Reloaded node configuration ''{0}''.", config.getName());
        config.setHost(host);
        host.getNodes().put( nodeName, config );
    }

}
