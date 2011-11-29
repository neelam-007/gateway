package com.l7tech.external.assertions.icapantivirusscanner.server;

import org.jboss.netty.channel.Channel;

/**
 * <p>A bean to hold the request specific information that describe a {@link Channel} and the server information that it is connected to.</p>
 *
 * @author KDiep
 */
public final class ChannelInfo {
    private final String host;
    private final int port;
    private final String serviceName;
    private final String serviceParameters;
    private final String failoverService;

    private final Channel channel;

    /**
     * <p>
     *     Construct a new ChannelInfo with the given Channel and information that describe it's connection.
     * </p>
     * @param channel
     * @param failoverService
     * @param host
     * @param port
     * @param serviceName
     * @param serviceParameters
     */
    public ChannelInfo(final Channel channel, final String failoverService, final String host, final int port, final String serviceName, final String serviceParameters){
        this.channel = channel;
        this.failoverService = failoverService;
        this.host = host;
        this.port = port;
        this.serviceName = serviceName;
        this.serviceParameters = serviceParameters;
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
     * @return the service name to send request to for the connected Channel.
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     *
     * @return the optional service parameters to attach to the target service.
     */
    public String getServiceParameters() {
        return serviceParameters;
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
        return String.format("icap://%s:%s/%s%s", host, port, serviceName, serviceParameters);
    }

    /**
     *
     * @return true if the Channel is still valid, false otherwise.
     */
    public boolean isChannelValid(){
        return (channel != null && (channel.isConnected() && channel.isOpen()) && (channel.isReadable() || channel.isWritable()));
    }


}
