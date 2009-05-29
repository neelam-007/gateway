package com.l7tech.external.assertions.ftpcredential.server;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.message.Message;
import com.l7tech.message.FtpRequestKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.external.assertions.ftpcredential.FtpCredentialAssertion;
import com.l7tech.common.TestDocuments;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.extensions.TestSetup;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.PasswordAuthentication;

/**
 * Test the ServerFtpCredentialAssertion.
 */
public class ServerFtpCredentialAssertionTest extends TestCase {

    private static ApplicationContext applicationContext;

    public ServerFtpCredentialAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite testSuite = new TestSuite(ServerFtpCredentialAssertionTest.class);
        return new TestSetup(testSuite) {
            protected void setUp() throws Exception {
                applicationContext = new ClassPathXmlApplicationContext(new String[]{
                        "com/l7tech/external/assertions/ftpcredential/server/ftpCredentialAssertionTestApplicationContext.xml"
                });
            }
        };
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private PolicyEnforcementContext makeContext(boolean addFtpKnob, final boolean withCreds) throws Exception {
        // create messages
        Message request = new Message(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT));
        Message response = new Message();

        // add knob
        if (addFtpKnob) {
            request.attachFtpKnob(new FtpRequestKnob(){
                public PasswordAuthentication getCredentials() {
                    PasswordAuthentication passwordAuthentication = null;

                    if (withCreds) {
                        passwordAuthentication = new PasswordAuthentication("user", "password".toCharArray());
                    }

                    return passwordAuthentication;
                }
                public String getFile() {
                    return null;
                }
                public String getPath() {
                    return null;
                }
                public String getRequestUri() {
                    return null;
                }
                public String getRequestUrl() {
                    return null;
                }
                public boolean isSecure() {
                    return false;
                }
                public boolean isUnique() {
                    return false;
                }
                public int getLocalPort() {
                    return 0;
                }
                public String getRemoteAddress() {
                    return null;
                }
                public String getRemoteHost() {
                    return null;
                }
            });
        }

        return new PolicyEnforcementContext(request, response);
    }

    private ServerAssertion makePolicy(FtpCredentialAssertion fca) throws Exception {
        return new ServerFtpCredentialAssertion(fca, applicationContext);
    }

    public void testNotFtpRequest() throws Exception {
        FtpCredentialAssertion fca = new FtpCredentialAssertion();
        ServerAssertion ass = makePolicy(fca);

        PolicyEnforcementContext pec = makeContext(false, false);
        AssertionStatus result = ass.checkRequest(pec);

        assertEquals("Incorrect assertion status", AssertionStatus.NOT_APPLICABLE, result);
    }

    public void testCredentials() throws Exception {
        FtpCredentialAssertion fca = new FtpCredentialAssertion();
        ServerAssertion ass = makePolicy(fca);

        PolicyEnforcementContext pec = makeContext(true, true);
        AssertionStatus result = ass.checkRequest(pec);

        assertEquals("Incorrect assertion status", AssertionStatus.NONE, result);
        assertNotNull("Credentials missing", pec.getDefaultAuthenticationContext().getLastCredentials());
        assertEquals("Incorrect login found", "user", pec.getDefaultAuthenticationContext().getLastCredentials().getLogin());
    }

    public void testMissingCredentials() throws Exception {
        FtpCredentialAssertion fca = new FtpCredentialAssertion();
        ServerAssertion ass = makePolicy(fca);

        PolicyEnforcementContext pec = makeContext(true, false);
        AssertionStatus result = ass.checkRequest(pec);

        assertEquals("Incorrect assertion status", AssertionStatus.AUTH_REQUIRED, result);
    }

}
