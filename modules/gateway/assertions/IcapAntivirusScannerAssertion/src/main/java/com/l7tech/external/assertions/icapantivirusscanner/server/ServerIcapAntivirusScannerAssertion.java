package com.l7tech.external.assertions.icapantivirusscanner.server;

import com.l7tech.common.io.failover.AbstractFailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion;
import com.l7tech.external.assertions.icapantivirusscanner.IcapConnectionDetail;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Server side implementation of the IcapAntivirusScannerAssertion.
 *
 * @see com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion
 */
public class ServerIcapAntivirusScannerAssertion extends AbstractMessageTargetableServerAssertion<IcapAntivirusScannerAssertion> {
    private FailoverStrategy<IcapConnectionDetail> failoverStrategy;

    public ServerIcapAntivirusScannerAssertion(final IcapAntivirusScannerAssertion assertion,
                                               final ApplicationContext context) throws PolicyAssertionException {
        super(assertion, assertion);
        failoverStrategy = AbstractFailoverStrategy.makeSynchronized(FailoverStrategyFactory.createFailoverStrategy(
                assertion.getFailoverStrategy(),
                assertion.getConnectionDetails().toArray(
                        new IcapConnectionDetail[assertion.getConnectionDetails().size()])));
    }

    @Override
    public AssertionStatus doCheckRequest(final PolicyEnforcementContext context, final Message message,
                                          final String messageDescription, final AuthenticationContext authContext)
            throws IOException, PolicyAssertionException {
        IcapAntivirusScanner scanner = getScanner();
        AssertionStatus status = AssertionStatus.NONE;
        if (scanner == null) {
            status = AssertionStatus.SERVICE_UNAVAILABLE;
            logAndAudit(AssertionMessages.USERDETAIL_WARNING,
                    "No useable connections, please ensure server entries are correct and valid.");
        } else {
            try {
                for (PartIterator pi = message.getMimeKnob().getParts(); pi.hasNext(); ) {
                    PartInfo partInfo = pi.next();
                    if (partInfo != null) {
                        status = scanPart(scanner, partInfo);
                    }
                }
            } finally {
                scanner.disconnect();
            }
        }
        return status;
    }

    private AssertionStatus scanPart(final IcapAntivirusScanner scanner, PartInfo partInfo) {
        AssertionStatus status = AssertionStatus.NONE;
        InputStream contentStream = null;
        try {
            contentStream = partInfo.getInputStream(false);
            final byte[] payload = IOUtils.slurpStream(contentStream);

            IcapAntivirusScanner.IcapResponse response = scanner.scan(partInfo.getContentId(true),
                    createHeader(partInfo), payload);
            if ("200".equals(response.getIcapHeader(IcapAntivirusScanner.IcapResponse.STATUS_CODE))) {
                String headerInfo = getResponseHeaderInfo(response);
                logAndAudit(AssertionMessages.USERDETAIL_WARNING, headerInfo);
                if (assertion.isFailOnVirusFound()) {
                    status = AssertionStatus.FAILED;
                }
            } else if (!"204".equals(response.getIcapHeader(IcapAntivirusScanner.IcapResponse.STATUS_CODE))) {
                logAndAudit(AssertionMessages.USERDETAIL_WARNING,
                        "Error returned from server: " +
                                response.getIcapHeader(IcapAntivirusScanner.IcapResponse.STATUS_CODE) + ":" +
                                response.getIcapHeader(IcapAntivirusScanner.IcapResponse.STATUS_TEXT));
                status = AssertionStatus.FAILED;
            }
        } catch (NoSuchPartException e) {
            //ignore
        } catch (IOException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[]{"Error attempting to scan content: " + ExceptionUtils.getMessage(e)},
                    ExceptionUtils.getDebugException(e));
            status = AssertionStatus.FAILED;
        } finally {
            if (contentStream != null) {
                try {
                    contentStream.close();
                } catch (IOException e) {
                    logger.warning("Error closing content stream: " + e.getMessage());
                }
            }
        }
        return status;
    }

    private Map<String, String> createHeader(final PartInfo partInfo) {
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", partInfo.getContentType().getType() + "/" +
                partInfo.getContentType().getSubtype() + "; charset=" + partInfo.getContentType().getEncoding());
        return headers;
    }

    private String getResponseHeaderInfo(final IcapAntivirusScanner.IcapResponse response) {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> ent : response.getIcapHeaders().entrySet()) {
            sb.append(ent.getKey()).append(" = ").append(ent.getValue()).append("\r\n");
        }
        return sb.toString();
    }

    private IcapAntivirusScanner getScanner() {
        IcapAntivirusScanner scanner = null;
        for (int i = 0; i < assertion.getConnectionDetails().size(); ++i) {
            IcapConnectionDetail connectionDetail = failoverStrategy.selectService();
            scanner = new IcapAntivirusScanner(connectionDetail);

            if (scanner.testConnection()) {
                failoverStrategy.reportSuccess(connectionDetail);
                break;
            }
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Error connecting to '" +
                    connectionDetail + "'"});
            failoverStrategy.reportFailure(connectionDetail);
            scanner = null;
        }
        return scanner;
    }
}
