package com.l7tech.common.security.token;

import com.l7tech.common.mime.PartInfo;

/**
 * Encapsulates a SOAP attachment and signing token.
 */
public interface SignedPart {

    /**
     * Get the signing token.
     *
     * @return The token.
     */
    SigningSecurityToken getSigningSecurityToken();

    /**
     * Get the part that was signed.
     *
     * @return The part
     */
    PartInfo getPartInfo();
}
