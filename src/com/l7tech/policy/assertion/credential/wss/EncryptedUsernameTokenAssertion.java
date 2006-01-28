/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;

/**
 * @author mike
 */
public class EncryptedUsernameTokenAssertion extends WssBasic implements SecurityHeaderAddressable {
    // This override looks useless, but it's here because WspWriter doesn't find inherited methods
    public XmlSecurityRecipientContext getRecipientContext() {
        return super.getRecipientContext();
    }

    // This override looks useless, but it's here because WspWriter doesn't find inherited methods
    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        super.setRecipientContext(recipientContext);
    }
}
