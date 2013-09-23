package com.l7tech.external.assertions.icapantivirusscanner.server;

import org.jboss.netty.channel.Channel;

import java.util.Collections;
import java.util.Map;

/**
 * <p>A bean to hold the request specific information that describe a {@link Channel} and the server information that it is connected to.</p>
 *
 * @author KDiep
 */
public final class ChannelInfo {
    private final String host;
    private final int port;
    private final String serviceName;
    private final String failoverService;

    private final Map<String, String> headers;

    private final Channel channel;

    private long lastUsed;

    /**
     * <p>
     *     Construct a new ChannelInfo with the given Channel and information that describe it's connection.
     * </p>
     * @param channel the channel where communication is taking place.
     * @param failoverService the failover service id.
     * @param host the icap hostname.
     * @param port the icap port.
     * @param serviceName the path to the icap service including any query parameters.
     * @param headers the headers being sent along with the request.
     */
    public ChannelInfo(final Channel channel, final String failoverService, final String host, final int port, final String serviceName, Map<String, String> headers){
        this.channel = channel;
        this.failoverService = failoverService;
        this.host = host;
        this.port = port;
        this.serviceName = serviceName;
        this.headers = Collections.unmodifiableMap(headers);
        this.lastUsed = System.currentTimeMillis();
    }

    /**
     *
     * @return the connected channel.
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     *
     * @return the host which the Channel is connected to.
     */
    public String getHost() {
        return host;
    }

    /**
     *
     * @return the port which the Channel is connected to.
     */
    public int getPort() {
        return port;
    }

    /**
     *
     * @return the current failover service.
     */
    public String getFailoverService() {
        return failoverService;
    }

    /**
     *
     * @return the host and port in the form of host:port
     */
    public String getHostAndPort(){
        return String.format("%s:%s", host, port);
    }

    /**
     *
     * @return the full ICAP uri including the protocol, host, port, service name and service parameters (if available).
     */
    public String getIcapUri(){
        return String.format("icap://%s:%s/%s", host, port, serviceName);
    }

    /**
     *
     * @return true if the Channel is still valid, false otherwise.
     */
    public boolean isChannelValid(){
        return (channel != null && (channel.isConnected() && channel.isOpen()) && (channel.isReadable() || channel.isWritable()));
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }
}
