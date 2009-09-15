/**
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xml;

import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.HardwareAccelerated;
import com.l7tech.policy.assertion.annotation.RequiresXML;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

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
        clearTarget(); // Backward compatibility; null implies old post-routing heuristic
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

    @Migration(mapName = MigrationMappingSelection.REQUIRED, mapValue = MigrationMappingSelection.NONE, resolver = PropertyResolver.Type.SCHEMA_ENTRY)
    @Override
    public AssertionResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    @Override
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

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        List<String> vars = new ArrayList<String>();
        vars.addAll(Arrays.asList(super.getVariablesUsed()));
        if (resourceInfo.getType() == AssertionResourceType.SINGLE_URL) {
            SingleUrlResourceInfo suri = (SingleUrlResourceInfo) resourceInfo;
            vars.addAll(Arrays.asList(Syntax.getReferencedNames(suri.getUrl())));
        }
        return vars.toArray(new String[vars.size()]);
    }

    private final static String baseName = "Validate XML Schema";
    
    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<SchemaValidation>(){
        @Override
        public String getAssertionName( final SchemaValidation assertion, final boolean decorate) {
            if(!decorate) return baseName;

            return AssertionUtils.decorateName(assertion, baseName);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml", "threatProtection"});

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "<html>Schema Validation can be used to protect against the following threats: " +
                "<ul> " +
                "<li><b>XML Parameter Tampering</b> - Validates all XML parameters in the request to ensure conformance to the Schema</li> " +
                "<li><b>XDoS Attacks</b> - Ensures that the message structure and content are correct</li> " +
                "</ul> " +
                "</html>");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        
        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.SchemaValidationPropertiesAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "XML Schema Validation Properties");

        return meta;
    }
}
