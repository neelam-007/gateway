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
import com.l7tech.external.assertions.icapantivirusscanner.IcapConnectionDetail;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;
import org.springframework.context.ApplicationContext;

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
 * @see com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion
 * @author Ken Diep
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

    private FailoverStrategy<IcapConnectionDetail> failoverStrategy;

    private IcapConnectionDetail currentConnection = null;


    public ServerIcapAntivirusScannerAssertion(final IcapAntivirusScannerAssertion assertion,
                                               final ApplicationContext context) throws PolicyAssertionException {
        super(assertion, assertion);
        failoverStrategy = AbstractFailoverStrategy.makeSynchronized(FailoverStrategyFactory.createFailoverStrategy(
                assertion.getFailoverStrategy(),
                assertion.getConnectionDetails().toArray(
                        new IcapConnectionDetail[assertion.getConnectionDetails().size()])));

    }

    private IcapResponseHandler getHandler(ClientBootstrap client, Channel channel) {
        IcapResponseHandler handler = null;
        for (int i = 0; i < assertion.getConnectionDetails().size(); ++i) {
            IcapConnectionDetail connectionDetail = failoverStrategy.selectService();
            currentConnection = connectionDetail;
            client = new ClientBootstrap(new OioClientSocketChannelFactory(Executors.newCachedThreadPool()));
            client.setPipelineFactory(new IcapClientChannelPipeline());
            client.setOption("connectTimeoutMillis", connectionDetail.getTimeoutMilli());
            ChannelFuture future = client.connect(new InetSocketAddress(connectionDetail.getHostname(), connectionDetail.getPort()));
            //open the channel to see if it's valid
            channel = future.awaitUninterruptibly().getChannel();
            if (!future.isSuccess()) {
                logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Unable to connect to the specified server: "
                        + connectionDetail);
                failoverStrategy.reportFailure(connectionDetail);
                client.releaseExternalResources();
            } else {
                handler = channel.getPipeline().get(IcapResponseHandler.class);
                IcapResponse response = handler.sendOptionsCommand(connectionDetail);
                if (response.getStatus() == IcapResponseStatus.OK) {
                    failoverStrategy.reportSuccess(connectionDetail);
                    break;
                } else {
                    logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Unable to connect to the specified ICAP service: "
                            + connectionDetail.getServiceName());
                    failoverStrategy.reportFailure(connectionDetail);
                    client.releaseExternalResources();
                    handler = null;
                }
            }
        }
        return handler;
    }

    @Override
    public AssertionStatus doCheckRequest(final PolicyEnforcementContext context, final Message message,
                                          final String messageDescription, final AuthenticationContext authContext)
            throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.NONE;
        ClientBootstrap client = null;
        Channel channel = null;
        IcapResponseHandler handler = null;
        try {
            handler = getHandler(client, channel);
            if (handler != null) {
                for (PartIterator pi = message.getMimeKnob().getParts(); pi.hasNext(); ) {
                    status = scan(handler, pi.next());
                    if (status != AssertionStatus.NONE) {
                        break;
                    }
                }
            } else {
                logAndAudit(AssertionMessages.USERDETAIL_WARNING, "No valid ICAP server entries found");
                status = AssertionStatus.FAILED;
            }
        } finally {
            if (channel != null) {
                channel.close();
            }
            if (client != null) {
                client.releaseExternalResources();
            }
        }
        return status;
    }

    private AssertionStatus scan(IcapResponseHandler handler, PartInfo partInfo) throws IOException {
        try {
            if (partInfo.getContentType().isMultipart()) {
                byte[] content = IOUtils.slurpStream(partInfo.getInputStream(false));
                MimeBody mimeBody = new MimeBody(content, partInfo.getContentType(), content.length);
                for (PartIterator pit = mimeBody.iterator(); pit.hasNext(); ) {
                    PartInfo pi = pit.next();
                    //recursively traverse all the multiparts
                    //break when it failed
                    AssertionStatus status = scan(handler, pi);
                    if (status == AssertionStatus.FAILED) {
                        return status;
                    }
                }
            } else {
                IcapResponse response = handler.scan(currentConnection, partInfo);
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
            }
        } catch (NoSuchPartException e) {
            logAndAudit(AssertionMessages.NO_SUCH_PART, new String[]{assertion.getTargetName(), "1"},
                    ExceptionUtils.getDebugException(e));
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


}
