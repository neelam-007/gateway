package com.l7tech.policy.assertion.xmlsec;

/**
 * Something to which can be attached a SOAP security header actor and recipient
 * recipient {@link XmlSecurityRecipientContext}.
 *
 * @author flascelles@layer7-tech.com
 */
public interface SecurityHeaderAddressable {
    /** @return the recipient context for this assertion.  Never null. */
    XmlSecurityRecipientContext getRecipientContext();

    /** @param recipientContext the recipient context, or (for backward compatibility) null to request the default recipient. */
    void setRecipientContext(XmlSecurityRecipientContext recipientContext);
}
