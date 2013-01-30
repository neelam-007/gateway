package com.l7tech.external.assertions.ssh.server;

import com.l7tech.common.TestDocuments;
import com.l7tech.external.assertions.ssh.SshCredentialAssertion;
import com.l7tech.message.SshKnob;
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
 * Test the ServerSshCredentialAssertion.
 */
public class ServerSshCredentialAssertionTest {

    private PolicyEnforcementContext makeContext(boolean addSshKnob, final boolean withPasswordCreds, final boolean withPublicKeyCreds) throws Exception {
        // create messages
        Message request = new Message(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT));
        Message response = new Message();

        // add knob
        if (addSshKnob) {
            request.attachKnob(SshKnob.class, new SshKnob() {

                @Override
                public String getFile() {
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
                public PasswordAuthentication getPasswordAuthentication() {
                    PasswordAuthentication passwordAuthentication = null;

                    if (withPasswordCreds) {
                        passwordAuthentication = new PasswordAuthentication("user", "password".toCharArray());
                    }

                    return passwordAuthentication;
                }

                @Override
                public PublicKeyAuthentication getPublicKeyAuthentication() {
                    PublicKeyAuthentication publicKeyAuthentication = null;
                    if (withPublicKeyCreds) {
                        publicKeyAuthentication = new PublicKeyAuthentication("user",
                                "-----BEGIN PUBLIC KEY-----\n" +
                                "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAIW6mPdh9KfTBBS0dsNQ6gdNh2W1nBlV\n" +
                                "NY/dYBknJGsD6+U6We2AGAmgT9TpQ4vaqbqF/+VgtMVJm6aztUuknLsCAwEAAQ==\n" +
                                "-----END PUBLIC KEY-----");
                    }
                    return publicKeyAuthentication;
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

                @Override
                public FileMetadata getFileMetadata() {
                    FileMetadata metadata = new FileMetadata(System.currentTimeMillis(), System.currentTimeMillis(), 644);
                    return metadata;
                }

                @Override
                public CommandType getCommandType() {
                    return CommandType.PUT;
                }

                @Override
                public String getParameter(String name) {
                    return null;
                }
            });
        }

        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private ServerAssertion makePolicy(SshCredentialAssertion fca) throws Exception {
        return new ServerSshCredentialAssertion(fca);
    }

    @Test
    public void testNotSshRequest() throws Exception {
        SshCredentialAssertion fca = new SshCredentialAssertion();
        fca.setPermitPasswordCredential(true);
        fca.setPermitPublicKeyCredential(true);
        ServerAssertion ass = makePolicy(fca);

        PolicyEnforcementContext pec = makeContext(false, false, false);
        AssertionStatus result = ass.checkRequest(pec);

        assertEquals("Incorrect assertion status", AssertionStatus.AUTH_REQUIRED, result);
    }

    @Test
    public void testCredentials() throws Exception {
        SshCredentialAssertion fca = new SshCredentialAssertion();
        fca.setPermitPasswordCredential(true);
        fca.setPermitPublicKeyCredential(true);
        ServerAssertion ass = makePolicy(fca);

        // use password credentials
        PolicyEnforcementContext pec = makeContext(true, true, false);
        AssertionStatus result = ass.checkRequest(pec);

        assertEquals("Incorrect assertion status", AssertionStatus.NONE, result);
        assertNotNull("Credentials missing", pec.getDefaultAuthenticationContext().getLastCredentials());
        assertEquals("Incorrect login found", "user", pec.getDefaultAuthenticationContext().getLastCredentials().getLogin());

        // use public key credentials
        pec = makeContext(true, false, true);
        result = ass.checkRequest(pec);

        assertEquals("Incorrect assertion status", AssertionStatus.NONE, result);
        assertNotNull("Credentials missing", pec.getDefaultAuthenticationContext().getLastCredentials());
        assertEquals("Incorrect login found", "user", pec.getDefaultAuthenticationContext().getLastCredentials().getLogin());
    }

    @Test
    public void testMissingCredentials() throws Exception {
        SshCredentialAssertion fca = new SshCredentialAssertion();
        fca.setPermitPasswordCredential(true);
        fca.setPermitPublicKeyCredential(true);
        ServerAssertion ass = makePolicy(fca);

        PolicyEnforcementContext pec = makeContext(true, false, false);
        AssertionStatus result = ass.checkRequest(pec);

        assertEquals("Incorrect assertion status", AssertionStatus.AUTH_REQUIRED, result);
    }

    @Test
    public void testNotPermittedCredentials() throws Exception {
        SshCredentialAssertion fca = new SshCredentialAssertion();

        // not permitted
        fca.setPermitPasswordCredential(false);
        fca.setPermitPublicKeyCredential(false);
        ServerAssertion ass = makePolicy(fca);

        // use password credentials
        PolicyEnforcementContext pec = makeContext(true, true, false);
        AssertionStatus result = ass.checkRequest(pec);
        assertEquals("Incorrect assertion status", AssertionStatus.AUTH_REQUIRED, result);

        // use public key credentials
        pec = makeContext(true, false, true);
        result = ass.checkRequest(pec);
        assertEquals("Incorrect assertion status", AssertionStatus.AUTH_REQUIRED, result);
    }
}
