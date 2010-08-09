package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.samlpassertion.ProcessSamlAuthnRequestAssertion;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.AuditContextStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.transport.http.ConnectionId;
import com.l7tech.server.util.SimpleSingletonBeanFactory;

import static org.junit.Assert.*;

import com.l7tech.util.Charsets;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import java.text.MessageFormat;
import java.util.HashMap;

/**
 * Tests for Process SAML AuthnRequest assertion
 */
public class ServerProcessSamlAuthnRequestAssertionTest {

    private static final String REQUEST_INVALID_SIG =
            "<samlp:AuthnRequest\n" +
            "    AssertionConsumerServiceURL=\"https://www.google.com/a/g.feide.no/acs\"\n" +
            "    Consent=\"consent\" Destination=\"destination\"\n" +
            "    ID=\"djikeehkdinmglljlaeianmgabajfnplkldoamkl\" IsPassive=\"false\"\n" +
            "    IssueInstant=\"2008-05-27T08:19:29Z\"\n" +
            "    ProtocolBinding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n" +
            "    ProviderName=\"google.com\" Version=\"2.0\" xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n" +
            "    <saml:Issuer Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:entity\" NameQualifier=\"nameQualifier\"\n" +
            "        SPNameQualifier=\"spNameQualifier\" SPProvidedID=\"spProvidedId\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">google.com</saml:Issuer>\n" +
            "    <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "        <ds:SignedInfo>\n" +
            "            <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
            "            <ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>\n" +
            "            <ds:Reference URI=\"#djikeehkdinmglljlaeianmgabajfnplkldoamkl\">\n" +
            "                <ds:Transforms>\n" +
            "                    <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
            "                </ds:Transforms>\n" +
            "                <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>\n" +
            "                <ds:DigestValue>3/x4TqOBK9R1d2w1ViB8CDsOwBA=</ds:DigestValue>\n" +
            "            </ds:Reference>\n" +
            "        </ds:SignedInfo>\n" +
            "        <ds:SignatureValue>lYmiNRX1PhwZMnTZg8sDvysb7MebkOVgUvymM2aLPLlnRL+0Q0A9u8lFo1hU9zPnfrAUZuzj9YOQRL69uWzkl8QLBjuY0A0mIz9o+unkcYM/yhG8ZK2OZcvONcmLpftRkAo2YRPE5a/BX29U5//sO2xc3Z86dw/5I1mX8AFlANQ=</ds:SignatureValue>\n" +
            "        <ds:KeyInfo>\n" +
            "            <ds:X509Data>\n" +
            "                <ds:X509Certificate>\n" +
            "MIIB3DCCAUWgAwIBAgIDGdGfMA0GCSqGSIb3DQEBBQUAMBExDzANBgNVBAMTBnNq\n" +
            "b25lczAeFw0wOTA1MTQyMjUwNTlaFw0xNDA1MTMyMjUwNTlaMBExDzANBgNVBAMT\n" +
            "BnNqb25lczCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA9pAY/Nxfwu/ddNsA\n" +
            "zl/PqlBmRp2XIeaLmtN24/w+JwS8l9yzSiCquU7HRU/Ru4cMQJJkZWVK0BpLPJy4\n" +
            "FkMq/OdLzwpoQCs+WnmgRQjFYLMWbz64BqkGzDNJqtcdpJ9Z9I62ghhMb9aycAMg\n" +
            "POBvVaf1X/bxS7YQnrG847ICEpUCAwEAAaNCMEAwHQYDVR0OBBYEFERtS4O46o8M\n" +
            "A/KmMmnGbOdMfRFpMB8GA1UdIwQYMBaAFERtS4O46o8MA/KmMmnGbOdMfRFpMA0G\n" +
            "CSqGSIb3DQEBBQUAA4GBAJLfpagKIV7FUy84YdxZed2zgQQ3aUaeNHZitHk1gnzm\n" +
            "9qBNVKQ1ixmPwDrl7jkfY7f1k6eeqXah5vxcAqNhgmkl60rqBrkyWNZKvd5zcduG\n" +
            "X1ll57iLyQo/apOGAW0ObyPA7MbhewVTr/LoLHpgb5nQ7R0WVxpYDOMf3hOiDRSa\n" +
            "                    </ds:X509Certificate>\n" +
            "            </ds:X509Data>\n" +
            "        </ds:KeyInfo>\n" +
            "    </ds:Signature>\n" +
            "    <saml:Subject xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "        <saml:NameID Format=\"subjectFormat\" NameQualifier=\"subjectNameQualifier\"\n" +
            "                  SPNameQualifier=\"subjectSPNameQualifier\" SPProvidedID=\"subjectSPProvidedId\">subject</saml:NameID>\n" +
            "    </saml:Subject>\n" +
            "    <samlp:NameIDPolicy AllowCreate=\"true\" Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\"/>\n" +
            "    {0}\n" +
            "</samlp:AuthnRequest>";

    private static final String REQUEST_SIGNED = 
            "<samlp:AuthnRequest AssertionConsumerServiceURL=\"https://www.google.com/a/g.feide.no/acs\" Consent=\"consent\" Destination=\"destination\" ID=\"djikeehkdinmglljlaeianmgabajfnplkldoamkl\" IsPassive=\"false\" IssueInstant=\"2008-05-27T08:19:29Z\" ProtocolBinding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" ProviderName=\"google.com\" Version=\"2.0\" xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n" +
                    "    <saml:Issuer Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:entity\" NameQualifier=\"nameQualifier\" SPNameQualifier=\"spNameQualifier\" SPProvidedID=\"spProvidedId\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">irishman.l7tech.com</saml:Issuer>\n" +
                    "    <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#djikeehkdinmglljlaeianmgabajfnplkldoamkl\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>TLTfnmrkb9dhaCs5gfbcw426gCs=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>frpwJJEv0glNxSXi6mfsT1C2J9YRqHIkXLCRDOgbgBNM3qaRllTlmSichfOgLMsfCTYbFbIsvXXcIQ74g2PLDI8MIleXfvxXfyfllMagw7Sbb22klvmVz3GPs3PgxIp9p2otRsqGUmyJee8vC9S5bVG9Y/dgKxcakb6hSvi3IT5mmoMYbSGa6Dn7SYYbfW4qifO4F+ukA1Zsbbk11GtRt9D05o8zw6jtXw730E4DCKl5wsikOd7zUrOAYVc80Icggi9g6JDwoDsqeQW60K59Duzw4LuWUiwoyqxxkscF9ISTeRNYIf5vDlkzfsYeAnOjZIeaqqhJi78FXjDsnzrexw==</ds:SignatureValue><ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIIDATCCAemgAwIBAgIJAPNZCY/tNKOHMA0GCSqGSIb3DQEBDAUAMB4xHDAaBgNVBAMTE2lyaXNobWFuLmw3dGVjaC5jb20wHhcNMTAwMzIyMTY0OTI1WhcNMjAwMzE5MTY0OTI1WjAeMRwwGgYDVQQDExNpcmlzaG1hbi5sN3RlY2guY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjfUq3KAsOUL7lAO/gbYVA8x22jgpSu5s9jNZfI0iksvga5rRDGYF/tAALAUj3eL+lG/9cIOtCL3kjPg//IJ+AgZYksCGDDttu1Llx0B0KVBzy0jrZvPtg24Ml3yqkXTylqwShEXRSib5jkQbrRkOuivrVEKzqzYCPZ+4lI8sNkdIAU+baARJh9Wv/LHOwV6JoyahdN34BV+WLWZL5XlmnUSHDW9qpMwbe/pkEjTrAEavZRQKL3XiPRWFxWP/Id47V51adOnUM9tX4MCpXbbCISb1ocYWiAr3YsTd3D8sTX3G7mmNJhslKqIhAtLXJJMO37FqNLkRO3DvIoqjBlFdGQIDAQABo0IwQDAdBgNVHQ4EFgQUIUo+uwBf6SpbPxgl5rj4QWYCX/UwHwYDVR0jBBgwFoAUIUo+uwBf6SpbPxgl5rj4QWYCX/UwDQYJKoZIhvcNAQEMBQADggEBAGQUrOqaSGpx9ZCpG3SYM8iynm6mv8oaqDZ7pBVEmJtuVzTQ/jMdMBjsHDtaIJK5gbFIS+M04wzbnJHa1TTLfjgIUpoMY9/pcZ65xJNS+OEz2E7xWVQ5ANMNUntkYOzJM7ZgFZrzctDOXddjAcS0/TyuzFaJtb+XvvH9OJ3BMEd8MIAZyZ4oHtEPfsidoi8hsE2yiqa1OPktgdl0hnUpjQ2bJhoO23HYrV4pcC3IEbgJ/hbyt+ng24IlkS08bgKywCBdnQp4H+M1EeEvGrFmj+RyyAaxyelwt5L1AtvTlFrirgiR77juht55rPF3HReW9ZZrFpEcSaJZsrrE0I6Qvc0=</ds:X509Certificate></ds:X509Data></ds:KeyInfo></ds:Signature>\n" +
                    "    <saml:Subject xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
                    "        <saml:NameID>subject</saml:NameID>\n" +
                    "    </saml:Subject>\n" +
                    "    <samlp:NameIDPolicy AllowCreate=\"true\" Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\"/>\n" +
                    "</samlp:AuthnRequest>";

    @Test
    public void testVariables() throws Exception {
        final ProcessSamlAuthnRequestAssertion processSamlAuthnRequestAssertion =
                new ProcessSamlAuthnRequestAssertion();
        processSamlAuthnRequestAssertion.setVerifySignature( false );

        final ServerProcessSamlAuthnRequestAssertion serverProcessSamlAuthnRequestAssertion =
                buildServerAssertion( processSamlAuthnRequestAssertion );

        final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(
                new Message( XmlUtil.parse(REQUEST_INVALID_SIG)),
                null );
        
        try {
            AssertionStatus status = serverProcessSamlAuthnRequestAssertion.checkRequest( pec );
            assertEquals( "success", AssertionStatus.NONE, status );
            assertEquals( "subject", "subject", pec.getVariable( "authnRequest.subject" ) );
            assertEquals( "subject.nameQualifier", "subjectNameQualifier", pec.getVariable( "authnRequest.subject.nameQualifier" ) );
            assertEquals( "subject.spNameQualifier", "subjectSPNameQualifier", pec.getVariable( "authnRequest.subject.spNameQualifier" ) );
            assertEquals( "subject.format", "subjectFormat", pec.getVariable( "authnRequest.subject.format" ) );
            assertEquals( "subject.spProvidedId", "subjectSPProvidedId", pec.getVariable( "authnRequest.subject.spProvidedId" ) );
            assertEquals( "x509CertBase64", "MIIB3DCCAUWgAwIBAgIDGdGfMA0GCSqGSIb3DQEBBQUAMBExDzANBgNVBAMTBnNqb25lczAeFw0wOTA1MTQyMjUwNTlaFw0xNDA1MTMyMjUwNTlaMBExDzANBgNVBAMTBnNqb25lczCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA9pAY/Nxfwu/ddNsAzl/PqlBmRp2XIeaLmtN24/w+JwS8l9yzSiCquU7HRU/Ru4cMQJJkZWVK0BpLPJy4FkMq/OdLzwpoQCs+WnmgRQjFYLMWbz64BqkGzDNJqtcdpJ9Z9I62ghhMb9aycAMgPOBvVaf1X/bxS7YQnrG847ICEpUCAwEAAaNCMEAwHQYDVR0OBBYEFERtS4O46o8MA/KmMmnGbOdMfRFpMB8GA1UdIwQYMBaAFERtS4O46o8MA/KmMmnGbOdMfRFpMA0GCSqGSIb3DQEBBQUAA4GBAJLfpagKIV7FUy84YdxZed2zgQQ3aUaeNHZitHk1gnzm9qBNVKQ1ixmPwDrl7jkfY7f1k6eeqXah5vxcAqNhgmkl60rqBrkyWNZKvd5zcduGX1ll57iLyQo/apOGAW0ObyPA7MbhewVTr/LoLHpgb5nQ7R0WVxpYDOMf3hOiDRSa",
                    pec.getVariable( "authnRequest.x509CertBase64" ) );
            assertEquals( "acsUrl", "https://www.google.com/a/g.feide.no/acs", pec.getVariable( "authnRequest.acsUrl" ) );
            assertEquals( "id", "djikeehkdinmglljlaeianmgabajfnplkldoamkl", pec.getVariable( "authnRequest.id" ) );
            assertEquals( "version", "2.0", pec.getVariable( "authnRequest.version" ) );
            assertEquals( "issueInstant", "2008-05-27T08:19:29.000Z", pec.getVariable( "authnRequest.issueInstant" ) );
            assertEquals( "destination", "destination", pec.getVariable( "authnRequest.destination" ) );
            assertEquals( "consent", "consent", pec.getVariable( "authnRequest.consent" ) );
            assertEquals( "issuer", "google.com", pec.getVariable( "authnRequest.issuer" ) );
            assertEquals( "issuer.nameQualifier", "nameQualifier", pec.getVariable( "authnRequest.issuer.nameQualifier" ) );
            assertEquals( "issuer.spNameQualifier", "spNameQualifier", pec.getVariable( "authnRequest.issuer.spNameQualifier" ) );
            assertEquals( "issuer.format", "urn:oasis:names:tc:SAML:2.0:nameid-format:entity", pec.getVariable( "authnRequest.issuer.format" ) );
            assertEquals( "issuer.spProvidedId", "spProvidedId", pec.getVariable( "authnRequest.issuer.spProvidedId" ) );
        } finally {
            pec.close();
        }
    }

    @Test
    public void testConditions() throws Exception {
        final ProcessSamlAuthnRequestAssertion processSamlAuthnRequestAssertion =
                new ProcessSamlAuthnRequestAssertion();
        processSamlAuthnRequestAssertion.setVerifySignature( false );

        final ServerProcessSamlAuthnRequestAssertion serverProcessSamlAuthnRequestAssertion =
                buildServerAssertion( processSamlAuthnRequestAssertion );

        // Fail not yet valid
        {
            final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(
                    new Message( XmlUtil.parse( MessageFormat.format( REQUEST_INVALID_SIG, "<saml:Conditions NotBefore=\"2080-05-27T08:19:29.000Z\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"/>" )) ),
                    null );

            AssertionStatus status;
            try {
                status = serverProcessSamlAuthnRequestAssertion.checkRequest( pec );
            } catch ( AssertionStatusException ase) {
                status = ase.getAssertionStatus();
            } finally {
                pec.close();
            }
            assertEquals( "falsified", AssertionStatus.FALSIFIED, status );
        }

        // Fail expired
        {
            final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(
                    new Message( XmlUtil.parse( MessageFormat.format( REQUEST_INVALID_SIG, "<saml:Conditions NotOnOrAfter=\"2008-05-27T08:19:29.000Z\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"/>" )) ),
                    null );

            AssertionStatus status;
            try {
                status = serverProcessSamlAuthnRequestAssertion.checkRequest( pec );
            } catch ( AssertionStatusException ase) {
                status = ase.getAssertionStatus();
            } finally {
                pec.close();
            }
            assertEquals( "falsified", AssertionStatus.FALSIFIED, status );
        }

        // Valid times
        {
            final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(
                    new Message( XmlUtil.parse( MessageFormat.format( REQUEST_INVALID_SIG, "<saml:Conditions NotBefore=\"2000-01-01T00:00:00.000Z\" NotOnOrAfter=\"3000-01-01T00:00:00.000Z\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"/>" )) ),
                    null );

            AssertionStatus status;
            try {
                status = serverProcessSamlAuthnRequestAssertion.checkRequest( pec );
            } catch ( AssertionStatusException ase) {
                status = ase.getAssertionStatus();
            } finally {
                pec.close();
            }
            assertEquals( "success", AssertionStatus.NONE, status );
        }

        // Test audience restriction failure
        {
            processSamlAuthnRequestAssertion.setAudienceRestriction( "http://audience" );
            final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(
                    new Message( XmlUtil.parse( MessageFormat.format( REQUEST_INVALID_SIG, "<saml:Conditions xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml:AudienceRestriction><saml:Audience></saml:Audience>http://audience1<saml:Audience>http://audience2</saml:Audience></saml:AudienceRestriction></saml:Conditions>" )) ),
                    null );

            AssertionStatus status;
            try {
                status = serverProcessSamlAuthnRequestAssertion.checkRequest( pec );
            } catch ( AssertionStatusException ase) {
                status = ase.getAssertionStatus();
            } finally {
                pec.close();
            }
            assertEquals( "falsified", AssertionStatus.FALSIFIED, status );
        }

        // Test audience restriction success
        {
            processSamlAuthnRequestAssertion.setAudienceRestriction( "http://audience1" );
            final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(
                    new Message( XmlUtil.parse( MessageFormat.format( REQUEST_INVALID_SIG, "<saml:Conditions xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml:AudienceRestriction><saml:Audience>http://audience1</saml:Audience><saml:Audience>http://audience2</saml:Audience></saml:AudienceRestriction></saml:Conditions>" )) ),
                    null );

            AssertionStatus status;
            try {
                status = serverProcessSamlAuthnRequestAssertion.checkRequest( pec );
            } catch ( AssertionStatusException ase) {
                status = ase.getAssertionStatus();
            } finally {
                pec.close();
            }
            assertEquals( "success", AssertionStatus.NONE, status );
        }
    }

    @Test
    public void testHTTPRedirectBinding() throws Exception {
        final String queryString = "SAMLRequest=fVLJTsMwEL0j8Q%2BW79mQgGI1QQWEqMQStYEDN%2BOMUzdegsdp4e9JUxBwgOvzm7eMZ3r%2BZjTZgEflbE6zOKUErHC1sk1OH6vraELPi8ODKXKjOzbrw8ou4LUHDGSYtMjGh5z23jLHUSGz3ACyINhydnfLjuKUdd4FJ5ymZH6VU8M77bp1re26bVtoZKeVkWZtGilbqBvZdiupXhpKnr5iHe1izRF7mFsM3IYBSrM0SidRelZlJ%2Bx4wtLjZ0rKT6cLZfcN%2Fov1sichu6mqMiofltUosFE1%2BPuBndPGuUZDLJzZ2ZccUW0GWHKNQMkMEXwYAl46i70BvwS%2FUQIeF7c5XYXQIUuS7XYbf8skPNH8HfxpALHaAwJpMW6XjQX9j7X%2BH59%2F2dPi22Ca%2FJAqPn9tV2Z%2BVTqtxDuZae22lx54GJoE3w9Frp03PPztlsXZiKg6kiOV9RY7EEoqqClJir3r7%2FMYjuYD&RelayState=https%3A%2F%2Fwww.google.com%2Fa%2Flayer7tech.com%2FServiceLogin%3Fservice%3Dwritely%26passive%3Dtrue%26continue%3Dhttp%253A%252F%252Fdocs.google.com%252Fa%252Flayer7tech.com%252F%26followup%3Dhttp%253A%252F%252Fdocs.google.com%252Fa%252Flayer7tech.com%252F%26ltmpl%3Dhomepage";
        final String url = "/googleapps?" + queryString;

        final ProcessSamlAuthnRequestAssertion processSamlAuthnRequestAssertion =
                new ProcessSamlAuthnRequestAssertion();
        processSamlAuthnRequestAssertion.setVerifySignature( false );
        processSamlAuthnRequestAssertion.setSamlProtocolBinding( ProcessSamlAuthnRequestAssertion.SamlProtocolBinding.HttpRedirect );

        final ServerProcessSamlAuthnRequestAssertion serverProcessSamlAuthnRequestAssertion =
                buildServerAssertion( processSamlAuthnRequestAssertion );

        // Success
        {
            final MockServletContext servletContext = new MockServletContext();
            final MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);

            hrequest.setMethod("GET");
            hrequest.setRemoteAddr("127.0.0.1");
            hrequest.setRequestURI(url);
            hrequest.setQueryString( queryString );
            hrequest.setParameter( "SAMLRequest", "fVLJTsMwEL0j8Q%2BW79mQgGI1QQWEqMQStYEDN%2BOMUzdegsdp4e9JUxBwgOvzm7eMZ3r%2BZjTZgEflbE6zOKUErHC1sk1OH6vraELPi8ODKXKjOzbrw8ou4LUHDGSYtMjGh5z23jLHUSGz3ACyINhydnfLjuKUdd4FJ5ymZH6VU8M77bp1re26bVtoZKeVkWZtGilbqBvZdiupXhpKnr5iHe1izRF7mFsM3IYBSrM0SidRelZlJ%2Bx4wtLjZ0rKT6cLZfcN%2Fov1sichu6mqMiofltUosFE1%2BPuBndPGuUZDLJzZ2ZccUW0GWHKNQMkMEXwYAl46i70BvwS%2FUQIeF7c5XYXQIUuS7XYbf8skPNH8HfxpALHaAwJpMW6XjQX9j7X%2BH59%2F2dPi22Ca%2FJAqPn9tV2Z%2BVTqtxDuZae22lx54GJoE3w9Frp03PPztlsXZiKg6kiOV9RY7EEoqqClJir3r7%2FMYjuYD" );
            hrequest.setParameter( "RelayState", "https%3A%2F%2Fwww.google.com%2Fa%2Flayer7tech.com%2FServiceLogin%3Fservice%3Dwritely%26passive%3Dtrue%26continue%3Dhttp%253A%252F%252Fdocs.google.com%252Fa%252Flayer7tech.com%252F%26followup%3Dhttp%253A%252F%252Fdocs.google.com%252Fa%252Flayer7tech.com%252F%26ltmpl%3Dhomepage" );
            ConnectionId.setConnectionId(new ConnectionId(0,0));
            hrequest.setAttribute("com.l7tech.server.connectionIdentifierObject", ConnectionId.getConnectionId());

            final HttpRequestKnob reqKnob = new HttpServletRequestKnob(hrequest);
            final Message httpRequestMessage = new Message();
            httpRequestMessage.attachHttpRequestKnob(reqKnob);

            final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(
                    httpRequestMessage,
                    null );

            AssertionStatus status;
            try {
                status = serverProcessSamlAuthnRequestAssertion.checkRequest( pec );
            } catch ( AssertionStatusException ase) {
                status = ase.getAssertionStatus();
            } finally {
                pec.close();
            }

            assertEquals( "none", AssertionStatus.NONE, status );
        }
    }

    @Test
    public void testHTTPPostBinding() throws Exception {
        final String url = "/googleapps";
        final String samlRequestParameter = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHNhbWxwOkF1dGhuUmVxdWVzdCBBc3NlcnRpb25Db25zdW1lclNlcnZpY2VVUkw9Imh0dHBzOi8vY3MyLnNhbGVzZm9yY2UuY29tIiBEZXN0aW5hdGlvbj0iaHR0cDovL2lyaXNobWFuOjgwODAvc2FsZXNmb3JjZV9zYW1sMiIgSUQ9Il8yUGZITU9TN0xicTZmRzh0cjJ5VnVVZnBISmYuZ1ZGRWo5Z0Q3bGV5dGd0bHVYWmxzUmhNN1VvT0JJLjBjVUNmblJZVzQ5NjlaRlJVLkszQUJQSU93S0Mya3BYMm5qeWR5MldkQWVFajliWFBDRlRNeDNsY0lyTTBpeEcucEZnTnVxM2R2M0luX004ZDVQWmpkSmNJb2xMdTEyY242STBsUy5aMWVIYVlyRzhlSnZiWlVtdGRGWEZrMHNoc0F0RXZDalJwZ2doMWhUSmtSeDN0aXpTYWt0Z0NtLkVGaFN3IiBJc3N1ZUluc3RhbnQ9IjIwMTAtMDgtMDlUMTg6MDU6MzcuNTE5WiIgUHJvdG9jb2xCaW5kaW5nPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YmluZGluZ3M6SFRUUC1QT1NUIiBWZXJzaW9uPSIyLjAiIHhtbG5zOnNhbWxwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wiPjxzYW1sOklzc3VlciB4bWxuczpzYW1sPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXNzZXJ0aW9uIj5odHRwczovL3NhbWwuc2FsZXNmb3JjZS5jb208L3NhbWw6SXNzdWVyPjxkczpTaWduYXR1cmUgeG1sbnM6ZHM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyMiPgo8ZHM6U2lnbmVkSW5mbyB4bWxuczpkcz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyI%2bCjxkczpDYW5vbmljYWxpemF0aW9uTWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS8xMC94bWwtZXhjLWMxNG4jIiB4bWxuczpkcz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyIvPgo8ZHM6U2lnbmF0dXJlTWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnI3JzYS1zaGExIiB4bWxuczpkcz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyIvPgo8ZHM6UmVmZXJlbmNlIFVSST0iI18yUGZITU9TN0xicTZmRzh0cjJ5VnVVZnBISmYuZ1ZGRWo5Z0Q3bGV5dGd0bHVYWmxzUmhNN1VvT0JJLjBjVUNmblJZVzQ5NjlaRlJVLkszQUJQSU93S0Mya3BYMm5qeWR5MldkQWVFajliWFBDRlRNeDNsY0lyTTBpeEcucEZnTnVxM2R2M0luX004ZDVQWmpkSmNJb2xMdTEyY242STBsUy5aMWVIYVlyRzhlSnZiWlVtdGRGWEZrMHNoc0F0RXZDalJwZ2doMWhUSmtSeDN0aXpTYWt0Z0NtLkVGaFN3IiB4bWxuczpkcz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyI%2bCjxkczpUcmFuc2Zvcm1zIHhtbG5zOmRzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjIj4KPGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNlbnZlbG9wZWQtc2lnbmF0dXJlIiB4bWxuczpkcz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyIvPgo8ZHM6VHJhbnNmb3JtIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS8xMC94bWwtZXhjLWMxNG4jIiB4bWxuczpkcz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyI%2bPGVjOkluY2x1c2l2ZU5hbWVzcGFjZXMgUHJlZml4TGlzdD0iZHMgc2FtbCBzYW1scCIgeG1sbnM6ZWM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIvPjwvZHM6VHJhbnNmb3JtPgo8L2RzOlRyYW5zZm9ybXM%2bCjxkczpEaWdlc3RNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjc2hhMSIgeG1sbnM6ZHM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyMiLz4KPGRzOkRpZ2VzdFZhbHVlIHhtbG5zOmRzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjIj4zNm90RDdSM05lS1ZjSkNrS1BwWEFXbkZkSTg9PC9kczpEaWdlc3RWYWx1ZT4KPC9kczpSZWZlcmVuY2U%2bCjwvZHM6U2lnbmVkSW5mbz4KPGRzOlNpZ25hdHVyZVZhbHVlIHhtbG5zOmRzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjIj4KY1JCM002eW45dHpwV2lFSWUrY2s5QjNVeUc5VlNGZDBkZS9ybGN4SzdwT2JHUkUvcXFTQ0NTSFpkTVN5b0UxditEaXlCNzU5WVFZZAphNm91YnpPb1ZCS3BXSkJKY2RPQ2srd0lQZGxHTnpBKy8xaWR0ajc5Mk9hdVIvTmxkdW1BZXVnV0NpejdrYmZnamR5cGpZc245TUhICkxDZlhQWnNyOTNKd0RLUXlRcG89CjwvZHM6U2lnbmF0dXJlVmFsdWU%2bCjxkczpLZXlJbmZvPjxkczpYNTA5RGF0YT48ZHM6WDUwOUNlcnRpZmljYXRlPk1JSUVTekNDQTdTZ0F3SUJBZ0lRZUx0R3ZxVXdBd0krQWJhS0o3SUZ3ekFOQmdrcWhraUc5dzBCQVFVRkFEQ0J1akVmTUIwR0ExVUUKQ2hNV1ZtVnlhVk5wWjI0Z1ZISjFjM1FnVG1WMGQyOXlhekVYTUJVR0ExVUVDeE1PVm1WeWFWTnBaMjRzSUVsdVl5NHhNekF4QmdOVgpCQXNUS2xabGNtbFRhV2R1SUVsdWRHVnlibUYwYVc5dVlXd2dVMlZ5ZG1WeUlFTkJJQzBnUTJ4aGMzTWdNekZKTUVjR0ExVUVDeE5BCmQzZDNMblpsY21semFXZHVMbU52YlM5RFVGTWdTVzVqYjNKd0xtSjVJRkpsWmk0Z1RFbEJRa2xNU1ZSWklFeFVSQzRvWXlrNU55QlcKWlhKcFUybG5iakFlRncwd09UQXhNRFl3TURBd01EQmFGdzB4TVRBeE1EY3lNelU1TlRsYU1JR09NUXN3Q1FZRFZRUUdFd0pWVXpFVApNQkVHQTFVRUNCTUtRMkZzYVdadmNtNXBZVEVXTUJRR0ExVUVCeFFOVTJGdUlFWnlZVzVqYVhOamJ6RWRNQnNHQTFVRUNoUVVVMkZzClpYTm1iM0pqWlM1amIyMHNJRWx1WXk0eEZEQVNCZ05WQkFzVUMwRndjR3hwWTJGMGFXOXVNUjB3R3dZRFZRUURGQlJ3Y205NGVTNXoKWVd4bGMyWnZjbU5sTG1OdmJUQ0JuekFOQmdrcWhraUc5dzBCQVFFRkFBT0JqUUF3Z1lrQ2dZRUF6S0VsbHVIUVlsVW5GbTE1Nk53dQpwOXZxa2Y5RHZuaE9KYzA5R05ZS09kejVQa3BKL2JGTHVOMmZybWZKVGx3NnBpNGtuRTJnZU4zajI2aUFGR0lwcWdrZldtQWk1a25qCmNJYk92SGJNWE1nMWFwdVZ5SzlqbWJLeTRwSVRaQ2o1NlB0SDdxTWpsbXdOK1pFY1FSVnkrdXJSR0pSZkJFeUUraHQ1S3Jld2hsY0MKQXdFQUFhT0NBWG93Z2dGMk1Ba0dBMVVkRXdRQ01BQXdDd1lEVlIwUEJBUURBZ1dnTUVZR0ExVWRId1EvTUQwd082QTVvRGVHTldoMApkSEE2THk5amNtd3VkbVZ5YVhOcFoyNHVZMjl0TDBOc1lYTnpNMGx1ZEdWeWJtRjBhVzl1WVd4VFpYSjJaWEl1WTNKc01FUUdBMVVkCklBUTlNRHN3T1FZTFlJWklBWWI0UlFFSEZ3TXdLakFvQmdnckJnRUZCUWNDQVJZY2FIUjBjSE02THk5M2QzY3VkbVZ5YVhOcFoyNHUKWTI5dEwzSndZVEFvQmdOVkhTVUVJVEFmQmdsZ2hrZ0JodmhDQkFFR0NDc0dBUVVGQndNQkJnZ3JCZ0VGQlFjREFqQTBCZ2dyQmdFRgpCUWNCQVFRb01DWXdKQVlJS3dZQkJRVUhNQUdHR0doMGRIQTZMeTl2WTNOd0xuWmxjbWx6YVdkdUxtTnZiVEJ1QmdnckJnRUZCUWNCCkRBUmlNR0NoWHFCY01Gb3dXREJXRmdscGJXRm5aUzluYVdZd0lUQWZNQWNHQlNzT0F3SWFCQlJMYTdrb2xnWU11OUJTT0pzcHJFc0gKaXlFRkdEQW1GaVJvZEhSd09pOHZiRzluYnk1MlpYSnBjMmxuYmk1amIyMHZkbk5zYjJkdk1TNW5hV1l3RFFZSktvWklodmNOQVFFRgpCUUFEZ1lFQUVGcHh4bFpLTnBjSE1LcEo4a1IyOEE4TU9ObVp1alRTaW5NZURFQkVSUzlDTDZReUs4eUlyRnIxb2ZlL3FhZmZGZ3lDCnpvWnMxTUl0eHliT3RTdlZUbG9YTEtaK2s2WGIxcExWQXBoYmhhb0ZmNFlWOHUrWG45MWdpb3QvaDlOTmtNU3U3OHY3dG5RZU5mSk8KVk1RQjQ5M0FhR25ib2xJWWtqVFEwbnJVNkhFPTwvZHM6WDUwOUNlcnRpZmljYXRlPjwvZHM6WDUwOURhdGE%2bPC9kczpLZXlJbmZvPjwvZHM6U2lnbmF0dXJlPjwvc2FtbHA6QXV0aG5SZXF1ZXN0Pg%3d%3d";

        final ProcessSamlAuthnRequestAssertion processSamlAuthnRequestAssertion =
                new ProcessSamlAuthnRequestAssertion();
        //processSamlAuthnRequestAssertion.setVerifySignature( false );
        processSamlAuthnRequestAssertion.setSamlProtocolBinding( ProcessSamlAuthnRequestAssertion.SamlProtocolBinding.HttpPost );

        final ServerProcessSamlAuthnRequestAssertion serverProcessSamlAuthnRequestAssertion =
                buildServerAssertion( processSamlAuthnRequestAssertion );

        // Success
        {
            final MockServletContext servletContext = new MockServletContext();
            final MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);

            hrequest.setMethod("POST");
            hrequest.setRemoteAddr("127.0.0.1");
            hrequest.setRequestURI(url);
            hrequest.addHeader("Content-Type", ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED.getFullValue() );
            hrequest.setContentType( ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED.getFullValue() );
            hrequest.setParameter( "SAMLRequest", samlRequestParameter );
            ConnectionId.setConnectionId(new ConnectionId(0,0));
            hrequest.setAttribute("com.l7tech.server.connectionIdentifierObject", ConnectionId.getConnectionId());
            hrequest.setContent( ("SAMLRequest=" + samlRequestParameter).getBytes( Charsets.UTF8 ));

            final HttpRequestKnob reqKnob = new HttpServletRequestKnob(hrequest);
            final Message httpRequestMessage = new Message();
            httpRequestMessage.attachHttpRequestKnob(reqKnob);

            final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(
                    httpRequestMessage,
                    null );

            AssertionStatus status;
            try {
                status = serverProcessSamlAuthnRequestAssertion.checkRequest( pec );
            } catch ( AssertionStatusException ase) {
                status = ase.getAssertionStatus();
            } finally {
                pec.close();
            }

            assertEquals( "none", AssertionStatus.NONE, status );
            assertEquals( "acsUrl", "https://cs2.salesforce.com", pec.getVariable( "authnRequest.acsUrl" ) );            
        }
    }

    @Test
    public void testSignature() throws Exception {
        final ProcessSamlAuthnRequestAssertion processSamlAuthnRequestAssertion =
                new ProcessSamlAuthnRequestAssertion();
        processSamlAuthnRequestAssertion.setVerifySignature( true );

        final ServerProcessSamlAuthnRequestAssertion serverProcessSamlAuthnRequestAssertion =
                buildServerAssertion( processSamlAuthnRequestAssertion );

        // Success
        {
            final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(
                    new Message( XmlUtil.parse( REQUEST_SIGNED) ),
                    null );

            AssertionStatus status;
            try {
                status = serverProcessSamlAuthnRequestAssertion.checkRequest( pec );
            } catch ( AssertionStatusException ase) {
                status = ase.getAssertionStatus();
            } finally {
                pec.close();
            }
            assertEquals( "none", AssertionStatus.NONE, status );
        }
    }

    private ServerProcessSamlAuthnRequestAssertion buildServerAssertion( final ProcessSamlAuthnRequestAssertion assertion ) throws ServerPolicyException {
        final SimpleSingletonBeanFactory beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("serverConfig", ServerConfig.getInstance());
            put("auditContext", new AuditContextStub());
            put("securityTokenResolver", new SimpleSecurityTokenResolver());
        }});

        final ApplicationContext applicationContext = new GenericApplicationContext(beanFactory){
            @Override
            public void publishEvent( final ApplicationEvent event ) {
            }
        };

        return new ServerProcessSamlAuthnRequestAssertion( assertion, applicationContext );
    }
}
