package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.message.Message;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.xmlsec.ServerSamlBrowserArtifact;
import com.l7tech.security.token.http.HttpBasicToken;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;

/**
 * Test Assertion/CompositeAssertion data structure management.
 */
public class SamlBrowserArtifactTest extends TestCase {

    public SamlBrowserArtifactTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SamlBrowserArtifactTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testStuff() throws Exception {
        SamlBrowserArtifact sba = new SamlBrowserArtifact();
            sba.setSsoEndpointUrl("http://192.168.1.80/FIM/sps/l7fed/saml/login?TARGET=https://redroom.l7tech.com/FIM");
//        HashMap extraFields = new HashMap();
//        extraFields.put("login-form-type", "pwd");
//        sba.setExtraFields(extraFields);
//        sba.setUsernameFieldname("username");
//        sba.setPasswordFieldname("password");
//        sba.setMethod(GenericHttpClient.METHOD_GET);

        ApplicationContext spring = null/*ApplicationContexts.getTestApplicationContext()*/;
        ServerSamlBrowserArtifact ssba = new ServerSamlBrowserArtifact(sba, spring);

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        pec.getDefaultAuthenticationContext().addCredentials(LoginCredentials.makeLoginCredentials(new HttpBasicToken("testuser", "passw0rd".toCharArray()), HttpBasic.class));
        ssba.checkRequest(pec);
    }

}
