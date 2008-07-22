/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;

/**
 * @author alex
 * @version $Revision$
 */
public class WssBasic extends WssCredentialSourceAssertion implements SecurityHeaderAddressable {
    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        this.recipientContext = recipientContext;
    }

    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
}
