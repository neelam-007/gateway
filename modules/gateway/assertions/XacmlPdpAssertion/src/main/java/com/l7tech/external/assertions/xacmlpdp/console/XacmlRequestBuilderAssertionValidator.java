package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ExceptionUtils;

import java.util.Map;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.jaxen.dom.DOMXPath;
import org.jaxen.XPathFunctionContext;
import org.jaxen.NamespaceContext;
import org.jaxen.VariableContext;
import org.jaxen.UnresolvableException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * AssertionValidator for XacmlRequestBuilderAssertion 
 */
public class XacmlRequestBuilderAssertionValidator implements AssertionValidator {

    //- PUBLIC

    public XacmlRequestBuilderAssertionValidator( final XacmlRequestBuilderAssertion assertion ) {
        this.assertion = assertion;
        this.errString = validate( assertion );
    }

    @Override
    public void validate( final AssertionPath path,
                          final Wsdl wsdl,
                          final boolean soap,
                          final PolicyValidatorResult result ) {
        if ( errString != null ) {
            result.addError( new PolicyValidatorResult.Error( assertion, path, errString, null ) );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( XacmlRequestBuilderAssertionValidator.class.getName() );

    private final XacmlRequestBuilderAssertion assertion;
    private final String errString;

    private String validate( final XacmlRequestBuilderAssertion assertion ) {
        String error = null;

        final Document testDocument = buildTestDocument();
        if ( testDocument != null ) {
            final XPathFunctionContext xpathFunctionContext = new XPathFunctionContext(false);
            final VariableContext variableContext = buildVariableContext();
            final Collection<XacmlRequestBuilderAssertion.MultipleAttributeConfig> macs = getMultipleAttributeConfigs(assertion);

            int errorCount = 0;
            out:
            for ( XacmlRequestBuilderAssertion.MultipleAttributeConfig mac : macs ) {
                final Collection<String> xpathExpressions = getXPathExpressions(mac);
                final NamespaceContext namespaceContext = buildNamespaceContext( mac.getNamespaces() );

                for ( String xpathExpression : xpathExpressions ) {
                    try {
                        DOMXPath xpath = new DOMXPath(xpathExpression);
                        xpath.setFunctionContext(xpathFunctionContext);
                        xpath.setNamespaceContext(namespaceContext);
                        xpath.setVariableContext(variableContext);
                        xpath.evaluate(testDocument);
                    } catch ( Exception e ) {
                        errorCount++;
                        logger.log( Level.WARNING,
                                "Invalid XPath expression '"+xpathExpression+"', '" + ExceptionUtils.getMessage(e) +"'.",
                                ExceptionUtils.getDebugException(e) );
                    }

                    if ( errorCount > 1 ) {
                        break out;
                    }
                }
            }

            if ( errorCount == 1 ) {
                error = "Invalid XPath expression.";
            } else if ( errorCount > 1 ) {
                error = "Invalid XPath expressions.";
            }
        }

        return error;
    }

    private Document buildTestDocument() {
        Document testDocument = null;
        try {
            testDocument = XmlUtil.stringToDocument("<blah xmlns=\"http://bzzt.com\"/>");
        } catch (SAXException e) {
            logger.log( Level.WARNING, "Error creating Document for XPath validation", e );
        }
        return testDocument;
    }

    private VariableContext buildVariableContext() {
        return new VariableContext() {
                @Override
                public Object getVariableValue( String ns, String prefix, String localName ) throws UnresolvableException {
                    return ""; // this will always succeed, variable usage already has a validator
                }
            };
    }

    private NamespaceContext buildNamespaceContext( final Map<String, String> namespaces ) {
        return new NamespaceContext(){
            @Override
            public String translateNamespacePrefixToUri( final String prefix ) {
                if ( namespaces == null )
                    return null;
                else
                    return namespaces.get(prefix);
            }
        };
    }

    private Collection<XacmlRequestBuilderAssertion.MultipleAttributeConfig> getMultipleAttributeConfigs( final XacmlRequestBuilderAssertion assertion ) {
        List<XacmlRequestBuilderAssertion.MultipleAttributeConfig> configs = new ArrayList<XacmlRequestBuilderAssertion.MultipleAttributeConfig>();

        List<XacmlRequestBuilderAssertion.RequestChildElement> rces = new ArrayList<XacmlRequestBuilderAssertion.RequestChildElement>();
        rces.addAll( assertion.getSubjects() );
        rces.addAll( assertion.getResources() );
        rces.add( assertion.getAction() );
        rces.add( assertion.getEnvironment() );

        for ( XacmlRequestBuilderAssertion.RequestChildElement rce : rces ) {
            for ( XacmlRequestBuilderAssertion.AttributeTreeNodeTag tag : rce.getAttributes() ) {
                if ( tag instanceof XacmlRequestBuilderAssertion.MultipleAttributeConfig ) {
                    configs.add( (XacmlRequestBuilderAssertion.MultipleAttributeConfig) tag );        
                }
            }
        }

        return configs;
    }

    private Collection<String> getXPathExpressions( final XacmlRequestBuilderAssertion.MultipleAttributeConfig mac ) {
        List<String> xpathExpressions = new ArrayList<String>();

        Set<XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName> relativeFieldNames
                = mac.getRelativeXPathFieldNames();
        if ( !relativeFieldNames.isEmpty() ) {
            xpathExpressions.add( mac.getXpathBase() );
        }
        for ( XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName name : relativeFieldNames ) {
            xpathExpressions.add( mac.getField(name).getValue() );
        }

        Set<XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName> absoluteFieldNames
                = mac.getAbsoluteXPathFieldNames();
        for ( XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName name : absoluteFieldNames ) {
            xpathExpressions.add( mac.getField(name).getValue() );
        }

        return xpathExpressions;
    }
}
