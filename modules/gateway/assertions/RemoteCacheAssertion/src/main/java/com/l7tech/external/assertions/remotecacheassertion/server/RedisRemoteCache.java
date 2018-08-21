package com.l7tech.external.assertions.remotecacheassertion.server;

import com.google.common.base.Strings;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.util.ResourceUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RedisRemoteCache implements RemoteCache {

    private static final Logger LOGGER = Logger.getLogger(RedisRemoteCache.class.getName());
    public static final String PROPERTY_SERVERS = "servers";
    public static final String PROPERTY_PASSWORD = "password";
    public static final String PROPERTY_IS_CLUSTER = "isCluster";

    //Apache Pool Config default value
    public static final String APACHE_POOL_LIFO = "lifo";
    public static final String APACHE_POOL_FAIRNESS = "fairness";
    public static final String APACHE_POOL_MAX_WAIT_MILLIS = "maxWaitMillis";
    public static final String APACHE_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS = "minEvictableIdleTimeMillis";
    public static final String APACHE_POOL_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS = "softMinEvictableIdleTimeMillis";
    public static final String APACHE_POOL_NUM_TESTS_PER_EVICTION_RUN = "numTestsPerEvictionRun";
    public static final String APACHE_POOL_TEST_ON_CREATE = "testOnCreate";
    public static final String APACHE_POOL_TEST_ON_BORROW = "testOnBorrow";
    public static final String APACHE_POOL_TEST_ON_RETURN = "testOnReturn";
    public static final String APACHE_POOL_TEST_WHILE_IDLE = "testWhileIdle";
    public static final String APACHE_POOL_TIME_BETWEEN_EVICTION_RUNS_MILLIS = "timeBetweenEvictionRunsMillis";
    public static final String APACHE_POOL_BLOCK_WHEN_EXHAUSTED = "blockWhenExhausted";
    public static final String APACHE_POOL_JMX_ENABLED = "jmxEnabled";
    public static final String APACHE_POOL_JMX_NAME_PREFIX = "jmxNamePrefix";
    public static final String APACHE_POOL_JMX_NAME_BASE = "jmxNameBase";
    public static final String APACHE_POOL_EVICTION_POLICY_CLASSNAME = "evictionPolicyClassName";
    public static final String APACHE_POOL_MAX_TOTAL = "maxTotal";
    public static final String APACHE_POOL_MAX_IDLE = "maxIdle";
    public static final String APACHE_POOL_MIN_IDLE = "minIdle";

    public static final int DEFAULT_MAX_ATTEMPTS = 5;

    private RemoteCacheEntity remoteCacheEntity;
    private JedisCluster cluster = null;
    private JedisPool pool = null;
    private HashMap<String, String> properties;

    public RedisRemoteCache(RemoteCacheEntity entity) throws Exception {
        remoteCacheEntity = entity;
        properties = remoteCacheEntity.getProperties();
        final int timeout = remoteCacheEntity.getTimeout();
        final String password = properties.get(PROPERTY_PASSWORD);
        final boolean usePassword = !Strings.isNullOrEmpty(password);

        GenericObjectPoolConfig poolConfig = createGenericObjectPoolConfig();
        try {
            if (isCluster()) {
                Set<HostAndPort> hosts = getClusterHosts();
                // All nodes in the cluster must have the same password
                cluster = usePassword ?
                        new JedisCluster(hosts, timeout, timeout, DEFAULT_MAX_ATTEMPTS, password, poolConfig) :
                        new JedisCluster(hosts, timeout, poolConfig);
            } else {
                //For JedisPool, we only need one server to connect to. If multiple servers are provided, then the rest of the servers are ignored.
                String server = getServerList()[0];
                String address = server.substring(0, server.indexOf(':')).trim();
                Integer port = Integer.parseInt(server.substring(server.indexOf(':') + 1).trim());

                pool = usePassword ?
                        new JedisPool(poolConfig, address, port, timeout, password) :
                        new JedisPool(poolConfig, address, port, timeout);
            }
        } catch (JedisDataException ex) {
            LOGGER.log(Level.WARNING, "Unable to connect to the Redis server(s).", ex);
            throw ex;
        }
    }

    /**
     * Constructor for unit test purpose
     */
    RedisRemoteCache(RemoteCacheEntity entity, JedisPool pool, JedisCluster cluster) {
        this.remoteCacheEntity = entity;
        this.pool = pool;
        this.cluster = cluster;
        properties = remoteCacheEntity.getProperties();
    }

    /**
     * JedisPool and JedisCluster, both make use of Apache GenericObjectToolConfig. Only a few of the PoolConfig are exposed to the user. This can be enhanced later on, to expose more of the configuration to the user
     * This method allows the user to set those properties as they see fit. If none are set, it will use the default values set by Jedis client.
     *
     * @return GenericObjectPoolConfig to be used during the creation of a Redis Cluster or Redis Pool.
     */
    GenericObjectPoolConfig createGenericObjectPoolConfig() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();

        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_TIME_BETWEEN_EVICTION_RUNS_MILLIS))) {
            poolConfig.setTimeBetweenEvictionRunsMillis(Long.parseLong(properties.get(APACHE_POOL_TIME_BETWEEN_EVICTION_RUNS_MILLIS)));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_TEST_WHILE_IDLE))) {
            poolConfig.setTestWhileIdle(Boolean.parseBoolean(properties.get(APACHE_POOL_TEST_WHILE_IDLE)));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS))) {
            poolConfig.setMinEvictableIdleTimeMillis(Long.parseLong(properties.get(APACHE_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS)));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_NUM_TESTS_PER_EVICTION_RUN))) {
            poolConfig.setNumTestsPerEvictionRun(Integer.parseInt(properties.get(APACHE_POOL_NUM_TESTS_PER_EVICTION_RUN)));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_LIFO))) {
            poolConfig.setLifo(Boolean.parseBoolean(properties.get(APACHE_POOL_LIFO)));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_FAIRNESS))) {
            poolConfig.setFairness(Boolean.parseBoolean(properties.get(APACHE_POOL_FAIRNESS)));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_MAX_WAIT_MILLIS))) {
            poolConfig.setMaxWaitMillis(Integer.parseInt(properties.get(APACHE_POOL_MAX_WAIT_MILLIS)));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS))) {
            poolConfig.setSoftMinEvictableIdleTimeMillis(Long.parseLong(properties.get(APACHE_POOL_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS)));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_TEST_ON_CREATE))) {
            poolConfig.setTestOnCreate(Boolean.parseBoolean(properties.get(APACHE_POOL_TEST_ON_CREATE)));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_TEST_ON_BORROW))) {
            poolConfig.setTestOnBorrow(Boolean.parseBoolean(properties.get(APACHE_POOL_TEST_ON_BORROW)));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_TEST_ON_RETURN))) {
            poolConfig.setTestOnReturn(Boolean.parseBoolean(properties.get(APACHE_POOL_TEST_ON_RETURN)));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_BLOCK_WHEN_EXHAUSTED))) {
            poolConfig.setBlockWhenExhausted(Boolean.parseBoolean(properties.get(APACHE_POOL_BLOCK_WHEN_EXHAUSTED)));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_JMX_ENABLED))) {
            poolConfig.setJmxEnabled(Boolean.parseBoolean(properties.get(APACHE_POOL_JMX_ENABLED)));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_JMX_NAME_PREFIX))) {
            poolConfig.setJmxNamePrefix(properties.get(APACHE_POOL_JMX_NAME_PREFIX));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_JMX_NAME_BASE))) {
            poolConfig.setJmxNameBase(properties.get(APACHE_POOL_JMX_NAME_BASE));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_EVICTION_POLICY_CLASSNAME))) {
            poolConfig.setEvictionPolicyClassName(properties.get(APACHE_POOL_EVICTION_POLICY_CLASSNAME));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_MAX_TOTAL))) {
            poolConfig.setMaxTotal(Integer.parseInt(properties.get(APACHE_POOL_MAX_TOTAL)));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_MAX_IDLE))) {
            poolConfig.setMaxIdle(Integer.parseInt(properties.get(APACHE_POOL_MAX_IDLE)));
        }
        if (StringUtils.isNotBlank(properties.get(APACHE_POOL_MIN_IDLE))) {
            poolConfig.setMinIdle(Integer.parseInt(properties.get(APACHE_POOL_MIN_IDLE)));
        }

        return poolConfig;
    }

    /**
     * Used to decide whether to create a JedisCluster or a JedisPool depending on the user input
     *
     * @return True if the redis server is a cluster, false if it is not
     */
    private boolean isCluster() {
        return Boolean.parseBoolean(properties.get(PROPERTY_IS_CLUSTER));
    }

    /**
     * helper method to return a list of comma separated servers uris
     *
     * @return List of server uris
     */
    private String[] getServerList() {
        return properties.get(PROPERTY_SERVERS).split(",");
    }

    /**
     * helper method - create a set of HostAndPort to be used during the JedisCluster creation
     *
     * @return set of HostAndPort
     */
    private Set<HostAndPort> getClusterHosts() {
        Set<HostAndPort> hosts = new HashSet<>();
        String[] serversList = getServerList();
        for (String server : serversList) {
            String address = server.substring(0, server.indexOf(':')).trim();
            Integer port = Integer.parseInt(server.substring(server.indexOf(':') + 1).trim());
            hosts.add(new HostAndPort(address, port));
        }
        return hosts;
    }

    /**
     * Get the value based on the provided key. Redis returns the value in String, but since we store it as byte array, it is converted into bytes
     *
     * @param key - Key to lookup the value
     * @return CachedMessagedData
     * @throws Exception - JedisException is thrown if there is an issue with the connection to the Redis Server
     */
    @Override
    public CachedMessageData get(String key) throws Exception {
        String value;
        if (isCluster()) {
            value = cluster.get(key);
        } else {
            try (Jedis client = pool.getResource()) {
                value = client.get(key);
            }
        }

        if (null == value) {
            throw new Exception("Cached entry not found for key: " + key);
        }

        return new CachedMessageData((value).getBytes());
    }

    /**
     * Sets the key and value in the cache with the given expiry
     * Currently only the String data type for redis has been implemented. This method would need to be enhanced if further data types are supported.
     *
     * @param key    - Key to set
     * @param value  - Value to store
     * @param expiry - TTL for the key-value to live in cache
     * @throws Exception - JedisException is thrown if there is an issue with the connection to the Redis Server
     */
    @Override
    public void set(String key, CachedMessageData value, int expiry) throws Exception {
        String reply;

        if (isCluster()) {
            reply = cluster.set(key, new String(value.toByteArray()));
            cluster.expire(key, expiry);
        } else {
            try (Jedis client = pool.getResource()) {
                reply = client.set(key, new String(value.toByteArray()));
                client.expire(key, expiry);
            }
        }
        if (null == reply || !reply.equals("OK")) {
            throw new Exception("Could not set the cache key" + key);
        }
    }

    /**
     * Remove key-value from the cache
     *
     * @param key the cache entry key
     * @throws Exception - JedisException is thrown if there is an issue with the connection to the Redis Server
     */
    @Override
    public void remove(String key) throws Exception {
        Long reply;

        if (isCluster()) {
            reply = cluster.del(key);
        } else {
            try (Jedis client = pool.getResource()) {
                reply = client.del(key);
            }
        }
        if (0 == reply) {
            throw new Exception("Unable to remove key " + key + " .Key does not exists!");
        }
    }

    /**
     * In case of a cluster, the cluser is close.
     * In case of a pool, the pool is closed. If client object is not null, it is returned to the pool and properly closed.
     */
    @Override
    public void shutdown() {
        ResourceUtils.closeQuietly(pool);
        ResourceUtils.closeQuietly(cluster);
    }
}
