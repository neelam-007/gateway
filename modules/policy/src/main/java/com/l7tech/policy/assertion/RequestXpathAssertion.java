/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.util.SoapConstants;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;

/**
 * Data for an assertion that verifies whether a request matches a specified
 * XPath pattern.
 *
 * @author alex
 *
 * @version $Revision$
 */
@ProcessesRequest
public class RequestXpathAssertion extends SimpleXpathAssertion {
    public static final String DEFAULT_VAR_PREFIX = "requestXpath";

    public RequestXpathAssertion() {
        super();
        initDefaultXpath();
    }

    public RequestXpathAssertion( XpathExpression xpath ) {
        super();
        setXpathExpression( xpath );
    }

    private void initDefaultXpath() {
        setXpathExpression(new XpathExpression( SoapConstants.SOAP_ENVELOPE_XPATH, createDefaultNamespaceMap()));
    }

    protected String defaultVariablePrefix() {
        return DEFAULT_VAR_PREFIX;
    }

    private final static String baseName = "Evaluate Request XPath";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<RequestXpathAssertion>(){
        @Override
        public String getAssertionName( final RequestXpathAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;

            StringBuffer sb = new StringBuffer(baseName + " against ");
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
        meta.put(AssertionMetadata.DESCRIPTION, "The request must match a specified XPath pattern.");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Evaluate Request XPath Properties");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        return meta;
    }
}
