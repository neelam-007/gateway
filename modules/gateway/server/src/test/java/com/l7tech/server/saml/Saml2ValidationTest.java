package com.l7tech.server.saml;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.processor.MockProcessorResult;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.policy.assertion.xmlsec.SamlAssertionValidate;
import com.l7tech.test.BugNumber;
import com.l7tech.util.SyspropUtil;
import com.l7tech.xml.saml.SamlAssertion;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Saml 2.0 assertion validation tests.
 */
public class Saml2ValidationTest {

    private SamlValidationCommon samlValidationCommon;

    @Before
    public void setUp() throws Exception{
        // restoreTimeSkewSettings
        SyspropUtil.setProperty( "com.l7tech.server.saml.validate.notBeforeOffsetMin", "0" );
        SyspropUtil.setProperty( "com.l7tech.server.saml.validate.notOnOrAfterOffsetMin", "0");

        RequireWssSaml requireWssSaml = new RequireWssSaml();
        requireWssSaml.setVersion(2);
        requireWssSaml.setCheckAssertionValidity(false);
        requireWssSaml.setRequireHolderOfKeyWithMessageSignature(false);
        requireWssSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);
        requireWssSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES});

        SamlAuthenticationStatement statement = new SamlAuthenticationStatement();
        statement.setAuthenticationMethods(new String []{SamlConstants.AUTHENTICATION_SAML2_UNSPECIFIED});
        requireWssSaml.setAuthenticationStatement(statement);

        samlValidationCommon = new SamlValidationCommon(requireWssSaml);
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.server.saml.validate.notBeforeOffsetMin",
            "com.l7tech.server.saml.validate.notOnOrAfterOffsetMin"
        );
    }

    /**
     * TEST that the assertion expiry time is validated.
     */
    @Test
    public void testExpiryValidation() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_EXPIRED);
        System.out.println("Testing expired assertion: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequireWssSaml templateSaml = new RequireWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(true);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(true);
        templateSaml.setNameFormats( SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);
        templateSaml.setSubjectConfirmationDataCheckValidity(true);

        // validate
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();
        sav.validate(null, fakeProcessorResults(assertion, true), results, null, null, Collections.<String, Object>emptyMap(), null);

        // check for error message
        String expectedErrorStart = "SAML Constraint Error: SAML ticket has expired as of:";
        String expectedErrorStart2 = "SAML Constraint Error: Subject Confirmations mismatch (some confirmations were rejected) presented/accepted";
        int foundErrorCount = 0;
        for ( final Object result : results ) {
            String errorStr = result.toString();
            System.out.println( errorStr );
            if ( errorStr.startsWith( expectedErrorStart ) ||
                    errorStr.startsWith( expectedErrorStart2 ) )
                foundErrorCount++;
        }

        if ( foundErrorCount != 2)
            fail("Expected to find error messages '" + expectedErrorStart + "...' and '" + expectedErrorStart2 + "...'.");
    }

    /**
     * TEST that the assertion expiry time is not validated when validation is off.
     */
    @Test
    public void testExpiryValidationOff() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_EXPIRED);
        System.out.println("Testing expired assertion: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequireWssSaml templateSaml = new RequireWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(false);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(true);
        templateSaml.setNameFormats( SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);
        templateSaml.setSubjectConfirmationDataCheckValidity(false);

        // validate
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();
        sav.validate(null, fakeProcessorResults(assertion, true), results, null, null, Collections.<String, Object>emptyMap(), null);

        // check for error message
        String expectedErrorStart = "SAML Constraint Error: SAML ticket has expired as of:";
        String expectedErrorStart2 = "SAML Constraint Error: Subject Confirmations mismatch (some confirmations were rejected) presented/accepted";
        int foundErrorCount = 0;
        for ( final Object result : results ) {
            String errorStr = result.toString();
            System.out.println( errorStr );
            if ( errorStr.startsWith( expectedErrorStart ) ||
                    errorStr.startsWith( expectedErrorStart2 ) )
                foundErrorCount++;
        }

        if ( foundErrorCount != 0)
            fail("Found unexpected error message '" + expectedErrorStart + "...' and/or '" + expectedErrorStart2 + "...'.");
    }

    /**
     * TEST that the assertion expiry time is validated.
     */
    @Test
    public void testExpiryValidationWithGracePeriod() throws Exception {
        SyspropUtil.setProperty( "com.l7tech.server.saml.validate.notOnOrAfterOffsetMin", "15000000");

        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_EXPIRED);
        System.out.println("Testing expired assertion: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequireWssSaml templateSaml = new RequireWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(true);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(true);
        templateSaml.setNameFormats( SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);
        templateSaml.setSubjectConfirmationDataCheckValidity(true);

        // validate
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();
        sav.validate(null, fakeProcessorResults(assertion, true), results, null, null, Collections.<String, Object>emptyMap(), null);

        // check for error message
        String expectedErrorStart = "SAML Constraint Error: SAML ticket has expired as of:";
        String expectedErrorStart2 = "SAML Constraint Error: Subject Confirmations mismatch (some confirmations were rejected) presented/accepted";
        boolean foundError = false;
        for ( final Object result : results ) {
            String errorStr = result.toString();
            System.out.println( errorStr );
            if ( errorStr.startsWith( expectedErrorStart ) ||
                    errorStr.startsWith( expectedErrorStart2 ) )
                foundError = true;
        }

        if (foundError)
            fail("Ticket should not have been considered expired, since we cranked up the validateAfterOffsetMinutes to 20 years or so.");
    }

    /**
     * TEST that the audience restriction is validated.
     */
    @Test
    public void testAudienceRestrictionValidation() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_AUDIENCE_RESTR);
        System.out.println("Testing audience restriction validation: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequireWssSaml templateSaml = new RequireWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(false);
        templateSaml.setAudienceRestriction("http://restricted.audience.com_DOES_NOT_EXIST_IN_ANY_CONFIGURED/");
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(true);
        templateSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);

        // validate
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();
        sav.validate(null, fakeProcessorResults(assertion, true), results, null, null, Collections.<String, Object>emptyMap(), new TestAudit());

        // check for error message
        samlValidationCommon.validateAudienceErrorFound(results);
    }

    /**
     * TEST that the subject confirmation data address is validated.
     */
    @Test
    public void testSubjectConfirmationDataAddressValidation() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument( ASSERTION_STR_SUBJECT_CONF_DATA );
        System.out.println("Testing subject confirmation data address validation: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequireWssSaml templateSaml = new RequireWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(false);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(true);
        templateSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);
        templateSaml.setSubjectConfirmationDataCheckValidity(false);

        String expectedErrorStart = "SAML Constraint Error: Subject Confirmations mismatch (some confirmations were rejected) presented/accepted";

        // validate - should be OK no address requirement
        validateAndCheckErrors( templateSaml, assertionDocument, assertion, expectedErrorStart, false, "127.0.0.2"  );

        // validate - should be OK address matches
        templateSaml.setSubjectConfirmationDataCheckAddress( true );
        validateAndCheckErrors( templateSaml, assertionDocument, assertion, expectedErrorStart, false, "127.0.0.1"  );

        // validate - should fail address mismatch
        templateSaml.setSubjectConfirmationDataCheckAddress( true );
        validateAndCheckErrors( templateSaml, assertionDocument, assertion, expectedErrorStart, true, "127.0.0.2"  );
    }

    /**
     * TEST that the subject confirmation data recipient is validated.
     */
    @Test
    public void testSubjectConfirmationDataRecipientValidation() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument( ASSERTION_STR_SUBJECT_CONF_DATA );
        System.out.println("Testing subject confirmation data recipient validation: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequireWssSaml templateSaml = new RequireWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(false);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(true);
        templateSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);
        templateSaml.setSubjectConfirmationDataCheckValidity(false);

        String expectedErrorStart = "SAML Constraint Error: Subject Confirmations mismatch (some confirmations were rejected) presented/accepted";

        // validate - should be OK no recipient requirement
        validateAndCheckErrors( templateSaml, assertionDocument, assertion, expectedErrorStart, false, "127.0.0.1" );

        // validate - should be OK recipient matches
        templateSaml.setSubjectConfirmationDataRecipient( "http://example.com/service" );
        validateAndCheckErrors( templateSaml, assertionDocument, assertion, expectedErrorStart, false, "127.0.0.1"  );

        // validate - should fail recipient mismatch
        templateSaml.setSubjectConfirmationDataRecipient( "http://example.com/service2" );
        validateAndCheckErrors( templateSaml, assertionDocument, assertion, expectedErrorStart, true, "127.0.0.1"  );
    }

    /**
     * TEST that the subject confirmation data address/recipient are only validated if present in the assertion.
     */
    @Test
    public void testSubjectConfirmationDataValidation() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument( ASSERTION_STR_AUDIENCE_RESTR );
        System.out.println("Testing subject confirmation data validation: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequireWssSaml templateSaml = new RequireWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(false);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(true);
        templateSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);
        templateSaml.setSubjectConfirmationDataCheckValidity(false);
        templateSaml.setSubjectConfirmationDataCheckAddress( true );
        templateSaml.setSubjectConfirmationDataRecipient( "http://example.com/service" );

        String expectedErrorStart = "SAML Constraint Error: Subject Confirmations mismatch (some confirmations were rejected) presented/accepted";

        // validate - should be OK no recipient/address in assertion
        validateAndCheckErrors( templateSaml, assertionDocument, assertion, expectedErrorStart, false, "127.0.0.1" );
    }

    private void validateAndCheckErrors( final RequireWssSaml templateSaml,
                                         final Document assertionDocument,
                                         final SamlAssertion assertion,
                                         final String expectedErrorStart,
                                         final boolean errorExpected,
                                         final String address ) {
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();
        sav.validate(null, fakeProcessorResults(assertion, true), results, null, Collections.singleton( address ),
                Collections.<String, Object>emptyMap(), null);

        // check for error message
        boolean foundError = false;
        for ( final Object result : results ) {
            String errorStr = result.toString();
            System.out.println( errorStr );
            if ( errorStr.startsWith(expectedErrorStart))
                foundError = true;
        }

        if ( errorExpected && !foundError ) {
            fail("Expected to find error message '" + expectedErrorStart + "...'.");
        } else if ( !errorExpected && foundError ) {
            fail("Unexpected error message '" + expectedErrorStart + "...'.");
        }
    }

    /**
     * TEST that assertions with no statements are permitted
     */
    @Test
    public void testSubjectOnly() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_SUBJECT_ONLY);
        System.out.println("Testing assertion with subject only: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequireWssSaml templateSaml = new RequireWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(false);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(false);
        templateSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);

        // validate
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();
        sav.validate(null, fakeProcessorResults(assertion, false), results, null, null, Collections.<String, Object>emptyMap(), null);

        boolean foundUnsignedError = false;
        for ( final Object result : results ) {
            String errorMessage = result.toString();
            System.out.println( errorMessage );
            if ( "SAML Constraint Error: Unsigned SAML assertion found in security Header".equals( errorMessage ) )
                foundUnsignedError = true;
        }

        // check only unsigned assertion error
        assertTrue("Should be no errors.", results.size()==1 && foundUnsignedError);
    }

    /**
     * TEST that assertions with repeated OneTimeUse conditions are rejected
     */
    @Test
    public void testMultipleOneTimeOnly() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_MULTIPLE_ONE_TIME_CONDS);
        System.out.println("Testing assertion repeated OneTimeUse conditions: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequireWssSaml templateSaml = new RequireWssSaml();
        templateSaml.setVersion(2);
        templateSaml.setCheckAssertionValidity(false);
        templateSaml.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});
        templateSaml.setRequireHolderOfKeyWithMessageSignature(false);
        templateSaml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS_SAML2);

        // validate
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();
        sav.validate(null, fakeProcessorResults(assertion, true), results, null, null, Collections.<String, Object>emptyMap(), null);

        boolean foundError = false;
        String expectedError = "SAML Constraint Error: Multiple OneTimeUse conditions are not permitted.";
        for ( final Object result : results ) {
            String errorMessage = result.toString();
            System.out.println( errorMessage );
            if ( expectedError.equals( errorMessage ) )
                foundError = true;
        }

        // check only unsigned assertion error
        assertTrue("Expected validation error not found '"+expectedError+"'.", foundError);
    }

    /**
     * TEST authentication statement validation
     */
    @Test
    public void testAuthenticationStatementValidation() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_AUTHN);
        System.out.println("Testing authentication statement assertion: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequireWssSaml templateSaml = new RequireWssSaml();
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
        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();
        sav.validate(null, fakeProcessorResults(assertion, false), results, null, null, Collections.<String, Object>emptyMap(), null);

        boolean foundError = false;
        for ( final Object result : results ) {
            String errorMessage = result.toString();
            System.out.println( errorMessage );
            if ( "SAML Constraint Error: Unsigned SAML assertion found in security Header".equals( errorMessage ) )
                foundError = true;
        }

        // check only unsigned assertion error
        assertTrue("Should be no errors.", results.size()==1 && foundError);

        // now check for failure
        sas.setAuthenticationMethods(new String[]{SamlConstants.UNSPECIFIED_AUTHENTICATION});

        // validate 2
        SamlAssertionValidate sav2 = new SamlAssertionValidate(templateSaml);
        List<SamlAssertionValidate.Error> results2 = new ArrayList<SamlAssertionValidate.Error>();
        sav2.validate(null, fakeProcessorResults(assertion, false), results2, null, null, Collections.<String, Object>emptyMap(), null);

        boolean foundError2 = false;
        boolean methodError = false;
        for ( final Object aResults2 : results2 ) {
            String errorMessage = aResults2.toString();
            System.out.println( errorMessage );

            if ( "SAML Constraint Error: Unsigned SAML assertion found in security Header".equals( errorMessage ) )
                foundError2 = true;

            if ( "SAML Constraint Error: Authentication method not matched. Received value: urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig Expected one of: urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified".equals( errorMessage ) )
                methodError = true;
        }

        // check only unsigned assertion error and method error
        assertTrue("Should be a method error.", results2.size()==2 && foundError2 && methodError);

    }

    /**
     * TEST attribute statement validation
     */
    @Test
    public void testAttributeStatementValidation() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_ATTR);
        System.out.println("Testing attribute statement assertion: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequireWssSaml templateSaml = new RequireWssSaml();
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
        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();
        sav.validate(null, fakeProcessorResults(assertion, false), results, null, null, Collections.<String, Object>emptyMap(), null);

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

    /**
     * TEST authorization statement validation
     */
    @Test
    public void testAuthorizationDecisionStatementValidation() throws Exception {
        // create doc
        Document assertionDocument = XmlUtil.stringToDocument(ASSERTION_STR_AUTHORIZATION);
        System.out.println("Testing authorization decision statement assertion: \n" + XmlUtil.nodeToFormattedString(assertionDocument));
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        // create validation template
        RequireWssSaml templateSaml = new RequireWssSaml();
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
        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();
        sav.validate(null, fakeProcessorResults(assertion, false), results, null, null, Collections.<String, Object>emptyMap(), null);

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

    /**
     * Validates that within a Conditions element that multi audience restriction elements are treated as a disjunction ("OR")
     * and not a conjunction ("AND").
     *
     */
    @BugNumber(11455)
    @Test
    public void testAudienceRestriction_AudienceValidation_MultiAudienceRestrictionElementsInIncoming() throws Exception {
        samlValidationCommon.testAudienceRestriction_ValidationAgainstMultipleConfiguredAudienceValues(ASSERTION_WITH_AUDIENCE_MULTIPLE_RESTRICTION_ELEMENTS);
    }

    private ProcessorResult fakeProcessorResults(final SamlAssertion assertion, final boolean overrideGetSigningTokens) {
        return new MockProcessorResult() {
            @Override
            public XmlSecurityToken[] getXmlSecurityTokens() {
                return new XmlSecurityToken[]{assertion};
            }

            @Override
            public SigningSecurityToken[] getSigningTokens(Element element) {
                return (overrideGetSigningTokens) ? new SigningSecurityToken[]{assertion} : super.getSigningTokens(element);
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
            "                            NotBefore=\"2006-06-27T20:15:00.000Z\" NotOnOrAfter=\"2006-06-27T20:20:00.000Z\"\n" +        
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

    private static final String ASSERTION_STR_SUBJECT_CONF_DATA =
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
            "                            Address=\"127.0.0.1\"\n" +
            "                            Recipient=\"http://example.com/service\"\n" +
            "                            NotBefore=\"2006-06-27T20:15:00.000Z\"\n" +
            "                            NotOnOrAfter=\"2006-06-27T20:20:00.000Z\"\n" +
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

    private String ASSERTION_WITH_AUDIENCE_MULTIPLE_RESTRICTION_ELEMENTS = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-28d0cfc161dc39a8636145362a9f592c\"\n" +
            "                 IssueInstant=\"2011-11-11T00:01:55.767Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "    <saml2:Issuer>irishman2.l7tech.local</saml2:Issuer>\n" +
            "    <saml2:Subject>\n" +
            "        <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\" NameQualifier=\"NameQualifierValue\"/>\n" +
            "        <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:sender-vouches\">\n" +
            "            <saml2:NameID>CN=irishman2.l7tech.local</saml2:NameID>\n" +
            "        </saml2:SubjectConfirmation>\n" +
            "    </saml2:Subject>\n" +
            "    <saml2:Conditions NotBefore=\"2011-11-10T23:59:55.825Z\" NotOnOrAfter=\"2011-11-11T00:06:55.825Z\">\n" +
            "        <saml2:AudienceRestriction>\n" +
            "            <saml2:Audience>http://restriction.com</saml2:Audience>\n" +
            "        </saml2:AudienceRestriction>\n" +
            "        <saml2:AudienceRestriction>\n" +
            "            <saml2:Audience>http://restriction2.com</saml2:Audience>\n" +
            "        </saml2:AudienceRestriction>\n" +
            "    </saml2:Conditions>\n" +
            "    <saml2:AuthnStatement AuthnInstant=\"2011-11-11T00:01:55.767Z\">\n" +
            "        <saml2:SubjectLocality Address=\"127.0.0.1\"/>\n" +
            "        <saml2:AuthnContext>\n" +
            "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml2:AuthnContextClassRef>\n" +
            "        </saml2:AuthnContext>\n" +
            "    </saml2:AuthnStatement>\n" +
            "</saml2:Assertion>";

    private String ASSERTION_WITH_AUDIENCE = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-28d0cfc161dc39a8636145362a9f592c\"\n" +
            "                 IssueInstant=\"2011-11-11T00:01:55.767Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "    <saml2:Issuer>irishman2.l7tech.local</saml2:Issuer>\n" +
            "    <saml2:Subject>\n" +
            "        <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\" NameQualifier=\"NameQualifierValue\"/>\n" +
            "        <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:sender-vouches\">\n" +
            "            <saml2:NameID>CN=irishman2.l7tech.local</saml2:NameID>\n" +
            "        </saml2:SubjectConfirmation>\n" +
            "    </saml2:Subject>\n" +
            "    <saml2:Conditions NotBefore=\"2011-11-10T23:59:55.825Z\" NotOnOrAfter=\"2011-11-11T00:06:55.825Z\">\n" +
            "        <saml2:AudienceRestriction>\n" +
            "            <saml2:Audience>http://restriction.com</saml2:Audience>\n" +
            "        </saml2:AudienceRestriction>\n" +
            "    </saml2:Conditions>\n" +
            "    <saml2:AuthnStatement AuthnInstant=\"2011-11-11T00:01:55.767Z\">\n" +
            "        <saml2:SubjectLocality Address=\"127.0.0.1\"/>\n" +
            "        <saml2:AuthnContext>\n" +
            "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml2:AuthnContextClassRef>\n" +
            "        </saml2:AuthnContext>\n" +
            "    </saml2:AuthnStatement>\n" +
            "</saml2:Assertion>";

    private String ASSERTION_WITH_NO_AUDIENCE = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-28d0cfc161dc39a8636145362a9f592c\"\n" +
            "                 IssueInstant=\"2011-11-11T00:01:55.767Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "    <saml2:Issuer>irishman2.l7tech.local</saml2:Issuer>\n" +
            "    <saml2:Subject>\n" +
            "        <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\" NameQualifier=\"NameQualifierValue\"/>\n" +
            "        <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:sender-vouches\">\n" +
            "            <saml2:NameID>CN=irishman2.l7tech.local</saml2:NameID>\n" +
            "        </saml2:SubjectConfirmation>\n" +
            "    </saml2:Subject>\n" +
            "    <saml2:Conditions NotBefore=\"2011-11-10T23:59:55.825Z\" NotOnOrAfter=\"2011-11-11T00:06:55.825Z\">\n" +
            "    </saml2:Conditions>\n" +
            "    <saml2:AuthnStatement AuthnInstant=\"2011-11-11T00:01:55.767Z\">\n" +
            "        <saml2:SubjectLocality Address=\"127.0.0.1\"/>\n" +
            "        <saml2:AuthnContext>\n" +
            "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml2:AuthnContextClassRef>\n" +
            "        </saml2:AuthnContext>\n" +
            "    </saml2:AuthnStatement>\n" +
            "</saml2:Assertion>";

    private String ASSERTION_WITH_MULTIPLE_AUDIENCES = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-28d0cfc161dc39a8636145362a9f592c\"\n" +
            "                 IssueInstant=\"2011-11-11T00:01:55.767Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "    <saml2:Issuer>irishman2.l7tech.local</saml2:Issuer>\n" +
            "    <saml2:Subject>\n" +
            "        <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\" NameQualifier=\"NameQualifierValue\"/>\n" +
            "        <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:sender-vouches\">\n" +
            "            <saml2:NameID>CN=irishman2.l7tech.local</saml2:NameID>\n" +
            "        </saml2:SubjectConfirmation>\n" +
            "    </saml2:Subject>\n" +
            "    <saml2:Conditions NotBefore=\"2011-11-10T23:59:55.825Z\" NotOnOrAfter=\"2011-11-11T00:06:55.825Z\">\n" +
            "        <saml2:AudienceRestriction>\n" +
            "            <saml2:Audience>http://audience1.com</saml2:Audience>\n" +
            "            <saml2:Audience>http://audience2.com</saml2:Audience>\n" +
            "            <saml2:Audience>http://audience3.com</saml2:Audience>\n" +
            "        </saml2:AudienceRestriction>\n" +
            "    </saml2:Conditions>\n" +
            "    <saml2:AuthnStatement AuthnInstant=\"2011-11-11T00:01:55.767Z\">\n" +
            "        <saml2:SubjectLocality Address=\"127.0.0.1\"/>\n" +
            "        <saml2:AuthnContext>\n" +
            "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml2:AuthnContextClassRef>\n" +
            "        </saml2:AuthnContext>\n" +
            "    </saml2:AuthnStatement>\n" +
            "</saml2:Assertion>";

}
