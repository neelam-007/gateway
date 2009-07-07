package com.l7tech.external.assertions.xacmlpdp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import static com.l7tech.policy.assertion.AssertionMetadata.SHORT_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.LONG_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_NODE_ICON;
import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_FOLDERS;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_ADVICE_CLASSNAME;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_EXTERNAL_NAME;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 13-Mar-2009
 * Time: 5:21:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class XacmlPdpAssertion extends Assertion implements SetsVariables {

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
            
            return allTypes.toArray(new String[]{});
        }

        private final String encapType;
    }

    private XacmlAssertionEnums.MessageLocation inputMessageSource = XacmlAssertionEnums.MessageLocation.DEFAULT_REQUEST;
    private String inputMessageVariableName;
    private XacmlAssertionEnums.MessageLocation outputMessageLocation = XacmlAssertionEnums.MessageLocation.DEFAULT_RESPONSE;
    private String outputMessageVariableName;
    private SoapEncapsulationType soapEncapsulation = SoapEncapsulationType.NONE;
    private AssertionResourceInfo resourceInfo = new StaticResourceInfo();
    private boolean failIfNotPermit = false;

    public XacmlPdpAssertion() {
    }

    public VariableMetadata[] getVariablesSet() {
        if(outputMessageLocation == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
            return new VariableMetadata[] {new VariableMetadata(outputMessageVariableName, false, false, null, false)};
        } else {
            return new VariableMetadata[0];
        }
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

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(SHORT_NAME, "XACML PDP Assertion");
        meta.put(LONG_NAME, "Evaluate a XACML access request");

        //meta.put(PALETTE_NODE_NAME, "CentraSite Metrics Assertion");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "xmlSecurity" });

        meta.put(POLICY_NODE_NAME, "XACML PDP Assertion");
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xacmlpdp.console.XacmlPdpPropertiesDialog");
        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, "com.l7tech.external.assertions.xacmlpdp.server.ServerXacmlPdpAssertion");

        meta.put(WSP_EXTERNAL_NAME, "XacmlPdpAssertion"); // keep same WSP name as pre-3.7 (Bug #3605)

        Collection<TypeMapping> othermappings = new ArrayList<TypeMapping>();
        othermappings.add(new Java5EnumTypeMapping(XacmlAssertionEnums.MessageLocation.class, "messageLocation"));
        othermappings.add(new Java5EnumTypeMapping(SoapEncapsulationType.class, "soapEncapsulation"));
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:EchoRouting" rather than "set:modularAssertions"
        //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(XacmlPdpAssertion.class.getName() + ".metadataInitialized", Boolean.TRUE);
        return meta;
    }
}
