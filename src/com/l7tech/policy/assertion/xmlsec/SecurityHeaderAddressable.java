package com.l7tech.policy.assertion.xmlsec;

/**
 * Something to which can be attached a SOAP security header actor and recipient
 * recipient {@see XmlSecurityRecipientContext}.
 *
 * @author flascelles@layer7-tech.com
 */
public interface SecurityHeaderAddressable {
    XmlSecurityRecipientContext getRecipientContext();
    void setRecipientContext(XmlSecurityRecipientContext recipientContext);
}
