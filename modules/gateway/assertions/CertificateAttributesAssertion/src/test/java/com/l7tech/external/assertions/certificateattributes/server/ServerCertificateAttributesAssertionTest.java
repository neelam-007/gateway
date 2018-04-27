package com.l7tech.external.assertions.certificateattributes.server;

import com.l7tech.common.io.X509GeneralName;
import com.l7tech.external.assertions.certificateattributes.CertificateAttributesAssertion;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.token.http.TlsClientCertToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import static org.junit.Assert.*;

import com.l7tech.util.HexUtils;
import org.junit.*;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 */
public class ServerCertificateAttributesAssertionTest {
    private static final Logger logger = Logger.getLogger(ServerCertificateAttributesAssertionTest.class.getName());

    @Test
    @BugNumber(6455)
    public void testMissingDnSubcomponents() throws Exception {
        X509Certificate cert = new TestCertificateGenerator()
                .subject("cn=blah, o=foo, dc=deeceeone, dc=deeceetwo, L=vancouver, ST=bc, ou=marketing, C=canada, STREET=123 mystreet")
                .subjectAlternativeNames(true, new X509GeneralName(X509GeneralName.Type.dNSName, "test.ca.com"))
                .generate();

        CertificateAttributesAssertion ass = new CertificateAttributesAssertion();
        ServerCertificateAttributesAssertion sass = new ServerCertificateAttributesAssertion(ass);

        PolicyEnforcementContext context = pec(cert);
        sass.checkRequest(context);

        Object subject = context.getVariable("certificate.subject");
        Object cn = context.getVariable("certificate.subject.dn.cn");
        Object o = context.getVariable("certificate.subject.dn.o");
        assertEquals("cn=blah, o=foo, dc=deeceeone, dc=deeceetwo, l=vancouver, st=bc, ou=marketing, c=canada, street=123 mystreet", subject.toString().toLowerCase());
        assertEquals("blah", ((Object[])cn)[0]);
        assertEquals("foo", ((Object[])o)[0]);
        assertEquals("", expand(context, "${certificate.subject.dn.emailaddress}"));
        assertEquals("blah", expand(context, "${certificate.subject.dn.cn}"));
        assertEquals("deeceeone", expand(context, "${certificate.subject.dn.dc[0]}"));
        assertEquals("deeceetwo", expand(context, "${certificate.subject.dn.dc[1]}"));
        assertEquals("deeceeone**(ZERF)**deeceetwo", expand(context, "${certificate.subject.dn.dc|**(ZERF)**}"));
        assertEquals("bc", expand(context, "${certificate.subject.dn.st}"));
        assertEquals("marketing", expand(context, "${certificate.subject.dn.ou}"));
        assertEquals("canada", expand(context, "${certificate.subject.dn.c}"));
        assertEquals("123 mystreet", expand(context, "${certificate.subject.dn.street}"));
        assertEquals( HexUtils.encodeBase64( cert.getPublicKey().getEncoded(), true ),
                expand(context, "${certificate.subjectPublicKey}"));
    }

    @Test
    @BugNumber(8016)
    public void testBackwardCompatWithOldSubattributes() throws Exception {
        X509Certificate cert = new TestCertificateGenerator().subject("cn=blah, o=foo, dc=deeceeone, dc=deeceetwo, L=vancouver, ST=bc, ou=marketing, C=canada, STREET=123 mystreet").generate();

        CertificateAttributesAssertion ass = new CertificateAttributesAssertion();
        ServerCertificateAttributesAssertion sass = new ServerCertificateAttributesAssertion(ass);

        PolicyEnforcementContext context = pec(cert);
        sass.checkRequest(context);

        assertEquals("", expand(context, "${certificate.subject.emailaddress}"));
        assertEquals("blah", expand(context, "${certificate.subject.cn}"));
        assertEquals("deeceeone", expand(context, "${certificate.subject.dc[0]}"));
        assertEquals("deeceetwo", expand(context, "${certificate.subject.dc[1]}"));
        assertEquals("deeceeone**(ZERF)**deeceetwo", expand(context, "${certificate.subject.dc|**(ZERF)**}"));
        assertEquals("bc", expand(context, "${certificate.subject.st}"));
        assertEquals("marketing", expand(context, "${certificate.subject.ou}"));
        assertEquals("canada", expand(context, "${certificate.subject.c}"));
        assertEquals("123 mystreet", expand(context, "${certificate.subject.street}"));

    }

    @Test
    public void testSubjectAlternativeName() throws Exception {
        X509Certificate cert = new TestCertificateGenerator()
                .subject("cn=test")
                .subjectAlternativeNames(true,
                        new X509GeneralName(X509GeneralName.Type.dNSName, "test.ca.com"),
                        new X509GeneralName(X509GeneralName.Type.dNSName, "test2.ca.com"),
                        new X509GeneralName(X509GeneralName.Type.directoryName, "cn=blah, o=foo, dc=deeceeone, dc=deeceetwo, L=vancouver, ST=bc, ou=marketing, C=canada, STREET=123 mystreet"),
                        new X509GeneralName(X509GeneralName.Type.iPAddress, "111.222.33.44"),
                        new X509GeneralName(X509GeneralName.Type.uniformResourceIdentifier, "https://test.ca.com?test=test&test2=test2"),
                        new X509GeneralName(X509GeneralName.Type.rfc822Name, "test@ca.com"))
                .generate();

        CertificateAttributesAssertion ass = new CertificateAttributesAssertion();
        ServerCertificateAttributesAssertion sass = new ServerCertificateAttributesAssertion(ass);

        PolicyEnforcementContext context = pec(cert);
        sass.checkRequest(context);

        Object san1 = context.getVariable("certificate.subjectAltNameDNS");
        Object san2 = context.getVariable("certificate.subjectAltNameDirName");
        String[] dn = Arrays.stream(san2.toString().toLowerCase().split(",")).sorted().collect(Collectors.toList()).toArray(new String[0]);
        Object san3 = context.getVariable("certificate.subjectAltNameIP");
        Object san4 = context.getVariable("certificate.subjectAltNameURI");
        Object san5 = context.getVariable("certificate.subjectAltNameEmail");

        assertEquals("test.ca.com", san1);
        assertArrayEquals(Arrays.stream("street=123 mystreet,c=canada,ou=marketing,st=bc,l=vancouver,dc=deeceetwo,dc=deeceeone,o=foo,cn=blah".split(",")).sorted().collect(Collectors.toList()).toArray(new String[0]), dn);
        assertEquals("111.222.33.44", san3);
        assertEquals("https://test.ca.com?test=test&test2=test2", san4);
        assertEquals("test@ca.com", san5);
    }

    @Test
    public void testSubjectAlternativeNames() throws Exception {
        X509Certificate cert = new TestCertificateGenerator()
                .subject("cn=test")
                .subjectAlternativeNames(true,
                        new X509GeneralName(X509GeneralName.Type.dNSName, "test.ca.com"),
                        new X509GeneralName(X509GeneralName.Type.dNSName, "test2.ca.com"),
                        new X509GeneralName(X509GeneralName.Type.directoryName, "cn=blah, o=foo, dc=deeceeone, dc=deeceetwo, L=vancouver, ST=bc, ou=marketing, C=canada, STREET=123 mystreet"),
                        new X509GeneralName(X509GeneralName.Type.iPAddress, "111.222.33.44"),
                        new X509GeneralName(X509GeneralName.Type.uniformResourceIdentifier, "https://test.ca.com?test=test&test2=test2"),
                        new X509GeneralName(X509GeneralName.Type.rfc822Name, "test@ca.com"))
                .generate();

        CertificateAttributesAssertion ass = new CertificateAttributesAssertion();
        ServerCertificateAttributesAssertion sass = new ServerCertificateAttributesAssertion(ass);

        PolicyEnforcementContext context = pec(cert);
        sass.checkRequest(context);

        Object[] san1 = (Object[])context.getVariable("certificate.subjectAltNameDNSs");
        Object[] san2 = (Object[]) context.getVariable("certificate.subjectAltNameDirNames");
        String[] dn = Arrays.stream(san2[0].toString().toLowerCase().split(",")).sorted().collect(Collectors.toList()).toArray(new String[0]);
        Object[] san3 = (Object[])context.getVariable("certificate.subjectAltNameIPs");
        Object[] san4 = (Object[])context.getVariable("certificate.subjectAltNameURIs");
        Object[] san5 = (Object[])context.getVariable("certificate.subjectAltNameEmails");

        assertEquals("test.ca.com", san1[0]);
        assertEquals("test2.ca.com", san1[1]);
        assertArrayEquals(Arrays.stream("street=123 mystreet,c=canada,ou=marketing,st=bc,l=vancouver,dc=deeceetwo,dc=deeceeone,o=foo,cn=blah".split(",")).sorted().collect(Collectors.toList()).toArray(new String[0]), dn);
        assertEquals("111.222.33.44", san3[0]);
        assertEquals("https://test.ca.com?test=test&test2=test2", san4[0]);
        assertEquals("test@ca.com", san5[0]);
    }

    @BugId("DE351077")
    @Test
    public void testSubjectAlternativeNames_DN() throws Exception {
        X509Certificate cert = new TestCertificateGenerator()
                .subject("cn=test")
                .subjectAlternativeNames(true, new X509GeneralName(X509GeneralName.Type.directoryName, "2.5.4.46=#1309736f6d657468696e67"))
                .generate();

        CertificateAttributesAssertion ass = new CertificateAttributesAssertion();
        ServerCertificateAttributesAssertion sass = new ServerCertificateAttributesAssertion(ass);

        PolicyEnforcementContext context = pec(cert);
        sass.checkRequest(context);
        Object[] actual_multi = (Object[]) context.getVariable("certificate.subjectAltNameDirNames");
        assertEquals("2.5.4.46=something", actual_multi[0]);
        Object  actual_single = context.getVariable("certificate.subjectAltNameDirName");
        assertEquals("2.5.4.46=something", actual_single);
    }

    @BugId("DE351030")
    @Test
    public void testMultipleSubjetAlternativeNamesOfTheSameType() throws Exception {
        String[] expected = {"test-1.ca.com","test-2.ca.com","test-3.ca.com"};
        X509Certificate cert = new TestCertificateGenerator()
                .subject("cn=test")
                .subjectAlternativeNames(true,
                        new X509GeneralName(X509GeneralName.Type.dNSName, expected[0]),
                        new X509GeneralName(X509GeneralName.Type.dNSName, expected[1]),
                        new X509GeneralName(X509GeneralName.Type.dNSName, expected[2]))
                .generate();

        CertificateAttributesAssertion ass = new CertificateAttributesAssertion();
        ServerCertificateAttributesAssertion sass = new ServerCertificateAttributesAssertion(ass);

        PolicyEnforcementContext context = pec(cert);
        sass.checkRequest(context);

        Object[] san = (Object[])context.getVariable("certificate.subjectAltNameDNSs");

        assertArrayEquals(expected, Arrays.copyOf(san, san.length, String[].class));
    }

    private String expand(PolicyEnforcementContext context, String str) {
        final LoggingAudit audit = new LoggingAudit(logger);
        return ExpandVariables.process(str, context.getVariableMap(Syntax.getReferencedNames(str), audit), audit);
    }

    private PolicyEnforcementContext pec(final X509Certificate cert) {
        Message req = new Message();
        Message resp = new Message();
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, resp);
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(new TlsClientCertToken(cert), SslAssertion.class);
        context.getDefaultAuthenticationContext().addCredentials(creds);
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new UserBean(), creds.getSecurityTokens(), cert, false));
        return context;
    }

}
