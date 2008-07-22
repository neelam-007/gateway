package com.l7tech.security.wsfederation;

import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.wsfederation.FederationPassiveClient;
import com.l7tech.security.wsfederation.InvalidHtmlException;
import junit.framework.TestCase;

import java.net.URL;

/**
 * Tests for the Federation Passive Client.
 *
 * @author $Author$
 * @version $Revision$
 */
public class FederationPassiveClientTest extends TestCase {

    //- PUBLIC

    /**
     * Basic test, should successfully parse and return a token.
     */
    public void testBasic() throws Exception {
        byte[] data = dataString.getBytes("UTF-8");
        MockGenericHttpClient client = new MockGenericHttpClient(200, null, ContentTypeHeader.parseValue("text/html; charset=UTF-8"), new Long(data.length), data);
        GenericHttpRequestParams httpParams = new GenericHttpRequestParams();
        httpParams.setTargetUrl(new URL("http://adfs.l7tech.com/adfs"));
        String realm = "blah";
        boolean addTimestamp = false;

        XmlSecurityToken token = FederationPassiveClient.obtainFederationToken(client
                                                         ,httpParams
                                                         ,realm
                                                         ,null
                                                         ,null
                                                         ,addTimestamp);
    }

    /**
     * ADFS beta returns an invalid (empty) FORM element in the HTML response ... this ensures we handle it
     */
    public void testAdfsBeta() throws Exception {
        byte[] data = invalidFormDataString.getBytes("UTF-8");
        MockGenericHttpClient client = new MockGenericHttpClient(200, null, ContentTypeHeader.parseValue("text/html; charset=UTF-8"), new Long(data.length), data);
        GenericHttpRequestParams httpParams = new GenericHttpRequestParams();
        httpParams.setTargetUrl(new URL("http://adfs.l7tech.com/adfs"));
        String realm = "blah";
        boolean addTimestamp = false;

        XmlSecurityToken token = FederationPassiveClient.obtainFederationToken(client
                                                         ,httpParams
                                                         ,realm
                                                         ,null
                                                         ,null
                                                         ,addTimestamp);
    }

    /**
     * Ensures TEXT/HTML response
     */
    public void testContentType() throws Exception {
        byte[] data = "Plain text page.".getBytes("UTF-8");
        MockGenericHttpClient client = new MockGenericHttpClient(200, null, ContentTypeHeader.parseValue("text/plain; charset=UTF-8"), new Long(data.length), data);
        GenericHttpRequestParams httpParams = new GenericHttpRequestParams();
        httpParams.setTargetUrl(new URL("http://adfs.l7tech.com/adfs"));
        String realm = "blah";
        boolean addTimestamp = false;

        try {
            XmlSecurityToken token = FederationPassiveClient.obtainFederationToken(client
                                                             ,httpParams
                                                             ,realm
                                                             ,null
                                                             ,null
                                                             ,addTimestamp);
        }
        catch( InvalidHtmlException ihe) {
            if(!ihe.getMessage().startsWith("Response is not html content")) {
                ihe.printStackTrace();
                fail("Expected InvalidHtmlException('Response is not html content .')");
            }
        }
    }

    /**
     * Fail non-adfs / missing wresult
     */
    public void testRandomHtmlResponse() throws Exception {
        String htmlPage = "<html><head><title>HTML Page</title></head><body>Page body.</body></html>";
        byte[] data = htmlPage.getBytes("UTF-8");
        MockGenericHttpClient client = new MockGenericHttpClient(200, null, ContentTypeHeader.parseValue("text/html; charset=UTF-8"), new Long(data.length), data);
        GenericHttpRequestParams httpParams = new GenericHttpRequestParams();
        httpParams.setTargetUrl(new URL("http://adfs.l7tech.com/adfs"));
        String realm = "blah";
        boolean addTimestamp = false;

        try {
            XmlSecurityToken token = FederationPassiveClient.obtainFederationToken(client
                                                         ,httpParams
                                                         ,realm
                                                         ,null
                                                         ,null
                                                         ,addTimestamp);
        }
        catch(InvalidHtmlException ihe) {
            if(!ihe.getMessage().equals("Missing wresult element in HTML/FORM")) {
                ihe.printStackTrace();
                fail("Expected InvalidHtmlException('Missing wresult element in HTML/FORM')");
            }
        }
    }

    /**
     * Fail multiple wresults in response form.
     */
    public void testMultipleWresultTest() throws Exception {
        byte[] data = multipleWresultDataString.getBytes("UTF-8");
        MockGenericHttpClient client = new MockGenericHttpClient(200, null, ContentTypeHeader.parseValue("text/html; charset=UTF-8"), new Long(data.length), data);
        GenericHttpRequestParams httpParams = new GenericHttpRequestParams();
        httpParams.setTargetUrl(new URL("http://adfs.l7tech.com/adfs"));
        String realm = "blah";
        boolean addTimestamp = false;

        try {
            XmlSecurityToken token = FederationPassiveClient.obtainFederationToken(client
                                                         ,httpParams
                                                         ,realm
                                                         ,null
                                                         ,null
                                                         ,addTimestamp);
        }
        catch(InvalidHtmlException ihe) {
            if(!ihe.getMessage().equals("Multiple wresult elements in HTML/FORM")) {
                ihe.printStackTrace();
                fail("Expected InvalidHtmlException('Multiple wresult elements in HTML/FORM')");
            }
        }
    }

    //- PRIVATE

    private static final String dataString = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
            "<html>\n" +
            "<head><title>Working...</title></head>\n" +
            "\n" +
            "<body>\n" +
            "\n" +
            "<form method=\"POST\" action=\"https://fedserv.l7tech.com/\">\n" +
            "<input type=\"hidden\" name=\"wa\" value=\"wsignin1.0\"/>\n" +
            "<input type=\"hidden\" name=\"wresult\" value=\"&lt;wst:RequestSecurityTokenResponse xmlns:wst=&quot;http://schemas.xmlsoap.org/ws/2005/02/trust&quot;>&lt;wst:RequestedSecurityToken>&lt;saml:Assertion AssertionID=&quot;_80dc823c-4472-4271-b830-8a519a7f30b5&quot; IssueInstant=&quot;2005-10-07T19:15:16Z&quot; Issuer=&quot;urn:federation:myOrganization&quot; MajorVersion=&quot;1&quot; MinorVersion=&quot;1&quot; xmlns:saml=&quot;urn:oasis:names:tc:SAML:1.0:assertion&quot;>&lt;saml:Conditions NotBefore=&quot;2005-10-07T19:15:16Z&quot; NotOnOrAfter=&quot;2005-10-07T20:15:16Z&quot;>&lt;saml:AudienceRestrictionCondition>&lt;saml:Audience>https://fedserv.l7tech.com/&lt;/saml:Audience>&lt;/saml:AudienceRestrictionCondition>&lt;/saml:Conditions>&lt;saml:Advice>&lt;adfs:ClaimSource xmlns:adfs=&quot;urn:microsoft:federation&quot;>ldap://127.0.0.1:50000/&lt;/adfs:ClaimSource>&lt;adfs:CookieInfoHash xmlns:adfs=&quot;urn:microsoft:federation&quot;>5bpscN5PferDngEy5SkfMc/J+pc=&lt;/adfs:CookieInfoHash>&lt;/saml:Advice>&lt;saml:AuthenticationStatement AuthenticationInstant=&quot;2005-10-07T19:15:16Z&quot; AuthenticationMethod=&quot;urn:oasis:names:tc:SAML:1.0:am:password&quot;>&lt;saml:Subject>&lt;saml:NameIdentifier Format=&quot;http://schemas.xmlsoap.org/claims/UPN&quot;>test&lt;/saml:NameIdentifier>&lt;/saml:Subject>&lt;/saml:AuthenticationStatement>&lt;Signature xmlns=&quot;http://www.w3.org/2000/09/xmldsig#&quot;>&lt;SignedInfo>&lt;CanonicalizationMethod Algorithm=&quot;http://www.w3.org/2001/10/xml-exc-c14n#&quot; />&lt;SignatureMethod Algorithm=&quot;http://www.w3.org/2000/09/xmldsig#rsa-sha1&quot; />&lt;Reference URI=&quot;#_80dc823c-4472-4271-b830-8a519a7f30b5&quot;>&lt;Transforms>&lt;Transform Algorithm=&quot;http://www.w3.org/2000/09/xmldsig#enveloped-signature&quot; />&lt;Transform Algorithm=&quot;http://www.w3.org/2001/10/xml-exc-c14n#&quot; />&lt;/Transforms>&lt;DigestMethod Algorithm=&quot;http://www.w3.org/2000/09/xmldsig#sha1&quot; />&lt;DigestValue>2h2SbfELy7pH6vHhp7jSPtlY+RM=&lt;/DigestValue>&lt;/Reference>&lt;/SignedInfo>&lt;SignatureValue>ArH/F2MSAYAiiS3YamXuVYLR9ERl3fqmC065vecjWq0f3r7qyd4KqTesL8rmCRAJv3PEvLUO9T28vC8b3oknGlrCZ3jJVN2uMxFPNFA+YE0KfxyRXVST3LdnwtQ3pLU+zvBPeNCeK5ovqDMjDuo1HD5KU209W5P7ik6mKfhW1Nc=&lt;/SignatureValue>&lt;KeyInfo>&lt;X509Data>&lt;X509Certificate>MIICGjCCAYOgAwIBAgIIBEWBazcqgAEwDQYJKoZIhvcNAQEFBQAwITEfMB0GA1UEAxMWcm9vdC5hZHNlcnYubDd0ZWNoLmNvbTAeFw0wNTA5MDgxNzEwMzZaFw0wNzA5MDgxNzIwMzZaMBwxGjAYBgNVBAMTEWFkc2Vydi5sN3RlY2guY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCMRHOqt7nE4oVHI0XcyvwuMeFpKpFrbW7iVHCLeGHZa7maZkiKIFknrulpjvaHoQX3MCPKHuC8k6YIKyJ4FrA3A/oYFoPMTLRM7fx2jcULW4pJx4RFO/OVp/kq2LuJXY1Qew2fSgNeMFICFk7S4NjCj5K5dvIIGN8wPgJKrb53RQIDAQABo2AwXjAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIF4DAdBgNVHQ4EFgQUtobzFvBU6T3u75XlCvMaNiz4huUwHwYDVR0jBBgwFoAU0vqLKpRxxFOKI99KF6PcUoMQwxwwDQYJKoZIhvcNAQEFBQADgYEAQJonP4VXVPpH5E9i03Z5ZrhfdT0sq32pET9I7pQtAJWkbMxHcr4P/TZpcjFcgxrLoc8H2P3OknY+8YN3BeNv/mb/dfJfEEmI26sdHcxSCM0Q++KcMcfMSdE+Bmfr4rX2AfN6ZyKlHWJ4IMqwmzxsQGKoca+xXhRYA6uW4e87m8E=&lt;/X509Certificate>&lt;/X509Data>&lt;/KeyInfo>&lt;/Signature>&lt;/saml:Assertion>&lt;/wst:RequestedSecurityToken>&lt;wsp:AppliesTo xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2004/09/policy&quot;>&lt;wsa:EndpointReference xmlns:wsa=&quot;http://schemas.xmlsoap.org/ws/2004/08/addressing&quot;>&lt;wsa:Address>https://fedserv.l7tech.com/&lt;/wsa:Address>&lt;/wsa:EndpointReference>&lt;/wsp:AppliesTo>&lt;/wst:RequestSecurityTokenResponse>\"/>\n" +
            "<noscript>\n" +
            "        <p>Script is disabled. Please click Submit to continue.</p>\n" +
            "        <input type=\"submit\" value=\"Submit\"/>\n" +
            "</noscript>\n" +
            "</form>\n" +
            "<script language=\"javascript\">window.setTimeout('document.forms[0].submit()', 0);</script>\n" +
            "</body>\n" +
            "</html>";

    private static final String invalidFormDataString = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
            "<html>\n" +
            "<head><title>Working...</title></head>\n" +
            "\n" +
            "<body>\n" +
            "\n" +
            "<form method=\"POST\" action=\"https://fedserv.l7tech.com/\"/>\n" +
            "<input type=\"hidden\" name=\"wa\" value=\"wsignin1.0\"/>\n" +
            "<input type=\"hidden\" name=\"wresult\" value=\"&lt;wst:RequestSecurityTokenResponse xmlns:wst=&quot;http://schemas.xmlsoap.org/ws/2005/02/trust&quot;>&lt;wst:RequestedSecurityToken>&lt;saml:Assertion AssertionID=&quot;_80dc823c-4472-4271-b830-8a519a7f30b5&quot; IssueInstant=&quot;2005-10-07T19:15:16Z&quot; Issuer=&quot;urn:federation:myOrganization&quot; MajorVersion=&quot;1&quot; MinorVersion=&quot;1&quot; xmlns:saml=&quot;urn:oasis:names:tc:SAML:1.0:assertion&quot;>&lt;saml:Conditions NotBefore=&quot;2005-10-07T19:15:16Z&quot; NotOnOrAfter=&quot;2005-10-07T20:15:16Z&quot;>&lt;saml:AudienceRestrictionCondition>&lt;saml:Audience>https://fedserv.l7tech.com/&lt;/saml:Audience>&lt;/saml:AudienceRestrictionCondition>&lt;/saml:Conditions>&lt;saml:Advice>&lt;adfs:ClaimSource xmlns:adfs=&quot;urn:microsoft:federation&quot;>ldap://127.0.0.1:50000/&lt;/adfs:ClaimSource>&lt;adfs:CookieInfoHash xmlns:adfs=&quot;urn:microsoft:federation&quot;>5bpscN5PferDngEy5SkfMc/J+pc=&lt;/adfs:CookieInfoHash>&lt;/saml:Advice>&lt;saml:AuthenticationStatement AuthenticationInstant=&quot;2005-10-07T19:15:16Z&quot; AuthenticationMethod=&quot;urn:oasis:names:tc:SAML:1.0:am:password&quot;>&lt;saml:Subject>&lt;saml:NameIdentifier Format=&quot;http://schemas.xmlsoap.org/claims/UPN&quot;>test&lt;/saml:NameIdentifier>&lt;/saml:Subject>&lt;/saml:AuthenticationStatement>&lt;Signature xmlns=&quot;http://www.w3.org/2000/09/xmldsig#&quot;>&lt;SignedInfo>&lt;CanonicalizationMethod Algorithm=&quot;http://www.w3.org/2001/10/xml-exc-c14n#&quot; />&lt;SignatureMethod Algorithm=&quot;http://www.w3.org/2000/09/xmldsig#rsa-sha1&quot; />&lt;Reference URI=&quot;#_80dc823c-4472-4271-b830-8a519a7f30b5&quot;>&lt;Transforms>&lt;Transform Algorithm=&quot;http://www.w3.org/2000/09/xmldsig#enveloped-signature&quot; />&lt;Transform Algorithm=&quot;http://www.w3.org/2001/10/xml-exc-c14n#&quot; />&lt;/Transforms>&lt;DigestMethod Algorithm=&quot;http://www.w3.org/2000/09/xmldsig#sha1&quot; />&lt;DigestValue>2h2SbfELy7pH6vHhp7jSPtlY+RM=&lt;/DigestValue>&lt;/Reference>&lt;/SignedInfo>&lt;SignatureValue>ArH/F2MSAYAiiS3YamXuVYLR9ERl3fqmC065vecjWq0f3r7qyd4KqTesL8rmCRAJv3PEvLUO9T28vC8b3oknGlrCZ3jJVN2uMxFPNFA+YE0KfxyRXVST3LdnwtQ3pLU+zvBPeNCeK5ovqDMjDuo1HD5KU209W5P7ik6mKfhW1Nc=&lt;/SignatureValue>&lt;KeyInfo>&lt;X509Data>&lt;X509Certificate>MIICGjCCAYOgAwIBAgIIBEWBazcqgAEwDQYJKoZIhvcNAQEFBQAwITEfMB0GA1UEAxMWcm9vdC5hZHNlcnYubDd0ZWNoLmNvbTAeFw0wNTA5MDgxNzEwMzZaFw0wNzA5MDgxNzIwMzZaMBwxGjAYBgNVBAMTEWFkc2Vydi5sN3RlY2guY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCMRHOqt7nE4oVHI0XcyvwuMeFpKpFrbW7iVHCLeGHZa7maZkiKIFknrulpjvaHoQX3MCPKHuC8k6YIKyJ4FrA3A/oYFoPMTLRM7fx2jcULW4pJx4RFO/OVp/kq2LuJXY1Qew2fSgNeMFICFk7S4NjCj5K5dvIIGN8wPgJKrb53RQIDAQABo2AwXjAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIF4DAdBgNVHQ4EFgQUtobzFvBU6T3u75XlCvMaNiz4huUwHwYDVR0jBBgwFoAU0vqLKpRxxFOKI99KF6PcUoMQwxwwDQYJKoZIhvcNAQEFBQADgYEAQJonP4VXVPpH5E9i03Z5ZrhfdT0sq32pET9I7pQtAJWkbMxHcr4P/TZpcjFcgxrLoc8H2P3OknY+8YN3BeNv/mb/dfJfEEmI26sdHcxSCM0Q++KcMcfMSdE+Bmfr4rX2AfN6ZyKlHWJ4IMqwmzxsQGKoca+xXhRYA6uW4e87m8E=&lt;/X509Certificate>&lt;/X509Data>&lt;/KeyInfo>&lt;/Signature>&lt;/saml:Assertion>&lt;/wst:RequestedSecurityToken>&lt;wsp:AppliesTo xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2004/09/policy&quot;>&lt;wsa:EndpointReference xmlns:wsa=&quot;http://schemas.xmlsoap.org/ws/2004/08/addressing&quot;>&lt;wsa:Address>https://fedserv.l7tech.com/&lt;/wsa:Address>&lt;/wsa:EndpointReference>&lt;/wsp:AppliesTo>&lt;/wst:RequestSecurityTokenResponse>\"/>\n" +
            "<noscript>\n" +
            "        <p>Script is disabled. Please click Submit to continue.</p>\n" +
            "        <input type=\"submit\" value=\"Submit\"/>\n" +
            "</noscript>\n" +
            "</form>\n" +
            "<script language=\"javascript\">window.setTimeout('document.forms[0].submit()', 0);</script>\n" +
            "</body>\n" +
            "</html>";

    private static final String multipleWresultDataString = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
            "<html>\n" +
            "<head><title>Working...</title></head>\n" +
            "\n" +
            "<body>\n" +
            "\n" +
            "<form method=\"POST\" action=\"https://fedserv.l7tech.com/\">\n" +
            "<input type=\"hidden\" name=\"wa\" value=\"wsignin1.0\"/>\n" +
            "<input type=\"hidden\" name=\"wresult\" value=\"&lt;wst:RequestSecurityTokenResponse xmlns:wst=&quot;http://schemas.xmlsoap.org/ws/2005/02/trust&quot;>&lt;wst:RequestedSecurityToken>&lt;saml:Assertion AssertionID=&quot;_80dc823c-4472-4271-b830-8a519a7f30b5&quot; IssueInstant=&quot;2005-10-07T19:15:16Z&quot; Issuer=&quot;urn:federation:myOrganization&quot; MajorVersion=&quot;1&quot; MinorVersion=&quot;1&quot; xmlns:saml=&quot;urn:oasis:names:tc:SAML:1.0:assertion&quot;>&lt;saml:Conditions NotBefore=&quot;2005-10-07T19:15:16Z&quot; NotOnOrAfter=&quot;2005-10-07T20:15:16Z&quot;>&lt;saml:AudienceRestrictionCondition>&lt;saml:Audience>https://fedserv.l7tech.com/&lt;/saml:Audience>&lt;/saml:AudienceRestrictionCondition>&lt;/saml:Conditions>&lt;saml:Advice>&lt;adfs:ClaimSource xmlns:adfs=&quot;urn:microsoft:federation&quot;>ldap://127.0.0.1:50000/&lt;/adfs:ClaimSource>&lt;adfs:CookieInfoHash xmlns:adfs=&quot;urn:microsoft:federation&quot;>5bpscN5PferDngEy5SkfMc/J+pc=&lt;/adfs:CookieInfoHash>&lt;/saml:Advice>&lt;saml:AuthenticationStatement AuthenticationInstant=&quot;2005-10-07T19:15:16Z&quot; AuthenticationMethod=&quot;urn:oasis:names:tc:SAML:1.0:am:password&quot;>&lt;saml:Subject>&lt;saml:NameIdentifier Format=&quot;http://schemas.xmlsoap.org/claims/UPN&quot;>test&lt;/saml:NameIdentifier>&lt;/saml:Subject>&lt;/saml:AuthenticationStatement>&lt;Signature xmlns=&quot;http://www.w3.org/2000/09/xmldsig#&quot;>&lt;SignedInfo>&lt;CanonicalizationMethod Algorithm=&quot;http://www.w3.org/2001/10/xml-exc-c14n#&quot; />&lt;SignatureMethod Algorithm=&quot;http://www.w3.org/2000/09/xmldsig#rsa-sha1&quot; />&lt;Reference URI=&quot;#_80dc823c-4472-4271-b830-8a519a7f30b5&quot;>&lt;Transforms>&lt;Transform Algorithm=&quot;http://www.w3.org/2000/09/xmldsig#enveloped-signature&quot; />&lt;Transform Algorithm=&quot;http://www.w3.org/2001/10/xml-exc-c14n#&quot; />&lt;/Transforms>&lt;DigestMethod Algorithm=&quot;http://www.w3.org/2000/09/xmldsig#sha1&quot; />&lt;DigestValue>2h2SbfELy7pH6vHhp7jSPtlY+RM=&lt;/DigestValue>&lt;/Reference>&lt;/SignedInfo>&lt;SignatureValue>ArH/F2MSAYAiiS3YamXuVYLR9ERl3fqmC065vecjWq0f3r7qyd4KqTesL8rmCRAJv3PEvLUO9T28vC8b3oknGlrCZ3jJVN2uMxFPNFA+YE0KfxyRXVST3LdnwtQ3pLU+zvBPeNCeK5ovqDMjDuo1HD5KU209W5P7ik6mKfhW1Nc=&lt;/SignatureValue>&lt;KeyInfo>&lt;X509Data>&lt;X509Certificate>MIICGjCCAYOgAwIBAgIIBEWBazcqgAEwDQYJKoZIhvcNAQEFBQAwITEfMB0GA1UEAxMWcm9vdC5hZHNlcnYubDd0ZWNoLmNvbTAeFw0wNTA5MDgxNzEwMzZaFw0wNzA5MDgxNzIwMzZaMBwxGjAYBgNVBAMTEWFkc2Vydi5sN3RlY2guY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCMRHOqt7nE4oVHI0XcyvwuMeFpKpFrbW7iVHCLeGHZa7maZkiKIFknrulpjvaHoQX3MCPKHuC8k6YIKyJ4FrA3A/oYFoPMTLRM7fx2jcULW4pJx4RFO/OVp/kq2LuJXY1Qew2fSgNeMFICFk7S4NjCj5K5dvIIGN8wPgJKrb53RQIDAQABo2AwXjAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIF4DAdBgNVHQ4EFgQUtobzFvBU6T3u75XlCvMaNiz4huUwHwYDVR0jBBgwFoAU0vqLKpRxxFOKI99KF6PcUoMQwxwwDQYJKoZIhvcNAQEFBQADgYEAQJonP4VXVPpH5E9i03Z5ZrhfdT0sq32pET9I7pQtAJWkbMxHcr4P/TZpcjFcgxrLoc8H2P3OknY+8YN3BeNv/mb/dfJfEEmI26sdHcxSCM0Q++KcMcfMSdE+Bmfr4rX2AfN6ZyKlHWJ4IMqwmzxsQGKoca+xXhRYA6uW4e87m8E=&lt;/X509Certificate>&lt;/X509Data>&lt;/KeyInfo>&lt;/Signature>&lt;/saml:Assertion>&lt;/wst:RequestedSecurityToken>&lt;wsp:AppliesTo xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2004/09/policy&quot;>&lt;wsa:EndpointReference xmlns:wsa=&quot;http://schemas.xmlsoap.org/ws/2004/08/addressing&quot;>&lt;wsa:Address>https://fedserv.l7tech.com/&lt;/wsa:Address>&lt;/wsa:EndpointReference>&lt;/wsp:AppliesTo>&lt;/wst:RequestSecurityTokenResponse>\"/>\n" +
            "<input type=\"hidden\" name=\"wresult\" value=\"&lt;wst:RequestSecurityTokenResponse xmlns:wst=&quot;http://schemas.xmlsoap.org/ws/2005/02/trust&quot;>&lt;wst:RequestedSecurityToken>&lt;saml:Assertion AssertionID=&quot;_80dc823c-4472-4271-b830-8a519a7f30b5&quot; IssueInstant=&quot;2005-10-07T19:15:16Z&quot; Issuer=&quot;urn:federation:myOrganization&quot; MajorVersion=&quot;1&quot; MinorVersion=&quot;1&quot; xmlns:saml=&quot;urn:oasis:names:tc:SAML:1.0:assertion&quot;>&lt;saml:Conditions NotBefore=&quot;2005-10-07T19:15:16Z&quot; NotOnOrAfter=&quot;2005-10-07T20:15:16Z&quot;>&lt;saml:AudienceRestrictionCondition>&lt;saml:Audience>https://fedserv.l7tech.com/&lt;/saml:Audience>&lt;/saml:AudienceRestrictionCondition>&lt;/saml:Conditions>&lt;saml:Advice>&lt;adfs:ClaimSource xmlns:adfs=&quot;urn:microsoft:federation&quot;>ldap://127.0.0.1:50000/&lt;/adfs:ClaimSource>&lt;adfs:CookieInfoHash xmlns:adfs=&quot;urn:microsoft:federation&quot;>5bpscN5PferDngEy5SkfMc/J+pc=&lt;/adfs:CookieInfoHash>&lt;/saml:Advice>&lt;saml:AuthenticationStatement AuthenticationInstant=&quot;2005-10-07T19:15:16Z&quot; AuthenticationMethod=&quot;urn:oasis:names:tc:SAML:1.0:am:password&quot;>&lt;saml:Subject>&lt;saml:NameIdentifier Format=&quot;http://schemas.xmlsoap.org/claims/UPN&quot;>test&lt;/saml:NameIdentifier>&lt;/saml:Subject>&lt;/saml:AuthenticationStatement>&lt;Signature xmlns=&quot;http://www.w3.org/2000/09/xmldsig#&quot;>&lt;SignedInfo>&lt;CanonicalizationMethod Algorithm=&quot;http://www.w3.org/2001/10/xml-exc-c14n#&quot; />&lt;SignatureMethod Algorithm=&quot;http://www.w3.org/2000/09/xmldsig#rsa-sha1&quot; />&lt;Reference URI=&quot;#_80dc823c-4472-4271-b830-8a519a7f30b5&quot;>&lt;Transforms>&lt;Transform Algorithm=&quot;http://www.w3.org/2000/09/xmldsig#enveloped-signature&quot; />&lt;Transform Algorithm=&quot;http://www.w3.org/2001/10/xml-exc-c14n#&quot; />&lt;/Transforms>&lt;DigestMethod Algorithm=&quot;http://www.w3.org/2000/09/xmldsig#sha1&quot; />&lt;DigestValue>2h2SbfELy7pH6vHhp7jSPtlY+RM=&lt;/DigestValue>&lt;/Reference>&lt;/SignedInfo>&lt;SignatureValue>ArH/F2MSAYAiiS3YamXuVYLR9ERl3fqmC065vecjWq0f3r7qyd4KqTesL8rmCRAJv3PEvLUO9T28vC8b3oknGlrCZ3jJVN2uMxFPNFA+YE0KfxyRXVST3LdnwtQ3pLU+zvBPeNCeK5ovqDMjDuo1HD5KU209W5P7ik6mKfhW1Nc=&lt;/SignatureValue>&lt;KeyInfo>&lt;X509Data>&lt;X509Certificate>MIICGjCCAYOgAwIBAgIIBEWBazcqgAEwDQYJKoZIhvcNAQEFBQAwITEfMB0GA1UEAxMWcm9vdC5hZHNlcnYubDd0ZWNoLmNvbTAeFw0wNTA5MDgxNzEwMzZaFw0wNzA5MDgxNzIwMzZaMBwxGjAYBgNVBAMTEWFkc2Vydi5sN3RlY2guY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCMRHOqt7nE4oVHI0XcyvwuMeFpKpFrbW7iVHCLeGHZa7maZkiKIFknrulpjvaHoQX3MCPKHuC8k6YIKyJ4FrA3A/oYFoPMTLRM7fx2jcULW4pJx4RFO/OVp/kq2LuJXY1Qew2fSgNeMFICFk7S4NjCj5K5dvIIGN8wPgJKrb53RQIDAQABo2AwXjAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIF4DAdBgNVHQ4EFgQUtobzFvBU6T3u75XlCvMaNiz4huUwHwYDVR0jBBgwFoAU0vqLKpRxxFOKI99KF6PcUoMQwxwwDQYJKoZIhvcNAQEFBQADgYEAQJonP4VXVPpH5E9i03Z5ZrhfdT0sq32pET9I7pQtAJWkbMxHcr4P/TZpcjFcgxrLoc8H2P3OknY+8YN3BeNv/mb/dfJfEEmI26sdHcxSCM0Q++KcMcfMSdE+Bmfr4rX2AfN6ZyKlHWJ4IMqwmzxsQGKoca+xXhRYA6uW4e87m8E=&lt;/X509Certificate>&lt;/X509Data>&lt;/KeyInfo>&lt;/Signature>&lt;/saml:Assertion>&lt;/wst:RequestedSecurityToken>&lt;wsp:AppliesTo xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2004/09/policy&quot;>&lt;wsa:EndpointReference xmlns:wsa=&quot;http://schemas.xmlsoap.org/ws/2004/08/addressing&quot;>&lt;wsa:Address>https://fedserv.l7tech.com/&lt;/wsa:Address>&lt;/wsa:EndpointReference>&lt;/wsp:AppliesTo>&lt;/wst:RequestSecurityTokenResponse>\"/>\n" +
            "<noscript>\n" +
            "        <p>Script is disabled. Please click Submit to continue.</p>\n" +
            "        <input type=\"submit\" value=\"Submit\"/>\n" +
            "</noscript>\n" +
            "</form>\n" +
            "<script language=\"javascript\">window.setTimeout('document.forms[0].submit()', 0);</script>\n" +
            "</body>\n" +
            "</html>";

}
