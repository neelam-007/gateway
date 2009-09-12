/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.annotation.ProcessesResponse;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.xpath.XpathExpression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data for an assertion that verifies whether a response matches a specified
 * XPath pattern.
 *
 * <p>Related function specifications:
 * <ul>
 *  <li><a href="http://sarek.l7tech.com/mediawiki/index.php?title=XML_Variables">XML Variables</a> (4.3)
 * </ul>
 *
 * @author alex
 */
@ProcessesResponse
public class ResponseXpathAssertion extends SimpleXpathAssertion implements UsesVariables {
    public static final String DEFAULT_VAR_PREFIX = "responseXpath";

    protected String xmlMsgSrc;

    public ResponseXpathAssertion() {
        super();
        initDefaultXpath();
    }

    public ResponseXpathAssertion( XpathExpression xpath ) {
        super();
        setXpathExpression( xpath );
    }

    private void initDefaultXpath() {
        setXpathExpression(new XpathExpression( SoapConstants.SOAP_ENVELOPE_XPATH, createDefaultNamespaceMap()));
    }

    protected String defaultVariablePrefix() {
        return DEFAULT_VAR_PREFIX;
    }

    /**
     * Returns the XML message source to operate on.
     *
     * @return <code>null</code> for default (i.e., message in policy enforcement context); otherwise name of a message type context variable
     */
    public String getXmlMsgSrc() {
        return xmlMsgSrc;
    }

    /**
     * Specifies the XML message source to operate on.
     *
     * @param src <code>null</code> for default (i.e., message in policy enforcement context); otherwise name of a message type context variable
     */
    public void setXmlMsgSrc(final String src) {
        xmlMsgSrc = src;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        List<String> used = new ArrayList<String>(Arrays.asList(super.getVariablesUsed()));
        if (xmlMsgSrc != null)
            used.add(xmlMsgSrc);
        return used.toArray(new String[used.size()]);
    }

    private final static String baseName = "Evaluate Response XPath";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<ResponseXpathAssertion>(){
        @Override
        public String getAssertionName( final ResponseXpathAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            
            StringBuffer sb = new StringBuffer(baseName);
            final String variableName = assertion.getXmlMsgSrc();
            if(variableName != null){
                sb.append(" from variable ");
                sb.append(Syntax.SYNTAX_PREFIX);
                sb.append(variableName);
                sb.append(Syntax.SYNTAX_SUFFIX);
            }
            sb.append(" against ");
            if (assertion.getXpathExpression() == null) {
                sb.append("[XPath expression not set]");
            } else {
                sb.append(assertion.getXpathExpression().getExpression());
            }
            return sb.toString();
        }
    };
    
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String []{"xml"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "The response must match a specified XPath pattern.");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Evaluate Response XPath Properties");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put( AssertionMetadata.CLIENT_ASSERTION_TARGETS, new String[]{"response"} );

        return meta;
    }

}
