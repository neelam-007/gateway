package com.l7tech.server.saml;

import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.test.BugNumber;
import org.junit.Before;
import org.junit.Test;

/**
 * Validation tests for SAML 1.1
 */
public class Saml1ValidationTest {

    private SamlValidationCommon samlValidationCommon;

    @Before
    public void setUp() throws Exception {
        RequireWssSaml templateSaml = new RequireWssSaml();
        templateSaml.setVersion(1);
        templateSaml.setCheckAssertionValidity(false);
        templateSaml.setRequireHolderOfKeyWithMessageSignature(false);
        templateSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SENDER_VOUCHES});

        SamlAuthenticationStatement statement = new SamlAuthenticationStatement();
        statement.setAuthenticationMethods(new String []{SamlConstants.UNSPECIFIED_AUTHENTICATION});
        templateSaml.setAuthenticationStatement(statement);

        samlValidationCommon = new SamlValidationCommon(templateSaml);
    }

    @BugNumber(9090)
    @Test
    public void testContextVariableSupport_AudienceRestriction_And_NameQualifier() throws Exception{
        samlValidationCommon.testContextVariableSupport_AudienceRestriction_And_NameQualifier(ASSERTION_WITH_AUDIENCE);
    }

    @Test
    public void testAudienceRestriction_NoIncomingAudienceRequired() throws Exception {
        samlValidationCommon.testAudienceRestriction_NoIncomingAudienceRequired(ASSERTION_WITH_NO_AUDIENCE);
    }


    @Test
    public void testAudienceRestriction_NoIncomingAudience() throws Exception {
        samlValidationCommon.testAudienceRestriction_NoIncomingAudience(ASSERTION_WITH_NO_AUDIENCE);
    }

    @Test
    @BugNumber(11388)
    public void testAudienceRestriction_ValidationAgainstMultipleConfiguredAudienceValues() throws Exception {
        samlValidationCommon.testAudienceRestriction_ValidationAgainstMultipleConfiguredAudienceValues(ASSERTION_WITH_AUDIENCE);
    }

    @Test
    @BugNumber(11388)
    public void testAudienceRestriction_ValidationAgainstMultipleConfiguredAudienceValues_NoMatch() throws Exception {
        samlValidationCommon.testAudienceRestriction_ValidationAgainstMultipleConfiguredAudienceValues_NoMatch(ASSERTION_WITH_AUDIENCE);
    }

    @Test
    @BugNumber(11388)
    public void testAudienceRestriction_AudienceValidationAgainstSingleValuedVariable() throws Exception {
        samlValidationCommon.testAudienceRestriction_AudienceValidationAgainstSingleValuedVariable(ASSERTION_WITH_AUDIENCE);
    }

    @Test
    @BugNumber(11388)
    public void testAudienceRestriction_AudienceValidationAgainstMultiValuedVariable() throws Exception {
        samlValidationCommon.testAudienceRestriction_AudienceValidationAgainstMultiValuedVariable(ASSERTION_WITH_AUDIENCE);
    }

    @Test
    public void testAudienceRestriction_AudienceValidation_MultiIn_MultiConfigured_Match() throws Exception {
        samlValidationCommon.testAudienceRestriction_AudienceValidation_MultiIn_MultiConfigured_Match(ASSERTION_WITH_MULTIPLE_AUDIENCES);
    }

    /**
     * Validate that we support multiple incoming audience values in addition to multiple configured allowed audience
     * values.
     */
    @Test
    public void testAudienceRestriction_AudienceValidation_MultiIn_MultiConfigured_NoMatch() throws Exception {
        samlValidationCommon.testAudienceRestriction_AudienceValidation_MultiIn_MultiConfigured_NoMatch(ASSERTION_WITH_MULTIPLE_AUDIENCES);
    }

    private static final String ASSERTION_WITH_AUDIENCE =
            "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" MinorVersion=\"1\" MajorVersion=\"1\"\n" +
                    "                AssertionID=\"SamlAssertion-f90a22365f67ea5024d91f960ead8ad1\" Issuer=\"irishman2.l7tech.local\"\n" +
                    "                IssueInstant=\"2011-11-10T21:46:12.283Z\">\n" +
                    "    <saml:Conditions NotBefore=\"2011-11-10T21:44:12.285Z\" NotOnOrAfter=\"2011-11-10T21:51:12.285Z\">\n" +
                    "        <saml:AudienceRestrictionCondition>\n" +
                    "            <saml:Audience>http://restriction.com</saml:Audience>\n" +
                    "        </saml:AudienceRestrictionCondition>\n" +
                    "    </saml:Conditions>\n" +
                    "    <saml:AuthenticationStatement AuthenticationMethod=\"urn:oasis:names:tc:SAML:1.0:am:unspecified\"\n" +
                    "                                  AuthenticationInstant=\"2011-11-10T21:46:12.283Z\">\n" +
                    "        <saml:Subject>\n" +
                    "            <saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\" NameQualifier=\"NameQualifierValue\"/>\n" +
                    "            <saml:SubjectConfirmation>\n" +
                    "                <saml:ConfirmationMethod>urn:oasis:names:tc:SAML:1.0:cm:sender-vouches</saml:ConfirmationMethod>\n" +
                    "            </saml:SubjectConfirmation>\n" +
                    "        </saml:Subject>\n" +
                    "        <saml:SubjectLocality IPAddress=\"127.0.0.1\"/>\n" +
                    "    </saml:AuthenticationStatement>\n" +
                    "</saml:Assertion>";

    private static final String ASSERTION_WITH_NO_AUDIENCE =
            "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" MinorVersion=\"1\" MajorVersion=\"1\"\n" +
                    "                AssertionID=\"SamlAssertion-f90a22365f67ea5024d91f960ead8ad1\" Issuer=\"irishman2.l7tech.local\"\n" +
                    "                IssueInstant=\"2011-11-10T21:46:12.283Z\">\n" +
                    "    <saml:Conditions NotBefore=\"2011-11-10T21:44:12.285Z\" NotOnOrAfter=\"2011-11-10T21:51:12.285Z\">\n" +
                    "    </saml:Conditions>\n" +
                    "    <saml:AuthenticationStatement AuthenticationMethod=\"urn:oasis:names:tc:SAML:1.0:am:unspecified\"\n" +
                    "                                  AuthenticationInstant=\"2011-11-10T21:46:12.283Z\">\n" +
                    "        <saml:Subject>\n" +
                    "            <saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\" NameQualifier=\"NameQualifierValue\"/>\n" +
                    "            <saml:SubjectConfirmation>\n" +
                    "                <saml:ConfirmationMethod>urn:oasis:names:tc:SAML:1.0:cm:sender-vouches</saml:ConfirmationMethod>\n" +
                    "            </saml:SubjectConfirmation>\n" +
                    "        </saml:Subject>\n" +
                    "        <saml:SubjectLocality IPAddress=\"127.0.0.1\"/>\n" +
                    "    </saml:AuthenticationStatement>\n" +
                    "</saml:Assertion>";

    private static final String ASSERTION_WITH_MULTIPLE_AUDIENCES =
            "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" MinorVersion=\"1\" MajorVersion=\"1\"\n" +
                    "                AssertionID=\"SamlAssertion-f90a22365f67ea5024d91f960ead8ad1\" Issuer=\"irishman2.l7tech.local\"\n" +
                    "                IssueInstant=\"2011-11-10T21:46:12.283Z\">\n" +
                    "    <saml:Conditions NotBefore=\"2011-11-10T21:44:12.285Z\" NotOnOrAfter=\"2011-11-10T21:51:12.285Z\">\n" +
                    "        <saml:AudienceRestrictionCondition>\n" +
                    "            <saml:Audience>http://audience1.com</saml:Audience>\n" +
                    "            <saml:Audience>http://audience2.com</saml:Audience>\n" +
                    "            <saml:Audience>http://audience3.com</saml:Audience>\n" +
                    "        </saml:AudienceRestrictionCondition>\n" +
                    "    </saml:Conditions>\n" +
                    "    <saml:AuthenticationStatement AuthenticationMethod=\"urn:oasis:names:tc:SAML:1.0:am:unspecified\"\n" +
                    "                                  AuthenticationInstant=\"2011-11-10T21:46:12.283Z\">\n" +
                    "        <saml:Subject>\n" +
                    "            <saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\" NameQualifier=\"NameQualifierValue\"/>\n" +
                    "            <saml:SubjectConfirmation>\n" +
                    "                <saml:ConfirmationMethod>urn:oasis:names:tc:SAML:1.0:cm:sender-vouches</saml:ConfirmationMethod>\n" +
                    "            </saml:SubjectConfirmation>\n" +
                    "        </saml:Subject>\n" +
                    "        <saml:SubjectLocality IPAddress=\"127.0.0.1\"/>\n" +
                    "    </saml:AuthenticationStatement>\n" +
                    "</saml:Assertion>";

}
