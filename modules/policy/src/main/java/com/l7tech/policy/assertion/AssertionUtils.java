package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;

/**
 * Assertion utility methods
 */
public class AssertionUtils {

    /**
     * Add decoration to the given assertion name.
     *
     * <p>This will add any prefix/suffix for known assertion aspects.</p>
     *
     * @param assertion The assertion
     * @param name The name
     * @return The decorated name
     */
    public static String decorateName( final Assertion assertion, final String name ) {
        return decorateName( assertion, new StringBuilder(name) );
    }

    /**
     * Add decoration to the given assertion name.
     *
     * <p>This will add any prefix/suffix for known assertion aspects.</p>
     *
     * @param assertion The assertion
     * @param name The name
     * @return The decorated name
     */
    public static String decorateName( final Assertion assertion, final StringBuilder name ) {
        StringBuilder newName = new StringBuilder(name);

        if (assertion instanceof MessageTargetable) {
            String targetName = ((MessageTargetable)assertion).getTargetName();
            if ( targetName != null ) {
                newName.insert(0, targetName + ": ");
            }
        }

        if (assertion instanceof SecurityHeaderAddressable)
            newName.append(SecurityHeaderAddressableSupport.getActorSuffix(assertion));

        if (assertion instanceof PrivateKeyable) {
            if ( !((PrivateKeyable)assertion).isUsesDefaultKeyStore() ) {
                newName.append(" (Key: ").append(((PrivateKeyable)assertion).getKeyAlias()).append(")");                
            }
        }

        if (assertion instanceof IdentityTagable) {
            String idTag = ((IdentityTagable)assertion).getIdentityTag();
            if ( idTag != null && !idTag.isEmpty() ) {
                newName.append(" as \"").append(idTag).append("\"");
            }
        }

        if (assertion instanceof IdentityTargetable) {
            IdentityTarget identityTarget = ((IdentityTargetable)assertion).getIdentityTarget();
            if ( identityTarget != null && identityTarget.getTargetIdentityType() != null ) {
                newName.append(" [");
                newName.append(identityTarget.describeIdentityForDisplay());
                newName.append("]");
            }
        }

        return newName.toString();
    }

    /**
     * Test if the given assertions target the same message.
     *
     * @param a1 The first assertion
     * @param a2 The second assertion
     * @return True if the assertions target the same message.
     */
    public static boolean isSameTargetMessage( final Assertion a1, final Assertion a2 ) {
        boolean sameTarget = false;

        if ( Assertion.isRequest(a1) && Assertion.isRequest(a2) ) {
            sameTarget = true;
        } else if ( Assertion.isResponse(a1) && Assertion.isResponse(a2) ) {
            sameTarget = true;
        } else if ( a1 instanceof MessageTargetable && a2 instanceof MessageTargetable ) {
            MessageTargetable mt1 = (MessageTargetable) a1;
            MessageTargetable mt2 = (MessageTargetable) a2;
            sameTarget = mt1.getTarget()==mt2.getTarget() &&
                    (mt1.getTarget()!=TargetMessageType.OTHER ||
                     (mt1.getOtherTargetMessageVariable()!=null &&
                      mt1.getOtherTargetMessageVariable().equalsIgnoreCase(mt2.getOtherTargetMessageVariable())));
        }

        return sameTarget;
    }

    /**
     * Test if the given assertion matches the given message target.
     *
     * @param assertion The assertion
     * @param messageTargetable The message target
     * @return True if the assertion matches the target.
     */
    public static boolean isSameTargetMessage( final Assertion assertion, final MessageTargetable messageTargetable ) {
        boolean sameTarget = false;

        if ( Assertion.isRequest(assertion) && messageTargetable.getTarget()==TargetMessageType.REQUEST ) {
            sameTarget = true;
        } else if ( Assertion.isResponse(assertion) && messageTargetable.getTarget()==TargetMessageType.RESPONSE ) {
            sameTarget = true;
        } else if ( assertion instanceof MessageTargetable ) {
            MessageTargetable mt1 = (MessageTargetable) assertion;
            sameTarget = mt1.getTarget()==messageTargetable.getTarget() &&
                    (mt1.getTarget()!=TargetMessageType.OTHER ||
                     (mt1.getOtherTargetMessageVariable()!=null &&
                      mt1.getOtherTargetMessageVariable().equalsIgnoreCase(messageTargetable.getOtherTargetMessageVariable())));
        }

        return sameTarget;
    }

    /**
     * Test if the given assertions target the same actor.
     *
     * @param a1 The first assertion
     * @param a2 The second assertion
     * @return True if the assertions target the same recipient.
     */
    public static boolean isSameTargetRecipient( final Assertion a1, final Assertion a2 ) {
        String actor1 = getActor(a1);
        String actor2 = getActor(a2);
        return a1!=null && a2!=null && actor1.equals(actor2);
    }

    /**
     * Get a target message name for the given assertion.
     *
     * @param assertion The assertion to process
     * @return The (possibly guessed) target name.
     */
    public static String getTargetName( final Assertion assertion ) {
        String targetName = null;

        if ( assertion instanceof MessageTargetable ) {
            targetName = ((MessageTargetable)assertion).getTargetName();
        } else if ( Assertion.isResponse(assertion) ) {
            targetName = "Response";
        }

        if ( targetName == null ) {
            targetName = "Request";            
        }

        return targetName;
    }

    private static String getActor( final Assertion assertion ) {
        String actor = "";

        if ( assertion instanceof SecurityHeaderAddressable ) {
            SecurityHeaderAddressable securityHeaderAddressable = (SecurityHeaderAddressable) assertion;
            XmlSecurityRecipientContext recipientContext = securityHeaderAddressable.getRecipientContext();
            if ( recipientContext != null && recipientContext.getActor() != null ) {
                actor = recipientContext.getActor();
            }
        }

        return actor;
    }
}
