package com.l7tech.policy.assertion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Collection;
import java.util.Set;
import java.util.Collections;

import com.l7tech.wsdl.BindingInfo;
import com.l7tech.wsdl.MimePartInfo;
import com.l7tech.wsdl.BindingOperationInfo;
import com.l7tech.util.Functions;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
@ProcessesRequest
public class RequestSwAAssertion extends SwAAssertion {

    private static final String FEATURE_SIGNED_ATTACHMENTS = "feature:SignedAttachments";

    /**
     * Default behaviour, fail if a request contains unbound extra attachments.
     */
    public static final int UNBOUND_ATTACHMENT_POLICY_FAIL = 0; //

    /**
     * Drop any unbound extra attachments, let the request through.
     */
    public static final int UNBOUND_ATTACHMENT_POLICY_DROP = 1;

    /**
     * Allow unbound extra attachments, any that match must meet size / count rules.
     */
    public static final int UNBOUND_ATTACHMENT_POLICY_PASS = 2;

    private int unboundAttachmentPolicy;
    private Map<String, BindingInfo> bindings = new LinkedHashMap<String, BindingInfo>();     // map of binding name (String) to binding info (BindingInfo);
    private Map<String, String> namespaceMap = new LinkedHashMap<String, String>(); // map of prefix (String) to uri (String);

    public RequestSwAAssertion() {
    }

    public RequestSwAAssertion(final Map<String,BindingInfo> bindings) {
        this.bindings = bindings;
    }

    /**
     * Get the policy for handling extra unbound attachments.
     *
     * @return the code for the policy
     * @see #UNBOUND_ATTACHMENT_POLICY_FAIL
     * @see #UNBOUND_ATTACHMENT_POLICY_DROP
     * @see #UNBOUND_ATTACHMENT_POLICY_PASS
     */
    public int getUnboundAttachmentPolicy() {
        return unboundAttachmentPolicy;
    }

    /**
     * Set the policy for handling extra unbound attachments.
     *
     * @param unboundAttachmentPolicy The policy to use.
     * @see #UNBOUND_ATTACHMENT_POLICY_FAIL
     * @see #UNBOUND_ATTACHMENT_POLICY_DROP
     * @see #UNBOUND_ATTACHMENT_POLICY_PASS
     */
    public void setUnboundAttachmentPolicy(final int unboundAttachmentPolicy) {
        this.unboundAttachmentPolicy = unboundAttachmentPolicy;
    }

    /**
     * Get the binding map (binding name (String) to binding info (BindingInfo))
     *
     * @return the Bindings map.  Never null.
     * @see com.l7tech.wsdl.BindingInfo
     */
    public Map<String,BindingInfo> getBindings() {
        return bindings;
    }

    /**
     * @param bindings the new Binding info.  May not be null.
     */
    public void setBindings(final Map<String,BindingInfo> bindings) {
        if (bindings == null)
            throw new IllegalArgumentException("bindings map may not be null");
        this.bindings.putAll(bindings);
    }

    /**
     * Get the map with namespace values.
     *
     * @return The map of prefix (String) to uri (String)
     */
    public Map<String,String> getNamespaceMap() {
        return namespaceMap;
    }

    public void setNamespaceMap(final Map<String,String> namespaceMap) {
        if (namespaceMap == null)
            throw new IllegalArgumentException("Namespace map may not be null");
        this.namespaceMap.putAll(namespaceMap);
    }

    /**
     * Does any part of any operation for any binding require a signature.
     *
     * @return true if any operation requires a signature
     */
    public boolean requiresSignature() {
        boolean requireSig = false;

        outer:
        for (BindingInfo binding : bindings.values()) {
            // for each operation of the binding found in assertion
            for (BindingOperationInfo bo : binding.getBindingOperations().values()) {
                // for each part in the operation
                for (MimePartInfo part : (Collection<MimePartInfo>) bo.getMultipart().values()) {
                    if ( part.isRequireSignature() ) {
                        requireSig = true;
                        break outer;
                    }
                }
            }
        }

        return requireSig;
    }

    @Override
    public Object clone() {
        RequestSwAAssertion clone = (RequestSwAAssertion) super.clone();
        clone.bindings = (Map) ((LinkedHashMap)bindings).clone();

        for(Iterator iterator = clone.bindings.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            entry.setValue(((BindingInfo)entry.getValue()).clone());
        }

        clone.namespaceMap = (Map) ((LinkedHashMap)namespaceMap).clone();

        return clone;
    }

    private final static String baseName = "Validate SOAP Attachments";
    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<RequestSwAAssertion>(){
        @Override
        public String getAssertionName( final RequestSwAAssertion assertion, final boolean decorate) {
            return baseName;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});
        
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "The request must fulfill the specified attachment requirements.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");

        //not needed right now, but in place in case this String needs to be decorated in the future
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "SOAP Attachment Properties");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.RequestSwAAssertionPropertiesAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Properties16.gif");
        
        meta.put(AssertionMetadata.FEATURE_SET_FACTORY, new Functions.Unary<Set<String>,Assertion>(){
            @Override
            public Set<String> call(final Assertion assertion) {
                Set features = Collections.emptySet();

                if ( ((RequestSwAAssertion)assertion).requiresSignature() ) {
                    features = Collections.singleton(FEATURE_SIGNED_ATTACHMENTS);
                }

                return features;
            }
        });

        return meta;
    }
}
