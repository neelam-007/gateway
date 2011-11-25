package com.l7tech.external.assertions.icapantivirusscanner.server;

import ch.mimo.netty.handler.codec.icap.IcapResponse;
import ch.mimo.netty.handler.codec.icap.IcapResponseStatus;
import com.l7tech.common.io.failover.AbstractFailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.mime.MimeBody;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.UnaryVoid;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.ValidationUtils;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.ChannelGroupFutureListener;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;

/**
 * <p>Server side implementation of the IcapAntivirusScannerAssertion.</p>
 *
 * @author Ken Diep
 * @see com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion
 */
public class ServerIcapAntivirusScannerAssertion extends AbstractMessageTargetableServerAssertion<IcapAntivirusScannerAssertion> {
    private static final int MAX_PORT = 65535;
    private static final int DEFAULT_TIMEOUT = 30000;
    private static final String URL_ENCODING = "UTF-8";
    private static final int DEFAULT_CHANNEL_IDLE_TIMEOUT = 60;
    private static final String CHANNEL_TIMEOUT_PROPERTY = "gateway.icap.channelIdleTimeout";
    private static final int DEFAULT_CHANNEL_THREAD_POOL_SIZE = 10;
    private static final String CHANNEL_THREAD_POOL_SIZE_PROPERTY = "gateway.icap.threadPoolSize";

    /**
     * The max timeout value in terms of seconds.  This is defined as 1 hour.
     */
    public static final int MAX_TIMEOUT = 3600;

    private FailoverStrategy<String> failoverStrategy;

    @Inject @Named("stashManagerFactory")
    private StashManagerFactory stashManagerFactory;

    private String currentIcapUri = null;
    private String currentHost = null;
    private String selectedService = null;

    private final Map<String, Queue<Channel>> channelPool = new HashMap<String, Queue<Channel>>();

    private final ChannelGroup channelGroup = new DefaultChannelGroup();

    private final org.jboss.netty.util.Timer idleTimer = new HashedWheelTimer();

    private final ClientSocketChannelFactory socketFactory;

    public ServerIcapAntivirusScannerAssertion(final IcapAntivirusScannerAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        failoverStrategy = AbstractFailoverStrategy.makeSynchronized(FailoverStrategyFactory.createFailoverStrategy(
                assertion.getFailoverStrategy(),
                assertion.getIcapServers().toArray(new String[assertion.getIcapServers().size()])));
        int poolSize = ServerConfig.getInstance().getIntProperty(CHANNEL_THREAD_POOL_SIZE_PROPERTY, DEFAULT_CHANNEL_THREAD_POOL_SIZE);
        socketFactory = new OioClientSocketChannelFactory(Executors.newFixedThreadPool(poolSize));
    }

    private ClientBootstrap intializeClient(final PolicyEnforcementContext context) {
        ClientBootstrap client = new ClientBootstrap(socketFactory);
        int channelIdleTimeout = ServerConfig.getInstance().getIntProperty(CHANNEL_TIMEOUT_PROPERTY, DEFAULT_CHANNEL_IDLE_TIMEOUT);
        client.setPipelineFactory(new IcapClientChannelPipeline(idleTimer, new UnaryVoid <Integer>() {
            @Override
            public void call(final Integer channelId) {
                //this is to remove stale and/or invalid channels as triggered by the IcapResponseHandler
                synchronized (channelPool) {
                    for (Queue<Channel> channels : channelPool.values()) {
                        for (Iterator<Channel> it = channels.iterator(); it.hasNext(); ) {
                            Channel c = it.next();
                            if (c.getId().equals(channelId)) {
                                it.remove();
                            }
                        }
                    }
                }
            }
        }, channelIdleTimeout));
        client.setOption("soLinger", 0);
        client.setOption("connectTimeoutMillis", getTimeoutValue(context, assertion.getConnectionTimeout()));
        client.setOption("readTimeoutMillis", getTimeoutValue(context, assertion.getReadTimeout()));
        return client;
    }

    private Channel getChannelForEndpoint(String endpoint) {
        synchronized (channelPool) {
            Queue<Channel> available = channelPool.get(endpoint);
            if (available == null || available.isEmpty()) {
                return null;
            }
            Channel cur = null;
            for (Iterator<Channel> it = available.iterator(); it.hasNext(); ) {
                cur = it.next();
                if (!cur.isConnected() || !cur.isOpen()) {
                    it.remove();
                }
                else {
                    break;
                }
            }
            return cur;
        }
    }

    private int getTimeoutValue(final PolicyEnforcementContext context, String value) {
        int timeout = DEFAULT_TIMEOUT;
        String timeoutStr = getContextVariable(context, value);
        if (ValidationUtils.isValidInteger(timeoutStr, false, 1, MAX_TIMEOUT)) {
            timeout = Integer.parseInt(timeoutStr) * 1000;
        } else {
            logAndAudit(AssertionMessages.USERDETAIL_INFO, "Invalid timeout value from " + value + " (" + timeoutStr + ").  Timeout value must be a valid integer with range 1 to 3600 inclusive.");
        }
        return timeout;
    }

    private String getContextVariable(final PolicyEnforcementContext context, final String conVar) {
        String retVal = conVar;
        if (retVal != null && retVal.length() > 0) {
            Map<String, Object> vars = context.getVariableMap(Syntax.getReferencedNames(conVar), getAudit());
            retVal = ExpandVariables.process(conVar, vars, getAudit());
        }
        return retVal;
    }

    Channel getChannel(ClientBootstrap client, PolicyEnforcementContext context) {
        Channel channel = null;
        for (int i = 0; i < assertion.getIcapServers().size(); ++i) {
            selectedService = failoverStrategy.selectService();
            if (selectedService == null) {
                continue;
            }
            Matcher matcher = IcapAntivirusScannerAssertion.ICAP_URI.matcher(selectedService);
            if (matcher.matches()) {
                String hostname = getContextVariable(context, matcher.group(1).trim());
                String portText = getContextVariable(context, matcher.group(2).trim());
                if (!ValidationUtils.isValidInteger(portText, false, 1, MAX_PORT)) {
                    logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Invalid port specified, port must be between 1 and 65535: " + selectedService);
                    failoverStrategy.reportFailure(selectedService);
                    continue;
                }
                String serviceName = getContextVariable(context, matcher.group(3).trim());
                String currentService = String.format("icap://%s:%s/%s", hostname, Integer.parseInt(portText), serviceName);
                channel = getChannelForEndpoint(selectedService);
                if(channel == null){
                    ChannelFuture future = client.connect(new InetSocketAddress(InetAddressUtil.getHostForUrl(hostname), Integer.parseInt(portText)));
                    channel = future.awaitUninterruptibly().getChannel();
                    channelGroup.add(channel);//add newly created channels to the channel group
                    if (!future.isSuccess()) {
                        future.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                        logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Unable to connect to the specified server: "
                                + selectedService);
                        failoverStrategy.reportFailure(selectedService);
                        channel = null;
                        continue;
                    }
                    currentHost = hostname;
                    currentIcapUri = currentService + getServiceQueryString(context);
                    break;
                }
                return channel;
            } else {
                logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Invalid ICAP URI: " + selectedService);
                failoverStrategy.reportFailure(selectedService);
            }
        }
        return channel;
    }

    @Override
    public AssertionStatus doCheckRequest(final PolicyEnforcementContext context, final Message message,
                                          final String messageDescription, final AuthenticationContext authContext)
            throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.NONE;
        Channel channel = null;
        try {
            ClientBootstrap client = intializeClient(context);
            channel = getChannel(client, context);
            if (channel == null) {
                logAndAudit(AssertionMessages.USERDETAIL_WARNING, "No valid ICAP server entries found");
                return AssertionStatus.FAILED;
            }
            status = scanMessage(context, message, channel);
        } finally {
            if (channel != null) {
                if ((channel.isConnected() && channel.isOpen()) && (channel.isReadable() || channel.isWritable())) {
                    synchronized (channelPool) {
                        Queue<Channel> channels = channelPool.get(selectedService);
                        if (channels == null) {
                            channels = new LinkedList<Channel>();
                        }
                        for(Iterator<Channel> it = channels.iterator(); it.hasNext(); ){
                            Channel c = it.next();
                            if(c.getId().equals(channel.getId())){
                                it.remove();
                                break;
                            }
                        }
                        channels.add(channel);
                        channelPool.put(selectedService, channels);
                    }
                } else {
                    channel.close().addListener(ChannelFutureListener.CLOSE);
                }
            }
        }
        return status;
    }

    //making this package default so that it can be tested in the test class
    AssertionStatus scanMessage(final PolicyEnforcementContext context, final Message message, final Channel channel) {
        AssertionStatus status = AssertionStatus.NONE;
        List<String> infectedParts = new ArrayList<String>();
        try {
            for (PartIterator pi = message.getMimeKnob().getParts(); pi.hasNext(); ) {
                status = scan(context, pi.next(), 0, infectedParts, channel);
                if (status != AssertionStatus.NONE) {
                    break;
                }
            }
        } catch (IOException e) {
            logAndAudit(AssertionMessages.USERDETAIL_WARNING, "I/O error occurred while scanning message "
                    + assertion.getTargetName());
            status = AssertionStatus.FAILED;
        }
        if(!infectedParts.isEmpty()){
            context.setVariable(IcapAntivirusScannerAssertion.INFECTED_PARTS,
                    infectedParts.toArray(new String[infectedParts.size()]));
        }
        return status;
    }

    private AssertionStatus scan(final PolicyEnforcementContext context, PartInfo partInfo, int currentDepth, List<String> infectedParts, Channel channel) {
        try {
            if (currentDepth != assertion.getMaxMimeDepth() && partInfo.getContentType().isMultipart()) {
                MimeBody mimeBody = null;
                try {
                    mimeBody = new MimeBody(stashManagerFactory.createStashManager(),
                            partInfo.getContentType(), partInfo.getInputStream(false), 0L);
                    for (final PartInfo pi : mimeBody) {
                        //recursively traverse all the multiparts
                        //break when it failed
                        AssertionStatus status = scan(context, pi, currentDepth++, infectedParts, channel);
                        if (status == AssertionStatus.FAILED) {
                            return status;
                        }
                    }
                } catch (IOException e) {
                    logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Error reading MIME content from " + assertion.getTargetName() + " : " + e.getMessage());
                    return AssertionStatus.FAILED;
                } finally {
                    if (mimeBody != null) {
                        mimeBody.close();
                    }
                }
            } else {
                try {
                    AbstractIcapResponseHandler handler = (AbstractIcapResponseHandler) channel.getPipeline().get("handler");
                    IcapResponse response = handler.scan(currentIcapUri, currentHost, partInfo);
                    if (response == null) {
                        logAndAudit(AssertionMessages.USERDETAIL_WARNING, "No ICAP response received.");
                        failoverStrategy.reportFailure(selectedService);
                        return AssertionStatus.FAILED;
                    }
                    //204 - NO CONTENT signify no virus found
                    //Symantec return 200 if a virus is found when using the avscan service
                    //         return 201 if a virus is found when using the SYMCScanResp-AV service
                    //other virus engines return 200 if a virus is found, 204 when there's no virus.
                    if (response.getStatus() == IcapResponseStatus.OK || response.getStatus() == IcapResponseStatus.CREATED) {
                        logVirusInformation(context, response, partInfo.getContentId(true), infectedParts);
                        if (!assertion.isContinueOnVirusFound()) {
                            return AssertionStatus.FAILED;
                        }
                    } else if (response.getStatus() == IcapResponseStatus.SERVICE_UNAVAILABLE){
                        logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Service not available " + currentIcapUri);
                        failoverStrategy.reportFailure(selectedService);
                        return AssertionStatus.FAILED;
                    }
                    failoverStrategy.reportSuccess(selectedService);
                } catch (IOException e) {
                    logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Error occurred while scanning content " + assertion.getTargetName() + " : " + e.getMessage());
                    return AssertionStatus.FAILED;
                }
            }
        } catch (NoSuchPartException e) {
            logAndAudit(AssertionMessages.NO_SUCH_PART, new String[]{assertion.getTargetName(),
                    String.valueOf(partInfo.getPosition())}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }

    private void logVirusInformation(PolicyEnforcementContext context, IcapResponse response, String contentId, List<String> infectedParts) {
        String partName = assertion.getTarget() == TargetMessageType.OTHER ? assertion.getOtherTargetMessageVariable() : contentId;
        if (partName == null) {
            partName = "Unknown";
        }
        infectedParts.add(partName);
        List<String> headerNames = new ArrayList<String>();
        List<String> headerValues = new ArrayList<String>();

        logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Virus detected in " + assertion.getTargetName() + " (" + partName + ")");
        logAndAudit(AssertionMessages.USERDETAIL_WARNING, "ICAP Status: (" + response.getStatus().getCode() + ": " + response.getStatus() + ")");
        StringBuilder sb = new StringBuilder("Headers returned from ICAP service:\r\n");
        for (Map.Entry<String, String> ent : response.getHeaders()) {
            headerNames.add(ent.getKey());
            headerValues.add(ent.getValue());
            sb.append("  ").append(ent.getKey()).append(": ").append(ent.getValue()).append("\r\n");
        }
        context.setVariable(IcapAntivirusScannerAssertion.VARIABLE_NAMES + "." + (infectedParts.size() - 1), headerNames.toArray(new String[headerNames.size()]));
        context.setVariable(IcapAntivirusScannerAssertion.VARIABLE_VALUES + "."  + (infectedParts.size() - 1), headerValues.toArray(new String[headerValues.size()]));

        String prefix = IcapAntivirusScannerAssertion.VARIABLE_NAME + "."  + (infectedParts.size() - 1) + ".";
        for(int i = 0; i < Math.max(headerNames.size(), headerValues.size()); ++i){
            context.setVariable(prefix + headerNames.get(i), headerValues.get(i));
        }
        logAndAudit(AssertionMessages.USERDETAIL_WARNING, sb.toString());
    }

    public void close() {
        idleTimer.stop();
        for (Queue<Channel> channels : channelPool.values()) {
            for(Channel c : channels){
                c.close();
            }
        }
        channelGroup.close().addListener(new ChannelGroupFutureListener() {
            @Override
            public void operationComplete(final ChannelGroupFuture channelGroupFuture) throws Exception {
                //releasing extern resources requires all opened channels to be closed
                socketFactory.releaseExternalResources();
            }
        });
    }

    private String getServiceQueryString(final PolicyEnforcementContext context) {
        StringBuilder sb = new StringBuilder("?");
        try {
            for (Map.Entry<String, String> ent : assertion.getServiceParameters().entrySet()) {
                String key = getContextVariable(context, ent.getKey());
                String value = getContextVariable(context, ent.getValue());
                sb.append(URLEncoder.encode(key, URL_ENCODING)).append("=").append(URLEncoder.encode(value, URL_ENCODING)).append("&");
            }
            sb = sb.delete(sb.length() - 1, sb.length());
        } catch (UnsupportedEncodingException e) {
            logAndAudit(AssertionMessages.USERDETAIL_WARNING, new String[]{assertion.getTargetName(), ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
        }
        return sb.toString();
    }

}