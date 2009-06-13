package com.l7tech.external.assertions.certificateattributes.server;

import com.l7tech.external.assertions.certificateattributes.CertificateAttributesAssertion;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.token.http.HttpClientCertToken;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.test.BugNumber;
import static org.junit.Assert.*;
import org.junit.*;

import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 *
 */
public class ServerCertificateAttributesAssertionTest {
    private static final Logger logger = Logger.getLogger(ServerCertificateAttributesAssertionTest.class.getName());

    @Test
    @BugNumber(6455)
    public void testMissingDnSubcomponents() throws Exception {
        X509Certificate cert = new TestCertificateGenerator().subject("cn=blah, o=foo, dc=deeceeone, dc=deeceetwo").generate();

        CertificateAttributesAssertion ass = new CertificateAttributesAssertion();
        ServerCertificateAttributesAssertion sass = new ServerCertificateAttributesAssertion(ass, null);

        PolicyEnforcementContext context = pec(cert);
        sass.checkRequest(context);

        Object subject = context.getVariable("certificate.subject");
        Object cn = context.getVariable("certificate.subject.dn.cn");
        Object o = context.getVariable("certificate.subject.dn.o");
        assertEquals("cn=blah, o=foo, dc=deeceeone, dc=deeceetwo", subject.toString().toLowerCase());
        assertEquals("blah", ((Object[])cn)[0]);
        assertEquals("foo", ((Object[])o)[0]);
        assertEquals("", expand(context, "${certificate.subject.dn.c}"));
        assertEquals("blah", expand(context, "${certificate.subject.dn.cn}"));
        assertEquals("deeceeone", expand(context, "${certificate.subject.dn.dc[0]}"));
        assertEquals("deeceetwo", expand(context, "${certificate.subject.dn.dc[1]}"));
        assertEquals("deeceeone**(ZERF)**deeceetwo", expand(context, "${certificate.subject.dn.dc|**(ZERF)**}"));
    }

    private String expand(PolicyEnforcementContext context, String str) {
        final LogOnlyAuditor audit = new LogOnlyAuditor(logger);
        return ExpandVariables.process(str, context.getVariableMap(Syntax.getReferencedNames(str), audit), audit);
    }

    private PolicyEnforcementContext pec(final X509Certificate cert) {
        Message req = new Message();
        Message resp = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(req, resp);
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(new HttpClientCertToken(cert), SslAssertion.class);
        context.getDefaultAuthenticationContext().addCredentials(creds);
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new UserBean(), creds.getSecurityToken(), cert, false));
        return context;
    }

}
