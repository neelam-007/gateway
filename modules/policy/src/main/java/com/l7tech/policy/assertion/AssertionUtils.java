package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;

/**
 * Assertion utility methods
 */
public class AssertionUtils {

    public static String decorateName( final Assertion assertion, final String name ) {
        return decorateName( assertion, new StringBuilder(name) );
    }

    public static String decorateName( final Assertion assertion, final StringBuilder name ) {
        StringBuilder newName = new StringBuilder(name);

        if (assertion instanceof MessageTargetable) {
            newName.insert(0, ((MessageTargetable)assertion).getTargetName() + ": ");
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

        return newName.toString();
    }

}
