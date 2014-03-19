package com.l7tech.external.assertions.ftpcredential.server;

import com.l7tech.common.TestDocuments;
import com.l7tech.external.assertions.ftpcredential.FtpCredentialAssertion;
import com.l7tech.message.FtpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.junit.Test;

import java.net.PasswordAuthentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test the ServerFtpCredentialAssertion.
 */
public class ServerFtpCredentialAssertionTest {

    private PolicyEnforcementContext makeContext(boolean addFtpKnob, final boolean withCreds) throws Exception {
        // create messages
        Message request = new Message(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT));
        Message response = new Message();

        // add knob
        if (addFtpKnob) {
            request.attachFtpRequestKnob(new FtpRequestKnob() {
                @Override
                public PasswordAuthentication getCredentials() {
                    PasswordAuthentication passwordAuthentication = null;

                    if (withCreds) {
                        passwordAuthentication = new PasswordAuthentication("user", "password".toCharArray());
                    }

                    return passwordAuthentication;
                }

                @Override
                public String getArgument() {
                    return null;
                }

                @Override
                public String getCommand() {
                    return null;
                }

                @Override
                public String getPath() {
                    return null;
                }

                @Override
                public String getRequestUri() {
                    return "";
                }

                @Override
                public String getRequestUrl() {
                    return null;
                }

                @Override
                public boolean isSecure() {
                    return false;
                }

                @Override
                public boolean isUnique() {
                    return false;
                }

                @Override
                public int getLocalPort() {
                    return 0;
                }

                @Override
                public int getLocalListenerPort() {
                    return 0;
                }

                @Override
                public String getRemoteAddress() {
                    return null;
                }

                @Override
                public String getRemoteHost() {
                    return null;
                }

                @Override
                public int getRemotePort() {
                    return 0;
                }

                @Override
                public String getLocalAddress() {
                    return null;
                }

                @Override
                public String getLocalHost() {
                    return null;
                }
            });
        }

        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private ServerAssertion makePolicy(FtpCredentialAssertion fca) throws Exception {
        return new ServerFtpCredentialAssertion(fca);
    }

    @Test
    public void testNotFtpRequest() throws Exception {
        FtpCredentialAssertion fca = new FtpCredentialAssertion();
        ServerAssertion ass = makePolicy(fca);

        PolicyEnforcementContext pec = makeContext(false, false);
        AssertionStatus result = ass.checkRequest(pec);

        assertEquals("Incorrect assertion status", AssertionStatus.NOT_APPLICABLE, result);
    }

    @Test
    public void testCredentials() throws Exception {
        FtpCredentialAssertion fca = new FtpCredentialAssertion();
        ServerAssertion ass = makePolicy(fca);

        PolicyEnforcementContext pec = makeContext(true, true);
        AssertionStatus result = ass.checkRequest(pec);

        assertEquals("Incorrect assertion status", AssertionStatus.NONE, result);
        assertNotNull("Credentials missing", pec.getDefaultAuthenticationContext().getLastCredentials());
        assertEquals("Incorrect login found", "user", pec.getDefaultAuthenticationContext().getLastCredentials().getLogin());
    }

    @Test
    public void testMissingCredentials() throws Exception {
        FtpCredentialAssertion fca = new FtpCredentialAssertion();
        ServerAssertion ass = makePolicy(fca);

        PolicyEnforcementContext pec = makeContext(true, false);
        AssertionStatus result = ass.checkRequest(pec);

        assertEquals("Incorrect assertion status", AssertionStatus.AUTH_REQUIRED, result);
    }

}
