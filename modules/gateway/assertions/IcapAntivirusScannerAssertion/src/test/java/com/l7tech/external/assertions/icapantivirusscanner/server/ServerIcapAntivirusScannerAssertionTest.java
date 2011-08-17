package com.l7tech.external.assertions.icapantivirusscanner.server;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion;
import com.l7tech.external.assertions.icapantivirusscanner.IcapConnectionDetail;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.util.Collections;

/**
 *
 */
public class ServerIcapAntivirusScannerAssertionTest {

    private static final byte[] EICAR_PAYLOAD = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*".getBytes();
    private static final byte[] CLEAN_PAYLOAD = "ServerIcapAntivirusScannerAssertionTest".getBytes();

    private ApplicationContext appCtx;

    private IcapConnectionDetail connectionDetail;

    private PolicyEnforcementContext getContext() {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    @Before
    public void setUp() {
        // Get the spring app context
        if (appCtx == null) {
            appCtx = ApplicationContexts.getTestApplicationContext();
            Assert.assertNotNull("Fail - Unable to get applicationContext instance", appCtx);
        }
        connectionDetail = new IcapConnectionDetail();
        connectionDetail.setHostname("sophosav-1");
        connectionDetail.setPort(1344);
        connectionDetail.setServiceName("avscan");
    }

    @Test
    public void testCleanMessage() {
        IcapAntivirusScannerAssertion assertion = new IcapAntivirusScannerAssertion();
        assertion.setConnectionDetails(Collections.singletonList(connectionDetail));
        assertion.setContinueOnVirusFound(true);
        assertion.setFailoverStrategy("ordered");
        try {
            ByteArrayStashManager basm = new ByteArrayStashManager();
            basm.stash(0, CLEAN_PAYLOAD);
            Message message = new Message(basm, ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream(CLEAN_PAYLOAD));

            ServerIcapAntivirusScannerAssertion serverAssertion = new ServerIcapAntivirusScannerAssertion(assertion, appCtx);
            AssertionStatus status = serverAssertion.doCheckRequest(getContext(), message, "testCleanMessage", getContext().getAuthenticationContext(message));
            Assert.assertEquals("testCleanMessage()", AssertionStatus.NONE, status);

        } catch (Exception e) {
            Assert.fail("testCleanMessage failed: " + e.getMessage());
        }
    }

    @Test
    public void testInfectedMessageWithContinue() {

        IcapAntivirusScannerAssertion assertion = new IcapAntivirusScannerAssertion();
        assertion.setConnectionDetails(Collections.singletonList(connectionDetail));
        assertion.setContinueOnVirusFound(true);
        assertion.setFailoverStrategy("ordered");
        try {
            ByteArrayStashManager basm = new ByteArrayStashManager();
            basm.stash(0, EICAR_PAYLOAD);
            Message message = new Message(basm, ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream(EICAR_PAYLOAD));

            ServerIcapAntivirusScannerAssertion serverAssertion = new ServerIcapAntivirusScannerAssertion(assertion, appCtx);
            AssertionStatus status = serverAssertion.doCheckRequest(getContext(), message, "testInfectedMessageWithContinue", getContext().getAuthenticationContext(message));
            Assert.assertEquals("testInfectedMessageWithContinue()", AssertionStatus.NONE, status);

        } catch (Exception e) {
            Assert.fail("testInfectedMessageWithContinue failed: " + e.getMessage());
        }
    }

    @Test
    public void testInfectedMessageWithoutContinue() {

        IcapAntivirusScannerAssertion assertion = new IcapAntivirusScannerAssertion();
        assertion.setConnectionDetails(Collections.singletonList(connectionDetail));
        assertion.setContinueOnVirusFound(false);
        assertion.setFailoverStrategy("ordered");
        try {
            ByteArrayStashManager basm = new ByteArrayStashManager();

            basm.stash(0, EICAR_PAYLOAD);
            Message message = new Message(basm, ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream(EICAR_PAYLOAD));

            ServerIcapAntivirusScannerAssertion serverAssertion = new ServerIcapAntivirusScannerAssertion(assertion, appCtx);
            AssertionStatus status = serverAssertion.doCheckRequest(getContext(), message, "testInfectedMessageWithoutContinue", getContext().getAuthenticationContext(message));
            Assert.assertEquals("testInfectedMessageWithoutContinue()", AssertionStatus.FAILED, status);

        } catch (Exception e) {
            Assert.fail("testInfectedMessageWithoutContinue failed: " + e.getMessage());
        }
    }
}
