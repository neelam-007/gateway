/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;

/**
 * @author mike
 */
@RequiresSOAP(wss=true)
public class RequestWssReplayProtection extends MessageTargetableAssertion implements SecurityHeaderAddressable {
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();

    public RequestWssReplayProtection() {
    }

    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        this.recipientContext = recipientContext;
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, "WSS Replay Protection");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_CLASSNAME, "com.l7tech.console.tree.policy.RequestWssReplayProtectionTreeNode");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "threatProtection", "xmlSecurity" });
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);

        return meta;
    }
}
