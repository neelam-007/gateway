package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 14/11/11
 * Time: 4:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class MemcachedRemoteCache implements RemoteCache {

    private static final Logger LOGGER = Logger.getLogger(MemcachedRemoteCache.class.getName());

    public static final String PROP_BUCKETNAME = "bucketName";
    public static final String PROP_BUCKET_SPECIFIED = "bucketSpecified";
    public static final String PROP_PASSWORD = "password";
    public static final String PROP_SERVERPORTS = "ports";
    private MemcachedClient client;
    private RemoteCacheEntity remoteCacheEntity;

    static {
        try {
            System.setProperty("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.Log4JLogger");
        } catch (SecurityException e) {
            LOGGER.log(Level.WARNING, "Could not set the spymemcached log implementation - log output may differ", e);
        }
    }

    public MemcachedRemoteCache(RemoteCacheEntity entity) throws Exception {
        this.remoteCacheEntity = entity;
        try {
            client = getMemcachedClient();
        } catch (IOException ioe) {
            throw new Exception("Failed to create the memcached connection.");
        }
    }

    ArrayList<InetSocketAddress> getServerList() {
        String serverPortsString = this.remoteCacheEntity.getProperties().get(MemcachedRemoteCache.PROP_SERVERPORTS);
        String[] serverPorts = StringUtils.defaultString(serverPortsString).split(",");
        ArrayList<InetSocketAddress> addresses = new ArrayList<>(serverPorts.length);
        for (String hostPort : serverPorts) {
            //provide a list of addresses for failover.
            addresses.add(new InetSocketAddress(hostPort.substring(0, hostPort.indexOf(":")), Integer.parseInt(hostPort.substring(hostPort.indexOf(":") + 1))));
        }
        return addresses;
    }

    public MemcachedClient getMemcachedClient() throws IOException {
        MemcachedClient client;
        ArrayList<InetSocketAddress> addresses = getServerList();

        if (Boolean.parseBoolean(this.remoteCacheEntity.getProperties().get(MemcachedRemoteCache.PROP_BUCKET_SPECIFIED))) {
            String password = this.remoteCacheEntity.getProperties().get(MemcachedRemoteCache.PROP_PASSWORD);
            String bucketName = this.remoteCacheEntity.getProperties().get(MemcachedRemoteCache.PROP_BUCKETNAME);

            AuthDescriptor authDescriptor = new AuthDescriptor(new String[]{"PLAIN"},
                    new PlainCallbackHandler(bucketName, password));

            ConnectionFactory fact = new ConnectionFactoryBuilder()
                    .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
                    .setAuthDescriptor(authDescriptor)
                    .build();
            client = new MemcachedClient(fact, addresses);
        } else {
            client = new MemcachedClient(addresses);
        }
        return client;
    }

    /**
     * Constructor for unit test purpose
     */
    MemcachedRemoteCache(RemoteCacheEntity entity, MemcachedClient client) {
        this.remoteCacheEntity = entity;
        this.client = client;
    }

    @Override
    public CachedMessageData get(String key) throws Exception {
        Future<Object> future = null;
        CachedMessageData message;
        try {
            future = client.asyncGet(key);
            Object result = future.get(remoteCacheEntity.getTimeout(), TimeUnit.SECONDS);
            if (null == result) {
                throw new Exception("Cached entry not found for key: " + key);
            }
            if ((result instanceof byte[])) {
                message = new CachedMessageData((byte[]) result);
            } else {
                message = new CachedMessageData((String) result);
            }
        } catch (TimeoutException e) {
            if (future != null) {
                future.cancel(false);
            }
            throw new Exception("Timed out while retrieving value with key: " + key);
        }
        return message;
    }

    @Override
    public void set(String key, CachedMessageData value, int expiry) throws Exception {
        int expiryTime = (int) (System.currentTimeMillis() / 1000) + expiry;

        Future<Boolean> result = null;
        try {
            CachedMessageData.ValueType valueType = value.getValueType();
            if (valueType.equals(CachedMessageData.ValueType.JSON)) {
                result = client.set(key, expiryTime, value.getCacheMessageData(valueType));
            } else {
                result = client.set(key, expiryTime, value.toByteArray());
            }

            result.get(this.remoteCacheEntity.getTimeout(), TimeUnit.SECONDS);
        } catch (IllegalStateException e) {
            throw new Exception();
        } catch (TimeoutException e) {
            if (result != null) {
                result.cancel(false);
                throw new Exception("Could not set the cache key" + key);
            }
        }
    }

    @Override
    public void remove(String key) throws Exception {
        Future<Boolean> result = client.delete(key);
        try {
            result.get(this.remoteCacheEntity.getTimeout(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new Exception("Timed out while removing cache entry with key: " + key);
        }
    }

    public void shutdown() {
        client.shutdown(60, TimeUnit.SECONDS);
    }
}
