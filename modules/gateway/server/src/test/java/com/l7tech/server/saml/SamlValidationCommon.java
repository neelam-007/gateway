package com.l7tech.server.saml;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.token.http.TlsClientCertToken;
import com.l7tech.security.xml.processor.MockProcessorResult;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.policy.assertion.xmlsec.SamlAssertionValidate;
import com.l7tech.xml.saml.SamlAssertion;
import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;
import java.util.*;

import static org.junit.Assert.assertTrue;

/**
 * Test coverage for logic which is identical for SAML Version 1 and 2.
 */
public class SamlValidationCommon {

    public SamlValidationCommon(RequireWssSaml templateSaml) throws Exception{
        this.templateSaml = templateSaml;
        final X509Certificate[] clientCertChain = {TestDocuments.getEttkClientCertificate()};
        loginCredentials = LoginCredentials.makeLoginCredentials(new TlsClientCertToken(clientCertChain[0]), SslAssertion.class);
    }

    /**
     * Tests support for context variables in the name qualifier and audience fields.
     */
    public void testContextVariableSupport_AudienceRestriction_And_NameQualifier(final String assertionWithAudienceXml) throws Exception{
        String audience = "audience";
        templateSaml.setAudienceRestriction("${" + audience + "}");
        String nameQualifier = "namequalifier";
        templateSaml.setNameQualifier("${" + nameQualifier + "}");

        // validate
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);
        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();

        Map<String, Object> serverVariables = new HashMap<String, Object>();
        serverVariables.put(audience, "http://restriction.com");
        serverVariables.put(nameQualifier, "NameQualifierValue");

        Document assertionDocument = XmlUtil.stringToDocument(assertionWithAudienceXml);
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        sav.validate(loginCredentials, fakeProcessorResults(assertion), results, null, null, serverVariables, new TestAudit());

        // we expect no errors, but to future proof test, simply ensure no error with any messages were found
        for (SamlAssertionValidate.Error result : results) {
            String errorMessage = result.toString();
            System.out.println( errorMessage );
            Assert.assertTrue("Unexpected audience issue", !errorMessage.toLowerCase().contains("audience"));
            Assert.assertTrue("Unexpected name qualifier issue", !errorMessage.toLowerCase().contains("qualifier"));
        }

        //remove this line iff test fails due to new additional logic or simply update the tests configuration to satisfy new rule.
        assertTrue("Should be no errors.", results.size()==0);
    }

    /**
     * Validate no incoming audience is required if assertion config does not require any.
     */
    public void testAudienceRestriction_NoIncomingAudienceRequired(final String assertionWithNoAudience) throws Exception {
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);

        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();

        Map<String, Object> serverVariables = new HashMap<String, Object>();
        Document assertionDocument = XmlUtil.stringToDocument(assertionWithNoAudience);
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        sav.validate(loginCredentials, fakeProcessorResults(assertion), results, null, null, serverVariables, new TestAudit());

        //remove this line iff test fails due to new additional logic or simply update the tests configuration to satisfy new rule.
        assertTrue("Should be no errors.", results.size()==0);
    }

    /**
     * Test that an incoming audience value is required when assertion configuration requires it.
     */
    public void testAudienceRestriction_NoIncomingAudience(final String assertionWithNoAudience) throws Exception {
        String audience = "audience";
        templateSaml.setAudienceRestriction("${" + audience + "}");
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);

        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();

        Map<String, Object> serverVariables = new HashMap<String, Object>();
        String configuredAllowedAudienceValues = "http://audience4.com/ http://audience5.com/";
        serverVariables.put(audience, configuredAllowedAudienceValues);

        Document assertionDocument = XmlUtil.stringToDocument(assertionWithNoAudience);
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        sav.validate(loginCredentials, fakeProcessorResults(assertion), results, null, null, serverVariables, new TestAudit());

        validateAudienceErrorFound(results);
    }

    /**
     * Tests support for multiple configured audience values.
     */
    public void testAudienceRestriction_ValidationAgainstMultipleConfiguredAudienceValues(final String assertionWithAudienceXml) throws Exception {
        templateSaml.setAudienceRestriction("http://audience4.com/ http://audience5.com/ http://restriction.com");
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);

        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();

        Map<String, Object> serverVariables = new HashMap<String, Object>();
        Document assertionDocument = XmlUtil.stringToDocument(assertionWithAudienceXml);
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        sav.validate(loginCredentials, fakeProcessorResults(assertion), results, null, null, serverVariables, new TestAudit());

        validateNoAudienceErrorsFound(results);
    }

    /**
     * Tests support for multiple configured audience values - when none match.
     */
    public void testAudienceRestriction_ValidationAgainstMultipleConfiguredAudienceValues_NoMatch(final String assertionWithAudienceXml) throws Exception {
        templateSaml.setAudienceRestriction("http://audience4.com/ http://audience5.com/ http://anothernomatch");
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);

        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();

        Map<String, Object> serverVariables = new HashMap<String, Object>();
        Document assertionDocument = XmlUtil.stringToDocument(assertionWithAudienceXml);
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        sav.validate(loginCredentials, fakeProcessorResults(assertion), results, null, null, serverVariables, new TestAudit());

        validateAudienceErrorFound(results);
    }

    /**
     * Tests support for multiple configured audience values, where the runtime allowable values come from a single
     * valued variable.
     */
    public void testAudienceRestriction_AudienceValidationAgainstSingleValuedVariable(final String assertionWithAudienceXml) throws Exception {
        templateSaml.setAudienceRestriction("${single}                  some none matching values here");
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);

        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();

        Map<String, Object> serverVariables = new HashMap<String, Object>();
        serverVariables.put("single", "http://restriction.com");

        Document assertionDocument = XmlUtil.stringToDocument(assertionWithAudienceXml);
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        sav.validate(loginCredentials, fakeProcessorResults(assertion), results, null, null, serverVariables, new TestAudit());

        validateNoAudienceErrorsFound(results);
    }

    /**
     * Tests support for multiple configured audience values, where the runtime allowable values come from a multi
     * valued variable.
     */
    public void testAudienceRestriction_AudienceValidationAgainstMultiValuedVariable(final String assertionWithAudienceXml) throws Exception {
        templateSaml.setAudienceRestriction("sdfdsfds ${multi}                  some none matching values here");
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);

        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();

        Map<String, Object> serverVariables = new HashMap<String, Object>();
        serverVariables.put("multi", Arrays.asList("Not a match", "http://restriction.com"));

        Document assertionDocument = XmlUtil.stringToDocument(assertionWithAudienceXml);
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        sav.validate(loginCredentials, fakeProcessorResults(assertion), results, null, null, serverVariables, new TestAudit());

        validateNoAudienceErrorsFound(results);
    }

    /**
     * Validate that we support multiple incoming audience values in addition to multiple configured allowed audience
     * values.
     */
    public void testAudienceRestriction_AudienceValidation_MultiIn_MultiConfigured_Match(final String assertionWithMultipleAudiences) throws Exception {
        templateSaml.setAudienceRestriction("sdfdsfds ${multi}                  some none matching values here");
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);

        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();

        Map<String, Object> serverVariables = new HashMap<String, Object>();
        serverVariables.put("multi", Arrays.asList("http://audience2.com", "http://restriction.com"));// - http://audience2.com is the match

        Document assertionDocument = XmlUtil.stringToDocument(assertionWithMultipleAudiences);
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        sav.validate(loginCredentials, fakeProcessorResults(assertion), results, null, null, serverVariables, new TestAudit());

        validateNoAudienceErrorsFound(results);
    }

    /**
     * Validate that we support multiple incoming audience values in addition to multiple configured allowed audience
     * values.
     */
    public void testAudienceRestriction_AudienceValidation_MultiIn_MultiConfigured_NoMatch(final String assertionWithMultipleAudiences) throws Exception {
        templateSaml.setAudienceRestriction("sdfdsfds ${multi}                  some none matching values here");
        SamlAssertionValidate sav = new SamlAssertionValidate(templateSaml);

        List<SamlAssertionValidate.Error> results = new ArrayList<SamlAssertionValidate.Error>();

        Map<String, Object> serverVariables = new HashMap<String, Object>();
        serverVariables.put("multi", Arrays.asList("Nomatchhere", "http://restriction.com"));

        Document assertionDocument = XmlUtil.stringToDocument(assertionWithMultipleAudiences);
        SamlAssertion assertion = SamlAssertion.newInstance(assertionDocument.getDocumentElement());

        sav.validate(loginCredentials, fakeProcessorResults(assertion), results, null, null, serverVariables, new TestAudit());

        validateAudienceErrorFound(results);
    }

    // - PRIVATE

    private final RequireWssSaml templateSaml;
    private final LoginCredentials loginCredentials;

    private ProcessorResult fakeProcessorResults(final SamlAssertion assertion) {
        return new MockProcessorResult() {
            @Override
            public XmlSecurityToken[] getXmlSecurityTokens() {
                return new XmlSecurityToken[]{assertion};
            }

            @Override
            public SigningSecurityToken[] getSigningTokens(Element element) {
                return new SigningSecurityToken[]{assertion};
            }
        };
    }

    void validateNoAudienceErrorsFound(List<SamlAssertionValidate.Error> results) {
        for ( final Object result : results ) {
            String errorMessage = result.toString();
            System.out.println( errorMessage );
            Assert.assertTrue("Unexpected audience issue", !errorMessage.toLowerCase().contains("audience"));
        }

        //remove this line iff test fails due to new additional logic or simply update the tests configuration to satisfy new rule.
        assertTrue("Should be no errors.", results.size()==0);
    }

    void validateAudienceErrorFound(List<SamlAssertionValidate.Error> results) {
        boolean foundAudienceError = false;
        for ( final Object result : results ) {
            String errorMessage = result.toString();
            System.out.println( errorMessage );
            foundAudienceError = foundAudienceError || errorMessage.toLowerCase().contains("audience");
        }

        assertTrue("Audience restriction error should have been found.", foundAudienceError);
    }

}
