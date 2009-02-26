package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;

/**
 *
 */
public class SecurityHeaderAddressableSupport extends Assertion implements SecurityHeaderAddressable {
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();

    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        this.recipientContext = recipientContext;
    }

    public static String getActorSuffix(Assertion maybeSecurityHeaderAddressable) {
        if (!(maybeSecurityHeaderAddressable instanceof SecurityHeaderAddressable))
            return "";
        SecurityHeaderAddressable addressable = (SecurityHeaderAddressable) maybeSecurityHeaderAddressable;
        String actor = "";
        XmlSecurityRecipientContext context = addressable.getRecipientContext();
        if (context != null)
            actor = context.localRecipient()
                    ? ""
                    : " [\'" + addressable.getRecipientContext().getActor() + "\' actor]";
        return actor;
    }

    public static boolean isLocalRecipient(Assertion maybeSecurityHeaderAddressable) {
        if (!(maybeSecurityHeaderAddressable instanceof SecurityHeaderAddressable))
            return true;
        SecurityHeaderAddressable addressable = (SecurityHeaderAddressable) maybeSecurityHeaderAddressable;
        XmlSecurityRecipientContext recipient = addressable.getRecipientContext();
        return recipient == null || recipient.localRecipient();
    }
}
