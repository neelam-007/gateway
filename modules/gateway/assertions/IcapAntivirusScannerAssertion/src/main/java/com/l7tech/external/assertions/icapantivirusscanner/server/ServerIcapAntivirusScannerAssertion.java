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
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Server side implementation of the IcapAntivirusScannerAssertion.</p>
 *
 * @author Ken Diep
 * @see com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion
 */
public class ServerIcapAntivirusScannerAssertion extends AbstractMessageTargetableServerAssertion<IcapAntivirusScannerAssertion> {

    private static final Map<String, String> RESOLUTION_MAPPING;

    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("0", "was not fixed.");
        m.put("1", "was repaired.");
        m.put("2", "was blocked");
        RESOLUTION_MAPPING = Collections.unmodifiableMap(m);
    }

    private static final Pattern MULTI_VALUE_HEADER = Pattern.compile("(.*?)=(.*?);");

    private ClientBootstrap client = null;

    private FailoverStrategy<String> failoverStrategy;

    @Inject
    private StashManagerFactory stashManagerFactory;

    private IcapAntivirusScannerAssertion assertion;

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
            int connectTimeout = 30000;
            int readTimeout = 30000;
            if (ValidationUtils.isValidInteger(getContextVariable(context, assertion.getConnectionTimeout()), false, 1, 65535)) {
                connectTimeout = Integer.parseInt(assertion.getConnectionTimeout());
            }
            if (ValidationUtils.isValidInteger(getContextVariable(context, assertion.getReadTimeout()), false, 1, 65535)) {
                readTimeout = Integer.parseInt(assertion.getReadTimeout());
            }
            client.setOption("connectTimeoutMillis", connectTimeout);
            client.setOption("readTimeoutMillis", readTimeout);
        }
    }

    private String getContextVariable(final PolicyEnforcementContext context, final String conVar) {
        String retVal = conVar;
        if (retVal != null && retVal.length() > 0) {
            Map<String, Object> vars = context.getVariableMap(Syntax.getReferencedNames(conVar), getAudit());
            retVal = ExpandVariables.process(conVar, vars, getAudit());
        }
        return retVal;
    }

    @Override
    public AssertionStatus doCheckRequest(final PolicyEnforcementContext context, final Message message,
                                          final String messageDescription, final AuthenticationContext authContext)
            throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.NONE;

        Channel channel = null;
        String hostname = null;
        int port = 1344;
        AbstractIcapResponseHandler handler = null;
        String currentService = null;
        try {
            for (int i = 0; i < assertion.getIcapServers().size(); ++i) {
                String selectedService = failoverStrategy.selectService();
                if (selectedService == null) {
                    continue;
                }
                Matcher matcher = IcapAntivirusScannerAssertion.ICAP_URI.matcher(selectedService);
                if (matcher.matches()) {
                    hostname = getContextVariable(context, matcher.group(1).trim());
                    String portText = getContextVariable(context, matcher.group(2).trim());
                    if (ValidationUtils.isValidInteger(portText, false, 1, 65535)) {
                        port = Integer.parseInt(portText);
                    }
                    String serviceName = getContextVariable(context, matcher.group(3).trim());
                    currentService = String.format("icap://%s:%s/%s", hostname, port, serviceName);
                } else {
                    logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Invalid ICAP URI: " + selectedService);
                    failoverStrategy.reportFailure(selectedService);
                    continue;
                }
                intializeClient(context);
                ChannelFuture future = client.connect(new InetSocketAddress(hostname, port));

                //open the channel to see if it's valid
                channel = future.awaitUninterruptibly().getChannel();

                if (!future.isSuccess()) {
                    logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Unable to connect to the specified server: "
                            + selectedService);
                    failoverStrategy.reportFailure(selectedService);
                } else {
                    //here we don't need to attach the service params.
                    handler = channel.getPipeline().get(IcapResponseHandler.class);
                    IcapResponse response = handler.sendOptionsCommand(currentService, hostname);
                    if (response.getStatus() == IcapResponseStatus.OK) {
                        failoverStrategy.reportSuccess(selectedService);
                        break;
                    } else {
                        logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Unable to connect to the specified ICAP service: "
                                + currentService);
                        failoverStrategy.reportFailure(selectedService);
                        handler = null;
                    }
                }
            }
            if (handler != null) {
                String fullUri = currentService + getServiceQueryString(context);
                status = scanMessage(handler, fullUri, hostname, message);
            } else {
                logAndAudit(AssertionMessages.USERDETAIL_WARNING, "No valid ICAP server entries found");
                status = AssertionStatus.FAILED;
            }
        } finally {
            if (channel != null) {
                channel.close();
            }
        }
        return status;
    }

    //making this package default so that it can be tested in the test class
    AssertionStatus scanMessage(final AbstractIcapResponseHandler handler, final String icapUri, final String hostname, final Message message) {
        AssertionStatus status = AssertionStatus.NONE;
        try {
            for (PartIterator pi = message.getMimeKnob().getParts(); pi.hasNext(); ) {
                status = scan(handler, icapUri, hostname, pi.next(), 0);
                if (status != AssertionStatus.NONE) {
                    break;
                }
            }
        } catch (IOException e) {
            logAndAudit(AssertionMessages.USERDETAIL_WARNING, "I/O error occurred while scanning message "
                    + assertion.getTargetName());
            status = AssertionStatus.FAILED;
        }
        return status;
    }

    private AssertionStatus scan(AbstractIcapResponseHandler handler, String icapUri, String hostname, PartInfo partInfo, int currentDepth) {
        try {
            if (currentDepth != assertion.getMaxMimeDepth() && partInfo.getContentType().isMultipart()) {
                try {
                    MimeBody mimeBody = new MimeBody(stashManagerFactory.createStashManager(),
                            partInfo.getContentType(), partInfo.getInputStream(false), 0);
                    for (PartIterator pit = mimeBody.iterator(); pit.hasNext(); ) {
                        PartInfo pi = pit.next();
                        //recursively traverse all the multiparts
                        //break when it failed
                        AssertionStatus status = scan(handler, icapUri, hostname, pi, currentDepth++);
                        if (status == AssertionStatus.FAILED) {
                            return status;
                        }
                    }
                } catch (IOException e) {
                    logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Error reading MIME content from " + assertion.getTargetName());
                    return AssertionStatus.FAILED;
                }
            } else {
                try {
                    IcapResponse response = handler.scan(icapUri, hostname, partInfo);
                    if (response.getStatus() == IcapResponseStatus.OK) {
                        String partName = partInfo.getContentId(true);
                        Map<String, String> infectionInfo = parseMultivalueHeader(response.getHeader("X-Infection-Found"));
                        String service = response.getHeader("Service");
                        logAndAudit(AssertionMessages.ICAP_RESPONSE_WARNING, service, infectionInfo.get("Threat"),
                                partName, RESOLUTION_MAPPING.get(infectionInfo.get("Resolution")));
                        if (!assertion.isContinueOnVirusFound()) {
                            return AssertionStatus.FAILED;
                        }
                    }
                } catch (IOException e) {
                    logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Error occurred while scanning content " + assertion.getTargetName());
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

    private Map<String, String> parseMultivalueHeader(String header) {
        Map<String, String> ret = new HashMap<String, String>();
        if (header != null && !header.trim().isEmpty()) {
            Matcher matcher = MULTI_VALUE_HEADER.matcher(header);
            while (matcher.find()) {
                ret.put(matcher.group(1).trim(), matcher.group(2).trim());
            }
        }
        return ret;
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
}

