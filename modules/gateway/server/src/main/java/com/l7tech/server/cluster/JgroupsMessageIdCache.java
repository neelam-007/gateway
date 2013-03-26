package com.l7tech.server.cluster;

import com.l7tech.util.ConfigFactory;
import com.l7tech.util.SyspropUtil;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.config.EvictionConfig;
import org.jboss.cache.config.EvictionRegionConfig;
import org.jboss.cache.eviction.ExpirationAlgorithmConfig;
import org.jboss.cache.lock.TimeoutException;
import org.jgroups.stack.IpAddress;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *
 */
class JgroupsMessageIdCache implements MessageIdCache {
    private static final Logger logger = Logger.getLogger(JgroupsMessageIdCache.class.getName());

    private static final String MESSAGEID_PARENT_NODE = DistributedMessageIdManager.class.getName() + "/messageId";
    private static final String EXPIRES_ATTR = "expires";
    private static final long GC_PERIOD = ConfigFactory.getTimeUnitProperty("com.l7tech.server.cluster.cacheGcPeriod", SECONDS.toMillis(5L));

    private final Cache<String,Long> cache;

    JgroupsMessageIdCache(String address, int port, String interfaceAddress, String jgroupsConfigFile) throws MalformedURLException {
        CacheFactory<String,Long> factory = new DefaultCacheFactory<String,Long>();
        cache = factory.createCache( "com/l7tech/server/cluster/resources/jbosscache-config.xml", false );
        Configuration config = cache.getConfiguration();
        if ( jgroupsConfigFile != null ) {
            logger.config( "Using JGroups configuration file '" + jgroupsConfigFile + "'.");
            SyspropUtil.setProperty( "com.l7tech.server.cluster.jgroups.mcast_addr", address );
            SyspropUtil.setProperty( "com.l7tech.server.cluster.jgroups.mcast_port", Integer.toString(port) );
            SyspropUtil.setProperty( "com.l7tech.server.cluster.jgroups.bind_addr", interfaceAddress);
            config.setJgroupsConfigFile( new File(jgroupsConfigFile).toURI().toURL() );
        } else {
            String props = config.getClusterConfig(); // JGroups configuration string (not XML format)
            props = props.replaceFirst("mcast_addr=0.0.0.0", "mcast_addr=" + address);
            props = props.replaceFirst("mcast_port=0", "mcast_port=" + port);
            props = props.replaceFirst("bind_addr=0.0.0.0", "bind_addr=" + interfaceAddress);
            config.setClusterConfig( props );
        }

        setEvictConfig(config);
        cache.create();
        cache.start();

        logger.log(Level.INFO, "Using multicast replay protection with GMS address {0}", cache.getLocalAddress());
    }

    /**
     * Construct a local cache, no clustering support
     */
    protected JgroupsMessageIdCache() {
        Configuration config = new Configuration();
        setEvictConfig(config);
        config.setCacheMode(Configuration.CacheMode.LOCAL);
        CacheFactory<String,Long> factory = new DefaultCacheFactory<String,Long>();
        cache = factory.createCache(config, true);
    }

    /**
     * Set the cache eviction policy to the configuration object.
     * The cache in memmory will evict once the cache is expired.
     *
     * @param config The Cache configuration
     */
    private void setEvictConfig(Configuration config) {
        ExpirationAlgorithmConfig eac = new ExpirationAlgorithmConfig();
        EvictionRegionConfig erc = new EvictionRegionConfig(Fqn.fromString(MESSAGEID_PARENT_NODE), eac);
        EvictionConfig ec = new EvictionConfig(erc);
        ec.setWakeupInterval(GC_PERIOD);
        config.setEvictionConfig(ec);
    }

    @Override
    public Long get(String key) {
        return cache.get(MESSAGEID_PARENT_NODE + "/" + key, EXPIRES_ATTR);
    }

    @Override
    public void put(String id, Long expiry) {
        Fqn fqn = Fqn.fromString(MESSAGEID_PARENT_NODE + "/" + id);
        cache.getRoot().addChild(fqn).put(ExpirationAlgorithmConfig.EXPIRATION_KEY, Math.abs(expiry));
        cache.put(fqn, EXPIRES_ATTR, expiry);
    }

    @Override
    public void remove(String id) {
        cache.removeNode( MESSAGEID_PARENT_NODE + "/" + id );
    }

    @Override
    public Set<String> getAll() {
        return cache.getChildrenNames(MESSAGEID_PARENT_NODE);
    }

    @Override
    public void getMemberAddresses(List<String> addresses) {
        final List<? super IpAddress> members = cache.getMembers();
        for ( Object member : members ) {
            if ( member instanceof IpAddress ) {
                IpAddress memberAddress = (IpAddress) member;
                addresses.add(memberAddress.getIpAddress().getHostAddress());
            }
        }
    }

    @Override
    public boolean isPopulateNeeded() {
        return cache.getNode(MESSAGEID_PARENT_NODE) == null;
    }

    @Override
    public boolean isTimeoutError(Throwable t) {
        return t instanceof TimeoutException;
    }

    @Override
    public void restart() {
        // restart cache
        cache.stop();
        cache.start();
    }

    @Override
    public void startBatch() {
        cache.startBatch();
    }

    @Override
    public void endBatch(boolean batchSuccess) {
        cache.endBatch( batchSuccess );
    }

    @Override
    public void destroy() {
        final Cache cache = this.cache;
        if ( cache != null ) {
            logger.info("Stopping distributed cache.");
            cache.stop();
            cache.destroy();
            logger.info("Stopped distributed cache.");
        }
    }

    @Override
    public String toDatabaseId(String key) {
        return key;
    }

    @Override
    public String fromDatabaseId(String id) {
        return id;
    }
}
