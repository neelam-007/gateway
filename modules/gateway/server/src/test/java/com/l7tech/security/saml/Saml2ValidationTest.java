package com.l7tech.security.saml;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssTimestamp;
import com.l7tech.security.xml.SecurityActor;
import com.l7tech.security.token.EncryptedElement;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.token.SignedPart;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.policy.assertion.xmlsec.SamlAssertionValidate;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Saml 2.0 assertion validation tests.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class Saml2ValidationTest extends TestCase {

    /**
     *
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(Saml2ValidationTest.class);
        return suite;
    }

    /**
     *
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(Saml2ValidationTest.suite());
    }

    /**
     * TEST that the assertion expiry time is validated.
     */
    public void testExpiryValidation() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_EXPIRED);
        System.out.println("Testing expired assertion: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequestWssSaml templateSaml = new RequestWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(true);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(true);
        templateSaml.setNameFormats( SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);

        // validate
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List results = new ArrayList();
        sav.validate(assertionDocument, null, fakeProcessorResults(assertion), results);

        // check for error message
        String expectedErrorStart = "SAML Constraint Error: SAML ticket has expired as of:";
        String expectedErrorEnd = "";
        boolean foundError = false;
        for (Iterator iterator = results.iterator(); iterator.hasNext();) {
            String errorStr = iterator.next().toString();
            System.out.println(errorStr);
            if (errorStr.startsWith(expectedErrorStart) &&
                errorStr.endsWith(expectedErrorEnd))
                foundError = true;
        }

        if (!foundError)
            fail("Expected to find error message '" + expectedErrorStart + " ... " + expectedErrorEnd + "'.");
    }

    /**
     * TEST that the audience restriction is validated.
     */
    public void testAudienceRestrictionValidation() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_AUDIENCE_RESTR);
        System.out.println("Testing audience restriction validation: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequestWssSaml templateSaml = new RequestWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(false);
        templateSaml.setAudienceRestriction("http://restricted.audience.com/");
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(true);
        templateSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);

        // validate
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List results = new ArrayList();
        sav.validate(assertionDocument, null, fakeProcessorResults(assertion), results);

        // check for error message
        String expectedError = "SAML Constraint Error: Audience Restriction Check Failed";
        boolean foundError = false;
        for (Iterator iterator = results.iterator(); iterator.hasNext();) {
            String errorStr = iterator.next().toString();
            System.out.println(errorStr);
            if (expectedError.equals(errorStr))
                foundError = true;
        }

        if (!foundError)
            fail("Expected to find error message '" + expectedError + "'.");
    }

    /**
     * TEST that assertions with no statements are permitted
     */
    public void testSubjectOnly() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_SUBJECT_ONLY);
        System.out.println("Testing assertion with subject only: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequestWssSaml templateSaml = new RequestWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(false);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(false);
        templateSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);

        // validate
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List results = new ArrayList();
        sav.validate(assertionDocument, null, fakeProcessorResults(assertion), results);

        boolean foundUnsignedError = false;
        for (Iterator iterator = results.iterator(); iterator.hasNext();) {
            String errorMessage = iterator.next().toString();
            System.out.println(errorMessage);
            if ("SAML Constraint Error: Unsigned SAML assertion found in security Header".equals(errorMessage))
                foundUnsignedError = true;
        }

        // check only unsigned assertion error
        assertTrue("Should be no errors.", results.size()==1 && foundUnsignedError);
    }

    /**
     * TEST that assertions with repeated OneTimeUse conditions are rejected
     */
    public void testMultipleOneTimeOnly() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_MULTIPLE_ONE_TIME_CONDS);
        System.out.println("Testing assertion repeated OneTimeUse conditions: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequestWssSaml templateSaml = new RequestWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(false);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(false);
        templateSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);

        // validate
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List results = new ArrayList();
        sav.validate(assertionDocument, null, fakeProcessorResults(assertion), results);

        boolean foundError = false;
        String expectedError = "SAML Constraint Error: Multiple OneTimeUse conditions are not permitted.";
        for (Iterator iterator = results.iterator(); iterator.hasNext();) {
            String errorMessage = iterator.next().toString();
            System.out.println(errorMessage);
            if (expectedError.equals(errorMessage))
                foundError = true;
        }

        // check only unsigned assertion error
        assertTrue("Expected validation error not found '"+expectedError+"'.", foundError);
    }

    /**
     * TEST authentication statement validation
     */
    public void testAuthenticationStatementValidation() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_AUTHN);
        System.out.println("Testing authentication statement assertion: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequestWssSaml templateSaml = new RequestWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(false);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(false);
        templateSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);

        SamlAuthenticationStatement sas = new SamlAuthenticationStatement();
        sas.setAuthenticationMethods(new String[]{SamlConstants.XML_DSIG_AUTHENTICATION});
        templateSaml.setAuthenticationStatement(sas);

        // validate
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List results = new ArrayList();
        sav.validate(assertionDocument, null, fakeProcessorResults(assertion), results);

        boolean foundError = false;
        for (Iterator iterator = results.iterator(); iterator.hasNext();) {
            String errorMessage = iterator.next().toString();
            System.out.println(errorMessage);
            if ("SAML Constraint Error: Unsigned SAML assertion found in security Header".equals(errorMessage))
                foundError = true;
        }

        // check only unsigned assertion error
        assertTrue("Should be no errors.", results.size()==1 && foundError);

        // now check for failure
        sas.setAuthenticationMethods(new String[]{SamlConstants.UNSPECIFIED_AUTHENTICATION});

        // validate 2
        SamlAssertionValidate sav2 = new SamlAssertionValidate(templateSaml);
        List results2 = new ArrayList();
        sav.validate(assertionDocument, null, fakeProcessorResults(assertion), results2);

        boolean foundError2 = false;
        boolean methodError = false;
        for (Iterator iterator = results2.iterator(); iterator.hasNext();) {
            String errorMessage = iterator.next().toString();
            System.out.println(errorMessage);

            if ("SAML Constraint Error: Unsigned SAML assertion found in security Header".equals(errorMessage))
                foundError2 = true;

            if ("SAML Constraint Error: Authentication method not matched expected/received: urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified/urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig".equals(errorMessage))
                methodError = true;
        }

        // check only unsigned assertion error and method error
        assertTrue("Should be a method error.", results2.size()==2 && foundError2 && methodError);

    }

    /**
     * TEST attribute statement validation
     */
    public void testAttributeStatementValidation() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_ATTR);
        System.out.println("Testing attribute statement assertion: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequestWssSaml templateSaml = new RequestWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(false);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(false);
        templateSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);

        SamlAttributeStatement sas = new SamlAttributeStatement();
        sas.setAttributes(new SamlAttributeStatement.Attribute[]{new SamlAttributeStatement.Attribute("Flag", null, "", "", false, false)});
        templateSaml.setAttributeStatement(sas);

        // validate
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List results = new ArrayList();
        sav.validate(assertionDocument, null, fakeProcessorResults(assertion), results);

        boolean foundError = false;
        for (Iterator iterator = results.iterator(); iterator.hasNext();) {
            String errorMessage = iterator.next().toString();
            System.out.println(errorMessage);
            if ("SAML Constraint Error: Unsigned SAML assertion found in security Header".equals(errorMessage))
                foundError = true;
        }

        // check only unsigned assertion error
        assertTrue("Should be no errors.", results.size()==1 && foundError);
    }

    /**
     * TEST authorization statement validation
     */
    public void testAuthorizationDecisionStatementValidation() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_AUTHORIZATION);
        System.out.println("Testing authorization decision statement assertion: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequestWssSaml templateSaml = new RequestWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(false);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(false);
        templateSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);

        SamlAuthorizationStatement sas = new SamlAuthorizationStatement();
        sas.setAction("Get");
        sas.setResource("http://someresource.org");
        templateSaml.setAuthorizationStatement(sas);

        // validate
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List results = new ArrayList();
        sav.validate(assertionDocument, null, fakeProcessorResults(assertion), results);

        boolean foundError = false;
        for (Iterator iterator = results.iterator(); iterator.hasNext();) {
            String errorMessage = iterator.next().toString();
            System.out.println(errorMessage);
            if ("SAML Constraint Error: Unsigned SAML assertion found in security Header".equals(errorMessage))
                foundError = true;
        }

        // check only unsigned assertion error
        assertTrue("Should be no errors.", results.size()==1 && foundError);
    }


    private ProcessorResult fakeProcessorResults(final SamlAssertion assertion) {
        return new ProcessorResult() {
            public EncryptedElement[] getElementsThatWereEncrypted() { return new EncryptedElement[0];}
            public SignedElement[] getElementsThatWereSigned() { return new SignedElement[0]; }
            public SignedPart[] getPartsThatWereSigned() { return new SignedPart[0]; }
            public String getLastSignatureConfirmation() { return null; }
            public String getLastKeyEncryptionAlgorithm() { return null; }
            public String getLastSignatureValue() { return null; }
            public SecurityActor getProcessedActor() { return null; }
            public String getSecurityNS() { return SoapUtil.SECURITY_NAMESPACE; }
            public SigningSecurityToken[] getSigningTokens(Element element) { return new SigningSecurityToken[0]; }
            public WssTimestamp getTimestamp() { return null; }
            public String getWSUNS() { return null; }
            public boolean isDerivedKeySeen() { return false; }
            public boolean isWsse11Seen() { return false; }
            public XmlSecurityToken[] getXmlSecurityTokens() {
                return new XmlSecurityToken[]{assertion};
            }
        };
    }

    private static final String ASSERTION_STR_EXPIRED =
            "<saml:Assertion\n" +
            "                ID=\"BinarySecurityToken-0-7e8a1451b089b9c30c92ccd3e702494b\"\n" +
            "                IssueInstant=\"2006-06-27T23:15:30.040Z\" Version=\"2.0\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "                <saml:Issuer>Service Provider</saml:Issuer>\n" +
            "                <saml:Subject>\n" +
            "                    <saml:NameID\n" +
            "                        Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\">CN=John\n" +
            "                        Smith, OU=Java Technology Center, O=IBM,\n" +
            "                        L=Cupertino, ST=California, C=US</saml:NameID>\n" +
            "                    <saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:holder-of-key\">\n" +
            "                        <saml:SubjectConfirmationData\n" +
            "                            xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "                            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"urn:KeyInfoConfirmationDataType\">\n" +
            "                            <dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "                                <dsig:X509Data>\n" +
            "                                    <dsig:X509Certificate>MIICZjCCAc8CBD2+61QwDQYJKoZIhvcNAQEEBQAwejELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJNMR8wHQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNtaXRoMB4XDTAyMTAyOTIwMTEwMFoXDTA3MTAwMzIwMTEwMFowejELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJNMR8wHQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNtaXRoMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCtitis5FONvv236Xw1CKOvKAOQ0NYlvRd/FKwuf+T1XCFadHMrtvHhq/+Z/Dlcn2YQYOCp9auS+WkBcL0AUrJJPUbwZIB2CyBZJjnS7+jdb37RKYQUsNRNlgdIcoZM8bvCZldBSfnat4xDPyQOJB7ExDrMmI9tP0NY9GN0npfnwwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAFLNrEP8Y0xwUVIl4XigEiDM6jAdDJFCI+m8EA07nAsYWmV/Ic8kkqDzXaWyLkIBJQ0gElRlWHYe+W/K/pT9CNEWRFViKbZGevivBKek7GQXhuEgo+pWalpEtg4nA741+46iKeKpEQILL6OYEj7aHcreIxaQ1WH2v1iMig33Q0+S</dsig:X509Certificate>\n" +
            "                                </dsig:X509Data>\n" +
            "                            </dsig:KeyInfo>\n" +
            "                        </saml:SubjectConfirmationData>\n" +
            "                    </saml:SubjectConfirmation>\n" +
            "                </saml:Subject>\n" +
            "                <saml:Conditions NotBefore=\"2006-06-27T20:15:00.000Z\" NotOnOrAfter=\"2006-06-27T20:20:00.000Z\"/>\n" +
            "                <saml:AuthnStatement>\n" +
            "                    <saml:SubjectLocality Address=\"192.168.1.192\" DNSName=\"fish.l7tech.com\"/>\n" +
            "                    <saml:AuthnContext>\n" +
            "                        <saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig</saml:AuthnContextClassRef>\n" +
            "                        <saml:AuthnContextDecl\n" +
            "                            xmlns:saccxds=\"urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig\"\n" +
            "                            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"urn:AuthnContextDeclarationBaseType\">\n" +
            "                            <saccxds:AuthnMethod>\n" +
            "                                <saccxds:Authenticator>\n" +
            "                                    <saccxds:DigSig keyValidation=\"urn:oasis:names:tc:SAML:2.0:ac:classes:X509\"/>\n" +
            "                                </saccxds:Authenticator>\n" +
            "                            </saccxds:AuthnMethod>\n" +
            "                        </saml:AuthnContextDecl>\n" +
            "                    </saml:AuthnContext>\n" +
            "                </saml:AuthnStatement>\n" +
            "            </saml:Assertion>";

    private static final String ASSERTION_STR_AUDIENCE_RESTR =
            "<saml:Assertion\n" +
            "                ID=\"BinarySecurityToken-0-7e8a1451b089b9c30c92ccd3e702494b\"\n" +
            "                IssueInstant=\"2006-06-27T23:15:30.040Z\" Version=\"2.0\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "                <saml:Issuer>Service Provider</saml:Issuer>\n" +
            "                <saml:Subject>\n" +
            "                    <saml:NameID\n" +
            "                        Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\">CN=John\n" +
            "                        Smith, OU=Java Technology Center, O=IBM,\n" +
            "                        L=Cupertino, ST=California, C=US</saml:NameID>\n" +
            "                    <saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:holder-of-key\">\n" +
            "                        <saml:SubjectConfirmationData\n" +
            "                            xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "                            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"urn:KeyInfoConfirmationDataType\">\n" +
            "                            <dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "                                <dsig:X509Data>\n" +
            "                                    <dsig:X509Certificate>MIICZjCCAc8CBD2+61QwDQYJKoZIhvcNAQEEBQAwejELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJNMR8wHQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNtaXRoMB4XDTAyMTAyOTIwMTEwMFoXDTA3MTAwMzIwMTEwMFowejELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJNMR8wHQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNtaXRoMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCtitis5FONvv236Xw1CKOvKAOQ0NYlvRd/FKwuf+T1XCFadHMrtvHhq/+Z/Dlcn2YQYOCp9auS+WkBcL0AUrJJPUbwZIB2CyBZJjnS7+jdb37RKYQUsNRNlgdIcoZM8bvCZldBSfnat4xDPyQOJB7ExDrMmI9tP0NY9GN0npfnwwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAFLNrEP8Y0xwUVIl4XigEiDM6jAdDJFCI+m8EA07nAsYWmV/Ic8kkqDzXaWyLkIBJQ0gElRlWHYe+W/K/pT9CNEWRFViKbZGevivBKek7GQXhuEgo+pWalpEtg4nA741+46iKeKpEQILL6OYEj7aHcreIxaQ1WH2v1iMig33Q0+S</dsig:X509Certificate>\n" +
            "                                </dsig:X509Data>\n" +
            "                            </dsig:KeyInfo>\n" +
            "                        </saml:SubjectConfirmationData>\n" +
            "                    </saml:SubjectConfirmation>\n" +
            "                </saml:Subject>\n" +
            "                <saml:Conditions>\n" +
            "                    <saml:AudienceRestriction>\n" +
            "                        <saml:Audience>http://other.audience.com/</saml:Audience>\n" +
            "                        <saml:Audience>http://restricted.audience.com/</saml:Audience>\n" +
            "                    </saml:AudienceRestriction>\n" +
            "                    <saml:AudienceRestriction>\n" +
            "                        <saml:Audience>http://more.audience.com/</saml:Audience>\n" +
            "                    </saml:AudienceRestriction>\n" +
            "                </saml:Conditions>\n" +
            "                <saml:AuthnStatement>\n" +
            "                    <saml:SubjectLocality Address=\"192.168.1.192\" DNSName=\"fish.l7tech.com\"/>\n" +
            "                    <saml:AuthnContext>\n" +
            "                        <saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig</saml:AuthnContextClassRef>\n" +
            "                        <saml:AuthnContextDecl\n" +
            "                            xmlns:saccxds=\"urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig\"\n" +
            "                            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"urn:AuthnContextDeclarationBaseType\">\n" +
            "                            <saccxds:AuthnMethod>\n" +
            "                                <saccxds:Authenticator>\n" +
            "                                    <saccxds:DigSig keyValidation=\"urn:oasis:names:tc:SAML:2.0:ac:classes:X509\"/>\n" +
            "                                </saccxds:Authenticator>\n" +
            "                            </saccxds:AuthnMethod>\n" +
            "                        </saml:AuthnContextDecl>\n" +
            "                    </saml:AuthnContext>\n" +
            "                </saml:AuthnStatement>\n" +
            "            </saml:Assertion>";

    private static final String ASSERTION_STR_SUBJECT_ONLY =
            "<saml:Assertion\n" +
            "                ID=\"BinarySecurityToken-0-7e8a1451b089b9c30c92ccd3e702494b\"\n" +
            "                IssueInstant=\"2006-06-27T23:15:30.040Z\" Version=\"2.0\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "                <saml:Issuer>Service Provider</saml:Issuer>\n" +
            "                <saml:Subject>\n" +
            "                    <saml:NameID\n" +
            "                        Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\">CN=John\n" +
            "                        Smith, OU=Java Technology Center, O=IBM,\n" +
            "                        L=Cupertino, ST=California, C=US</saml:NameID>\n" +
            "                    <saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:holder-of-key\">\n" +
            "                        <saml:SubjectConfirmationData\n" +
            "                            xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "                            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"urn:KeyInfoConfirmationDataType\">\n" +
            "                            <dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "                                <dsig:X509Data>\n" +
            "                                    <dsig:X509Certificate>MIICZjCCAc8CBD2+61QwDQYJKoZIhvcNAQEEBQAwejELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJNMR8wHQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNtaXRoMB4XDTAyMTAyOTIwMTEwMFoXDTA3MTAwMzIwMTEwMFowejELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJNMR8wHQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNtaXRoMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCtitis5FONvv236Xw1CKOvKAOQ0NYlvRd/FKwuf+T1XCFadHMrtvHhq/+Z/Dlcn2YQYOCp9auS+WkBcL0AUrJJPUbwZIB2CyBZJjnS7+jdb37RKYQUsNRNlgdIcoZM8bvCZldBSfnat4xDPyQOJB7ExDrMmI9tP0NY9GN0npfnwwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAFLNrEP8Y0xwUVIl4XigEiDM6jAdDJFCI+m8EA07nAsYWmV/Ic8kkqDzXaWyLkIBJQ0gElRlWHYe+W/K/pT9CNEWRFViKbZGevivBKek7GQXhuEgo+pWalpEtg4nA741+46iKeKpEQILL6OYEj7aHcreIxaQ1WH2v1iMig33Q0+S</dsig:X509Certificate>\n" +
            "                                </dsig:X509Data>\n" +
            "                            </dsig:KeyInfo>\n" +
            "                        </saml:SubjectConfirmationData>\n" +
            "                    </saml:SubjectConfirmation>\n" +
            "                </saml:Subject>\n" +
            "            </saml:Assertion>";

    private static final String ASSERTION_STR_MULTIPLE_ONE_TIME_CONDS =
            "<saml:Assertion\n" +
            "                ID=\"BinarySecurityToken-0-7e8a1451b089b9c30c92ccd3e702494b\"\n" +
            "                IssueInstant=\"2006-06-27T23:15:30.040Z\" Version=\"2.0\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "                <saml:Issuer>Service Provider</saml:Issuer>\n" +
            "                <saml:Subject>\n" +
            "                    <saml:NameID\n" +
            "                        Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\">CN=John\n" +
            "                        Smith, OU=Java Technology Center, O=IBM,\n" +
            "                        L=Cupertino, ST=California, C=US</saml:NameID>\n" +
            "                    <saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:holder-of-key\">\n" +
            "                        <saml:SubjectConfirmationData\n" +
            "                            xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "                            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"urn:KeyInfoConfirmationDataType\">\n" +
            "                            <dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "                                <dsig:X509Data>\n" +
            "                                    <dsig:X509Certificate>MIICZjCCAc8CBD2+61QwDQYJKoZIhvcNAQEEBQAwejELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJNMR8wHQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNtaXRoMB4XDTAyMTAyOTIwMTEwMFoXDTA3MTAwMzIwMTEwMFowejELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJNMR8wHQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNtaXRoMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCtitis5FONvv236Xw1CKOvKAOQ0NYlvRd/FKwuf+T1XCFadHMrtvHhq/+Z/Dlcn2YQYOCp9auS+WkBcL0AUrJJPUbwZIB2CyBZJjnS7+jdb37RKYQUsNRNlgdIcoZM8bvCZldBSfnat4xDPyQOJB7ExDrMmI9tP0NY9GN0npfnwwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAFLNrEP8Y0xwUVIl4XigEiDM6jAdDJFCI+m8EA07nAsYWmV/Ic8kkqDzXaWyLkIBJQ0gElRlWHYe+W/K/pT9CNEWRFViKbZGevivBKek7GQXhuEgo+pWalpEtg4nA741+46iKeKpEQILL6OYEj7aHcreIxaQ1WH2v1iMig33Q0+S</dsig:X509Certificate>\n" +
            "                                </dsig:X509Data>\n" +
            "                            </dsig:KeyInfo>\n" +
            "                        </saml:SubjectConfirmationData>\n" +
            "                    </saml:SubjectConfirmation>\n" +
            "                </saml:Subject>\n" +
            "                <saml:Conditions>\n" +
            "                    <saml:OneTimeUse/>\n" +
            "                    <saml:OneTimeUse/>\n" +
            "                    <saml:OneTimeUse/>\n" +
            "                    <saml:OneTimeUse/>\n" +
            "                </saml:Conditions>\n" +
            "            </saml:Assertion>";

    private static final String ASSERTION_STR_AUTHN =
            "<saml:Assertion\n" +
            "                ID=\"BinarySecurityToken-0-7e8a1451b089b9c30c92ccd3e702494b\"\n" +
            "                IssueInstant=\"2006-06-27T23:15:30.040Z\" Version=\"2.0\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "                <saml:Issuer>Service Provider</saml:Issuer>\n" +
            "                <saml:Subject>\n" +
            "                    <saml:NameID\n" +
            "                        Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\">CN=John\n" +
            "                        Smith, OU=Java Technology Center, O=IBM,\n" +
            "                        L=Cupertino, ST=California, C=US</saml:NameID>\n" +
            "                    <saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:holder-of-key\">\n" +
            "                        <saml:SubjectConfirmationData\n" +
            "                            xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "                            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"urn:KeyInfoConfirmationDataType\">\n" +
            "                            <dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "                                <dsig:X509Data>\n" +
            "                                    <dsig:X509Certificate>MIICZjCCAc8CBD2+61QwDQYJKoZIhvcNAQEEBQAwejELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJNMR8wHQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNtaXRoMB4XDTAyMTAyOTIwMTEwMFoXDTA3MTAwMzIwMTEwMFowejELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJNMR8wHQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNtaXRoMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCtitis5FONvv236Xw1CKOvKAOQ0NYlvRd/FKwuf+T1XCFadHMrtvHhq/+Z/Dlcn2YQYOCp9auS+WkBcL0AUrJJPUbwZIB2CyBZJjnS7+jdb37RKYQUsNRNlgdIcoZM8bvCZldBSfnat4xDPyQOJB7ExDrMmI9tP0NY9GN0npfnwwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAFLNrEP8Y0xwUVIl4XigEiDM6jAdDJFCI+m8EA07nAsYWmV/Ic8kkqDzXaWyLkIBJQ0gElRlWHYe+W/K/pT9CNEWRFViKbZGevivBKek7GQXhuEgo+pWalpEtg4nA741+46iKeKpEQILL6OYEj7aHcreIxaQ1WH2v1iMig33Q0+S</dsig:X509Certificate>\n" +
            "                                </dsig:X509Data>\n" +
            "                            </dsig:KeyInfo>\n" +
            "                        </saml:SubjectConfirmationData>\n" +
            "                    </saml:SubjectConfirmation>\n" +
            "                </saml:Subject>\n" +
            "                <saml:AuthnStatement>\n" +
            "                    <saml:SubjectLocality Address=\"192.168.1.192\" DNSName=\"fish.l7tech.com\"/>\n" +
            "                    <saml:AuthnContext>\n" +
            "                        <saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig</saml:AuthnContextClassRef>\n" +
            "                        <saml:AuthnContextDecl\n" +
            "                            xmlns:saccxds=\"urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig\"\n" +
            "                            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"urn:AuthnContextDeclarationBaseType\">\n" +
            "                            <saccxds:AuthnMethod>\n" +
            "                                <saccxds:Authenticator>\n" +
            "                                    <saccxds:DigSig keyValidation=\"urn:oasis:names:tc:SAML:2.0:ac:classes:X509\"/>\n" +
            "                                </saccxds:Authenticator>\n" +
            "                            </saccxds:AuthnMethod>\n" +
            "                        </saml:AuthnContextDecl>\n" +
            "                    </saml:AuthnContext>\n" +
            "                </saml:AuthnStatement>\n" +
            "            </saml:Assertion>";

    private static final String ASSERTION_STR_ATTR =
            "<saml:Assertion\n" +
            "                ID=\"BinarySecurityToken-0-7e8a1451b089b9c30c92ccd3e702494b\"\n" +
            "                IssueInstant=\"2006-06-27T23:15:30.040Z\" Version=\"2.0\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "                <saml:Issuer>Service Provider</saml:Issuer>\n" +
            "                <saml:Subject>\n" +
            "                    <saml:NameID\n" +
            "                        Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\">CN=John\n" +
            "                        Smith, OU=Java Technology Center, O=IBM,\n" +
            "                        L=Cupertino, ST=California, C=US</saml:NameID>\n" +
            "                    <saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:holder-of-key\">\n" +
            "                        <saml:SubjectConfirmationData\n" +
            "                            xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "                            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"urn:KeyInfoConfirmationDataType\">\n" +
            "                            <dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "                                <dsig:X509Data>\n" +
            "                                    <dsig:X509Certificate>MIICZjCCAc8CBD2+61QwDQYJKoZIhvcNAQEEBQAwejELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJNMR8wHQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNtaXRoMB4XDTAyMTAyOTIwMTEwMFoXDTA3MTAwMzIwMTEwMFowejELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJNMR8wHQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNtaXRoMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCtitis5FONvv236Xw1CKOvKAOQ0NYlvRd/FKwuf+T1XCFadHMrtvHhq/+Z/Dlcn2YQYOCp9auS+WkBcL0AUrJJPUbwZIB2CyBZJjnS7+jdb37RKYQUsNRNlgdIcoZM8bvCZldBSfnat4xDPyQOJB7ExDrMmI9tP0NY9GN0npfnwwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAFLNrEP8Y0xwUVIl4XigEiDM6jAdDJFCI+m8EA07nAsYWmV/Ic8kkqDzXaWyLkIBJQ0gElRlWHYe+W/K/pT9CNEWRFViKbZGevivBKek7GQXhuEgo+pWalpEtg4nA741+46iKeKpEQILL6OYEj7aHcreIxaQ1WH2v1iMig33Q0+S</dsig:X509Certificate>\n" +
            "                                </dsig:X509Data>\n" +
            "                            </dsig:KeyInfo>\n" +
            "                        </saml:SubjectConfirmationData>\n" +
            "                    </saml:SubjectConfirmation>\n" +
            "                </saml:Subject>\n" +
            "                <saml:AttributeStatement>\n" +
            "                    <saml:Attribute Name=\"Flag\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"/>\n" +
            "                </saml:AttributeStatement>\n" +
            "            </saml:Assertion>";

    private static final String ASSERTION_STR_AUTHORIZATION =
            "<saml:Assertion\n" +
            "                ID=\"BinarySecurityToken-0-7e8a1451b089b9c30c92ccd3e702494b\"\n" +
            "                IssueInstant=\"2006-06-27T23:15:30.040Z\" Version=\"2.0\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "                <saml:Issuer>Service Provider</saml:Issuer>\n" +
            "                <saml:Subject>\n" +
            "                    <saml:NameID\n" +
            "                        Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\">CN=John\n" +
            "                        Smith, OU=Java Technology Center, O=IBM,\n" +
            "                        L=Cupertino, ST=California, C=US</saml:NameID>\n" +
            "                    <saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:holder-of-key\">\n" +
            "                        <saml:SubjectConfirmationData\n" +
            "                            xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "                            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"urn:KeyInfoConfirmationDataType\">\n" +
            "                            <dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "                                <dsig:X509Data>\n" +
            "                                    <dsig:X509Certificate>MIICZjCCAc8CBD2+61QwDQYJKoZIhvcNAQEEBQAwejELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJNMR8wHQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNtaXRoMB4XDTAyMTAyOTIwMTEwMFoXDTA3MTAwMzIwMTEwMFowejELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJNMR8wHQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNtaXRoMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCtitis5FONvv236Xw1CKOvKAOQ0NYlvRd/FKwuf+T1XCFadHMrtvHhq/+Z/Dlcn2YQYOCp9auS+WkBcL0AUrJJPUbwZIB2CyBZJjnS7+jdb37RKYQUsNRNlgdIcoZM8bvCZldBSfnat4xDPyQOJB7ExDrMmI9tP0NY9GN0npfnwwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAFLNrEP8Y0xwUVIl4XigEiDM6jAdDJFCI+m8EA07nAsYWmV/Ic8kkqDzXaWyLkIBJQ0gElRlWHYe+W/K/pT9CNEWRFViKbZGevivBKek7GQXhuEgo+pWalpEtg4nA741+46iKeKpEQILL6OYEj7aHcreIxaQ1WH2v1iMig33Q0+S</dsig:X509Certificate>\n" +
            "                                </dsig:X509Data>\n" +
            "                            </dsig:KeyInfo>\n" +
            "                        </saml:SubjectConfirmationData>\n" +
            "                    </saml:SubjectConfirmation>\n" +
            "                </saml:Subject>\n" +
            "                <saml:AuthzDecisionStatement Resource=\"http://someresource.org\" Decision=\"Permit\">\n" +
            "                    <saml:Action>Get</saml:Action>\n" +
            "                </saml:AuthzDecisionStatement>\n" +
            "            </saml:Assertion>";
}
