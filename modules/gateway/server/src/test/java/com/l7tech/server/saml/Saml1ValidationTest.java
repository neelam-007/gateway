/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.saml;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.processor.MockProcessorResult;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.policy.assertion.xmlsec.SamlAssertionValidate;
import com.l7tech.test.BugNumber;
import com.l7tech.xml.saml.SamlAssertion;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Validation tests for SAML 1.1
 */
public class Saml1ValidationTest {

    @BugNumber(9090)
    @Test
    public void testContextVariableSupport() throws Exception{
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(BUG_9090_ASSERTION);
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequireWssSaml templateSaml = new RequireWssSaml();
        templateSaml.setVersion(1);
        templateSaml.setCheckAssertionValidity(false);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_BEARER});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(false);
        templateSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS);

        //SAML 1.1 requires a single statement
        SamlAuthenticationStatement statement = new SamlAuthenticationStatement();
        statement.setAuthenticationMethods(new String []{SamlConstants.PASSWORD_AUTHENTICATION});
        templateSaml.setAuthenticationStatement(statement);

        String audienceValue = "http://restricted.audience.com/";
        String audience = "audience";
        templateSaml.setAudienceRestriction("${" + audience + "}");

        String nameQualifierValue = "NameQualifier1";
        String nameQualifier = "namequalifier";
        templateSaml.setNameQualifier("${" + nameQualifier + "}");

        // validate
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();

        Map<String, Object> serverVariables = new HashMap<String, Object>();
        serverVariables.put(audience, audienceValue);
        serverVariables.put(nameQualifier, nameQualifierValue);

        sav.validate(assertionDocument, null, fakeProcessorResults(assertion), results, null, null, serverVariables, null);

        boolean foundError = false;
        for ( final Object result : results ) {
            String errorMessage = result.toString();
            System.out.println( errorMessage );
            if ( "SAML Constraint Error: Unsigned SAML assertion found in security Header".equals( errorMessage ) )
                foundError = true;
        }

        // check only unsigned assertion error
        assertTrue("Should be no errors.", results.size()==1 && foundError);
    }

    private ProcessorResult fakeProcessorResults(final SamlAssertion assertion) {
        return new MockProcessorResult() {
            @Override
            public XmlSecurityToken[] getXmlSecurityTokens() {
                return new XmlSecurityToken[]{assertion};
            }
        };
    }

    private static final String BUG_9090_ASSERTION =
            "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" MinorVersion=\"1\" MajorVersion=\"1\"\n" +
            "                AssertionID=\"SamlAssertion-6352074431814efc5914d9c787f1f9c8\" Issuer=\"irishman.l7tech.com\"\n" +
            "                IssueInstant=\"2010-08-30T23:01:00.135Z\">\n" +
            "    <saml:Conditions NotBefore=\"2010-08-30T22:56:00.000Z\" NotOnOrAfter=\"2010-08-30T23:06:00.135Z\">\n" +
            "        <saml:AudienceRestrictionCondition>\n" +
            "            <saml:Audience>http://restricted.audience.com/</saml:Audience>\n" +
            "        </saml:AudienceRestrictionCondition>\n" +
            "    </saml:Conditions>\n" +
            "    <saml:AuthenticationStatement AuthenticationMethod=\"urn:oasis:names:tc:SAML:1.0:am:password\"\n" +
            "                                  AuthenticationInstant=\"2010-08-30T23:01:00.135Z\">\n" +
            "        <saml:Subject>\n" +
            "            <saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\"\n" +
            "                                 NameQualifier=\"NameQualifier1\">admin\n" +
            "            </saml:NameIdentifier>\n" +
            "            <saml:SubjectConfirmation>\n" +
            "                <saml:ConfirmationMethod>urn:oasis:names:tc:SAML:1.0:cm:bearer</saml:ConfirmationMethod>\n" +
            "            </saml:SubjectConfirmation>\n" +
            "        </saml:Subject>\n" +
            "        <saml:SubjectLocality IPAddress=\"10.7.48.153\"/>\n" +
            "    </saml:AuthenticationStatement>\n" +
            "    <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "        <ds:SignedInfo>\n" +
            "            <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
            "            <ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>\n" +
            "            <ds:Reference URI=\"#SamlAssertion-6352074431814efc5914d9c787f1f9c8\">\n" +
            "                <ds:Transforms>\n" +
            "                    <ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>\n" +
            "                    <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
            "                </ds:Transforms>\n" +
            "                <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>\n" +
            "                <ds:DigestValue>i5gQJ83qzP5rFAoaszmpyMEm3BQ=</ds:DigestValue>\n" +
            "            </ds:Reference>\n" +
            "        </ds:SignedInfo>\n" +
            "        <ds:SignatureValue>\n" +
            "            YbIeJSfYr7pUFDdlbfbOevA+8VUwbVfKFCcJ6TExRC+JFyybcudgTeZ22N/01PvZh9FnhHpd7ymfPw5U/mHjJ6Txn2MxuCzcH0L/3ifEb2VRUc7egHxKBtLbJzVlNfobBHp2HFFx2bJEqHNrQs0azIBcmFVFIqMygz9uKuBOiyqLfBTQxJ+bPLrD+9iuQqs1INRLUQjw+Agjzg3DcJ4y37OA1ex9X8VgMC3p0GcsmobBnRGbHQJXCDnQea4unMyTeq668NSQip/c8VdH3ZJF5Y2U80mP+pnqOBwJwln5UdvoqjdjodZV0Rrrq8q3vKhW3KINiB4K35Nv+/t4RVXmZA==\n" +
            "        </ds:SignatureValue>\n" +
            "        <KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "            <X509Data>\n" +
            "                <X509SubjectName>CN=irishman.l7tech.com</X509SubjectName>\n" +
            "                <X509Certificate>\n" +
            "                    MIIDATCCAemgAwIBAgIJAMiP24ASqS/zMA0GCSqGSIb3DQEBDAUAMB4xHDAaBgNVBAMTE2lyaXNobWFuLmw3dGVjaC5jb20wHhcNMTAwODA0MjM1NjUwWhcNMjAwODAxMjM1NjUwWjAeMRwwGgYDVQQDExNpcmlzaG1hbi5sN3RlY2guY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiqCJIdpAxwfs76PXgtT4vLeOAzV9sVSNRcobyoKNlK3kuexQRoT44FtXXCSvZhdMD6E5G5r7Ma/TrB6amYMtwHtkbpHT3zZXtW3nORCJsgmTa3IFvgAR9pvhthc12h9O37aJHjgqDtRyHL6ScTaVX5Vy+pXW/C9SzoI5VcqWFguHN6Sut6PrM2C4xbvxUaJStHk6D21M2RbOI0px3fteZc43geQf3pgSIKHEe2Qiw3EOGxkp5l7RnQMrlyrkGMe2wJ1G/a+rCHyuwqzYyucBZI0So3Z5Aa61VdZGRyQw0Gn1x7DzPao/T2k0AAlJy28ZhJO1PecckpQQKnUuscxwJwIDAQABo0IwQDAdBgNVHQ4EFgQUzu7Yxcegnns0ndmpYgtanlee/BUwHwYDVR0jBBgwFoAUzu7Yxcegnns0ndmpYgtanlee/BUwDQYJKoZIhvcNAQEMBQADggEBADXDxqtZ0helOFNkTrCF11MFVFXlECHp8H2S0PSXs7sO/FPLqJUUsehuqo4OoFo1Fmdqq+aCZNB46REGquc9mQYp4opIRfy2xKqsbgpIjLzzKE1Dw687/Fllp81Vzi6Sp/UuBzPStQvni54QscLSfuk43b/v2UWshSssEPGS7/XP+MvOjwM5xSVKNeBzPyC9Urq+pDiTOVLmanzOTrLew8TfxRRXkD7+e7z4S6UIkaXHBoEcBzoiDav5vHgH2yS5oZ7FgO5KEKZNK6pHIex7SdHGFSmVBT91YiTWEdhH+3O1oR5Zy/Oh67t1a9I3DzzL48OZJIXTExaTCcj3HblupgE=\n" +
            "                </X509Certificate>\n" +
            "            </X509Data>\n" +
            "        </KeyInfo>\n" +
            "    </ds:Signature>\n" +
            "</saml:Assertion>";

}
