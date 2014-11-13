package com.l7tech.server.cassandra;

import com.ca.datasources.cassandra.CassandraUtil;
import com.ca.datasources.cassandra.connection.CassandraConnectionHolder;
import com.ca.datasources.cassandra.connection.CassandraConnectionManager;
import com.datastax.driver.core.*;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationEvent;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 10/30/14
 */
public class CassandraConnectionManagerImpl implements CassandraConnectionManager {
    private static final Logger logger = Logger.getLogger(CassandraConnectionManagerImpl.class.getName());

    public static final String CORE_CONNECTION_PER_HOST = "cassandra.coreConnectionsPerHost";
    public static final String MAX_CONNECTION_PER_HOST = "cassandra.maxConnectionPerHost";
    public static final String MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD = "cassandra.maxSimultaneousRequestsPerConnectionThreshold";
    public static final String MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD = "cassandra.minSimultaneousRequestsPerConnectionThreshold";

    public static final int CORE_CONNECTION_PER_HOST_DEF = 1;
    public static final int MAX_CONNECTION_PER_HOST_DEF = 2;
    public static final int MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD_DEF = 128;
    public static final int MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD_DEF = 25;

    public static final String CONNECTION_TIMEOUT_MILLIS = "cassandra.connectTimeoutMillis";
    public static final String KEEP_ALIVE = "cassandra.keepAlive";
    public static final String RECEIVE_BUFFER_SIZE = "cassandra.receiveBufferSize";
    public static final String REUSE_ADDRESS = "cassandra.reuseAddress";
    public static final String SEND_BUFFER_SIZE = "cassandra.sendBufferSize";
    public static final String SO_LINGER = "cassandra.soLinger";
    public static final String TCP_NO_DELAY = "cassandra.tcpNoDelay";


    private final Map<String, CassandraConnectionHolder> cassandraConnections = new ConcurrentHashMap<>();
    private final CassandraConnectionEntityManager cassandraEntityManager;
    private final SecurePasswordManager securePasswordManager;
    private final ClusterPropertyManager clusterPropertyManager;
    private final TrustManager trustManager;
    private final SecureRandom secureRandom;

    public CassandraConnectionManagerImpl(CassandraConnectionEntityManager cassandraEntityManager, SecurePasswordManager securePasswordManager,
                                       ClusterPropertyManager clusterPropertyManager,
                                       TrustManager trustManager,
                                       SecureRandom secureRandom) {
        this.cassandraEntityManager = cassandraEntityManager;
        this.securePasswordManager = securePasswordManager;
        this.clusterPropertyManager = clusterPropertyManager;
        this.trustManager = trustManager;
        this.secureRandom = secureRandom;
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
                logger.log(Level.FINE, "Unable to find Cassandra connection name:" + name );
                return null;
            }
            //create a holder and add it to the list of connections
            connectionHolder = createConnection(entity);
        }
        return connectionHolder;
    }

    @Override
    public void closeAllConnections() {
        Iterator<CassandraConnectionHolder> iter = cassandraConnections.values().iterator();
        while(iter.hasNext()) {
            final CassandraConnectionHolder connectionHolder = iter.next();
            connectionHolder.getSession().close();
            connectionHolder.getCluster().close();
            iter.remove();
        }

    }

    @Override
    public void addConnection(CassandraConnection cassandraConnectionEntity) {
        CassandraConnectionHolder connection = getConnection(cassandraConnectionEntity.getName());
        if(connection == null) {

        }

    }

    @Override
    public void removeConnection(CassandraConnection cassandraConnectionEntity) {
        CassandraConnectionHolder connection = cassandraConnections.remove(cassandraConnectionEntity.getName());
        if(connection != null) {
            connection.getSession().close();
            connection.getCluster().close();
        }
    }

    @Override
    public void removeConnection(Goid goid) {
        CassandraConnection entity = null;
        try {
            entity = cassandraEntityManager.findByPrimaryKey(goid);
            removeConnection(entity);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find appropriate connection in the data source.", ExceptionUtils.getDebugException(e));
        }
        //this should never happen
        if(entity == null) {
            Iterator<CassandraConnectionHolder> iter = cassandraConnections.values().iterator();
            while(iter.hasNext()) {
                CassandraConnectionHolder holder = iter.next();
                if(holder.getCassandraConnectionEntity().getGoid().equals(goid)){
                    holder.getSession().close();
                    holder.getCluster().close();
                    iter.remove();
                }
            }
        }

    }

    @Override
    public void updateConnection(CassandraConnection cassandraConnectionEntity) {
        removeConnection(cassandraConnectionEntity);
        cassandraConnections.put(cassandraConnectionEntity.getName(), createConnection(cassandraConnectionEntity));
    }

    private CassandraConnectionHolder createConnection(CassandraConnection cassandraConnectionEntity){
        Cluster cluster = null;
        Session session = null;

        try{
            //TODO: provide more intelligent splitting
            String[] contactPoints = cassandraConnectionEntity.getContactPoints().split(",");
            //trim contact point string(s) to avoid IllegalArgumentException when InetAddress is created in driver
            int i = 0;
            for(String contactPoint: contactPoints){
                contactPoints[i] = contactPoint.trim();
                i++;
            }

            //start cluster creation
            Cluster.Builder clusterBuilder = Cluster.builder();

            //add basic cluster info

            addBasicClusterInfo(clusterBuilder, cassandraConnectionEntity, contactPoints);

            //add pooling option
            addPoolingOptions(clusterBuilder, cassandraConnectionEntity);

            //add socket option
            addSocketOptions(clusterBuilder, cassandraConnectionEntity);

            //add ssl option
            addSSLOptions(clusterBuilder, cassandraConnectionEntity);

            //build cluster
            cluster = clusterBuilder.build();

            //create our cassandra session
            session = cluster.connect(cassandraConnectionEntity.getKeyspaceName());

        } catch (Exception e){
            logger.log(Level.WARNING, "Unable to connect to cassandra cluster. Connection = " + cassandraConnectionEntity.getName(), e);
            return null;
        }

        return new CassandraConnectionHolder(cassandraConnectionEntity, cluster, session);
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
        int coreConnectionPerHost = connectionProperties != null? CassandraUtil.getIntOrDefault(connectionProperties.get(CORE_CONNECTION_PER_HOST), CORE_CONNECTION_PER_HOST_DEF) : CORE_CONNECTION_PER_HOST_DEF ;
        int maxConnectionPerHost = connectionProperties != null? CassandraUtil.getIntOrDefault(connectionProperties.get(MAX_CONNECTION_PER_HOST), MAX_CONNECTION_PER_HOST_DEF) : MAX_CONNECTION_PER_HOST_DEF;
        int maxSimultaneousRequestsPerConnectionThreshold = connectionProperties != null? CassandraUtil.getIntOrDefault(connectionProperties.get(MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD), MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD_DEF) : MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD_DEF;
        int minSimultaneousRequestsPerConnectionThreshold = connectionProperties != null? CassandraUtil.getIntOrDefault(connectionProperties.get(MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD), MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD_DEF) : MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD_DEF;

        //TODO: check the policy type so we can determine host distance setting or default

        poolingOptions.setCoreConnectionsPerHost(HostDistance.LOCAL, coreConnectionPerHost);
        poolingOptions.setMaxConnectionsPerHost(HostDistance.LOCAL, maxConnectionPerHost);
        poolingOptions.setMaxSimultaneousRequestsPerConnectionThreshold(HostDistance.LOCAL, maxSimultaneousRequestsPerConnectionThreshold);
        poolingOptions.setMinSimultaneousRequestsPerConnectionThreshold(HostDistance.LOCAL, minSimultaneousRequestsPerConnectionThreshold);

        clusterBuilder.withPoolingOptions(poolingOptions);
    }

    private void addSocketOptions(Cluster.Builder clusterBuilder, CassandraConnection cassandraConnectionEntity) {

        SocketOptions socketOptions = new SocketOptions();
        Map<String,String> connectionProperties = cassandraConnectionEntity.getProperties();
        boolean withSocketOption = setSocketOptionsProp(socketOptions, connectionProperties, CONNECTION_TIMEOUT_MILLIS) |
                setSocketOptionsProp(socketOptions, connectionProperties, KEEP_ALIVE) |
                setSocketOptionsProp(socketOptions, connectionProperties, RECEIVE_BUFFER_SIZE) |
                setSocketOptionsProp(socketOptions, connectionProperties, REUSE_ADDRESS) |
                setSocketOptionsProp(socketOptions, connectionProperties, SEND_BUFFER_SIZE) |
                setSocketOptionsProp(socketOptions, connectionProperties, SO_LINGER) |
                setSocketOptionsProp(socketOptions, connectionProperties, TCP_NO_DELAY);


        if (withSocketOption) {
            clusterBuilder.withSocketOptions(socketOptions);
        }
    }

    private boolean setSocketOptionsProp(SocketOptions socketOptions, Map<String, String> connectionProperties, String propName) {
        boolean propSet = false;
        if(connectionProperties.containsKey(propName)){
            Integer prop = CassandraUtil.getInteger(connectionProperties.get(propName));
            if(prop != null) {
                socketOptions.setConnectTimeoutMillis(prop);
                propSet = true;
            }
            else {
                logger.log(Level.FINE, "Unable to read property " + prop + ". Using default value.");
            }
        }
        return propSet;
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
        if (!Goid.isDefault(entity.getPasswordGoid())) {
            return securePasswordManager.decryptPassword(securePasswordManager.findByPrimaryKey(entity.getPasswordGoid()).getEncodedPassword());
        } else {
            return new char[]{};
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // if a SiteMinderConfiguration has been modified, remove it from the cache
        if (event instanceof EntityInvalidationEvent) {
            final EntityInvalidationEvent entityInvalidationEvent = (EntityInvalidationEvent) event;
            if (CassandraConnection.class.equals(entityInvalidationEvent.getEntityClass())) {
                final Goid[] ids = entityInvalidationEvent.getEntityIds();
                for (final Goid id : ids) {
                    removeConnection(id);
                }
            }
        }
    }
}
