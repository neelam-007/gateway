package com.l7tech.external.assertions.xacmlpdp;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;

import java.util.*;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * @author njordan
 */
public class XacmlPdpAssertion extends Assertion implements UsesVariables, SetsVariables {
    public static final String CPROP_XACML_POLICY_CACHE_MAX_ENTRIES = "xacml.pdp.policyCache.maxEntries";
    public static final String CPROP_XACML_POLICY_CACHE_MAX_AGE = "xacml.pdp.policyCache.maxAge";
    public static final String CPROP_XACML_POLICY_CACHE_MAX_STALE_AGE = "xacml.pdp.policyCache.maxStaleAge";
    public static final String PARAM_XACML_POLICY_CACHE_MAX_ENTRIES = ClusterProperty.asServerConfigPropertyName(CPROP_XACML_POLICY_CACHE_MAX_ENTRIES);
    public static final String PARAM_XACML_POLICY_CACHE_MAX_AGE = ClusterProperty.asServerConfigPropertyName(CPROP_XACML_POLICY_CACHE_MAX_AGE);
    public static final String PARAM_XACML_POLICY_CACHE_MAX_STALE_AGE = ClusterProperty.asServerConfigPropertyName(CPROP_XACML_POLICY_CACHE_MAX_STALE_AGE);
    public static final String XACML_PDP_MAX_DOWNLOAD_SIZE = "xacml.pdp.maxDownloadSize";

    public enum SoapEncapsulationType {
        NONE("None"),
        REQUEST("Request"),
        RESPONSE("Response"),
        REQUEST_AND_RESPONSE("Request and Response");

        private SoapEncapsulationType(String encapType){
            this.encapType = encapType;
        }

        public String getEncapType() {
            return encapType;
        }

        @Override
        public String toString(){
            return encapType;
        }

        public static SoapEncapsulationType findEncapsulationType(String type){
            for(SoapEncapsulationType aType: values()){
                if(aType.toString().equals(type)) return aType;
            }
            throw new IllegalArgumentException("Unknown type requested: '"+type+"'");
        }

        public static String [] allTypesAsStrings(){
            List<String> allTypes = new ArrayList<String>();
            for(SoapEncapsulationType aType: values()){
                allTypes.add(aType.toString());
            }
            
            return allTypes.toArray(new String[allTypes.size()]);
        }

        private final String encapType;
    }

    private static final String META_INITIALIZED = XacmlPdpAssertion.class.getName() + ".metadataInitialized";
    private XacmlAssertionEnums.MessageLocation inputMessageSource = XacmlAssertionEnums.MessageLocation.DEFAULT_REQUEST;
    private String inputMessageVariableName;
    private XacmlAssertionEnums.MessageLocation outputMessageLocation = XacmlAssertionEnums.MessageLocation.DEFAULT_RESPONSE;
    private String outputMessageVariableName;
    private SoapEncapsulationType soapEncapsulation = SoapEncapsulationType.NONE;
    private AssertionResourceInfo resourceInfo = new StaticResourceInfo();
    private boolean failIfNotPermit = false;

    public XacmlPdpAssertion() {
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if(outputMessageLocation == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
            return new VariableMetadata[] {new VariableMetadata(outputMessageVariableName, true, false, null, true, DataType.MESSAGE)};
        } else {
            return new VariableMetadata[0];
        }
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        List<String> variables = new ArrayList<String>();
        
        if (resourceInfo instanceof StaticResourceInfo) {
            StaticResourceInfo sri = (StaticResourceInfo) resourceInfo;
            String doc = sri.getDocument();
            if (doc != null)
                variables.addAll( Arrays.asList( Syntax.getReferencedNames(doc) ));
        } else if (resourceInfo instanceof SingleUrlResourceInfo ) {
            final SingleUrlResourceInfo singleUrlResourceInfo = (SingleUrlResourceInfo) resourceInfo;
            final String url = singleUrlResourceInfo.getUrl();
            if ( url != null ) {
                variables.addAll( Arrays.asList( Syntax.getReferencedNames(url) ));
            }
        }

        if ( inputMessageSource == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE &&
             inputMessageVariableName != null ) {
            variables.add( inputMessageVariableName );
        }

        return variables.toArray( new String[variables.size()] );
    }

    public XacmlAssertionEnums.MessageLocation getInputMessageSource() {
        return inputMessageSource;
    }

    public void setInputMessageSource(XacmlAssertionEnums.MessageLocation inputMessageSource) {
        this.inputMessageSource = inputMessageSource;
    }

    public String getInputMessageVariableName() {
        return inputMessageVariableName;
    }

    public void setInputMessageVariableName(String inputMessageVariableName) {
        this.inputMessageVariableName = inputMessageVariableName;
    }

    public XacmlAssertionEnums.MessageLocation getOutputMessageTarget() {
        return outputMessageLocation;
    }

    public void setOutputMessageTarget(XacmlAssertionEnums.MessageLocation outputMessageLocation) {
        this.outputMessageLocation = outputMessageLocation;
    }

    public String getOutputMessageVariableName() {
        return outputMessageVariableName;
    }

    public void setOutputMessageVariableName(String outputMessageVariableName) {
        this.outputMessageVariableName = outputMessageVariableName;
    }

    public SoapEncapsulationType getSoapEncapsulation() {
        return soapEncapsulation;
    }

    public void setSoapEncapsulation(SoapEncapsulationType soapEncapsulation) {
        this.soapEncapsulation = soapEncapsulation;
    }

    public AssertionResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    public void setResourceInfo(AssertionResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    public boolean getFailIfNotPermit() {
        return failIfNotPermit;
    }

    public void setFailIfNotPermit(boolean failIfNotPermit) {
        this.failIfNotPermit = failIfNotPermit;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, "Evaluate XACML Policy");
        meta.put(DESCRIPTION, "Evaluate a XACML policy and render an authorization decision for a resource.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "xmlSecurity" });

        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        Map<String, String[]> props = new HashMap<String, String[]>();
        props.put(CPROP_XACML_POLICY_CACHE_MAX_ENTRIES, new String[] {
                "Maximum number of cached policies loaded from URLs, 0 for no caching (Integer). Requires gateway restart.",
                "100"
        });
        props.put(CPROP_XACML_POLICY_CACHE_MAX_AGE, new String[] {
                "Maximum age to cache a downloaded policy before checking to see if it has been updated (Milliseconds). Requires gateway restart.",
                "300000"
        });
        props.put(CPROP_XACML_POLICY_CACHE_MAX_STALE_AGE, new String[] {
                "Maximum age of stale (expired) cached policies loaded from URLs, -1 for no expiry (Milliseconds). Requires gateway restart.",
                "-1"
        });
        props.put(XACML_PDP_MAX_DOWNLOAD_SIZE, new String[]{
                "Maximum size in bytes of a XACML policy document download, or 0 for unlimited (Integer).",
                "${documentDownload.maxSize}" } );

        meta.put(CLUSTER_PROPERTIES, props);

        meta.put(PROPERTIES_ACTION_NAME, "XACML Policy Properties");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xacmlpdp.console.XacmlPdpPropertiesDialog");

        meta.put(FEATURE_SET_NAME, "(fromClass)");
        meta.put(WSP_EXTERNAL_NAME, "XacmlPdpAssertion");

        Collection<TypeMapping> othermappings = new ArrayList<TypeMapping>();
        othermappings.add(new Java5EnumTypeMapping(XacmlAssertionEnums.MessageLocation.class, "messageLocation"));
        othermappings.add(new Java5EnumTypeMapping(SoapEncapsulationType.class, "soapEncapsulation"));
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:EchoRouting" rather than "set:modularAssertions"
        //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
