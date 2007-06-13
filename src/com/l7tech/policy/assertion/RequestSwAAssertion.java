package com.l7tech.policy.assertion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;

import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
@ProcessesRequest
public class RequestSwAAssertion extends SwAAssertion {

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
    private Map bindings = new LinkedHashMap();     // map of binding name (String) to binding info (BindingInfo)
    private Map namespaceMap = new LinkedHashMap(); // map of prefix (String) to uri (String)

    public RequestSwAAssertion() {
    }

    public RequestSwAAssertion(Map bindings) {
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
    public void setUnboundAttachmentPolicy(int unboundAttachmentPolicy) {
        this.unboundAttachmentPolicy = unboundAttachmentPolicy;
    }

    /**
     * Get the binding map (binding name (String) to binding info (BindingInfo))
     *
     * @return the Bindings map.  Never null.
     * @see com.l7tech.common.wsdl.BindingInfo
     */
    public Map getBindings() {
        return bindings;
    }

    /**
     * @param bindings the new Binding info.  May not be null.
     */
    public void setBindings(Map bindings) {
        if (bindings == null)
            throw new IllegalArgumentException("bindings map may not be null");
        this.bindings.putAll(bindings);
    }

    /**
     * Get the map with namespace values.
     *
     * @return The map of prefix (String) to uri (String)
     */
    public Map getNamespaceMap() {
        return namespaceMap;
    }

    public void setNamespaceMap(Map namespaceMap) {
        if (namespaceMap == null)
            throw new IllegalArgumentException("Namespace map may not be null");
        this.namespaceMap.putAll(namespaceMap);
    }

    public Object clone() {
        RequestSwAAssertion clone = (RequestSwAAssertion) super.clone();
        clone.bindings = (Map) ((LinkedHashMap)bindings).clone();

        for(Iterator iterator = clone.bindings.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            entry.setValue(((BindingInfo)entry.getValue()).clone());
        }

        return clone;
    }
}
