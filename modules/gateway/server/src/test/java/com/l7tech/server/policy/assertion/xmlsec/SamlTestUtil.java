package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.TestDocuments;
import com.l7tech.identity.mapping.NameFormat;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml2;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.saml.*;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.StubMessageIdManager;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.xml.saml.SamlAssertion;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 */
public class SamlTestUtil {
    private static final Logger logger = Logger.getLogger(SamlTestUtil.class.getName());

    public static SimpleSecurityTokenResolver securityTokenResolver = new SimpleSecurityTokenResolver();
    public static BeanFactory beanFactory = new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
        put("securityTokenResolver", securityTokenResolver);
        put("distributedMessageIdManager", new StubMessageIdManager());
    }});

    /**
     * Creates a request Message that includes a SAML assertion.
     *
     * @param version2 true to create SAML 2.0; false for SAML 1.0
     * @return a new message. Never null.
     * @throws Exception if something fails
     */
    public static Message makeSamlRequest(boolean version2) throws Exception {
        Message message = new Message(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT_ONELINE));

        PrivateKey privateKey = TestDocuments.getDotNetServerPrivateKey();
        SamlAssertionGenerator sag = new SamlAssertionGenerator(new SignerInfo(privateKey,
                new X509Certificate[] { TestDocuments.getDotNetServerCertificate() }));
        AttributeStatement st = new AttributeStatement();
        String otherformat = version2 ? NameFormat.OTHER.getSaml20Uri() : NameFormat.OTHER.getSaml11Uri();
        st.setAttributes(new Attribute[] {
                new Attribute("First Attr 32", "urn:me", "test value foo blah blartch"),
                new Attribute("2 Attribute: it is indeed!", "urn:me", "value for myotherattr blah"),
                new Attribute("moreattr", "urn:me", "value for moreattr blah"),
        });
        st.setConfirmationMethod(SamlConstants.CONFIRMATION_BEARER);
        st.setNameFormat(version2? NameFormat.OTHER.getSaml20Uri() : NameFormat.OTHER.getSaml11Uri());
        st.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        st.setSubjectConfirmationData("subjectconfirmationdata1");
        SamlAssertionGenerator.Options options = new SamlAssertionGenerator.Options();
        options.setVersion(version2 ? 2 : 1);
        options.setSignAssertion(true);
        Document samlAss = sag.createAssertion(st, options);

        WssDecorator decorator = new WssDecoratorImpl();
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setSenderSamlToken(SamlAssertion.newInstance(samlAss.getDocumentElement()), false);
        dreq.setIncludeTimestamp(false);
        decorator.decorateMessage(message, dreq);
        return message;
    }

    /**
     * Configure the specified assertion to permit an unsigned bearer token attribute statement.
     *
     * @param ass  the assertion to configure.  May be SAML 1.0 or SAML 2.0
     * @return the reconfigured assertion; same as ass
     */
    public static <RT extends RequireWssSaml> RT configureToAllowAttributeBearer(RT ass) {
        boolean version2 = ass instanceof RequireWssSaml2;
        String otherformat = version2 ? NameFormat.OTHER.getSaml20Uri() : NameFormat.OTHER.getSaml11Uri();

        ass.setCheckAssertionValidity(false);
        ass.setNameFormats(new String[] {otherformat});
        ass.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_HOLDER_OF_KEY, SamlConstants.CONFIRMATION_BEARER});
        SamlAttributeStatement atts = new SamlAttributeStatement();
        atts.setAttributes(new SamlAttributeStatement.Attribute[] {
                new SamlAttributeStatement.Attribute("First Attr 32", "urn:me", "urn:me", null, true, true),
                new SamlAttributeStatement.Attribute("2 Attribute: it is indeed!", "urn:me", "urn:me", null, true, true),
        });
        ass.setAttributeStatement(atts);
        return ass;
    }

    public static PolicyEnforcementContext createWssProcessedContext(Message request) {
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, new Message());
        ProcessorResult processorResult = WSSecurityProcessorUtils.getWssResults(request, "req", securityTokenResolver, new LogOnlyAuditor(logger));
        request.getSecurityKnob().setProcessorResult(processorResult);
        return context;
    }

    public static void checkContextVariableResults(PolicyEnforcementContext context) throws NoSuchVariableException {
        try {
            context.getVariable("saml.attr.moreattr");
            org.junit.Assert.fail("Attributes that are present in ticket but NOT validated must NOT set context variables");
        } catch (NoSuchVariableException e) {
            // Ok
        }

        Object attr1 = context.getVariable("saml.attr.first_attr_32");
        org.junit.Assert.assertTrue("Attributes that are present and validated must set context variables", attr1 instanceof String[]);
        org.junit.Assert.assertEquals("Attributes that are present and validated must set context variables", ((String[])attr1)[0], "test value foo blah blartch");

        Object attr2 = context.getVariable("saml.attr.n2_attribute__it_is_indeed_");
        org.junit.Assert.assertTrue("Attributes that are present and validated must set context variables", attr2 instanceof String[]);
        org.junit.Assert.assertEquals("Attributes that are present and validated must set context variables", ((String[])attr2)[0], "value for myotherattr blah");
    }
}
