/**
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xml;

import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.AssertionResourceType;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.UsesResourceInfo;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.annotation.HardwareAccelerated;
import com.l7tech.policy.assertion.annotation.RequiresXML;
import com.l7tech.policy.variable.Syntax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Contains the xml schema for which requests and/or responses need to be validated against.
 * At runtime, the element being validated is always the child of the soap body element.
 */
@RequiresXML
@HardwareAccelerated( type=HardwareAccelerated.Type.SCHEMA )
public class SchemaValidation extends MessageTargetableAssertion implements UsesResourceInfo, UsesVariables {
    public SchemaValidation() {
        this.target = null; // Backward compatibility; null implies old post-routing heuristic
    }

    /**
     * Return whether the schema validation has been configured for message/operation
     * arguments (true) or for the whole content of the soap:body (false). The arguments
     * are considered to be the child elements of the operation element, and the operation
     * node is the  first element under the soap:body.
     * This is used for example when configuring the schema validation from wsdl in cases
     * where the schema in  wsdl/types element describes only the arguments (rpc/lit) and
     * not the whole content of the soap:body.
     *
     * @return true if schema applies to arguments only, false otherwise
     */
    public boolean isApplyToArguments() {
        return applyToArguments;
    }

    /**
     * Set whether the schema validation applies to arguments o
     * @param applyToArguments set true to apply to arguments, false to apply to whole body.
     *                         The default is false.
     * @see #isApplyToArguments()
     */
    public void setApplyToArguments(boolean applyToArguments) {
        this.applyToArguments = applyToArguments;
    }

    public AssertionResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    public void setResourceInfo(AssertionResourceInfo sri) {
        this.resourceInfo = sri;
    }

    //todo: this constants should probably find a better home
    public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    public static final String TOP_SCHEMA_ELNAME = "schema";

    private boolean applyToArguments;
    private AssertionResourceInfo resourceInfo = new StaticResourceInfo();

    public String[] getVariablesUsed() {
        List<String> vars = new ArrayList<String>();
        vars.addAll(Arrays.asList(super.getVariablesUsed()));
        if (resourceInfo.getType() == AssertionResourceType.SINGLE_URL) {
            SingleUrlResourceInfo suri = (SingleUrlResourceInfo) resourceInfo;
            vars.addAll(Arrays.asList(Syntax.getReferencedNames(suri.getUrl())));
        }
        return vars.toArray(new String[vars.size()]);
    }
}
