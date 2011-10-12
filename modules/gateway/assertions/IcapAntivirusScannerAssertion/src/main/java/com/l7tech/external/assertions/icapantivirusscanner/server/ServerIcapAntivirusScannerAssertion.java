package com.l7tech.external.assertions.icapantivirusscanner.server;

import ch.mimo.netty.handler.codec.icap.IcapResponse;
import ch.mimo.netty.handler.codec.icap.IcapResponseStatus;
import com.l7tech.common.io.failover.AbstractFailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.mime.*;
import com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion;
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
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ValidationUtils;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private ClientBootstrap client = null;

    private FailoverStrategy<String> failoverStrategy;

    @Inject @Named("stashManagerFactory")
    private StashManagerFactory stashManagerFactory;

    private IcapAntivirusScannerAssertion assertion;

    private String currentIcapUri = null;
    private String currentHost = null;
    private String selectedService = null;


    public ServerIcapAntivirusScannerAssertion(final IcapAntivirusScannerAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        this.assertion = assertion;
        failoverStrategy = AbstractFailoverStrategy.makeSynchronized(FailoverStrategyFactory.createFailoverStrategy(
                assertion.getFailoverStrategy(),
                assertion.getIcapServers().toArray(new String[assertion.getIcapServers().size()])));
    }

    private void intializeClient(final PolicyEnforcementContext context) {
        if (client == null) {
            client = new ClientBootstrap(new OioClientSocketChannelFactory(Executors.newSingleThreadExecutor()));
            client.setPipelineFactory(new IcapClientChannelPipeline());
            client.setOption("connectTimeoutMillis", getTimeoutValue(context, assertion.getConnectionTimeout()));
            client.setOption("readTimeoutMillis", getTimeoutValue(context, assertion.getReadTimeout()));
        }
    }

    private long getTimeoutValue(final PolicyEnforcementContext context, String value) {
        long timeout = DEFAULT_TIMEOUT;
        String timeoutStr = getContextVariable(context, value);
        if (ValidationUtils.isValidInteger(timeoutStr, false, 1, Integer.MAX_VALUE)) {
            timeout = Integer.parseInt(timeoutStr) * 1000;
        } else {
            logAndAudit(AssertionMessages.USERDETAIL_INFO, "Invalid timeout value from " + value + " (" + timeoutStr + ").");
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

    private Channel getChannel(PolicyEnforcementContext context) {
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
                }
                String serviceName = getContextVariable(context, matcher.group(3).trim());
                String currentService = String.format("icap://%s:%s/%s", hostname, Integer.parseInt(portText), serviceName);
                ChannelFuture future = client.connect(new InetSocketAddress(hostname, Integer.parseInt(portText)));
                channel = future.awaitUninterruptibly().getChannel();
                if (!future.isSuccess()) {
                    logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Unable to connect to the specified server: "
                            + selectedService);
                    failoverStrategy.reportFailure(selectedService);
                    channel = null;
                    continue;
                }
                currentHost = hostname;
                currentIcapUri = currentService + getServiceQueryString(context);
                break;
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
        intializeClient(context);
        return scanMessage(context, message);
    }

    //making this package default so that it can be tested in the test class
    AssertionStatus scanMessage(final PolicyEnforcementContext context, final Message message) {
        AssertionStatus status = AssertionStatus.NONE;
        List<String> infectedParts = new ArrayList<String>();
        try {
            for (PartIterator pi = message.getMimeKnob().getParts(); pi.hasNext(); ) {
                status = scan(context, pi.next(), 0, infectedParts);
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

    private AssertionStatus scan(final PolicyEnforcementContext context, PartInfo partInfo, int currentDepth, List<String> infectedParts) {
        try {
            if (currentDepth != assertion.getMaxMimeDepth() && partInfo.getContentType().isMultipart()) {
                try {
                    MimeBody mimeBody = new MimeBody(stashManagerFactory.createStashManager(),
                            ContentTypeHeader.OCTET_STREAM_DEFAULT, partInfo.getInputStream(false), Message.getMaxBytes());
                    for (PartIterator pit = mimeBody.iterator(); pit.hasNext(); ) {
                        PartInfo pi = pit.next();
                        //recursively traverse all the multiparts
                        //break when it failed
                        AssertionStatus status = scan(context, pi, currentDepth++, infectedParts);
                        if (status == AssertionStatus.FAILED) {
                            return status;
                        }
                    }
                } catch (IOException e) {
                    logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Error reading MIME content from " + assertion.getTargetName() + " : " + e.getMessage());
                    return AssertionStatus.FAILED;
                }
            } else {
                Channel channel = null;
                try {
                    channel = getChannel(context);
                    if (channel == null) {
                        logAndAudit(AssertionMessages.USERDETAIL_WARNING, "No valid ICAP server entries found");
                        return AssertionStatus.FAILED;
                    }
                    AbstractIcapResponseHandler handler = (AbstractIcapResponseHandler) channel.getPipeline().get("handler");
                    IcapResponse response = handler.scan(currentIcapUri, currentHost, partInfo);
                    if (response == null) {
                        logAndAudit(AssertionMessages.USERDETAIL_WARNING, "No ICAP response received - please check ICAP connection.");
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
                finally {
                    if (channel != null) {
                        channel.close();
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

    @Override
    public void close() {
        if (client != null) {
            client.releaseExternalResources();
            client = null;
        }
    }

    private String getServiceQueryString(final PolicyEnforcementContext context) {
        StringBuilder sb = new StringBuilder("?");
        for (Map.Entry<String, String> ent : assertion.getServiceParameters().entrySet()) {
            String key = getContextVariable(context, ent.getKey());
            String value = getContextVariable(context, ent.getValue());
            sb.append(key).append("=").append(value).append("&");
        }
        sb = sb.delete(sb.length() - 1, sb.length());
        return sb.toString();
    }

    void setClient(final ClientBootstrap client) {
        this.client = client;
    }


}


