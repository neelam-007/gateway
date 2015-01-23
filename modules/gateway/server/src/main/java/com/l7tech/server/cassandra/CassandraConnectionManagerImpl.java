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
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.lang.reflect.Field;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 10/30/14
 */
public class CassandraConnectionManagerImpl implements CassandraConnectionManager {
    private static final Logger logger = Logger.getLogger(CassandraConnectionManagerImpl.class.getName());

    private final ConcurrentHashMap<String, CassandraConnectionHolder> cassandraConnections = new ConcurrentHashMap<>();
    private final CassandraConnectionEntityManager cassandraEntityManager;
    private final Config config;
    private final SecurePasswordManager securePasswordManager;
    private final TrustManager trustManager;
    private final SecureRandom secureRandom;
    private final static Audit auditor = new LoggingAudit(logger);
    private final Timer timer;
    private static final String PROP_CACHE_CLEAN_INTERVAL = "com.l7tech.server.cassandra.connection.cacheCleanInterval";
    private static final long CACHE_CLEAN_INTERVAL = ConfigFactory.getLongProperty(PROP_CACHE_CLEAN_INTERVAL, 15 * 60 * 1000L);

    protected final Lock lock = new ReentrantLock();


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
        timer.schedule(new CacheCleanupTask(cassandraConnections, this), 30797, CACHE_CLEAN_INTERVAL);
    }

    @Override
    public CassandraConnectionHolder getConnection(String name) {
        lock.lock();
        try {
            CassandraConnectionHolder connectionHolder = cassandraConnections.get(name);
            if (connectionHolder == null) {
                CassandraConnection entity = null;
                //first find connection in the data source
                try {
                    entity = cassandraEntityManager.getCassandraConnectionEntity(name);
                } catch (FindException e) {
                    auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_CANNOT_CONNECT, new String[]{name, "Unable to find Cassandra connection name:" + name}, ExceptionUtils.getDebugException(e));
                    return null;
                }

                if (entity == null) {
                    auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_CANNOT_CONNECT, new String[]{name, "Cassandra connection does not exist."});
                    return null;
                }

                //create a holder and add it to the list of connections
                if (entity.isEnabled()) {
                    connectionHolder = createConnection(entity);
                    if (connectionHolder != null) {
                        cassandraConnections.put(name, connectionHolder);
                    }
                } else {
                    auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_DISABLED, name);
                }
            }
            return connectionHolder;
        }finally {
            lock.unlock();
        }
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
    public void  removeConnection(CassandraConnection cassandraConnectionEntity) {
        lock.lock();
        try {
            CassandraConnectionHolder connection = cassandraConnections.get(cassandraConnectionEntity.getName());
            if (closeConnection(connection)) {
                cassandraConnections.remove(cassandraConnectionEntity.getName());
                auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Removed Cassandra connection " + cassandraConnectionEntity.getName());
            } else {
                auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Unable to remove Cassandra connection " + cassandraConnectionEntity.getName());
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addConnection(CassandraConnection cassandraConnectionEntity) {
        getConnection(cassandraConnectionEntity.getName());
    }


    @Override
    public void removeConnection(Goid goid) {
        lock.lock();
        try {
            CassandraConnection entity = null;
            try {
                entity = cassandraEntityManager.findByPrimaryKey(goid);
                removeConnection(entity);
            } catch (FindException e) {
                auditor.logAndAudit(AssertionMessages.CASSANDRA_CANNOT_REMOVE_CONNECTION, new String[]{"Unable to find appropriate connection in the data source."}, ExceptionUtils.getDebugException(e));
            }
            //this should never happen
            if (entity == null) {
                Iterator<CassandraConnectionHolder> iter = cassandraConnections.values().iterator();
                while (iter.hasNext()) {
                    CassandraConnectionHolder holder = iter.next();
                    if (holder.getCassandraConnectionEntity().getGoid().equals(goid)) {
                        closeConnection(holder);
                        iter.remove();
                    }
                }
            }
        } finally {
            lock.unlock();
        }

    }

    @Override
    public void updateConnection(CassandraConnection cassandraConnectionEntity) throws UpdateException {
        lock.lock();
        try {
            // Search through cached connections by goid because the name field could have been updated.
            boolean createNewConnection = false;
            CassandraConnectionHolder cachedConnection = null;
            Iterator<Map.Entry<String,CassandraConnectionHolder>> iter = cassandraConnections.entrySet().iterator();
            while(iter.hasNext()) {
                Map.Entry<String, CassandraConnectionHolder> holder = iter.next();
                if (holder.getValue().getCassandraConnectionEntity().getGoid().equals(cassandraConnectionEntity.getGoid())) {
                    cachedConnection  = holder.getValue();
                    // If cache exists, remove it and add back an updated one.
                    if (closeConnection(cachedConnection)) {
                        createNewConnection  =  true;
                        iter.remove();
                        auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Removed Cassandra connection " + cassandraConnectionEntity.getName());
                        break;
                    } else {
                        throw new UpdateException("Unable to close Cassandra connection " + cassandraConnectionEntity.getName());
                    }
                }
            }
            if(createNewConnection) {
                //create new connection
                CassandraConnectionHolder newConnectionHolder = createConnection(cassandraConnectionEntity);
                if (newConnectionHolder != null) {
                    if (cassandraConnectionEntity.isEnabled()) {
                        cassandraConnections.put(cassandraConnectionEntity.getName(), newConnectionHolder);
                    }
                }
            }

        } finally {
            lock.unlock();
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
            auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Created Cassandra Connection " + cassandraConnectionEntity.getName());
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
        }catch(Throwable t) {
            auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_CANNOT_CONNECT, new String[]{cassandraConnectionEntity.getName(), t.getMessage()}, ExceptionUtils.getDebugException(t));
            throw t;
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
        int defaultFetchSize = config.getIntProperty(ServerConfigParams.PARAM_CASSANDRA_FETCH_SIZE, DEFAULT_FETCH_SIZE);
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

        poolingOptions.setMaxConnectionsPerHost(hd, maxConnectionPerHost);
        poolingOptions.setCoreConnectionsPerHost(hd, coreConnectionPerHost);

        int defaultMaxSimultaneousReqPerHost = config.getIntProperty(ServerConfigParams.PARAM_CASSANDRA_MAX_SIMULTANEOUS_REQ_PER_HOST, (hd == HostDistance.LOCAL? MAX_SIMUL_REQ_PER_HOST_LOCAL_DEF: MAX_SIMUL_REQ_PER_HOST_REMOTE_DEF));
        int maxSimultaneousReqPerHost = connectionProperties != null ? CassandraUtil.getIntOrDefault(connectionProperties.get(MAX_SIMUL_REQ_PER_HOST), defaultMaxSimultaneousReqPerHost) : defaultMaxSimultaneousReqPerHost;

        poolingOptions.setMaxSimultaneousRequestsPerHostThreshold(hd, maxSimultaneousReqPerHost);

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

            SSLContext sslContext = SSLContext.getInstance("TLS");

            //initialize the SSLContext with the an array of trustmanager, and random number generator
            sslContext.init(null, trustManagers, secureRandom);

            //Create the cassandra SSL option
            //initialize with our SSLContext and ciphers
            //(using default ciphers that come with cassandra java driver)
            String cipherSuiteList = cassandraConnectionEntity.getTlsEnabledCipherSuites();
            SSLOptions sslOptions;
            if ( !( cipherSuiteList == null || cipherSuiteList.equals("") ) ) {
                sslOptions = new SSLOptions(sslContext, cipherSuiteList.split("\\s*,\\s*"));
            } else {
                sslOptions = new SSLOptions(sslContext, SSLOptions.DEFAULT_SSL_CIPHER_SUITES);
            }
            //add the ssl options to our cluster
            clusterBuilder.withSSL(sslOptions);
        }
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

    public synchronized boolean closeConnection(CassandraConnectionHolder connection) {
        if(connection != null) {
            Session session = connection.getSession();
            if(session != null )  {
                boolean querying = false;
                for(Host host : session.getState().getConnectedHosts()) {
                    querying |= session.getState().getInFlightQueries(host) > 0;
                }
                if(querying) return false;

                session.close();
            }
            Cluster cluster = connection.getCluster();
            if(cluster != null) cluster.close();
        }
        return true;
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
        private final CassandraConnectionManagerImpl connectionManager;

        private CacheCleanupTask(final ConcurrentHashMap<String, CassandraConnectionHolder> cassandraConnections, CassandraConnectionManagerImpl connectionManager) {
            this.cassandraConnections = cassandraConnections;
            this.connectionManager = connectionManager;
        }

        @Override
        public void run() {

            auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "CacheCleanUp task starting...");
            connectionManager.lock.lock();
            try {
                final long maximumAge = ConfigFactory.getTimeUnitProperty(MAX_CONNECTION_CACHE_AGE, DEFAULT_CONNECTION_MAX_AGE);
                final long maximumIdleTime = ConfigFactory.getTimeUnitProperty(MAX_CONNECTION_CACHE_IDLE_TIME, DEFAULT_CONNECTION_MAX_IDLE);
                final int maximumSize = ConfigFactory.getIntProperty(MAX_CONNECTION_CACHE_SIZE, DEFAULT_CONNECTION_CACHE_SIZE);

                final long timeNow = System.currentTimeMillis();
                final int overSize = cassandraConnections.size() - maximumSize;
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
                    if (maximumAge > 0 && (timeNow - cassandraConnectionHolderEntry.getValue().getCreatedTime()) > maximumAge) {
                        closeAndRemoveCachedConnection(cassandraConnectionHolderEntry);
                    } else if (maximumIdleTime > 0 && (timeNow - cassandraConnectionHolderEntry.getValue().getLastAccessTime().get()) > maximumIdleTime) {
                        closeAndRemoveCachedConnection(cassandraConnectionHolderEntry);
                    } else if (overSize > 0) {
                        evictionCandidates.add(cassandraConnectionHolderEntry);
                    }
                }

                // evict oldest first to reduce cache size
                final Iterator<Map.Entry<String, CassandraConnectionHolder>> evictionIterator = evictionCandidates.iterator();
                for (int i = 0; i < overSize && evictionIterator.hasNext(); i++) {
                    final Map.Entry<String, CassandraConnectionHolder> cassandraConnectionHolderEntry = evictionIterator.next();
                    closeAndRemoveCachedConnection(cassandraConnectionHolderEntry);
                }
            }  finally {
                connectionManager.lock.unlock();
            }
            auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "CacheCleanUp task finished.");
        }

        private void closeAndRemoveCachedConnection(Map.Entry<String, CassandraConnectionHolder> cassandraConnectionHolderEntry) {
            if(connectionManager.closeConnection(cassandraConnectionHolderEntry.getValue())) {
                 cassandraConnections.remove(cassandraConnectionHolderEntry.getKey());
                auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Removed connection " + cassandraConnectionHolderEntry.getKey());
            }
            else {
                auditor.logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_MANAGER_FINE_MESSAGE, "Unable to remove connection " + cassandraConnectionHolderEntry.getKey());
            }
        }
    }
}
