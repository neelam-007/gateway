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
}
