package com.l7tech.xml.saml;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.cert.TestKeysLoader;
import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.test.BugNumber;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test case for SAML 2.0 parser.
 */
public class SamlAssertionV2Test {
    @Test
    @BugNumber(9758)
    public void testSaml2SecretKeySubjectConfirmation() throws Exception {
        // Load private keys we'll need to process the trace messages
        SignerInfo[] privateKeys = TestKeysLoader.loadPrivateKeys(TestDocuments.DIR + "wcf_unum/", ".p12", "password".toCharArray(),
                "bookstoreservice_com", "bookstorests_com");
        final SimpleSecurityTokenResolver resolver = new SimpleSecurityTokenResolver(null, privateKeys);

        SamlAssertion ass = SamlAssertion.newInstance(XmlUtil.stringAsDocument(SAML2_ATTR_STMNT_WITH_SECRET_KEY_HOK).getDocumentElement(), resolver);
        assertTrue(ass.hasSubjectConfirmationEncryptedKey());
        EncryptedKey encryptedKey = ass.getSubjectConfirmationEncryptedKey(resolver, null);
        assertNotNull(encryptedKey);
        assertFalse("Key not yet unwrapped", encryptedKey.isUnwrapped());
        assertNotNull(encryptedKey.getSecretKey());
        assertTrue("Key is now unwrapped", encryptedKey.isUnwrapped());
        assertFalse("Possession proved not flagged (secret key has not yet been used to verify any signatures)", encryptedKey.isPossessionProved());
    }

    private static final String SAML2_ATTR_STMNT_WITH_SECRET_KEY_HOK =
            "<saml2:Assertion ID=\"SamlAssertion-95b1fc5bba94046b19364471bac978fb\" IssueInstant=\"2011-02-02T19:23:31.395Z\" Version=\"2.0\" " +
                    "xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Issuer>data.l7tech.local</saml2:Issuer><saml2:Subject>" +
                    "<saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\" NameQualifier=\"\">CN=BookStoreService.com</saml2:NameID>" +
                    "<saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:holder-of-key\"><saml2:SubjectConfirmationData " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"saml2:KeyInfoConfirmationDataType\"><KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">" +
                    "<e:EncryptedKey xmlns:e=\"http://www.w3.org/2001/04/xmlenc#\"><e:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p\">" +
                    "<DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/></e:EncryptionMethod><KeyInfo><o:SecurityTokenReference " +
                    "xmlns:o=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><o:KeyIdentifier " +
                    "ValueType=\"http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#ThumbprintSHA1\">vSFYjYb3aqlFvB4lyDK+newe/zc=</o:KeyIdentifier>" +
                    "</o:SecurityTokenReference></KeyInfo><e:CipherData>" +
                    "<e:CipherValue>ZoI6oalm3cWbpEWSb+iRB3dhlnbL0oZjKuKcqM/XrYDvQ9OmEEZlb4oLDWBgQcLvycyP0oQHIGfdXE73F3MCHWSQ4a4InRf4HzJQhtCFUHC1SlG1vrk3dQoBRCdqzDTNTfAWRoC1OyHm9J2W1FG+jHruuZX/L+2BGDAfJ662aF4=</e:CipherValue>" +
                    "</e:CipherData></e:EncryptedKey></KeyInfo></saml2:SubjectConfirmationData></saml2:SubjectConfirmation></saml2:Subject>" +
                    "<saml2:Conditions NotBefore=\"2011-02-02T19:21:31.000Z\" NotOnOrAfter=\"2011-02-02T19:28:31.396Z\"/><saml2:AttributeStatement>" +
                    "<saml2:Attribute Name=\"asdf\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue>qwer</saml2:AttributeValue>" +
                    "</saml2:Attribute></saml2:AttributeStatement><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo>" +
                    "<ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>" +
                    "<ds:Reference URI=\"#SamlAssertion-95b1fc5bba94046b19364471bac978fb\"><ds:Transforms><ds:Transform " +
                    "Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>" +
                    "</ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>A7L80z6ZZhZ/8Mkem7HNLUpZUrc=</ds:DigestValue>" +
                    "</ds:Reference></ds:SignedInfo><ds:SignatureValue>WnUncvbTYXe1N41MSTRvKRM6lcyZ4bBk/XIH4KrlFdnbVp7AgkLx9IYE+oPD5cHPIS7gWXkLidioXPUIQTyzKD7Moz7MW9ZaQGg/JR7s+60wkcFNMPJ0ziLT2iHnG0SG1186VeG7X4O1OKc5uY3afHeoHRuuagJ1yVy0Lt4JGE8=</ds:SignatureValue>" +
                    "<ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIICMDCCAZmgAwIBAgIJAOo890gqoCqcMA0GCSqGSIb3DQEBBQUAMBsxGTAXBgNVBAMTEEJvb2tTdG9yZVNUUy5jb20wHhcNMTAwMjE4MTkyODE2WhcNMTEwMjE4MTkyODE2WjAbMRkwFwYDVQQDExBCb29rU3RvcmVTVFMuY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCw6xJK+YN68u74SdcEaSaU0h65LJ5zx7C47ayJKk0nRMA+mNl12k1754ha4EGxjg9vItKxTHLu5+dPR+9aqHVFaaMQr8tTZMIT+iI4ZQColamnhYNtfXIOu6dOcQaxo5R8WmCEp9XVYLaF/sskIU3EYGNeSYo6pa9ElMq253bS6wIDAQABo3wwejAdBgNVHQ4EFgQU/pRAnjUTFkXAtGjydw+YF8vDyZ4wSwYDVR0jBEQwQoAU/pRAnjUTFkXAtGjydw+YF8vDyZ6hH6QdMBsxGTAXBgNVBAMTEEJvb2tTdG9yZVNUUy5jb22CCQDqPPdIKqAqnDAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBQUAA4GBAAEXmkOtbVOjS3ZhzU8Fg/X2BV3OJpFmjLMZwIKJ+dr8/c1t0VHfWuw52pPRXrTiu73ZWZDoMmpANpNPM7nJZ5WW1DXzavN2yKrMTMh2ZXoRO0kgxMOXEOzyVj8aJn/KZHgGjMBasTf5qnGbp5NhDcF7cx9fV3R5BJTBebmawBVJ</ds:X509Certificate>" +
                    "</ds:X509Data></ds:KeyInfo></ds:Signature></saml2:Assertion>";
}
