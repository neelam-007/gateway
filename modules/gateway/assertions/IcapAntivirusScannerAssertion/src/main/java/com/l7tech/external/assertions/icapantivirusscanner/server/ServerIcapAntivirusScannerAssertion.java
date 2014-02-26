package com.l7tech.external.assertions.icapantivirusscanner.server;

import ch.mimo.netty.handler.codec.icap.*;
import com.l7tech.common.io.failover.AbstractFailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.mime.MimeBody;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion;
import com.l7tech.external.assertions.icapantivirusscanner.IcapServiceParameter;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.*;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.regex.Matcher;

import static com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion.ICAP_DEFAULT_PORT;
import static com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion.getServiceName;

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

    /**
     * The max timeout value in terms of seconds.  This is defined as 1 hour.
     */
    public static final int MAX_TIMEOUT = 3600;

    private FailoverStrategy<String> failoverStrategy;

    @Inject @Named("stashManagerFactory")
    private StashManagerFactory stashManagerFactory;

    private final Map<String, BlockingDeque<ChannelInfo>> channelPool = new HashMap<String, BlockingDeque<ChannelInfo>>();

    private final Timer idleTimer = new Timer();

    private final ClientSocketChannelFactory socketFactory;

    private final Config config;

    final long channelIdleTimeout;

    public ServerIcapAntivirusScannerAssertion(final IcapAntivirusScannerAssertion assertion, final ApplicationContext applicationContext) throws PolicyAssertionException {
        super(assertion);
        config = validated( applicationContext.getBean( "serverConfig", Config.class ) );

        failoverStrategy = AbstractFailoverStrategy.makeSynchronized(FailoverStrategyFactory.createFailoverStrategy(
                assertion.getFailoverStrategy(),
                assertion.getIcapServers().toArray(new String[assertion.getIcapServers().size()])));
        socketFactory = new OioClientSocketChannelFactory(Executors.newCachedThreadPool());
        channelIdleTimeout = config.getTimeUnitProperty(IcapAntivirusScannerAssertion.CHANNEL_TIMEOUT_PROPERTY_NAME, TimeUnit.MINUTES.toMillis(1));
        idleTimer.schedule(new IdleTimerTask(), channelIdleTimeout, 5000);
    }

    private Config validated( final Config config ) {
        final ValidatedConfig vc = new ValidatedConfig( config, logger, new Resolver<String,String>(){
            @Override
            public String resolve( final String key ) {
                String resolved = key;
                if(IcapAntivirusScannerAssertion.CHANNEL_TIMEOUT_PROPERTY_NAME.equals(key)){
                    resolved = IcapAntivirusScannerAssertion.CLUSTER_PROPERTY_CHANNEL_TIMEOUT;
                }
                return resolved;
            }
        } );
        vc.setMinimumValue(IcapAntivirusScannerAssertion.CHANNEL_TIMEOUT_PROPERTY_NAME,
                IcapAntivirusScannerAssertion.MIN_CHANNEL_IDLE_TIMEOUT);
        vc.setMaximumValue(IcapAntivirusScannerAssertion.CHANNEL_TIMEOUT_PROPERTY_NAME,
                IcapAntivirusScannerAssertion.MAX_CHANNEL_IDLE_TIMEOUT);
        return vc;
    }

    private ClientBootstrap intializeClient(final PolicyEnforcementContext context) {
        final ClientBootstrap client = new ClientBootstrap(socketFactory);

        client.setPipelineFactory(new IcapClientChannelPipeline(getTimeoutValue(context, assertion.getResponseReadTimeout())));
        client.setOption("soLinger", 0);
        client.setOption("connectTimeoutMillis", getTimeoutValue(context, assertion.getConnectionTimeout()));
        client.setOption("readTimeoutMillis", getTimeoutValue(context, assertion.getReadTimeout()));
        return client;
    }

    private ChannelInfo getChannelForEndpoint(String endpoint) {
        synchronized (channelPool) {
            BlockingDeque<ChannelInfo> available = channelPool.get(endpoint);
            if (available == null) {
                return null;
            }
            while(!available.isEmpty()){
                ChannelInfo channel = available.poll();
                if (channel != null && channel.isChannelValid()) {
                    return channel;
                }
            }
        }
        return null;
    }

    private int getTimeoutValue(final PolicyEnforcementContext context, String value) {
        int timeout = DEFAULT_TIMEOUT;
        String timeoutStr = getContextVariableValue(context, value);
        if (ValidationUtils.isValidInteger(timeoutStr, false, 1, MAX_TIMEOUT)) {
            timeout = Integer.parseInt(timeoutStr) * 1000;
        } else {
            logAndAudit(AssertionMessages.ICAP_INVALID_TIMEOUT, timeoutStr);
        }
        return timeout;
    }

    private String getContextVariableValue(final PolicyEnforcementContext context, final String conVar) {
        String retVal = conVar;
        if (retVal != null && retVal.length() > 0) {
            Map<String, Object> vars = context.getVariableMap(Syntax.getReferencedNames(conVar), getAudit());
            retVal = ExpandVariables.process(conVar, vars, getAudit());
        }
        return retVal;
    }

    ChannelInfo getChannel(ClientBootstrap client, PolicyEnforcementContext context) {

        allServers: for (int i = 0; i < assertion.getIcapServers().size(); ++i) {
            String selectedService = failoverStrategy.selectService();
            if (selectedService == null) {
                continue;
            }
            Matcher matcher = IcapAntivirusScannerAssertion.ICAP_URI.matcher(selectedService);
            if (matcher.matches()) {
                final String fullExpression = matcher.group(1).trim();
                //what vars are referenced? - find out and resolve each value to enable more useful log / audit message
                final String[] referencedNames = Syntax.getReferencedNames(fullExpression);
                for (final String referencedName : referencedNames) {
                    final String varExpression = Syntax.getVariableExpression(referencedName);
                    final String varValue = getContextVariableValue(context, varExpression);
                    if (varValue == null || varValue.trim().isEmpty()) {
                        logAndAudit(AssertionMessages.ICAP_INVALID_URI, "URI referenced variable '" + referencedName + "' resolved to no value");
                        failoverStrategy.reportFailure(selectedService);
                        continue allServers;
                    }
                }
                final String icapUrl = getContextVariableValue(context, fullExpression);
                final String testUrl = "http" + icapUrl;
                final URL url;
                try {
                    url = new URL(testUrl);
                    //We assume a service name e.g. a path, this requirement could be removed later.
                    if (url.getPath().isEmpty() || url.getPath().equals("/")) {
                        throw new MalformedURLException("A service name is required");
                    }

                } catch (MalformedURLException e) {
                    logAndAudit(AssertionMessages.ICAP_INVALID_URI, "Invalid resolved URI for ICAP Server: icap'" + icapUrl + "': " + e.getMessage());
                    failoverStrategy.reportFailure(selectedService);
                    continue;
                }

                final String hostname = url.getHost();
                // port should always have a value, protect against this changing here
                final int port = (url.getPort() != -1)? url.getPort(): ICAP_DEFAULT_PORT;
                final String portText = String.valueOf(port);
                final String hostAndPort = String.format("%s:%s", hostname, portText);

                String serviceName = getServiceName(url.getPath()) + getServiceQueryString(context, url.getQuery());
                //its possible the context variable itself has a leading '/' - if this is the case it will show in logs and the user will need to fix the variable value.

                ChannelInfo channelInfo = getChannelForEndpoint(hostAndPort);
                if(channelInfo == null){
                    ChannelFuture future = client.connect(new InetSocketAddress(hostname, Integer.parseInt(portText)));
                    future.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                    Channel channel = future.awaitUninterruptibly().getChannel();
                    if (!(future.isDone() && future.isSuccess() && future.getChannel().isConnected())) {
                        logAndAudit(AssertionMessages.ICAP_CONNECTION_FAILED, hostAndPort);
                        failoverStrategy.reportFailure(selectedService);
                        continue;
                    }
                    return new ChannelInfo(channel, selectedService, hostname,  Integer.parseInt(portText),
                            serviceName, getHeaders(context));
                }
                return channelInfo;
            } else {
                logAndAudit(AssertionMessages.ICAP_INVALID_URI, selectedService);
                failoverStrategy.reportFailure(selectedService);
            }
        }
        return null;
    }

    @Override
    public AssertionStatus doCheckRequest(final PolicyEnforcementContext context, final Message message,
                                          final String messageDescription, final AuthenticationContext authContext)
            throws IOException, PolicyAssertionException {

        ClientBootstrap client = intializeClient(context);
        return scanMessage(context, message, client);
    }

    //making this package default so that it can be tested in the test class
    AssertionStatus scanMessage(final PolicyEnforcementContext context, final Message message, final ClientBootstrap client) {
        AssertionStatus status = AssertionStatus.NONE;
        List<String> infectedParts = new ArrayList<String>();
        try {
            for (PartIterator pi = message.getMimeKnob().getParts(); pi.hasNext(); ) {
                status = scan(context, pi.next(), 0, infectedParts, client);
                if (status != AssertionStatus.NONE) {
                    break;
                }
            }
        } catch (IOException e) {
            logAndAudit(AssertionMessages.ICAP_IO_ERROR, assertion.getTargetName());
            status = AssertionStatus.FAILED;
        }
        if(!infectedParts.isEmpty()){
            context.setVariable(prefix(IcapAntivirusScannerAssertion.INFECTED_PARTS),
                    infectedParts.toArray(new String[infectedParts.size()]));
        }
        return status;
    }

    private AssertionStatus scan(final PolicyEnforcementContext context, PartInfo partInfo, int currentDepth, List<String> infectedParts, ClientBootstrap client) {
        try {
            if (currentDepth != assertion.getMaxMimeDepth() && partInfo.getContentType().isMultipart()) {
                MimeBody mimeBody = null;
                try {
                    mimeBody = new MimeBody(stashManagerFactory.createStashManager(),
                            partInfo.getContentType(), partInfo.getInputStream(false), 0L);
                    for (final PartInfo pi : mimeBody) {
                        //recursively traverse all the multiparts break when it failed
                        AssertionStatus status = scan(context, pi, currentDepth++, infectedParts, client);
                        if (status == AssertionStatus.FAILED) {
                            return status;
                        }
                    }
                } catch (IOException e) {
                    logAndAudit(AssertionMessages.ICAP_MIME_ERROR, assertion.getTargetName(), e.getMessage());
                    return AssertionStatus.FAILED;
                } finally {
                    if (mimeBody != null) {
                        mimeBody.close();
                    }
                }
            } else {
                ChannelInfo channelInfo = getChannel(client, context);
                if(channelInfo == null){
                    logAndAudit(AssertionMessages.ICAP_NO_VALID_SERVER);
                    return AssertionStatus.FAILED;
                }
                if(!channelInfo.isChannelValid()){
                    logAndAudit(AssertionMessages.ICAP_CONNECTION_FAILED, channelInfo.getIcapUri());
                    return AssertionStatus.FAILED;
                }
                try {
                    final AbstractIcapResponseHandler handler = (AbstractIcapResponseHandler) channelInfo.getChannel().getPipeline().get("handler");
                    final IcapRequest request = createRequest(context, partInfo, channelInfo);

                    channelInfo.getChannel().write(request);
                    final IcapResponse response = handler.getResponse();

                    if (response == null) {
                        logAndAudit(AssertionMessages.ICAP_NO_RESPONSE);
                        failoverStrategy.reportFailure(channelInfo.getFailoverService());
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
                        logAndAudit(AssertionMessages.ICAP_SERVICE_UNAVAILABLE, channelInfo.getIcapUri());
                        failoverStrategy.reportFailure(channelInfo.getFailoverService());
                        return AssertionStatus.FAILED;
                    }
                    failoverStrategy.reportSuccess(channelInfo.getFailoverService());
                } catch (IOException e) {
                    logAndAudit(AssertionMessages.ICAP_SCAN_ERROR, assertion.getTargetName(), e.getMessage());
                    return AssertionStatus.FAILED;
                }
                finally {
                    if(channelInfo != null && channelInfo.isChannelValid()){
                        synchronized (channelPool) {
                            BlockingDeque<ChannelInfo> channels = channelPool.get(channelInfo.getHostAndPort());
                            if (channels == null) {
                                channels = new LinkedBlockingDeque<ChannelInfo>();
                            }
                            try {
                                channelInfo.setLastUsed(System.currentTimeMillis());
                                channels.put(channelInfo);
                                channelPool.put(channelInfo.getHostAndPort(), channels);
                            } catch (InterruptedException e) {
                                //can't add, we'll just close it
                                channelInfo.getChannel().close().addListener(ChannelFutureListener.CLOSE);
                            }
                        }
                    }
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

        logAndAudit(AssertionMessages.ICAP_VIRUS_DETECTED, assertion.getTargetName(), partName);
        logAndAudit(AssertionMessages.ICAP_VIRUS_RESPONSE_STATUS, String.valueOf(response.getStatus().getCode()), response.getStatus().toString());
        StringBuilder sb = new StringBuilder("Headers returned from ICAP service:\r\n");
        for (Map.Entry<String, String> ent : response.getHeaders()) {
            headerNames.add(ent.getKey());
            headerValues.add(ent.getValue());
            sb.append("  ").append(ent.getKey()).append(": ").append(ent.getValue()).append("\r\n");
        }
        context.setVariable(prefix(IcapAntivirusScannerAssertion.VARIABLE_NAMES) + "." + (infectedParts.size() - 1), headerNames.toArray(new String[headerNames.size()]));
        context.setVariable(prefix(IcapAntivirusScannerAssertion.VARIABLE_VALUES) + "."  + (infectedParts.size() - 1), headerValues.toArray(new String[headerValues.size()]));

        String prefix = prefix(IcapAntivirusScannerAssertion.VARIABLE_NAME) + "."  + (infectedParts.size() - 1) + ".";
        for(int i = 0; i < Math.max(headerNames.size(), headerValues.size()); ++i){
            context.setVariable(prefix + headerNames.get(i), headerValues.get(i));
        }
        logAndAudit(AssertionMessages.ICAP_VIRUS_RESPONSE_HEADERS, sb.toString());
    }

    public void close() {
        idleTimer.cancel();
        for (Queue<ChannelInfo> channels : channelPool.values()) {
            for (ChannelInfo c : channels ) {
                Channels.close(c.getChannel());
            }
        }
        socketFactory.releaseExternalResources();
    }

    private String prefix( final String name ) {
        String prefixed = name;

        if ( assertion.getVariablePrefix() != null ) {
            prefixed = assertion.getVariablePrefix()  + "." + name;
        }

        return prefixed;
    }

    private Map<String, String> getHeaders(final PolicyEnforcementContext context){
        Map<String, String> ret = new HashMap<String, String>();
        for (IcapServiceParameter p : assertion.getParameters()) {
            if(p.getType().equals(IcapServiceParameter.HEADER)){
                String key = getContextVariableValue(context, p.getName());
                String value = getContextVariableValue(context, p.getValue());
                ret.put(key, value);
            }
        }
        return ret;
    }

    private String getServiceQueryString(final PolicyEnforcementContext context, final String query) {
        StringBuilder sb = new StringBuilder();
        if(query != null && !query.isEmpty()){
            if(query.startsWith("?")){
                sb.append(query).append("&");
            }
            else {
                sb.append("?").append(query).append("&");
            }
        }
        else {
            sb.append("?");
        }
        try {
            StringBuilder s = new StringBuilder();
            for (IcapServiceParameter p : assertion.getParameters()) {
                if(p.getType().equals(IcapServiceParameter.QUERY)){
                    String key = getContextVariableValue(context, p.getName());
                    String value = getContextVariableValue(context, p.getValue());
                    s.append(URLEncoder.encode(key, URL_ENCODING)).append("=").append(URLEncoder.encode(value, URL_ENCODING)).append("&");
                }
            }
            if(s.length() > 0){
                s = s.delete(s.length() - 1, s.length());
            }
            sb.append(s);
        } catch (UnsupportedEncodingException e) {
            logAndAudit(AssertionMessages.ICAP_UNSUPPORTED_ENCODING, ExceptionUtils.getMessage(e));
        }
        return sb.length() == 1 ? "" : sb.toString();
    }

    private IcapRequest createRequest(final PolicyEnforcementContext context, final PartInfo partInfo, final ChannelInfo channelInfo) throws IOException, NoSuchPartException {
        final IcapRequest request = new DefaultIcapRequest(IcapVersion.ICAP_1_0, IcapMethod.RESPMOD,
                channelInfo.getIcapUri(),
                channelInfo.getHost());
        request.addHeader(HttpHeaders.Names.ALLOW, "204");
        for(Map.Entry<String, String> ent : getHeaders(context).entrySet()){
            request.addHeader(ent.getKey(), ent.getValue());
        }
        final HttpResponse httpResponse = new StreamedHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK);
        ((StreamedHttpResponse)httpResponse).setContent(partInfo.getInputStream(false));
        httpResponse.addHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        request.setHttpResponse(httpResponse);
        final HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/");
        request.setHttpRequest(httpRequest);
        return request;
    }

    private class IdleTimerTask extends TimerTask {
        @Override
        public void run() {
            synchronized (channelPool) {
                for (Queue<ChannelInfo> channels : channelPool.values()) {
                    for (Iterator<ChannelInfo> it = channels.iterator(); it.hasNext(); ) {
                        ChannelInfo c = it.next();
                        long currentTime = System.currentTimeMillis();
                        long lastUsed = c.getLastUsed();
                        if(currentTime - lastUsed >= channelIdleTimeout){
                            it.remove();
                            Channels.close(c.getChannel());
                        }
                    }
                }
            }
        }
    }
}