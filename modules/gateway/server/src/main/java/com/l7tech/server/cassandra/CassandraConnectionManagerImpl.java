package com.l7tech.server.cassandra;

import com.ca.datasources.cassandra.CassandraUtil;
import com.datastax.driver.core.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationEvent;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 10/30/14
 */
public class CassandraConnectionManagerImpl implements CassandraConnectionManager, PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(CassandraConnectionManagerImpl.class.getName());

    public static final String HOST_DISTANCE = "hostDistance";
    public static final String CORE_CONNECTION_PER_HOST = "coreConnectionsPerHost";
    public static final String MAX_CONNECTION_PER_HOST = "maxConnectionPerHost";
    public static final String MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD = "maxSimultaneousRequestsPerConnectionThreshold";
    public static final String MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD = "minSimultaneousRequestsPerConnectionThreshold";

    public static final int CORE_CONNECTION_PER_HOST_DEF = 1;
    public static final int CORE_CONNECTION_PER_HOST_LOCAL_DEF = 2;
    public static final int MAX_CONNECTION_PER_HOST_DEF = 2;
    public static final int MAX_CONNECTION_PER_HOST_LOCAL_DEF = 8;
    public static final int MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD_DEF = 128;
    public static final int MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD_DEF = 25;

    public static final String CONNECTION_TIMEOUT_MILLIS = "connectTimeoutMillis";
    public static final String READ_TIMEOUT_MILLIS = "readTimeoutMillis";
    public static final String KEEP_ALIVE = "keepAlive";
    public static final String RECEIVE_BUFFER_SIZE = "receiveBufferSize";
    public static final String REUSE_ADDRESS = "reuseAddress";
    public static final String SEND_BUFFER_SIZE = "sendBufferSize";
    public static final String SO_LINGER = "soLinger";
    public static final String TCP_NO_DELAY = "tcpNoDelay";

    private static final long DEFAULT_CONNECTION_MAX_AGE = 0L;
    private static final long DEFAULT_CONNECTION_MAX_IDLE = TimeUnit.MINUTES.toMillis(30);
    private static final int DEFAULT_CONNECTION_CACHE_SIZE = 20;
    public static final int DEFAULT_FETCH_SIZE = 5000;
    public static final String QUERY_FETCH_SIZE = "fetchSize";

    private final ConcurrentHashMap<String, CassandraConnectionHolder> cassandraConnections = new ConcurrentHashMap<>();
    private final CassandraConnectionEntityManager cassandraEntityManager;
    private final Config config;
    private final SecurePasswordManager securePasswordManager;
    private final TrustManager trustManager;
    private final SecureRandom secureRandom;
    private final Audit auditor = new LoggingAudit(logger);
    private final Timer timer;
    private final AtomicReference<ConnectionCacheConfig> connectionCacheConfig = new AtomicReference<>(new ConnectionCacheConfig(0, 30, 20));
    private static final String PROP_CACHE_CLEAN_INTERVAL = "com.l7tech.server.cassandra.connection.cacheCleanInterval";
    private static final long CACHE_CLEAN_INTERVAL = ConfigFactory.getLongProperty(PROP_CACHE_CLEAN_INTERVAL, 15 * 60 * 1000L);


    static {
        if (!JdkLoggerConfigurator.debugState()) {
            disableCassandraConnectionLogger();
        }
    }

    private static void disableCassandraConnectionLogger() {
        try {
            // Suppresses stack trace from Datastax Cassandra driver during conenction failover.
            org.slf4j.Logger connLogger = org.slf4j.LoggerFactory.getLogger("com.datastax.driver.core");

            if (connLogger == null) {
                logger.log(Level.WARNING, "Unable to get slf4j root logger.");
                return;
            }

            Field field = connLogger.getClass().getDeclaredField("logger");

            if (field != null) {
                field.setAccessible(true);
                Logger javaLogger = (Logger) field.get(connLogger);
                javaLogger.setLevel(Level.OFF);
            } else {
                logger.log(Level.WARNING, "Unable to get logger field from " + connLogger.getClass().getName());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to turn off logs from Datastax Cassandra driver Connection: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    public CassandraConnectionManagerImpl(CassandraConnectionEntityManager cassandraEntityManager, Config config,
                                          SecurePasswordManager securePasswordManager, TrustManager trustManager,
                                          SecureRandom secureRandom) {
        this.cassandraEntityManager = cassandraEntityManager;
        this.config = config;
        this.securePasswordManager = securePasswordManager;
        this.trustManager = trustManager;
        this.secureRandom = secureRandom;
        this.timer = new ManagedTimer("CassandraConnectionManager-CacheCleanup");
        timer.schedule(new CacheCleanupTask(cassandraConnections, connectionCacheConfig), 30797, CACHE_CLEAN_INTERVAL);
        updateConfig();
    }

    @Override
    public CassandraConnectionHolder getConnection(String name) {

        CassandraConnectionHolder connectionHolder = cassandraConnections.get(name);
        if(connectionHolder == null) {
            CassandraConnection entity = null;
            //first find connection in the data source
            try {
                entity = cassandraEntityManager.getCassandraConnectionEntity(name);
            } catch (FindException e) {
                auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_CANNOT_CONNECT, new String[]{name, "Unable to find Cassandra connection name:" + name}, ExceptionUtils.getDebugException(e) );
                return null;
            }

            if (entity == null) {
                auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_CANNOT_CONNECT, new String[]{name, "Cassandra connection does not exist."});
                return null;
            }

            //create a holder and add it to the list of connections
            if(entity.isEnabled()) {
                connectionHolder = createConnection(entity);
                if(connectionHolder != null) {
                    cassandraConnections.put(name, connectionHolder);
                }
            }
            else {
                auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_DISABLED, name);
            }
        }
        return connectionHolder;
    }

    @Override
    public void closeAllConnections() {
        Iterator<CassandraConnectionHolder> iter = cassandraConnections.values().iterator();
        while(iter.hasNext()) {
            final CassandraConnectionHolder connectionHolder = iter.next();
            closeConnection(connectionHolder);
            iter.remove();
        }

    }

    @Override
    public void addConnection(CassandraConnection cassandraConnectionEntity) {
        getConnection(cassandraConnectionEntity.getName());
    }

    @Override
    public void removeConnection(CassandraConnection cassandraConnectionEntity) {
        CassandraConnectionHolder connection = cassandraConnections.remove(cassandraConnectionEntity.getName());
        closeConnection(connection);
    }

    @Override
    public void removeConnection(Goid goid) {
        CassandraConnection entity = null;
        try {
            entity = cassandraEntityManager.findByPrimaryKey(goid);
            removeConnection(entity);
        } catch (FindException e) {
            auditor.logAndAudit(AssertionMessages.CASSANDRA_CANNOT_REMOVE_CONNECTION, new String[]{"Unable to find appropriate connection in the data source."}, ExceptionUtils.getDebugException(e) );
        }
        //this should never happen
        if(entity == null) {
            Iterator<CassandraConnectionHolder> iter = cassandraConnections.values().iterator();
            while(iter.hasNext()) {
                CassandraConnectionHolder holder = iter.next();
                if(holder.getCassandraConnectionEntity().getGoid().equals(goid)){
                    closeConnection(holder);
                    iter.remove();
                }
            }
        }

    }

    @Override
    public void updateConnection(CassandraConnection cassandraConnectionEntity) throws UpdateException {
        // Search through cached connections by goid because the name field could have been updated.
        CassandraConnectionHolder cachedConnection = getCachedConnection(cassandraConnectionEntity.getGoid());

        // If cache exists, remove it and add back an updated one.
        if (cachedConnection != null) {
            CassandraConnectionHolder newConnectionHolder = createConnection(cassandraConnectionEntity);
            if (newConnectionHolder != null) {
                removeConnection(cachedConnection.getCassandraConnectionEntity());
                if (cassandraConnectionEntity.isEnabled()) {
                    cassandraConnections.put(cassandraConnectionEntity.getName(), newConnectionHolder);
                }
            } else {
                throw new UpdateException("New cached connection cannot be created. Please check the settings.");
            }
        }
    }

    private CassandraConnectionHolder getCachedConnection(Goid goid) {
        for (CassandraConnectionHolder holder : cassandraConnections.values()) {
            if (holder.getCassandraConnectionEntity().getGoid().equals(goid)) {
                return holder;
            }
        }
        return null;
    }

    protected CassandraConnectionHolder createConnection(CassandraConnection cassandraConnectionEntity){
        Cluster cluster;
        Session session;

        try {
            cluster = this.buildCluster(cassandraConnectionEntity);

            //create our cassandra session
            if (StringUtils.isNotBlank(cassandraConnectionEntity.getKeyspaceName())) {
                session = cluster.connect(cassandraConnectionEntity.getKeyspaceName());
            } else {
                session = cluster.connect();
            }
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_CANNOT_CONNECT, new String[]{cassandraConnectionEntity.getName(), e.getMessage()}, ExceptionUtils.getDebugException(e));
            return null;
        }

        return new CassandraConnectionHolderImpl(cassandraConnectionEntity, cluster, session);
    }

    @Override
    public void testConnection(CassandraConnection cassandraConnectionEntity) throws Exception {
        Cluster cluster = null;
        Session session = null;

        try {
            cluster = this.buildCluster(cassandraConnectionEntity);

            //create our cassandra session
            if (StringUtils.isNotBlank(cassandraConnectionEntity.getKeyspaceName())) {
                session = cluster.connect(cassandraConnectionEntity.getKeyspaceName());
            } else {
                session = cluster.connect();
            }
        } finally {
            if (session != null) session.close();
            if (cluster != null) cluster.close();
        }
    }

    private Cluster buildCluster(CassandraConnection cassandraConnectionEntity) throws Exception {
        //start cluster creation
        Cluster.Builder clusterBuilder = Cluster.builder();
        //add basic cluster info
        addBasicClusterInfo(clusterBuilder, cassandraConnectionEntity, cassandraConnectionEntity.getContactPointsAsArray());
        //add pooling option
        addPoolingOptions(clusterBuilder, cassandraConnectionEntity);
        //add socket option
        addSocketOptions(clusterBuilder, cassandraConnectionEntity);
        //add ssl option
        addSSLOptions(clusterBuilder, cassandraConnectionEntity);
        //add query options
        addQueryOptions(clusterBuilder, cassandraConnectionEntity);
        //build cluster
        return clusterBuilder.build();
    }

    private void addQueryOptions(Cluster.Builder clusterBuilder, CassandraConnection cassandraConnectionEntity) {
        int defaultFetchSize = config.getIntProperty(ServerConfigParams.PARAM_CASSANDRA_MAX_RECORDS, DEFAULT_FETCH_SIZE);
        Map<String, String> connectionProperties = cassandraConnectionEntity.getProperties();
        int fetchSize = connectionProperties != null ? CassandraUtil.getIntOrDefault(connectionProperties.get(QUERY_FETCH_SIZE), defaultFetchSize) : defaultFetchSize;
        QueryOptions options = new QueryOptions();
        options.setFetchSize(fetchSize);
        clusterBuilder.withQueryOptions(options);
    }

    private void addBasicClusterInfo(Cluster.Builder clusterBuilder,
                                     CassandraConnection cassandraConnectionEntity,
                                     String[] contactPoints) throws ParseException, FindException {
        clusterBuilder.withCredentials(cassandraConnectionEntity.getUsername(), new String(getPassword(cassandraConnectionEntity))).
                withPort(Integer.parseInt(cassandraConnectionEntity.getPort())).
                addContactPoints(contactPoints).
                withCompression(ProtocolOptions.Compression.valueOf(cassandraConnectionEntity.getCompression()));
    }

    private void addPoolingOptions(Cluster.Builder clusterBuilder, CassandraConnection cassandraConnectionEntity) {
        Map<String,String> connectionProperties = cassandraConnectionEntity.getProperties();
        PoolingOptions poolingOptions = new PoolingOptions();
        String defaultHostDistance = config.getProperty(ServerConfigParams.PARAM_CASSANDRA_HOST_DISTANCE, "LOCAL");
        HostDistance defaultHD = HostDistance.valueOf(defaultHostDistance);
        if(defaultHD == null) {
            auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Invalid value of the property " + ServerConfigParams.PARAM_CASSANDRA_HOST_DISTANCE + ". Using default value.");
            defaultHD = HostDistance.LOCAL;
        }
        HostDistance hd = null;
        if(connectionProperties != null) {
            String hostDistance = connectionProperties.get(HOST_DISTANCE);
            if(hostDistance != null) {
                hd = HostDistance.valueOf(hostDistance);
            }
        }

        if (hd == null) {
            hd = defaultHD;
        }

        int defaultCoreConnectionPerHost = config.getIntProperty(ServerConfigParams.PARAM_CASSANDRA_CORE_CONNECTIONS_PER_HOST, (hd == HostDistance.LOCAL? CORE_CONNECTION_PER_HOST_LOCAL_DEF: CORE_CONNECTION_PER_HOST_DEF));
        int coreConnectionPerHost = connectionProperties != null? CassandraUtil.getIntOrDefault(connectionProperties.get(CORE_CONNECTION_PER_HOST), defaultCoreConnectionPerHost) : defaultCoreConnectionPerHost ;

        int defaultMaxConnectionPerHost = config.getIntProperty(ServerConfigParams.PARAM_CASSANDRA_MAX_CONNECTIONS_PER_HOST, (hd == HostDistance.LOCAL? MAX_CONNECTION_PER_HOST_LOCAL_DEF: MAX_CONNECTION_PER_HOST_DEF));
        int maxConnectionPerHost = connectionProperties != null? CassandraUtil.getIntOrDefault(connectionProperties.get(MAX_CONNECTION_PER_HOST), defaultMaxConnectionPerHost) : defaultMaxConnectionPerHost;

        int defaultMaxSimultaneousRequests = config.getIntProperty(ServerConfigParams.PARAM_CASSANDRA_MAX_SIMULTANEOUS_REQUESTS, MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD_DEF);
        int maxSimultaneousRequestsPerConnectionThreshold = connectionProperties != null? CassandraUtil.getIntOrDefault(connectionProperties.get(MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD), defaultMaxSimultaneousRequests) : defaultMaxSimultaneousRequests;

        int defaultMinSimultaneousRequests = config.getIntProperty(ServerConfigParams.PARAM_CASSANDRA_MIN_SIMULTANEOUS_REQUESTS, MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD_DEF);
        int minSimultaneousRequestsPerConnectionThreshold = connectionProperties != null? CassandraUtil.getIntOrDefault(connectionProperties.get(MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD), defaultMinSimultaneousRequests) : defaultMinSimultaneousRequests;

        //TODO: check the policy type so we can determine host distance setting or default

        poolingOptions.setMaxConnectionsPerHost(hd, maxConnectionPerHost);
        poolingOptions.setCoreConnectionsPerHost(hd, coreConnectionPerHost);
        poolingOptions.setMaxSimultaneousRequestsPerConnectionThreshold(hd, maxSimultaneousRequestsPerConnectionThreshold);
        poolingOptions.setMinSimultaneousRequestsPerConnectionThreshold(hd, minSimultaneousRequestsPerConnectionThreshold);

        clusterBuilder.withPoolingOptions(poolingOptions);
    }

    private void addSocketOptions(Cluster.Builder clusterBuilder, CassandraConnection cassandraConnectionEntity) {
        SocketOptions socketOptions = new SocketOptions();
        Map<String, String> connectionProperties = cassandraConnectionEntity.getProperties();

        setSocketOptionsProp(socketOptions, connectionProperties, CONNECTION_TIMEOUT_MILLIS);
        setSocketOptionsProp(socketOptions, connectionProperties, READ_TIMEOUT_MILLIS);
        setSocketOptionsProp(socketOptions, connectionProperties, KEEP_ALIVE);
        setSocketOptionsProp(socketOptions, connectionProperties, RECEIVE_BUFFER_SIZE);
        setSocketOptionsProp(socketOptions, connectionProperties, REUSE_ADDRESS);
        setSocketOptionsProp(socketOptions, connectionProperties, SEND_BUFFER_SIZE);
        setSocketOptionsProp(socketOptions, connectionProperties, SO_LINGER);
        setSocketOptionsProp(socketOptions, connectionProperties, TCP_NO_DELAY);

        clusterBuilder.withSocketOptions(socketOptions);
    }

    private void setSocketOptionsProp(SocketOptions socketOptions, Map<String, String> connectionProperties, String propName) {
        if (connectionProperties.containsKey(propName)) {
            Integer propInt;
            boolean propBool;

            switch (propName) {
                case CONNECTION_TIMEOUT_MILLIS:
                    propInt = CassandraUtil.getInteger(connectionProperties.get(propName));
                    if (propInt != null) {
                        socketOptions.setConnectTimeoutMillis(propInt);
                    } else {
                        auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Unable to read property " + propName + ". Using default value.");
                    }
                    break;

                case READ_TIMEOUT_MILLIS:
                    propInt = CassandraUtil.getInteger(connectionProperties.get(propName));
                    if (propInt != null) {
                        socketOptions.setReadTimeoutMillis(propInt);
                    } else {
                        auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Unable to read property " + propName + ". Using default value.");
                    }
                    break;

                case KEEP_ALIVE:
                    String val = connectionProperties.get(propName);
                    if(val != null && (Boolean.TRUE.toString().equalsIgnoreCase(val) || Boolean.FALSE.toString().equalsIgnoreCase(val))) {
                        propBool = Boolean.parseBoolean(val);
                    }
                    else {
                        propBool = config.getBooleanProperty(ServerConfigParams.PARAM_CASSANDRA_KEEP_ALIVE, false);
                    }

                    if (propBool) {
                        socketOptions.setKeepAlive(propBool);
                    } else {
                        auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Unable to read property " + propName + ". Using default value.");
                    }
                    break;

                case RECEIVE_BUFFER_SIZE:
                    propInt = CassandraUtil.getInteger(connectionProperties.get(propName));
                    if (propInt != null) {
                        socketOptions.setReceiveBufferSize(propInt);
                    } else {
                        auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Unable to read property " + propName + ". Using default value.");
                    }
                    break;

                case REUSE_ADDRESS:
                    propBool = Boolean.parseBoolean(connectionProperties.get(propName));
                    if (propBool) {
                        socketOptions.setReuseAddress(propBool);
                    } else {
                        auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Unable to read property " + propName + ". Using default value.");
                    }
                    break;

                case SEND_BUFFER_SIZE:
                    propInt = CassandraUtil.getInteger(connectionProperties.get(propName));
                    if (propInt != null) {
                        socketOptions.setSendBufferSize(propInt);
                    } else {
                        auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Unable to read property " + propName + ". Using default value.");
                    }
                    break;

                case SO_LINGER:
                    propInt = CassandraUtil.getInteger(connectionProperties.get(propName));
                    if (propInt != null) {
                        socketOptions.setSoLinger(propInt);
                    } else {
                        auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Unable to read property " + propName + ". Using default value.");
                    }
                    break;

                case TCP_NO_DELAY:
                    propBool = Boolean.parseBoolean(connectionProperties.get(propName));
                    if (propBool) {
                        socketOptions.setTcpNoDelay(propBool);
                    } else {
                        auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Unable to read property " + propName + ". Using default value.");
                    }
                    break;

                default:
                    auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Unrecognized property " + propName + ". Ignored.");

            }
        }
    }

    private void addSSLOptions(Cluster.Builder clusterBuilder,
                               CassandraConnection cassandraConnectionEntity) throws NoSuchAlgorithmException, KeyManagementException {

        if (cassandraConnectionEntity.isSsl()) {

            TrustManager[] trustManagers = new TrustManager[]{trustManager};

            //create the SSLContext using "TLSv1" protocol
            Provider provider = JceProvider.getInstance().getProviderFor("SSLContext.TLSv1");
            SSLContext sslContext = SSLContext.getInstance("TLSv1", provider);

            //initialize the SSLContext with the an array of trustmanager, and random number generator
            sslContext.init(null, trustManagers, secureRandom);

            //Create the cassandra SSL option
            //initialize with our SSLContext and ciphers
            //(using default ciphers that come with cassandra java driver)
            SSLOptions sslOptions = new SSLOptions(sslContext, SSLOptions.DEFAULT_SSL_CIPHER_SUITES);

            //add the ssl options to our cluster
            clusterBuilder.withSSL(sslOptions);
        }
    }

    private boolean useAuthentication(CassandraConnection entity) {
        return  (entity.getUsername() != null && !entity.getUsername().isEmpty()) ||
                ( !Goid.isDefault(entity.getPasswordGoid()) );
    }

    private char[] getPassword(CassandraConnection entity) throws FindException, ParseException {
        if (entity.getPasswordGoid() != null && !Goid.isDefault(entity.getPasswordGoid())) {
            SecurePassword securePassword = securePasswordManager.findByPrimaryKey(entity.getPasswordGoid());
            if (securePassword == null) {
                return new char[]{};
            }
            return securePasswordManager.decryptPassword(securePassword.getEncodedPassword());
        } else {
            return new char[]{};
        }
    }

    private void closeConnection(CassandraConnectionHolder connection) {
        if(connection != null) {
            Session session = connection.getSession();
            if(session != null) session.close();
            Cluster cluster = connection.getCluster();
            if(cluster != null) cluster.close();
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // Do nothing
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        updateConfig();
    }

    private void updateConfig() {
        logger.config("(Re)loading cache configuration.");

        final long maximumAge = config.getTimeUnitProperty("cassandra.maxConnectionCacheAge", DEFAULT_CONNECTION_MAX_AGE);
        final long maximumIdleTime = config.getTimeUnitProperty("cassandra.maxConnectionCacheIdleTime", DEFAULT_CONNECTION_MAX_IDLE);
        final int maximumSize = config.getIntProperty("cassandra.maxConnectionCacheSize", DEFAULT_CONNECTION_CACHE_SIZE);

        connectionCacheConfig.set(new ConnectionCacheConfig(
                        rangeValidate(maximumAge, DEFAULT_CONNECTION_MAX_AGE, 0L, Long.MAX_VALUE, "Cassandra Connection Maximum Age"),
                        rangeValidate(maximumIdleTime, DEFAULT_CONNECTION_MAX_IDLE, 0L, Long.MAX_VALUE, "Cassandra Connection Maximum Idle"),
                        rangeValidate(maximumSize, DEFAULT_CONNECTION_CACHE_SIZE, 0, Integer.MAX_VALUE, "Cassandra Connection Cache Size"))
        );
    }

    private <T extends Number> T rangeValidate(final T value,
                                               final T defaultValue,
                                               final T min,
                                               final T max,
                                               final String description) {
        T validatedValue;

        if (value.longValue() < min.longValue()) {
            logger.log(Level.WARNING,
                    "Configuration value for {0} is invalid ({1} minimum is {2}), using default value ({3}).",
                    new Object[]{description, value, min, defaultValue});
            validatedValue = defaultValue;
        } else if (value.longValue() > max.longValue()) {
            logger.log(Level.WARNING,
                    "Configuration value for {0} is invalid ({1} maximum is {2}), using default value ({3}).",
                    new Object[]{description, value, max, defaultValue});
            validatedValue = defaultValue;
        } else {
            validatedValue = value;
        }

        return validatedValue;
    }

    /**
     * Bean for cache configuration.
     */
    private static final class ConnectionCacheConfig {
        private final long maximumAge;
        private final long maximumIdleTime;
        private final int maximumSize;

        private ConnectionCacheConfig(final long maximumAge,
                                      final long maximumIdleTime,
                                      final int maximumSize) {
            this.maximumAge = maximumAge;
            this.maximumIdleTime = maximumIdleTime;
            this.maximumSize = maximumSize;
        }
    }

    // For unit testing
    public int getConnectionCacheSize() {
        return cassandraConnections.size();
    }

    /**
     * Timer task to remove expired or surplus connections from the cache.
     *
     * <p>When the cache size is exceeded the oldest Cassandra connections are
     * removed first.</p>
     */
    private static final class CacheCleanupTask extends TimerTask {
        private final ConcurrentHashMap<String, CassandraConnectionHolder> cassandraConnections;
        private final AtomicReference<ConnectionCacheConfig> cacheConfigReference;

        private CacheCleanupTask(final ConcurrentHashMap<String, CassandraConnectionHolder> cassandraConnections,
                                 final AtomicReference<ConnectionCacheConfig> cacheConfigReference) {
            this.cassandraConnections = cassandraConnections;
            this.cacheConfigReference = cacheConfigReference;
        }

        @Override
        public void run() {
            final ConnectionCacheConfig cacheConfig = cacheConfigReference.get();
            final long timeNow = System.currentTimeMillis();
            final int overSize = cassandraConnections.size() - cacheConfig.maximumSize;
            final Set<Map.Entry<String, CassandraConnectionHolder>> evictionCandidates = new TreeSet<>(
                    new Comparator<Map.Entry<String, CassandraConnectionHolder>>() {
                        @Override
                        public int compare(final Map.Entry<String, CassandraConnectionHolder> e1,
                                           final Map.Entry<String, CassandraConnectionHolder> e2) {
                            return Long.valueOf(e1.getValue().getCreatedTime()).compareTo(e2.getValue().getCreatedTime());
                        }
                    }
            );

            for (final Map.Entry<String, CassandraConnectionHolder> cassandraConnectionHolderEntry : cassandraConnections.entrySet()) {
                if (cacheConfig.maximumAge > 0 && (timeNow - cassandraConnectionHolderEntry.getValue().getCreatedTime()) > cacheConfig.maximumAge) {
                    cassandraConnections.remove(cassandraConnectionHolderEntry.getKey());
                } else if (cacheConfig.maximumIdleTime > 0 && (timeNow - cassandraConnectionHolderEntry.getValue().getLastAccessTime().get()) > cacheConfig.maximumIdleTime) {
                    cassandraConnections.remove(cassandraConnectionHolderEntry.getKey());
                } else if (overSize > 0) {
                    evictionCandidates.add(cassandraConnectionHolderEntry);
                }
            }

            // evict oldest first to reduce cache size
            final Iterator<Map.Entry<String, CassandraConnectionHolder>> evictionIterator = evictionCandidates.iterator();
            for (int i = 0; i < overSize && evictionIterator.hasNext(); i++) {

                final Map.Entry<String, CassandraConnectionHolder> cassandraConnectionHolderEntry = evictionIterator.next();
                cassandraConnections.remove(cassandraConnectionHolderEntry.getKey());
            }
        }
    }
}
